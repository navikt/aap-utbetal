package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetaling
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetalingRepository

private val utbetalingsdatoer:Set<Int> = setOf(10, 25)

class TilkjentYtelseService(private val connection: DBConnection) {

    /**
     * Lagre tilkjent ytelese. Oppretter SakUtbetaling dersom det er første tilkjente ytelse for denne saken.
     *
     * @param tilkjentYtelse tilkjent ytelse som skal lagres
     *
     * @return SakUtbetaling sin id
     */
    fun lagre(tilkjentYtelse: TilkjentYtelse): Long {
        val sakUtbetalingRepo = SakUtbetalingRepository(connection)
        val sakUtbetalingId = if (tilkjentYtelse.forrigeBehandlingsreferanse == null) {
            sakUtbetalingRepo.lagre(SakUtbetaling(saksnummer = tilkjentYtelse.saksnummer))
        } else {
            sakUtbetalingRepo.hent(tilkjentYtelse.saksnummer)?.id
                ?: throw IllegalStateException("Det skal finnes en rad i SAK_UTBETALING for saksnummer: ${tilkjentYtelse.saksnummer}")
        }
        TilkjentYtelseRepository(connection).lagre(tilkjentYtelse)
        return sakUtbetalingId
    }

}
