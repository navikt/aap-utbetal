package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository

class SjekkMigreringService(private val connection: DBConnection) {

    fun skalTilNyttGrensesnitt(fnr: String, saksnummer: Saksnummer): Boolean {
        // Sjekk migrering status i sak_utbetaling tabellen.
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer)

        if (sakUtbetaling != null) {
            // Send til nytt API dersom sak finnes fra før og den er migrert
            return sakUtbetaling.migrertTilKafka != null
        }

        if (Miljø.erProd()) {
            // Ikke send nye saker til nytt api enda
            return false
        } else {
            // Alle saker til nytt api dersom ikke i prod
            return true
        }
    }

}