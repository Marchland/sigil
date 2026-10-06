package dev.jacobandersen.sigil.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Baseline security for Sigil. The IndieAuth endpoints are public HTTP APIs
 * consumed by browsers and other services, so the wildcard CORS source is used
 * and everything is permitted at the framework level; protocol-level
 * authorization happens in the endpoints themselves.
 */
@Configuration
class GlobalSecurityConfig {
    @Bean("publicCorsConfigurationSource")
    fun publicCorsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = false
                maxAge = 3600L
            }
        return CorsConfigurationSource { _ -> config }
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .applySigilDefaults()
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
}
