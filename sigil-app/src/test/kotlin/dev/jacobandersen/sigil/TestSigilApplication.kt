package dev.jacobandersen.sigil

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<SigilApplication>().with(TestcontainersConfiguration::class).run(*args)
}
