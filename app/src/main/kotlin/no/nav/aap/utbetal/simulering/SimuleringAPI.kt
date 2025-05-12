package no.nav.aap.utbetal.simulering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.tilgang.AuthorizationRouteConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.klienter.helved.HelvedUtbetalingOppretter
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.tilTilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.tilkjentYtelse
import no.nav.aap.utbetal.utbetaling.UtbetalingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log: Logger = LoggerFactory.getLogger("POST /simulering")

fun NormalOpenAPIRoute.simulering(dataSource: DataSource, prometheus: PrometheusMeterRegistry, authConfig: AuthorizationRouteConfig) =

    route("/simulering").authorizedPost<Unit, List<UtbetalingOgSimuleringDto>, TilkjentYtelseDto>(authConfig, null) { _, dto ->
        prometheus.httpCallCounter("/simulering").increment()
        log.info("Simulering: {}", dto.copy(personIdent = "..........."))
        val utbetalingerOgSimuleringer = mutableListOf<UtbetalingOgSimuleringDto>()
        dataSource.transaction(readOnly = true) { connection ->
            val tilkjentYtelse = dto.tilTilkjentYtelse()
            val utbetalinger = UtbetalingService(connection).simulerOpprettelseAvUtbetalinger(tilkjentYtelse)
            val klient = UtbetalingKlient()
            utbetalinger.alle().forEach { utbetaling ->
                val simulering = if (utbetaling.perioder.isEmpty()) {
                    val helvedUtbetaling = UtbetalingKlient().hentUtbetaling(utbetaling.utbetalingRef)
                    klient.simuleringOpphør(utbetaling.utbetalingRef, helvedUtbetaling)
                } else {
                    val helvedUtbetaling = HelvedUtbetalingOppretter().opprettUtbetaling(utbetaling)
                    klient.simuleringUtbetaling(utbetaling.utbetalingRef, helvedUtbetaling)
                }
                utbetalingerOgSimuleringer.add(UtbetalingOgSimuleringDto(
                    utbetalingDto = utbetaling.tilUtbetalingDto(),
                    simuleringDto = simulering.tilSimuleringDto()
                ))
            }
        }
        if (Miljø.erDev()) {
            log.info("Simuleringsresultat: {}", utbetalingerOgSimuleringer)
        }
        respond(utbetalingerOgSimuleringer)
    }

