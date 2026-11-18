package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository

class SjekkMigreringService(private val connection: DBConnection) {

    fun skalTilNyttGrensesnitt(fnr: String, saksnummer: Saksnummer): Boolean {
        // Sjekk migrering status i sak_utbetaling tabellen.
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer)

        if (sakUtbetaling != null) {
            // Send til nytt API dersom sak finnes fra før og den er migrert
            return sakUtbetaling.migrertTilKafka != null
        }

        // Ruter til gammelt api dersom saksnummer starter med "APITEST_". Brukes av ApiTest.kt for å teste logikk for gammelt api.
        if (saksnummer.toString().startsWith("APITEST_")) {
            return false
        }

        // Alle andre saker går til nytt api
        return true
    }

}