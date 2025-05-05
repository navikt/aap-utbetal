package no.nav.aap.utbetal.utbetaling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationRouteConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetaling.HentUtbetalingerDto
import no.nav.aap.utbetaling.UtbetalingDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.hent(
    dataSource: DataSource,
    prometheus: PrometheusMeterRegistry,
    authConfig: AuthorizationRouteConfig,
) = route("/utbetalinger").authorizedPost<Unit, List<UtbetalingDto>, HentUtbetalingerDto>(authConfig, null) { _, utbetalingsplanDto ->
        prometheus.httpCallCounter("/utbetalinger").increment()
        val utbetalingsplan = dataSource.transaction(readOnly = true) { connection ->
            UtbetalingRepository(connection).hent(utbetalingsplanDto.behandlingsreferanse)
        }
        respond(utbetalingsplan.map { it.tilUtbetalingDto() })
    }

