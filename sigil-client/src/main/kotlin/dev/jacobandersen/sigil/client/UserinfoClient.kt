package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.UserProfile

/**
 * Fetches the user information associated with an access token (IndieAuth 9).
 * Requires the token to carry the `profile` and/or `email` scope.
 */
interface UserinfoClient {
    /** Returns the profile claims for [accessToken]. */
    fun userinfo(accessToken: String): UserProfile
}
