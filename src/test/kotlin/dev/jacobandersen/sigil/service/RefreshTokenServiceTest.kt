package dev.jacobandersen.sigil.service

import dev.jacobandersen.sigil.config.IndieAuthConfig
import dev.jacobandersen.sigil.data.entity.RefreshTokenEntity
import dev.jacobandersen.sigil.data.repository.RefreshTokenRepository
import dev.jacobandersen.sigil.security.Tokens
import dev.jacobandersen.sigil.type.IndieAuthError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.time.Instant

class RefreshTokenServiceTest {
    private val config = IndieAuthConfig(me = "https://sigil.test")
    private val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val accessTokenService = mock(AccessTokenService::class.java)
    private val service = RefreshTokenService(config, refreshTokenRepository, accessTokenService)

    private val clientId = "https://client.example"

    private fun stored(
        scope: String = "create update",
        expiresAt: Instant = Instant.now().plusSeconds(3600),
    ) = RefreshTokenEntity(
        tokenHash = Tokens.sha256("raw-refresh"),
        me = "https://sigil.test",
        clientId = clientId,
        scope = scope,
        issuedAt = Instant.now(),
        expiresAt = expiresAt,
    )

    @Test
    fun `issue returns null for empty scopes`() {
        assertNull(service.issue("https://sigil.test", clientId, ""))
    }

    @Test
    fun `issue persists a hashed refresh token`() {
        val raw = service.issue("https://sigil.test", clientId, "create")

        assertTrue(!raw.isNullOrBlank())
        val captor = ArgumentCaptor.forClass(RefreshTokenEntity::class.java)
        verify(refreshTokenRepository).save(captor.capture())
        assertEquals(Tokens.sha256(raw!!), captor.value.tokenHash)
    }

    @Test
    fun `refresh rotates and keeps scope when none requested`() {
        `when`(refreshTokenRepository.findByTokenHash(Tokens.sha256("raw-refresh"))).thenReturn(stored())
        `when`(refreshTokenRepository.claim(eq(Tokens.sha256("raw-refresh")), any())).thenReturn(1)
        `when`(accessTokenService.issue(eq("https://sigil.test"), eq(clientId), eq("create update")))
            .thenReturn(
                dev.jacobandersen.sigil.data.domain.IssuedToken(
                    "at",
                    "create update",
                    "https://sigil.test",
                    Instant.now(),
                ),
            )

        val rotated = service.refresh("raw-refresh", clientId, null)

        assertEquals("create update", rotated.scope)
        assertNotNull(rotated.refreshToken)
    }

    @Test
    fun `refresh rejects widening scope`() {
        `when`(refreshTokenRepository.findByTokenHash(Tokens.sha256("raw-refresh"))).thenReturn(stored(scope = "create"))

        val e =
            assertThrows(IndieAuthException::class.java) {
                service.refresh("raw-refresh", clientId, "create delete")
            }
        assertEquals(IndieAuthError.Code.INVALID_SCOPE, e.code)
    }

    @Test
    fun `refresh rejects an already used token`() {
        `when`(refreshTokenRepository.findByTokenHash(Tokens.sha256("raw-refresh"))).thenReturn(stored())
        `when`(refreshTokenRepository.claim(eq(Tokens.sha256("raw-refresh")), any())).thenReturn(0)

        val e =
            assertThrows(IndieAuthException::class.java) {
                service.refresh("raw-refresh", clientId, null)
            }
        assertEquals(IndieAuthError.Code.INVALID_GRANT, e.code)
    }
}
