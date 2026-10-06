package dev.jacobandersen.sigil

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@EnableWebSecurity
@ConfigurationPropertiesScan
class SigilApplication

fun main(args: Array<String>) {
    runApplication<SigilApplication>(*args)
}
