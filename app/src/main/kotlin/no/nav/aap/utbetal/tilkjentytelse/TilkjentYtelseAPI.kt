package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.httpCallCounter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log: Logger = LoggerFactory.getLogger("POST /tilkjentytelse")

fun NormalOpenAPIRoute.tilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =

    route("/tilkjentytelse").post<Unit, Unit, TilkjentYtelseDto> { _, dto ->
        prometheus.httpCallCounter("/tilkjentytelse").increment()
        log.info("Tilkjent ytelse: {}", dto.copy(personIdent = "..........."))

        val respomse = dataSource.transaction { connection ->
            TilkjentYtelseService(connection).håndterNyTilkjentYtelse(dto.tilTilkjentYtelse())
        }

        when (respomse) {
            TilkjentYtelseResponse.LOCKED -> respondWithStatus(HttpStatusCode.Locked)
            TilkjentYtelseResponse.CONFLICT -> respondWithStatus(HttpStatusCode.Conflict)
            TilkjentYtelseResponse.OK -> respondWithStatus(HttpStatusCode.OK)
        }
    }

