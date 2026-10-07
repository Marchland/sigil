package dev.jacobandersen.sigil.controller

import dev.jacobandersen.sigil.protocol.IndieAuthError
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

private val logger = KotlinLogging.logger {}

/**
 * Ensures framework-level failures use the OAuth/IndieAuth error shape
 * (`{"error": ..., "error_description": ...}`) instead of Spring Boot's default
 * error document. Protocol errors from the services are handled inside the
 * controllers (the authorization endpoint must decide between redirecting and
 * rendering); this advice covers everything else, including unexpected 500s.
 *
 * The HTTP status is chosen independently of the OAuth error code: a 401/403
 * from an `IndieAuthError.Code` is not always the right status for a framework
 * failure (e.g. 415, 405).
 */
@RestControllerAdvice
class IndieAuthExceptionHandler {
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun onMissingParameter(e: MissingServletRequestParameterException): ResponseEntity<IndieAuthError> =
        error(HttpStatus.BAD_REQUEST, IndieAuthError.Code.INVALID_REQUEST, "The '${e.parameterName}' parameter is required")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun onTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<IndieAuthError> =
        error(HttpStatus.BAD_REQUEST, IndieAuthError.Code.INVALID_REQUEST, "The '${e.name}' parameter is invalid")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun onUnreadableBody(e: HttpMessageNotReadableException): ResponseEntity<IndieAuthError> =
        error(HttpStatus.BAD_REQUEST, IndieAuthError.Code.INVALID_REQUEST, "The request body could not be read")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun onMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<IndieAuthError> {
        val builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        e.supportedHttpMethods?.let { builder.header(HttpHeaders.ALLOW, it.joinToString(", ") { method -> method.name() }) }
        return builder.body(IndieAuthError.of(IndieAuthError.Code.INVALID_REQUEST, "The HTTP method is not allowed for this endpoint"))
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun onMediaTypeNotSupported(e: HttpMediaTypeNotSupportedException): ResponseEntity<IndieAuthError> =
        error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, IndieAuthError.Code.INVALID_REQUEST, "The request content type is not supported")

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun onMediaTypeNotAcceptable(e: HttpMediaTypeNotAcceptableException): ResponseEntity<IndieAuthError> =
        error(HttpStatus.NOT_ACCEPTABLE, IndieAuthError.Code.INVALID_REQUEST, "No acceptable response content type was requested")

    @ExceptionHandler(NoResourceFoundException::class)
    fun onNoResource(e: NoResourceFoundException): ResponseEntity<IndieAuthError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(IndieAuthError("not_found", "No such endpoint"))

    @ExceptionHandler(Exception::class)
    fun onUnexpected(e: Exception): ResponseEntity<IndieAuthError> {
        logger.error(e) { "Unhandled IndieAuth request failure" }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, IndieAuthError.Code.SERVER_ERROR, "An unexpected error occurred")
    }

    private fun error(
        status: HttpStatus,
        code: IndieAuthError.Code,
        description: String? = null,
    ): ResponseEntity<IndieAuthError> = ResponseEntity.status(status).body(IndieAuthError.of(code, description))
}
