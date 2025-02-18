package no.nav.aap.utbetal.test

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.helvedUtbetalingFake() {

    routing {
        post("/utbetalinger/{uid}") {
            call.respond(status = HttpStatusCode.Created, "Utbetalingen er mottatt")
        }
        put("/utbetalinger/{uid}") {
            call.respond(status = HttpStatusCode.NoContent, "Endret utbetaling er mottatt")
        }
        get("/utbetaling/{uid}/status") {
            call.respond(status = HttpStatusCode.OK, "SENDT_TIL_OPPDRAG")
        }
    }

}