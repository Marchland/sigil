package dev.jacobandersen.sigil.client

/**
 * Revokes an access or refresh token (IndieAuth 7, RFC 7009). The endpoint
 * always responds success, including for unknown tokens, so this returns
 * normally unless the call itself fails.
 */
interface TokenRevoker {
    /** Revokes [token]; [tokenTypeHint] may be `access_token` or `refresh_token`. */
    fun revoke(
        token: String,
        tokenTypeHint: String? = null,
    )
}
