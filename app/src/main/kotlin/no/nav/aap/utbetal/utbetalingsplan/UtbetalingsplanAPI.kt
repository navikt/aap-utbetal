package no.nav.aap.utbetal.utbetalingsplan

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.tilkjentytelse.HentUtbetalingsplanDto
import no.nav.aap.utbetaling.Endringstype
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import no.nav.aap.utbetaling.UtbetalingsplanDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.hent(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =
    route("/utbetalingsplan").post<Unit, UtbetalingsplanDto, HentUtbetalingsplanDto> { _, utbetalingsplanDto ->
        prometheus.httpCallCounter("/utbetalingsplan").increment()
        val utbetalingsplan = dataSource.transaction(readOnly = true) { connection ->
            UtbetalingsplanRepository(connection).hent(utbetalingsplanDto.behandlingsreferanse)
        }
        if (utbetalingsplan == null) {
            respondWithStatus(HttpStatusCode.NoContent)
        } else {
            respond(utbetalingsplan.tilUtbetalingDto())
        }
    }

private fun Utbetalingsplan.tilUtbetalingDto(): UtbetalingsplanDto {
    return UtbetalingsplanDto(
        behandlingsreferanse = this.behandlingsreferanse,
        forrigeBehandlingsreferanse = this.forrigeBehandlingsreferanse,
        perioder = this.perioder.map { it.tilUtbetalingsperiodeDto() },
    )
}

private fun Utbetalingsperiode.tilUtbetalingsperiodeDto() =
    UtbetalingsperiodeDto(
        fom = this.periode.fom,
        tom = this.periode.tom,
        redusertDagsats = this.utbetaling.redusertDagsats.verdi(),
        dagsats = this.utbetaling.dagsats.verdi(),
        gradering = this.utbetaling.gradering.prosentverdi(),
        grunnlag = this.utbetaling.grunnlag.verdi(),
        grunnlagsfaktor = this.utbetaling.grunnlagsfaktor.verdi(),
        grunnbeløp = this.utbetaling.grunnbeløp.verdi(),
        antallBarn = this.utbetaling.antallBarn,
        barnetilleggsats = this.utbetaling.barnetilleggsats.verdi(),
        barnetillegg = this.utbetaling.barnetillegg.verdi(),
        endringstype = this.utbetalingsperiodeType.tilEndringstype()
    )

private fun UtbetalingsperiodeType.tilEndringstype() = when (this) {
    UtbetalingsperiodeType.NY -> Endringstype.NY
    UtbetalingsperiodeType.ENDRET -> Endringstype.ENDRET
    UtbetalingsperiodeType.UENDRET -> Endringstype.UENDRET
}
