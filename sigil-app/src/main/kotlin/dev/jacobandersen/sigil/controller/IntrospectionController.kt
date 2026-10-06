package dev.jacobandersen.sigil.controller

import dev.jacobandersen.sigil.protocol.IndieAuthEndpoints
import dev.jacobandersen.sigil.protocol.IndieAuthError
import dev.jacobandersen.sigil.security.Tokens
import dev.jacobandersen.sigil.service.IntrospectionService
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
 * The endpoint is authorized by self-introspection: the bearer credential must
 * be the same token being introspected (compared in constant time). Callers can
 * therefore only ever learn about tokens they already hold, which satisfies the
 * spec's requirement without a separate service credential. This is easy to
 * extend later if cross-token or audience-scoped introspection is ever needed.
 *
 * Insufficient authorization yields 401. Unknown or expired subject tokens
 * yield 200 with `active: false` and no further detail.
 */
@RestController
class IntrospectionController(
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
        if (bearer == null || token == null || !Tokens.constantTimeEquals(bearer, token)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(IndieAuthError.of(IndieAuthError.Code.INVALID_TOKEN, "Valid bearer authorization is required"))
        }
        if (token.isBlank()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(IndieAuthError.of(IndieAuthError.Code.INVALID_REQUEST, "The 'token' parameter is required"))
        }
        return ResponseEntity.ok(introspectionService.introspect(token))
    }

    private fun bearer(authorization: String?): String? =
        authorization
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring("Bearer ".length)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
