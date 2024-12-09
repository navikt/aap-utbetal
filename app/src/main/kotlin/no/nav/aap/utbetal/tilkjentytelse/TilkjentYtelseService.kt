package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanRepository
import javax.sql.DataSource

class TilkjentYtelseService {

    fun lagre(dataSource: DataSource, tilkjentYtelse: TilkjentYtelse) {
        dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(tilkjentYtelse)
            val utbetalingRepo = UtbetalingsplanRepository(connection)
            if (tilkjentYtelse.forrigeBehandlingsreferanse == null) {
                TODO("Opprett sak_utbetaling rad")
            }
            TODO("Opprett utbetaling_request med detajer")
        }
    }

    fun simulerUtbetaling(dataSource: DataSource, nyTilkjentYtelse: TilkjentYtelse): Utbetalingsplan {
        val forrigBehandlingsreferanse = nyTilkjentYtelse.forrigeBehandlingsreferanse
        val forrigeTilkjentYtelse = if (forrigBehandlingsreferanse != null) {
            dataSource.transaction(readOnly = true) { connection ->
                TilkjentYtelseRepository(connection).hent(forrigBehandlingsreferanse) ?:
                    throw IllegalArgumentException("Angitt forrige behandlingsreferanse finnes ikke: $forrigBehandlingsreferanse")

            }
        } else {
            null
        }
        return UtbetalingsplanBeregner().tilkjentYtelseTilUtbetalingsplan(forrigeTilkjentYtelse, nyTilkjentYtelse)
    }

}
