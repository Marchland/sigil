package dev.jacobandersen.sigil.indieauth.controller

import dev.jacobandersen.sigil.indieauth.IndieAuthEndpoints
import dev.jacobandersen.sigil.indieauth.config.IndieAuthConfig
import dev.jacobandersen.sigil.indieauth.security.Tokens
import dev.jacobandersen.sigil.indieauth.service.AccessTokenService
import dev.jacobandersen.sigil.indieauth.service.IntrospectionService
import dev.jacobandersen.sigil.indieauth.type.IndieAuthError
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Token introspection for resource servers (IndieAuth 6, RFC 7662 plus `me`).
 *
 * The caller must present either an active Sigil-issued access token or the
 * configured service token (used by trusted resource servers such as Bastion,
 * which hold no user token of their own). Insufficient authorization yields
 * 401. Unknown or expired subject tokens yield 200 with `active: false` and no
 * further detail.
 */
@RestController
class IntrospectionController(
    private val config: IndieAuthConfig,
    private val accessTokenService: AccessTokenService,
    private val introspectionService: IntrospectionService,
) {
    @PostMapping(
        path = [IndieAuthEndpoints.INTROSPECTION],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    fun introspect(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestParam("token", required = false) token: String?,
    ): ResponseEntity<*> {
        val bearer = bearer(authorization)
        if (bearer == null || !authorized(bearer)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(IndieAuthError.of(IndieAuthError.Code.INVALID_TOKEN, "Valid bearer authorization is required"))
        }
        if (token.isNullOrBlank()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(IndieAuthError.of(IndieAuthError.Code.INVALID_REQUEST, "The 'token' parameter is required"))
        }
        return ResponseEntity.ok(introspectionService.introspect(token))
    }

    /**
     * A bearer credential is accepted when it is the configured service token
     * (compared in constant time) or resolves to an active issued access token.
     */
    private fun authorized(bearer: String): Boolean {
        val serviceToken = config.service.token
        if (serviceToken.isNotBlank() && Tokens.constantTimeEquals(serviceToken, bearer)) {
            return true
        }
        return accessTokenService.resolve(bearer) != null
    }

    private fun bearer(authorization: String?): String? =
        authorization
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring("Bearer ".length)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
