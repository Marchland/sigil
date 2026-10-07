package dev.jacobandersen.sigil.service

import dev.jacobandersen.sigil.protocol.IndieAuthError

/**
 * A protocol error raised by the IndieAuth services, carrying the OAuth error
 * code and a human-readable description. Controllers translate it into the
 * appropriate response shape (a redirect on the authorization endpoints, JSON
 * on the token endpoint).
 */
open class IndieAuthException(
    val code: IndieAuthError.Code,
    description: String? = null,
) : RuntimeException(description) {
    fun toError(): IndieAuthError = IndieAuthError.of(code, message)
}

/**
 * A failure validating the client or its `redirect_uri`. OAuth 2.0 (4.1.2.1)
 * forbids redirecting an error to an untrusted `redirect_uri`, so the
 * authorization endpoint renders these instead of redirecting.
 */
class UntrustedClientException(
    code: IndieAuthError.Code,
    description: String? = null,
) : IndieAuthException(code, description)
