package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetaling
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import javax.sql.DataSource

private val utbetalingsdatoer:Set<Int> = setOf(10, 25)

class TilkjentYtelseService(private val connection: DBConnection) {

    fun lagre(tilkjentYtelse: TilkjentYtelse) {
        val sakUtbetalingRepo = SakUtbetalingRepository(connection)
        val sakUtbetalingId = if (tilkjentYtelse.forrigeBehandlingsreferanse == null) {
            sakUtbetalingRepo.lagre(SakUtbetaling(saksnummer = tilkjentYtelse.saksnummer))
        } else {
            sakUtbetalingRepo.hent(tilkjentYtelse.saksnummer)?.id
                ?: throw IllegalStateException("Det skal finnes en rad i SAK_UTBETALING for saksnummer: ${tilkjentYtelse.saksnummer}")
        }
        TilkjentYtelseRepository(connection).lagre(tilkjentYtelse)
    }

}
