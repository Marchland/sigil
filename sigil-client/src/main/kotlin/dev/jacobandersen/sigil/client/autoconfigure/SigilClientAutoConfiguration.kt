package dev.jacobandersen.sigil.client.autoconfigure

import dev.jacobandersen.sigil.client.AuthorizationClient
import dev.jacobandersen.sigil.client.DiscoveryClient
import dev.jacobandersen.sigil.client.ProviderCatalog
import dev.jacobandersen.sigil.client.SigilClient
import dev.jacobandersen.sigil.client.SigilClientProperties
import dev.jacobandersen.sigil.client.TokenIntrospector
import dev.jacobandersen.sigil.client.TokenRevoker
import dev.jacobandersen.sigil.client.UserinfoClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.ObjectMapper

/**
 * Registers a [SigilClient] and each narrow client interface as beans, so a
 * consumer can inject either the facade or only the capability it needs.
 *
 * Activated when `sigil.client.base-url` is set. Every bean is
 * [ConditionalOnMissingBean], so a consumer can override any piece.
 */
@AutoConfiguration
@EnableConfigurationProperties(SigilClientProperties::class)
@ConditionalOnProperty(prefix = "sigil.client", name = ["base-url"])
class SigilClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun sigilClient(
        properties: SigilClientProperties,
        objectMapper: ObjectMapper,
    ): SigilClient = SigilClient(properties, objectMapper)

    @Bean
    @ConditionalOnMissingBean
    fun tokenIntrospector(sigilClient: SigilClient): TokenIntrospector = sigilClient

    @Bean
    @ConditionalOnMissingBean
    fun authorizationClient(sigilClient: SigilClient): AuthorizationClient = sigilClient

    @Bean
    @ConditionalOnMissingBean
    fun discoveryClient(sigilClient: SigilClient): DiscoveryClient = sigilClient

    @Bean
    @ConditionalOnMissingBean
    fun tokenRevoker(sigilClient: SigilClient): TokenRevoker = sigilClient

    @Bean
    @ConditionalOnMissingBean
    fun userinfoClient(sigilClient: SigilClient): UserinfoClient = sigilClient

    @Bean
    @ConditionalOnMissingBean
    fun providerCatalog(sigilClient: SigilClient): ProviderCatalog = sigilClient
}
