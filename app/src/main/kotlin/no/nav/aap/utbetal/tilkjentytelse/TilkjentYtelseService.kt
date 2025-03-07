package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.utbetal.utbetaling.SakUtbetaling
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository

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

}
