package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.IntrospectionResponse

/**
 * Validates access tokens via Sigil's introspection endpoint (IndieAuth 6,
 * RFC 7662 plus `me`). This is the hot path for resource servers.
 *
 * Results are cached for [SigilClientProperties.introspectionCacheTtl]; that
 * TTL is the upper bound on how long a revoked token may still be accepted.
 * Inactive results are not cached, and transport failures reject the token.
 */
interface TokenIntrospector {
    /** Introspects [token], returning the response (which may be inactive). */
    fun introspect(token: String): IntrospectionResponse
}
