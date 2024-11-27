package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import javax.sql.DataSource

class TilkjentYtelseService {

    fun lagre(dataSource: DataSource, tilkjentYtelse: TilkjentYtelseDto) {
        dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(tilkjentYtelse)
        }
    }

    fun simulerUtbetaling(dataSource: DataSource, nyTilkjentYtelse: TilkjentYtelseDto): Utbetalingsplan {
        val forrigBehandlingsreferanse = nyTilkjentYtelse.forrigeBehandlingsreferanse
        val forrigeTilkjentYtelse = if (forrigBehandlingsreferanse != null) {
            dataSource.transaction(readOnly = true) { connection ->
                TilkjentYtelseRepository(connection).hent(forrigBehandlingsreferanse) ?:
                    throw IllegalArgumentException("Angitt forrige behandlingsreferanse finnes ikke: $forrigBehandlingsreferanse")
            }
        } else null
        return UtbetalingsplanBeregner().tilkjentYtelseTilUtbetalingsplan(forrigeTilkjentYtelse, nyTilkjentYtelse)
    }

}
