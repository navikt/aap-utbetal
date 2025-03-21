package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.utbetaling.SakUtbetaling
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class TilkjentYtelseResponse {
    /**
     * Kan ikke motta ny tilkjent ytelse fordi en tidligere utbetaling ikke er ferdigbehandlet.
     */
    LOCKED,
    /**
     * Har mottatt en duplikat innsending av tilkjent ytelse som er forskjellig fra den forrige.
     */
    CONFLICT,
    /**
     * Tilkjent ytelse er mottatt.
     */
    OK
}

class TilkjentYtelseService(private val connection: DBConnection) {

    fun håndterNyTilkjentYtelse(tilkjentYtelse: TilkjentYtelse): TilkjentYtelseResponse {
        val utbetalingRepo = UtbetalingRepository(connection)
        val utbetalingerForSak = utbetalingRepo.hent(tilkjentYtelse.saksnummer)
        val locked = utbetalingerForSak.any {it.utbetalingStatus != no.nav.aap.utbetaling.UtbetalingStatus.BEKREFTET}
        if (locked) {
            return TilkjentYtelseResponse.LOCKED
        }
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val eksisterendeTilkjentYtelse = tilkjentYtelseRepo.hent(tilkjentYtelse.behandlingsreferanse)
        if (eksisterendeTilkjentYtelse == null) {
            lagre(tilkjentYtelse)
            UtbetalingJobbService(connection).opprettUtbetalingJobb(
                tilkjentYtelse.saksnummer.toString(),
                tilkjentYtelse.behandlingsreferanse
            )
        } else {
            // Sjekk om duplikat ikke er lik, slik at det kan sendes Conflict http code til klienten
            if (!eksisterendeTilkjentYtelse.erLik(tilkjentYtelse)) {
                return TilkjentYtelseResponse.CONFLICT
            }
        }
        return TilkjentYtelseResponse.OK
    }

    /**
     * Lagre tilkjent ytelese. Oppretter SakUtbetaling dersom det er første tilkjente ytelse for denne saken.
     *
     * @param tilkjentYtelse tilkjent ytelse som skal lagres
     *
     * @return SakUtbetaling sin id
     */
    private fun lagre(tilkjentYtelse: TilkjentYtelse): Long {
        val sakUtbetalingRepo = SakUtbetalingRepository(connection)
        val sakUtbetalingId = if (tilkjentYtelse.forrigeBehandlingsreferanse == null) {
            sakUtbetalingRepo.lagre(SakUtbetaling(saksnummer = tilkjentYtelse.saksnummer))
        } else {
            val sakUtbetaling = sakUtbetalingRepo.hent(tilkjentYtelse.saksnummer)
            if (sakUtbetaling != null) {
                sakUtbetaling.id!!
            } else {
                // Opprett SakUtbetaling dersom den ikke finnes.
                sakUtbetalingRepo.lagre(SakUtbetaling(saksnummer = tilkjentYtelse.saksnummer))
            }
        }
        TilkjentYtelseRepository(connection).lagre(tilkjentYtelse)
        return sakUtbetalingId
    }


    /**
     * Sjekk om tilkjent ytelser er like. Kan ikke bruke vanlig equals siden bl.a. BigDecimal ikke fungerer så bra i sammenligning av data classes.
     */
    private fun TilkjentYtelse.erLik(tilkjentYtelse: TilkjentYtelse): Boolean {

        if (saksnummer != tilkjentYtelse.saksnummer) return false
        if (vedtakstidspunkt.avrundet() != tilkjentYtelse.vedtakstidspunkt.avrundet()) return false
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

    private fun LocalDateTime.avrundet() = truncatedTo(ChronoUnit.MILLIS)


}
