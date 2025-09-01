package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationRouteConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.utbetal.httpCallCounter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log: Logger = LoggerFactory.getLogger("POST /tilkjentytelse")

fun NormalOpenAPIRoute.tilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry, authConfig: AuthorizationRouteConfig) =

    route("/tilkjentytelse").authorizedPost<Unit, Unit, TilkjentYtelseDto>(authConfig, null) { _, dto ->
        prometheus.httpCallCounter("/tilkjentytelse").increment()
        log.info("Tilkjent ytelse: {}", dto.copy(personIdent = "..........."))
        if (dto.avvent != null) {
            log.info("Avvent utbetaling for saksnummer ${dto.saksnummer} og behandling ${dto.behandlingsreferanse}: ${dto.avvent}")
        }
        val start = System.currentTimeMillis()
        val response = dataSource.transaction { connection ->
            TilkjentYtelseService(connection).håndterNyTilkjentYtelse(dto.tilTilkjentYtelse())
        }
        log.info("Tilkjent ytelse mottak tok ${System.currentTimeMillis() - start} ms")

        when (response) {
            TilkjentYtelseResponse.LOCKED -> respondWithStatus(HttpStatusCode.Locked)
            TilkjentYtelseResponse.CONFLICT -> respondWithStatus(HttpStatusCode.Conflict)
            TilkjentYtelseResponse.OK -> respondWithStatus(HttpStatusCode.OK)
        }
    }

