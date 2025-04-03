package no.nav.aap.utbetal.test

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus


fun Application.helvedUtbetalingFake() {

    routing {
        post("/utbetalinger/{uid}") {
            call.respond(HttpStatusCode.Created)
        }
        put("/utbetalinger/{uid}") {
            call.respond(HttpStatusCode.NoContent)
        }
        get("/utbetalinger/{uid}/status") {
            call.respond(status = HttpStatusCode.OK, UtbetalingStatus.OK)
        }
    }

}