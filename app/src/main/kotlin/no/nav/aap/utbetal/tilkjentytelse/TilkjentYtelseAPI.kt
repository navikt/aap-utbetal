package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log: Logger = LoggerFactory.getLogger("POST /tilkjentytelse")

fun NormalOpenAPIRoute.tilkjentYtelse(dataSource: DataSource, prometheus: PrometheusMeterRegistry) =

    route("/tilkjentytelse").post<Unit, Unit, TilkjentYtelseDto> { _, dto ->
        prometheus.httpCallCounter("/tilkjentytelse").increment()
        log.info("Tilkjent ytelse: {}", dto.copy(personIdent = "..........."))
        var conflict = false
        dataSource.transaction { connection ->
            val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
            val eksisterendeTilkjentYtelse = tilkjentYtelseRepo.hent(dto.behandlingsreferanse)
            val nyTilkjentYtelse = dto.tilTilkjentYtelse()
            if (eksisterendeTilkjentYtelse == null) {
                TilkjentYtelseService(connection).lagre(dto.tilTilkjentYtelse())
                UtbetalingJobbService(connection).opprettUtbetalingJobb(
                    dto.saksnummer,
                    dto.behandlingsreferanse
                )
            } else {
                // Sjekk om duplikat ikke er lik, slik at det kan sendes Conflict http code til klienten
                if (!eksisterendeTilkjentYtelse.erLik(nyTilkjentYtelse)) {
                    conflict = true
                }
            }
        }
        if (conflict) {
            respondWithStatus(HttpStatusCode.Conflict)
        } else {
            respondWithStatus(HttpStatusCode.OK)
        }
    }

/**
 * Sjekk om tilkjent ytelser er like. Kan ikke bruke vanlig equals siden bl.a. BigDecimal ikke fungerer så bra i sammenligning av data classes.
 */
private fun TilkjentYtelse.erLik(tilkjentYtelse: TilkjentYtelse): Boolean {

    if (saksnummer != tilkjentYtelse.saksnummer) return false
    if (vedtakstidspunkt != tilkjentYtelse.vedtakstidspunkt) return false
    if (forrigeBehandlingsreferanse != tilkjentYtelse.forrigeBehandlingsreferanse) return false
    if (personIdent != tilkjentYtelse.personIdent) return false
    if (beslutterId != tilkjentYtelse.beslutterId) return false
    if (saksbehandlerId != tilkjentYtelse.saksbehandlerId) return false
    if (perioder.size != tilkjentYtelse.perioder.size) return false
    for (index in tilkjentYtelse.perioder.indices) {
        val periode1 = perioder[index]
        val periode2 = tilkjentYtelse.perioder[index]
        if (periode1.periode != periode2.periode) return false
        val detaljer1 = periode1.detaljer
        val detaljer2 = periode2.detaljer
        if (detaljer1.redusertDagsats.avrundet() != detaljer2.redusertDagsats.avrundet()) return false
        if (detaljer1.gradering.prosentverdi() != detaljer2.gradering.prosentverdi()) return false
        if (detaljer1.dagsats.avrundet() != detaljer2.dagsats.avrundet()) return false
        if (detaljer1.grunnlag.avrundet() != detaljer2.grunnlag.avrundet()) return false
        if (detaljer1.grunnlagsfaktor.compareTo(detaljer2.grunnlagsfaktor) != 0) return false
        if (detaljer1.grunnbeløp.avrundet() != detaljer2.grunnbeløp.avrundet()) return false
        if (detaljer1.antallBarn != detaljer2.antallBarn) return false
        if (detaljer1.barnetilleggsats.avrundet() != detaljer2.barnetilleggsats.avrundet()) return false
        if (detaljer1.barnetillegg.avrundet() != detaljer2.barnetillegg.avrundet()) return false
        if (detaljer1.ventedagerSamordning != detaljer2.ventedagerSamordning) return false
        if (detaljer1.utbetalingsdato != detaljer2.utbetalingsdato) return false
    }
    return true
}

private fun Beløp.avrundet() = verdi.toLong()

