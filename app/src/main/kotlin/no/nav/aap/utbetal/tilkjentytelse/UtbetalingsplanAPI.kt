package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanRepository
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import no.nav.aap.utbetaling.UtbetalingsplanDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.simulerUtbetalingsplan(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =
    route("/simulering").post<Unit, UtbetalingsplanDto, TilkjentYtelseDto> { _, tilkjentYtelse ->
        prometheus.httpCallCounter("/simulering").increment()
        val utbetaling = TilkjentYtelseService().simulerUtbetaling(dataSource, tilkjentYtelse)
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
    val perioder = this.perioder.map {
        UtbetalingsperiodeDto(
            fom = it.periode.fom,
            tom = it.periode.tom,
            dagsats = it.dagsats.verdi(),
            gradering = it.gradering.prosentverdi(),
            grunnlag = it.grunnlag.verdi(),
            grunnlagsfaktor = it.grunnlagsfaktor.verdi(),
            grunnbeløp = it.grunnbeløp.verdi(),
            antallBarn = it.antallBarn,
            barnetilleggsats = it.barnetilleggsats.verdi(),
            barnetillegg = it.barnetillegg.verdi(),
            endretSidenForrrige = it.endretSidenForrige
        )
    }
    return UtbetalingsplanDto(
        behandlingsreferanse = this.behandlingsreferanse,
        forrigeBehandlingsreferanse = this.forrigeBehandlingsreferanse,
        perioder = perioder,
    )
}
