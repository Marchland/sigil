package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.IntrospectionResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration

class SigilClientTest {
    private val baseUrl = "https://sigil.test"
    private val objectMapper = jacksonObjectMapper()

    private fun client(configure: RestClient.Builder.() -> Unit = {}): Pair<SigilClient, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl(baseUrl)
        builder.configure()
        val server = MockRestServiceServer.bindTo(builder).build()
        val properties = SigilClientProperties(baseUrl = baseUrl, introspectionCacheTtl = Duration.ofSeconds(30))
        return SigilClient(properties, objectMapper, builder.build()) to server
    }

    @Test
    fun `introspection sends the token as both bearer and body and parses the response`() {
        val (client, server) = client()
        server
            .expect(requestTo("$baseUrl/indieauth/introspect"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer abc"))
            .andExpect(content().formData(LinkedMultiValueMap<String, String>().apply { add("token", "abc") }))
            .andRespond(
                withSuccess(
                    """{"active":true,"me":"https://sigil.test","client_id":"https://c.example","scope":"create"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val response = client.introspect("abc")

        assertTrue(response.active)
        assertEquals("https://sigil.test", response.me)
        assertEquals("create", response.scope)
        server.verify()
    }

    @Test
    fun `active introspections are cached and not re-requested`() {
        val (client, server) = client()
        server
            .expect(requestTo("$baseUrl/indieauth/introspect"))
            .andRespond(withSuccess("""{"active":true,"me":"https://sigil.test"}""", MediaType.APPLICATION_JSON))

        client.introspect("abc")
        client.introspect("abc")

        // Only one request registered and verified; the second hit the cache.
        server.verify()
    }

    @Test
    fun `inactive introspections are not cached`() {
        val (client, server) = client()
        server
            .expect(requestTo("$baseUrl/indieauth/introspect"))
            .andRespond(withSuccess("""{"active":false}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("$baseUrl/indieauth/introspect"))
            .andRespond(withSuccess("""{"active":false}""", MediaType.APPLICATION_JSON))

        assertFalse(client.introspect("abc").active)
        assertFalse(client.introspect("abc").active)

        server.verify()
    }

    @Test
    fun `protocol errors surface as SigilClientException with the typed error`() {
        val (client, server) = client()
        server
            .expect(requestTo("$baseUrl/indieauth/introspect"))
            .andRespond(
                withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"error":"invalid_request","error_description":"nope"}"""),
            )

        val exception = assertThrows(SigilClientException::class.java) { client.introspect("abc") }

        assertEquals("invalid_request", exception.error?.error)
        assertEquals("nope", exception.error?.errorDescription)
        server.verify()
    }
}
