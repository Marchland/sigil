package dev.jacobandersen.sigil.client

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Connection settings for the Sigil client. When [baseUrl] is set, the Spring
 * Boot auto-configuration registers a ready-to-use [SigilClient] and its narrow
 * interfaces.
 */
@ConfigurationProperties(prefix = "sigil.client")
data class SigilClientProperties(
    /** Base URL of the Sigil service, e.g. `https://sigil.example.com`. */
    val baseUrl: String = "",
    /**
     * How long an introspection result is cached. Bounds the load on Sigil and
     * how long a revoked token may still be accepted (revocation lag).
     */
    val introspectionCacheTtl: Duration = Duration.ofSeconds(30),
    /** Maximum number of cached introspection results before eviction. */
    val introspectionCacheMaxSize: Long = 10_000,
    /** TCP connect timeout for Sigil calls. */
    val connectTimeout: Duration = Duration.ofSeconds(3),
    /** Read timeout for Sigil calls. */
    val readTimeout: Duration = Duration.ofSeconds(3),
)
