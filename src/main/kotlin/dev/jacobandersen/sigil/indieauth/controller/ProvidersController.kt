package dev.jacobandersen.sigil.indieauth.controller

import dev.jacobandersen.sigil.indieauth.IndieAuthEndpoints
import dev.jacobandersen.sigil.indieauth.config.IndieAuthConfig
import dev.jacobandersen.sigil.indieauth.identity.GitHubIdentityProvider
import dev.jacobandersen.sigil.indieauth.type.ProviderInfo
import dev.jacobandersen.sigil.indieauth.type.ProvidersResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Exposes the set of identity providers Herald can offer on the auth page.
 *
 * This is an unauthenticated, public endpoint - `client_id`, `authorize_url`
 * and `redirect_uri` are not secrets (they appear in the browser redirect to
 * the provider) and letting Herald fetch them avoids hard-coding provider
 * configuration in the frontend.
 */
@RestController
class ProvidersController(
    private val config: IndieAuthConfig,
) {
    @GetMapping(IndieAuthEndpoints.PROVIDERS)
    fun providers(): ProvidersResponse {
        val providers = mutableListOf<ProviderInfo>()

        val github = config.github
        if (github.clientId.isNotBlank()) {
            providers.add(
                ProviderInfo(
                    provider = GitHubIdentityProvider.PROVIDER,
                    clientId = github.clientId,
                    authorizeUrl = github.authorizeUrl,
                    redirectUri = github.redirectUri.takeIf { it.isNotBlank() },
                ),
            )
        }

        return ProvidersResponse(providers = providers)
    }
}
