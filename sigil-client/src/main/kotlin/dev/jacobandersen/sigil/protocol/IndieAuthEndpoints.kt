package dev.jacobandersen.sigil.protocol

/**
 * The paths of Sigil's IndieAuth HTTP surface. These are stable API endpoints:
 * the server's controllers map to them and clients build requests against them.
 */
object IndieAuthEndpoints {
    const val AUTHORIZATION = "/indieauth/auth"
    const val CALLBACK = "/indieauth/auth/callback"
    const val TOKEN = "/indieauth/token"
    const val INTROSPECTION = "/indieauth/introspect"
    const val REVOCATION = "/indieauth/revocation"
    const val USERINFO = "/indieauth/userinfo"
    const val PROVIDERS = "/indieauth/providers"
}
