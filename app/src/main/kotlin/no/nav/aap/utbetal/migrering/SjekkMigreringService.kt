package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository

class SjekkMigreringService(private val connection: DBConnection) {

    fun skalTilNyttGrensesnitt(fnr: String, saksnummer: Saksnummer): Boolean {
        //Safe-guard slik at disse ikke slipper ut i prod enda.
        if (Miljø.erProd()) {
            return false
        }

        // Sjekk migrering status i sak_utbetaling tabellen.
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer)
        if (sakUtbetaling != null) {
            // Send til nytt API dersom sak finnes fra før og den er migrert
            return sakUtbetaling.migrertTilKafka != null
        }

        //Ny sak, send den til nytt API
        return true
    }

}