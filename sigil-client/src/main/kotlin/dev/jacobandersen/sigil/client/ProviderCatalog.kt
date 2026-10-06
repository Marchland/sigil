package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.ProviderInfo

/**
 * Lists the identity providers Sigil offers, e.g. for rendering login buttons
 * on an authorization UI host.
 */
interface ProviderCatalog {
    /** Returns the configured identity providers. */
    fun providers(): List<ProviderInfo>
}
