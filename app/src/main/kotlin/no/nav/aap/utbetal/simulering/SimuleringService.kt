package no.nav.aap.utbetal.simulering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.utbetal.helved.tilUtbetalingMelding
import no.nav.aap.utbetal.klienter.helved.Simulering
import no.nav.aap.utbetal.klienter.helved.UtbetalingV2Klient
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingIdMap
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetaling
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import java.util.UUID

class SimuleringService(private val connection: DBConnection) {


    fun simuler(behandlingRef: UUID): Simulering {
        val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)
            ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingRef")

        val meldeperiodeUtbetalingMapping = finnMeldeperiodeUtbetalingMapping(tilkjentYtelse)
        val utbetalingMelding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMapping)
        return UtbetalingV2Klient().simuleringUtbetaling(utbetalingMelding)
    }

    private fun finnMeldeperiodeUtbetalingMapping(tilkjentYtelse: TilkjentYtelse): MeldeperiodeUtbetalingIdMap {
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(tilkjentYtelse.saksnummer)

        val meldeperiodeUtbetalingMap = if (sakUtbetaling != null) {
            MeldeperiodeUtbetalingMappingRepository(connection).hentMeldeperiodeUtbetalingMapping(sakUtbetaling.id!!).toMutableMap()
        } else {
            mutableMapOf()
        }

        tilkjentYtelse.perioder.forEach { tilkjentYtelsePeriode ->
            val meldeperiode = tilkjentYtelsePeriode.detaljer.meldeperiode
                ?: error("Meldeperiode må være satt for å kunne oppdatere meldeperiode utbetaling mapping")
            val lagretMeldeperiode = meldeperiodeUtbetalingMap
                .keys
                .firstOrNull { it.overlapper(meldeperiode) }
            if (lagretMeldeperiode == null) {
                val utbetalingId = UUID.randomUUID()
                meldeperiodeUtbetalingMap[meldeperiode] = utbetalingId
            }
        }
        return meldeperiodeUtbetalingMap.toMap()
    }

}