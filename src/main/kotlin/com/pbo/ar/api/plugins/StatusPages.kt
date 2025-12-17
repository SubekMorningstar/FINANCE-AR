package com.pbo.ar.api.plugins

import com.pbo.ar.api.dto.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>(cause.message ?: "Bad Request"))
        }
        
        exception<IllegalStateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ApiResponse.error<Unit>(cause.message ?: "Conflict"))
        }
        
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiResponse.error<Unit>(cause.message ?: "Not Found"))
        }
        
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<Unit>("Internal Server Error: ${cause.message}"))
        }
    }
}
