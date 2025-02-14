package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import javax.sql.DataSource


fun NormalOpenAPIRoute.registrerTilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =

    route("/tilkjentytelse").post<Unit, Unit, TilkjentYtelseDto> { _, tilkjentYtelseDto ->
        prometheus.httpCallCounter("/tilkjentytelse").increment()
        dataSource.transaction { connection ->
            TilkjentYtelseService(connection).lagre(tilkjentYtelseDto.tilTilkjentYtelse())
            if (tilkjentYtelseDto.forrigeBehandlingsreferanse == null) {
                UtbetalingJobbService(connection).opprettUtbetalingJobb(tilkjentYtelseDto.saksnummer, tilkjentYtelseDto.behandlingsreferanse)
            }
        }
        respondWithStatus(HttpStatusCode.OK)
    }
