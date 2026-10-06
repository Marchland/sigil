package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.AuthorizationServerMetadata

/**
 * Resolves a profile URL's IndieAuth server metadata via the `indieauth-metadata`
 * link relation (IndieAuth 4.1). Consumers typically use this once to discover
 * the other endpoints, then use the endpoint-specific clients.
 */
interface DiscoveryClient {
    /**
     * Fetches the metadata document advertised by [profileUrl]. The metadata URL
     * itself is discovered from the HTTP `Link` header or HTML `<link>` element
     * with `rel=indieauth-metadata`.
     */
    fun fetchMetadata(profileUrl: String): AuthorizationServerMetadata

    /** Fetches a metadata document directly from a known [metadataUrl]. */
    fun fetchMetadataAt(metadataUrl: String): AuthorizationServerMetadata
}
