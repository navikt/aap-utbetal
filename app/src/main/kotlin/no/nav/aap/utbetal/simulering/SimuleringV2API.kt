package no.nav.aap.utbetal.simulering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationRouteConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.tilTilkjentYtelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log: Logger = LoggerFactory.getLogger("POST /simulering/v2")

fun NormalOpenAPIRoute.simuleringV2(dataSource: DataSource, prometheus: PrometheusMeterRegistry, authConfig: AuthorizationRouteConfig) =

    route("/simulering/v2").authorizedPost<Unit, SimuleringDto, TilkjentYtelseDto>(authConfig, null) { _, dto ->
        prometheus.httpCallCounter("/simulering/v2").increment()
        log.info("Simulering v2 kalt for behandling: {}", dto.behandlingsreferanse)
        val simulering = dataSource.transaction(readOnly = true) { connection ->
            val tilkjentYtelse = dto.tilTilkjentYtelse()
            SimuleringService(connection).simuler(tilkjentYtelse.behandlingsreferanse)
        }
        log.info("Simulering utbetalinger: $simulering") //TODO: fjern(eller reduser loggingen) når vi har testet integrasjonen nok
        respond(simulering.tilSimuleringDto())
    }

