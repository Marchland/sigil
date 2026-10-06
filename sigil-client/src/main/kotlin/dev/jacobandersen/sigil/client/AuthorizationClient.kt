package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.ProfileUrlResponse
import dev.jacobandersen.sigil.protocol.TokenResponse

/**
 * The authorization-code flow (IndieAuth 5): building the authorization
 * request, and redeeming the returned code at either the token endpoint (for
 * an access token) or the authorization endpoint (for the profile URL only).
 */
interface AuthorizationClient {
    /**
     * Builds the authorization request URL to redirect the browser to. Passing
     * null/blank [codeChallenge] omits PKCE parameters; when supplied,
     * [codeChallengeMethod] defaults to `S256`.
     */
    fun buildAuthorizationUrl(
        authorizationEndpoint: String,
        clientId: String,
        redirectUri: String,
        state: String,
        scope: String? = null,
        me: String? = null,
        codeChallenge: String? = null,
        codeChallengeMethod: String? = null,
    ): String

    /** Exchanges an authorization code for an access token at the token endpoint. */
    fun exchangeCode(
        tokenEndpoint: String,
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String? = null,
    ): TokenResponse

    /**
     * Redeems an authorization code at the authorization endpoint, returning the
     * canonical profile URL without an access token (5.3.2).
     */
    fun redeemProfileUrl(
        authorizationEndpoint: String,
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String? = null,
    ): ProfileUrlResponse

    /**
     * Exchanges a refresh token for a new access token (5.5.1). A null/blank
     * [scope] keeps the originally granted scope; a narrower scope is allowed.
     */
    fun refresh(
        tokenEndpoint: String,
        refreshToken: String,
        clientId: String,
        scope: String? = null,
    ): TokenResponse
}
