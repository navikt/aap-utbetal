package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository

private val FNR_SOM_SKAL_TIL_GAMMELT_API = listOf(
    "22498627300", "05478624672", "03468629177", "26488627790", "16508638918"
)

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
            if (fnr in FNR_SOM_SKAL_TIL_GAMMELT_API) {
                return false
            }
            // Alle saker til nytt api dersom ikke i prod
            return true
        }
    }

}