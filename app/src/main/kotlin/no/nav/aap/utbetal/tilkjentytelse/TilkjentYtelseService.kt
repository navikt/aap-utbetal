package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetaling
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import javax.sql.DataSource

private val utbetalingsdatoer:Set<Int> = setOf(10, 25)

class TilkjentYtelseService {

    fun lagre(dataSource: DataSource, tilkjentYtelse: TilkjentYtelse) {
        dataSource.transaction { connection ->
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
