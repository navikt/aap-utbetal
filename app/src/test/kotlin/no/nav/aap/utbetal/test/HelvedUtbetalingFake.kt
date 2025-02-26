package no.nav.aap.utbetal.test

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

private data class OppdragStatusDto(
    val status: OppdragStatus,
    val feilmelding: String? = null
)

private enum class OppdragStatus {
    LAGT_PÅ_KØ,
    KVITTERT_OK,
    KVITTERT_MED_MANGLER,
    KVITTERT_FUNKSJONELL_FEIL,
    KVITTERT_TEKNISK_FEIL,
    KVITTERT_UKJENT,
    OK_UTEN_UTBETALING,
}

fun Application.helvedUtbetalingFake() {

    routing {
        post("/utbetalinger/{uid}") {
            call.respond(HttpStatusCode.Created)
        }
        put("/utbetalinger/{uid}") {
            call.respond(HttpStatusCode.NoContent)
        }
        get("/utbetalinger/{uid}/status") {
            val uid = call.parameters["uid"]
            println("Finn utbetaling for $uid")
            call.respond(status = HttpStatusCode.OK, OppdragStatusDto(status = OppdragStatus.KVITTERT_OK))
        }
    }

}