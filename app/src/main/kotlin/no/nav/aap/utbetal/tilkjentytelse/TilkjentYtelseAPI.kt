package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import javax.sql.DataSource


fun NormalOpenAPIRoute.førstegangsTilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =

    route("/tilkjentytelse").post<Unit, Unit, FørstegangTilkjentYtelseDto> { _, dto ->
        prometheus.httpCallCounter("/tilkjentytelse").increment()
        dataSource.transaction { connection ->
            TilkjentYtelseService(connection).lagre(dto.tilTilkjentYtelse())
            UtbetalingJobbService(connection).opprettUtbetalingJobb(
                dto.saksnummer,
                dto.behandlingsreferanse,
                Periode(dto.førstegangsutbetalingFom, dto.førstegangsutbetalingTom)
            )
        }
        respondWithStatus(HttpStatusCode.OK)
    }


fun NormalOpenAPIRoute.oppdatertTilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =

    route("/tilkjentytelse").put<Unit, Unit, OppdatertTilkjentYtelseDto> { _, dto ->
        prometheus.httpCallCounter("/tilkjentytelse").increment()
        dataSource.transaction { connection ->
            TilkjentYtelseService(connection).lagre(dto.tilTilkjentYtelse())
        }
        respondWithStatus(HttpStatusCode.OK)
    }