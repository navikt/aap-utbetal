package no.nav.aap.utbetal.test

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.utbetal.helved.Utbetalingsmelding as HelvedUtbetalingsmelding
import no.nav.aap.utbetal.klienter.helved.SlettAvvent
import no.nav.aap.utbetal.klienter.helved.Simulering
import no.nav.aap.utbetal.klienter.helved.Simuleringsperiode
import no.nav.aap.utbetal.klienter.helved.SimulertUtbetaling
import no.nav.aap.utbetal.klienter.helved.Utbetaling
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus
import no.nav.aap.utbetal.simulering.SimuleringDto
import java.time.LocalDate
import java.util.*

fun Application.helvedUtbetalingFake(
    utbetalinger: MutableMap<UUID, Utbetaling>,
    slettAvventMap: MutableMap<UUID, SlettAvvent>,
    kall: MutableList<HelvedKall> = mutableListOf(),
) {

    routing {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        post("/utbetalinger/{uid}") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            utbetalinger[utbetalingRef] = utbetaling
            kall.add(HelvedKall("POST", utbetalingRef, utbetaling))
            call.respond(HttpStatusCode.Created)
        }
        put("/utbetalinger/{uid}") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            utbetalinger[utbetalingRef] = utbetaling
            kall.add(HelvedKall("PUT", utbetalingRef, utbetaling))
            call.respond(HttpStatusCode.NoContent)
        }
        delete("/utbetalinger/{uid}") {
            call.respond(HttpStatusCode.OK)
        }
        get("/utbetalinger/{uid}") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            call.respond(status = HttpStatusCode.OK, utbetalinger[utbetalingRef]!!)
        }
        get("/utbetalinger/{uid}/status") {
            call.respond(status = HttpStatusCode.OK, UtbetalingStatus.OK)
        }
        post("/utbetalinger/{uid}/simuler") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            utbetalinger[utbetalingRef] = utbetaling
            call.respond(status = HttpStatusCode.OK, SimuleringDto(perioder = listOf()))
        }
        delete("/utbetalinger/{uid}/simuler") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val utbetaling = call.receive<Utbetaling>()
            utbetalinger[utbetalingRef] = utbetaling
            call.respond(status = HttpStatusCode.OK, SimuleringDto(perioder = listOf()))
        }
        post("/utbetalinger/{uid}/avvent") {
            val utbetalingRef = UUID.fromString(call.parameters["uid"])
            val slettAvvent = call.receive<SlettAvvent>()
            slettAvventMap[utbetalingRef] = slettAvvent
            kall.add(HelvedKall("POST", utbetalingRef, null, slettAvvent))
            call.respond(HttpStatusCode.Created)
        }
        post("/api/dryrun/aap") {
            // Brukes av SimuleringService (nytt grensesnitt) for å avgjøre om en endring i
            // avvent-periode faktisk medfører en beløpsendring (og dermed skal feilregistreres).
            // Simulerer her at det ikke er utbetalt noe tidligere, slik at enhver sats > 0
            // fremstår som en beløpsendring i testene.
            val melding = call.receive<HelvedUtbetalingsmelding>()
            val simulering = Simulering(
                perioder = melding.utbetalinger.map { utbetaling ->
                    Simuleringsperiode(
                        fom = LocalDate.parse(utbetaling.fom),
                        tom = LocalDate.parse(utbetaling.tom),
                        utbetalinger = listOf(
                            SimulertUtbetaling(
                                sakId = melding.sakId,
                                utbetalesTil = melding.ident,
                                tidligereUtbetalt = 0,
                                nyttBeløp = utbetaling.utbetaltBeløp.toInt(),
                            )
                        )
                    )
                }
            )
            call.respond(HttpStatusCode.OK, simulering)
        }
    }

}