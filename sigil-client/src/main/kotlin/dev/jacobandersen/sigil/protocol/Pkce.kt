package dev.jacobandersen.sigil.protocol

import java.security.MessageDigest
import java.util.Base64

/**
 * PKCE (RFC 7636) helpers for the authorization-code flow, shared by the Sigil
 * server and any client. Only the `S256` challenge method is supported; the
 * `plain` method offers no protection against a leaked authorization code.
 */
object Pkce {
    /** The only challenge method Sigil accepts. */
    const val METHOD_S256 = "S256"

    /** Computes the S256 challenge for a verifier. */
    fun s256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Verifies a code verifier against a stored challenge using constant-time
     * comparison. Returns false for malformed input rather than throwing.
     */
    fun verify(
        challenge: String,
        verifier: String,
    ): Boolean = constantTimeEquals(challenge, s256(verifier))

    private fun constantTimeEquals(
        a: String,
        b: String,
    ): Boolean = MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
}
