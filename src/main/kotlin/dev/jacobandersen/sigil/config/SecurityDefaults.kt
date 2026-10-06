package dev.jacobandersen.sigil.config

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy

/**
 * Shared baseline applied to every SecurityFilterChain in Sigil.
 *
 * Sigil is API-only: CORS is enabled per-chain, CSRF, form login, HTTP basic
 * and server-side sessions are disabled so the IndieAuth endpoints are
 * stateless.
 */
fun HttpSecurity.applySigilDefaults(): HttpSecurity =
    cors { }
        .csrf { it.disable() }
        .formLogin { it.disable() }
        .httpBasic { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
