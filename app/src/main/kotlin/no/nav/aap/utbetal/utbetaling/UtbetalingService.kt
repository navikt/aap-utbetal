package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import java.util.UUID

class UtbetalingService(private val connection: DBConnection) {


    fun opprettUtbetaling(saksnummer: Saksnummer, behandlingsreferanse: UUID, periode: Periode): Long {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val forrigeTilkjentYtelse = nyTilkjentYtelse.forrigeBehandlingsreferanse?.let {tilkjentYtelseRepo.hent(it)}
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, forrigeTilkjentYtelse, nyTilkjentYtelse)


        return UtbetalingRepository(connection).lagre(utbetaling)
    }

}