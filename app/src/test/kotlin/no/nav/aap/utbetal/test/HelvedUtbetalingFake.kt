package no.nav.aap.utbetal.test

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.utbetal.klienter.helved.Utbetaling
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus
import no.nav.aap.utbetal.simulering.SimuleringDto
import java.util.*


fun Application.helvedUtbetalingFake() {

    val fakeDatabase = mutableMapOf<UUID, Utbetaling>()


    routing {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        post("/utbetalinger/{uid}") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            fakeDatabase[utbetalingRef] = utbetaling
            call.respond(HttpStatusCode.Created)
        }
        put("/utbetalinger/{uid}") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            fakeDatabase[utbetalingRef] = utbetaling
            call.respond(HttpStatusCode.NoContent)
        }
        delete("/utbetalinger/{uid}") {
            call.respond(HttpStatusCode.OK)
        }
        get("/utbetalinger/{uid}") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            call.respond(status = HttpStatusCode.OK, fakeDatabase[utbetalingRef]!!)
        }
        get("/utbetalinger/{uid}/status") {
            call.respond(status = HttpStatusCode.OK, UtbetalingStatus.OK)
        }
        post("/utbetalinger/{uid}/simuler") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            fakeDatabase[utbetalingRef] = utbetaling
            call.respond(status = HttpStatusCode.OK, SimuleringDto(perioder = listOf()))
        }
        delete("/utbetalinger/{uid}/simuler") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            fakeDatabase[utbetalingRef] = utbetaling
            call.respond(status = HttpStatusCode.OK, SimuleringDto(perioder = listOf()))
        }
    }

}