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
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseService
import no.nav.aap.utbetal.tilkjentytelse.tilTilkjentYtelse
import no.nav.aap.utbetaling.Endringstype
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import no.nav.aap.utbetaling.UtbetalingsplanDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.simulerUtbetalingsplan(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =
    route("/simulering").post<Unit, UtbetalingsplanDto, TilkjentYtelseDto> { _, tilkjentYtelse ->
        prometheus.httpCallCounter("/simulering").increment()
        val utbetaling = TilkjentYtelseService().simulerUtbetaling(dataSource, tilkjentYtelse.tilTilkjentYtelse())
        respond(utbetaling.tilUtbetalingDto())
    }

fun NormalOpenAPIRoute.hentUtbetalingsplan(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =
    route("/utbetalingsplan").post<Unit, UtbetalingsplanDto, HentUtbetalingsplanDto> { _, utbetalingsplanDto ->
        prometheus.httpCallCounter("/utbetalingsplan").increment()
        val utbetalingsplan = dataSource.transaction(readOnly = true) { connection ->
            UtbetalingsplanRepository(connection).hentUtbetalingsplan(utbetalingsplanDto.behandlingsreferanse)
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

private fun Utbetalingsperiode.tilUtbetalingsperiodeDto(): UtbetalingsperiodeDto {
    return when(this) {
        is Utbetalingsperiode.EndretPeriode -> this.tilUtbetalingsperiodeDto()
        is Utbetalingsperiode.NyPeriode -> this.tilUtbetalingsperiodeDto()
        is Utbetalingsperiode.UendretPeriode -> this.tilUtbetalingsperiodeDto()
    }
}

private fun Utbetalingsperiode.NyPeriode.tilUtbetalingsperiodeDto() =
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
        endringstype = Endringstype.NY
    )

private fun Utbetalingsperiode.EndretPeriode.tilUtbetalingsperiodeDto() =
    UtbetalingsperiodeDto(
        fom = this.periode.fom,
        tom = this.periode.tom,
        redusertDagsats = this.nyUtbetaling.redusertDagsats.verdi(),
        dagsats = this.nyUtbetaling.dagsats.verdi(),
        gradering = this.nyUtbetaling.gradering.prosentverdi(),
        grunnlag = this.nyUtbetaling.grunnlag.verdi(),
        grunnlagsfaktor = this.nyUtbetaling.grunnlagsfaktor.verdi(),
        grunnbeløp = this.nyUtbetaling.grunnbeløp.verdi(),
        antallBarn = this.nyUtbetaling.antallBarn,
        barnetilleggsats = this.nyUtbetaling.barnetilleggsats.verdi(),
        barnetillegg = this.nyUtbetaling.barnetillegg.verdi(),
        endringstype = Endringstype.ENDRET
    )


private fun Utbetalingsperiode.UendretPeriode.tilUtbetalingsperiodeDto() =
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
        endringstype = Endringstype.UENDRET
    )

