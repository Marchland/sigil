package dev.jacobandersen.sigil.client

import dev.jacobandersen.sigil.protocol.IndieAuthError

/**
 * Thrown when Sigil returns a protocol error (a non-2xx response carrying an
 * [IndieAuthError] body) or the exchange fails unexpectedly. Carries the typed
 * [error] when Sigil supplied one.
 */
class SigilClientException(
    message: String,
    val error: IndieAuthError? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
