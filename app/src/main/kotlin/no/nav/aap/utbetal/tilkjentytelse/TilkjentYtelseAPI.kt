package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.utbetal.httpCallCounter
import javax.sql.DataSource


fun NormalOpenAPIRoute.registrerTilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =

    route("/tilkjentytelse").post<Unit, Unit, TilkjentYtelseDto> { _, tilkjentYtelse ->
        prometheus.httpCallCounter("/neste-oppgave").increment()
        TilkjentYtelseService().lagre(dataSource, tilkjentYtelse)
        respondWithStatus(HttpStatusCode.OK)
    }

