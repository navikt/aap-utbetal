package no.nav.aap.utbetal.utbetaling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetaling.HentUtbetalingerDto
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import no.nav.aap.utbetaling.UtbetalingDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.hent(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =
    route("/utbetalingsplaner").post<Unit, List<UtbetalingDto>, HentUtbetalingerDto> { _, utbetalingsplanDto ->
        prometheus.httpCallCounter("/utbetalingsplan").increment()
        val utbetalingsplan = dataSource.transaction(readOnly = true) { connection ->
            UtbetalingRepository(connection).hent(utbetalingsplanDto.behandlingsreferanse)
        }
        respond(utbetalingsplan.map { it.tilUtbetalingDto() })
    }

private fun Utbetaling.tilUtbetalingDto(): UtbetalingDto {
    return UtbetalingDto(
        utbetalingOversendt = this.utbetalingOversendt,
        utbetalingBekreftet = this.utbetalingBekreftet,
        utbetalingStatus = this.utbetalingStatus,
        perioder = this.perioder.map { it.tilUtbetalingsperiodeDto() }
    )
}

private fun Utbetalingsperiode.tilUtbetalingsperiodeDto() =
    UtbetalingsperiodeDto(
        fom = this.periode.fom,
        tom = this.periode.tom,
        redusertDagsats = this.detaljer.redusertDagsats.verdi(),
        dagsats = this.detaljer.dagsats.verdi(),
        gradering = this.detaljer.gradering.prosentverdi(),
        grunnlag = this.detaljer.grunnlag.verdi(),
        grunnlagsfaktor = this.detaljer.grunnlagsfaktor.verdi(),
        grunnbeløp = this.detaljer.grunnbeløp.verdi(),
        antallBarn = this.detaljer.antallBarn,
        barnetilleggsats = this.detaljer.barnetilleggsats.verdi(),
        barnetillegg = this.detaljer.barnetillegg.verdi(),
        utbetalingsperiodeType = this.utbetalingsperiodeType
    )
