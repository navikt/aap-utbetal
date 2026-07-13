package no.nav.aap.utbetal.admin

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.Drift
import no.nav.aap.tilgang.RollerConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.utbetaling.UtbetalingLight
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import javax.sql.DataSource

private val harDriftsRolleConfig = RollerConfig(listOf(Drift))
fun NormalOpenAPIRoute.hentStatus(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =
    route("/admin/status").authorizedGet<Unit, UtbetalingStatusDto>(harDriftsRolleConfig) {
        prometheus.httpCallCounter("/admin/status").increment()
        val utbetalingerPerStatus = dataSource.transaction(readOnly = true) { connection ->
            val utbetalinger = UtbetalingRepository(connection).hentUtbetalingerSomManglerKvittering()
            utbetalinger.groupBy { it.utbetalingStatus }
        }
        respond(response = utbetalingerPerStatus.tilListAvUtbetalingStatusDto())
    }

private fun Map<UtbetalingStatus, List<UtbetalingLight>>.tilListAvUtbetalingStatusDto() =
    UtbetalingStatusDto(
        utbetalingerSomManglerKvittering = this[UtbetalingStatus.SENDT]?.map { it.tilUtbetalingInfoDto() }
            ?: emptyList(),
        utbetalingerMedFeiletStatus = this[UtbetalingStatus.FEILET]?.map { it.tilUtbetalingInfoDto() } ?: emptyList(),
    )

private fun UtbetalingLight.tilUtbetalingInfoDto() = UtbetalingInfoDto(
    utbetalingRef = this.utbetalingRef,
    saksnummer = this.saksnummer.toString(),
    behandlingsreferanse = this.behandlingsreferanse,
    utbetalingStatus = this.utbetalingStatus,
    utbetalingOpprettet = this.utbetalingOpprettet,
    utbetalingEndret = this.utbetalingEndret,
)
