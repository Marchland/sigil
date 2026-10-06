package dev.jacobandersen.sigil.client

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.jacobandersen.sigil.protocol.AuthorizationServerMetadata
import dev.jacobandersen.sigil.protocol.IndieAuthEndpoints
import dev.jacobandersen.sigil.protocol.IndieAuthError
import dev.jacobandersen.sigil.protocol.IntrospectionResponse
import dev.jacobandersen.sigil.protocol.ProfileUrlResponse
import dev.jacobandersen.sigil.protocol.ProviderInfo
import dev.jacobandersen.sigil.protocol.ProvidersResponse
import dev.jacobandersen.sigil.protocol.TokenResponse
import dev.jacobandersen.sigil.protocol.UserProfile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.ObjectMapper

/**
 * The Sigil client facade. Implements every narrow client interface so a caller
 * can inject [SigilClient] itself or only the capability it needs
 * ([TokenIntrospector], [AuthorizationClient], ...).
 *
 * Construct it directly, or let the Spring Boot auto-configuration build one
 * from [SigilClientProperties].
 */
class SigilClient(
    private val properties: SigilClientProperties,
    private val objectMapper: ObjectMapper,
    private val restClient: RestClient = defaultRestClient(properties.baseUrl, properties.connectTimeout, properties.readTimeout),
) : DiscoveryClient,
    AuthorizationClient,
    TokenIntrospector,
    TokenRevoker,
    UserinfoClient,
    ProviderCatalog {
    private val baseUrl: String = properties.baseUrl.trimEnd('/')

    private val introspectionCache: Cache<String, IntrospectionResponse> =
        Caffeine
            .newBuilder()
            .maximumSize(properties.introspectionCacheMaxSize)
            .expireAfterWrite(properties.introspectionCacheTtl)
            .build()

    // ---------------------------------------------------------------- discovery

    override fun fetchMetadata(profileUrl: String): AuthorizationServerMetadata {
        val metadataUrl = discoverMetadataUrl(profileUrl)
        return fetchMetadataAt(metadataUrl)
    }

    override fun fetchMetadataAt(metadataUrl: String): AuthorizationServerMetadata =
        execute {
            restClient
                .get()
                .uri(metadataUrl)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(AuthorizationServerMetadata::class.java)
        } ?: throw SigilClientException("Sigil returned an empty metadata document for $metadataUrl")

    /**
     * Fetches [profileUrl] and resolves the `indieauth-metadata` link relation
     * from the HTTP `Link` header (preferred) or the first HTML `<link>` element
     * with that rel, per IndieAuth 4.1.
     */
    private fun discoverMetadataUrl(profileUrl: String): String {
        val response =
            execute {
                restClient
                    .get()
                    .uri(profileUrl)
                    .header(HttpHeaders.ACCEPT, "text/html, application/xhtml+xml")
                    .retrieve()
                    .toEntity(String::class.java)
            } ?: throw SigilClientException("Could not fetch profile URL $profileUrl")

        linkHeaderMetadata(response.headers.getFirst(HttpHeaders.LINK))?.let { return resolve(profileUrl, it) }
        htmlMetadata(response.body)?.let { return resolve(profileUrl, it) }

        throw SigilClientException("No 'indieauth-metadata' link relation found at $profileUrl")
    }

    private fun linkHeaderMetadata(header: String?): String? {
        if (header.isNullOrBlank()) return null
        var index = 0
        while (index < header.length) {
            val open = header.indexOf('<', index)
            if (open == -1) break
            val close = header.indexOf('>', open)
            if (close == -1) break
            val target = header.substring(open + 1, close)
            val end = header.indexOf(',', close).let { if (it == -1) header.length else it }
            val params = header.substring(close + 1, end)
            if (params.split(';').any {
                    it.trim().startsWith("rel", ignoreCase = true) &&
                        it.contains("indieauth-metadata", ignoreCase = true)
                }
            ) {
                return target
            }
            index = end + 1
        }
        return null
    }

    private fun htmlMetadata(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match =
            Regex(
                "<link\\b[^>]*\\brel=[\"']([^\"']*)[\"'][^>]*>",
                RegexOption.IGNORE_CASE,
            ).findAll(body).firstOrNull { link ->
                val rel = link.groupValues[1].split(Regex("\\s+"))
                rel.any { it.equals("indieauth-metadata", ignoreCase = true) }
            } ?: return null
        return Regex("\\bhref=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(match.value)
            ?.groupValues
            ?.get(1)
    }

    private fun resolve(
        base: String,
        candidate: String,
    ): String =
        runCatching {
            java.net
                .URI(base)
                .resolve(candidate.trim())
                .toString()
        }.getOrDefault(candidate)

    // ------------------------------------------------------------ authorization

    override fun buildAuthorizationUrl(
        authorizationEndpoint: String,
        clientId: String,
        redirectUri: String,
        state: String,
        scope: String?,
        me: String?,
        codeChallenge: String?,
        codeChallengeMethod: String?,
    ): String {
        val builder =
            UriComponentsBuilder
                .fromUriString(authorizationEndpoint)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
        if (!scope.isNullOrBlank()) builder.queryParam("scope", scope)
        if (!me.isNullOrBlank()) builder.queryParam("me", me)
        if (!codeChallenge.isNullOrBlank()) {
            builder.queryParam("code_challenge", codeChallenge)
            builder.queryParam("code_challenge_method", codeChallengeMethod ?: "S256")
        }
        return builder.build().encode().toUriString()
    }

    override fun exchangeCode(
        tokenEndpoint: String,
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String?,
    ): TokenResponse {
        val body =
            formBody(
                "grant_type" to "authorization_code",
                "code" to code,
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "code_verifier" to codeVerifier,
            )
        return postForm(tokenEndpoint, body, TokenResponse::class.java)
            ?: throw SigilClientException("Sigil returned an empty token response")
    }

    override fun redeemProfileUrl(
        authorizationEndpoint: String,
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String?,
    ): ProfileUrlResponse {
        val body =
            formBody(
                "grant_type" to "authorization_code",
                "code" to code,
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "code_verifier" to codeVerifier,
            )
        return postForm(authorizationEndpoint, body, ProfileUrlResponse::class.java)
            ?: throw SigilClientException("Sigil returned an empty profile URL response")
    }

    override fun refresh(
        tokenEndpoint: String,
        refreshToken: String,
        clientId: String,
        scope: String?,
    ): TokenResponse {
        val body =
            formBody(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to clientId,
                "scope" to scope,
            )
        return postForm(tokenEndpoint, body, TokenResponse::class.java)
            ?: throw SigilClientException("Sigil returned an empty token response")
    }

    // ------------------------------------------------------------ introspection

    override fun introspect(token: String): IntrospectionResponse {
        introspectionCache.getIfPresent(token)?.let { return it }

        val body = formBody("token" to token)
        val response =
            execute {
                restClient
                    .post()
                    .uri("$baseUrl${IndieAuthEndpoints.INTROSPECTION}")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .body(IntrospectionResponse::class.java)
            } ?: return IntrospectionResponse(active = false)

        if (response.active) {
            introspectionCache.put(token, response)
        }
        return response
    }

    // --------------------------------------------------------------- revocation

    override fun revoke(
        token: String,
        tokenTypeHint: String?,
    ) {
        val body = formBody("token" to token, "token_type_hint" to tokenTypeHint)
        execute {
            restClient
                .post()
                .uri("$baseUrl${IndieAuthEndpoints.REVOCATION}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        }
        introspectionCache.invalidate(token)
    }

    // ----------------------------------------------------------------- userinfo

    override fun userinfo(accessToken: String): UserProfile =
        execute {
            restClient
                .get()
                .uri("$baseUrl${IndieAuthEndpoints.USERINFO}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(UserProfile::class.java)
        } ?: UserProfile()

    // ---------------------------------------------------------------- providers

    override fun providers(): List<ProviderInfo> =
        execute {
            restClient
                .get()
                .uri("$baseUrl${IndieAuthEndpoints.PROVIDERS}")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(ProvidersResponse::class.java)
        }?.providers ?: emptyList()

    // ------------------------------------------------------------------ helpers

    private fun <T : Any> postForm(
        url: String,
        body: LinkedMultiValueMap<String, String>,
        type: Class<T>,
    ): T? =
        execute {
            restClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(type)
        }

    private fun formBody(vararg pairs: Pair<String, String?>): LinkedMultiValueMap<String, String> {
        val body = LinkedMultiValueMap<String, String>()
        pairs.forEach { (key, value) -> if (!value.isNullOrBlank()) body.add(key, value) }
        return body
    }

    /**
     * Runs [block], translating Sigil protocol errors into [SigilClientException]
     * with the parsed [IndieAuthError].
     */
    private fun <T> execute(block: () -> T): T =
        try {
            block()
        } catch (e: RestClientResponseException) {
            throw SigilClientException(
                "Sigil request failed (HTTP ${e.statusCode.value()})",
                parseError(e.responseBodyAsString),
                e,
            )
        } catch (e: SigilClientException) {
            throw e
        } catch (e: Exception) {
            throw SigilClientException("Sigil request failed: ${e.message}", cause = e)
        }

    private fun parseError(body: String?): IndieAuthError? {
        if (body.isNullOrBlank()) return null
        return runCatching { objectMapper.readValue(body, IndieAuthError::class.java) }.getOrNull()
    }

    companion object {
        private fun defaultRestClient(
            baseUrl: String,
            connectTimeout: java.time.Duration,
            readTimeout: java.time.Duration,
        ): RestClient =
            RestClient
                .builder()
                .baseUrl(baseUrl.trimEnd('/'))
                .requestFactory(
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(connectTimeout)
                        setReadTimeout(readTimeout)
                    },
                ).build()
    }
}
