package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository

class SjekkMigreringService(private val connection: DBConnection) {

    fun skalTilNyttGrensesnitt(fnr: String): Boolean {
        //Safe-guard slik at disse ikke slipper ut i prod enda.
        if (Miljø.erProd()) {
            return false
        }

        // Sjekk migrering status i sak_utbetaling tabellen.
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(Saksnummer(fnr))
        if (sakUtbetaling != null && sakUtbetaling.migrertTilKafka != null) {
            return true
        }

        // Sjekk whitelist
        if (whitelisteMigrerteFødselsnummer.contains(fnr)) {
            return true
        }

        return false
    }

    // Dette er fnr som brukes til testing av nytt utbetalings-api. Skal bare brukes i test, men må ligge i prod scope av kode.
    // Slettes når testing er fullført.
    private val whitelisteMigrerteFødselsnummer = setOf<String>(
        "29509000997",
        "01509033583",
        "30519028723",
        "15419137747",
        "13429149309",
        "04509024311",
        "31529003245",
        "06509025930",
        "26529003764",
        "25519006025",
        "26429121166",
        "24429119320",
        "30499012639",
        "23509035938",
        "27499011909",
        "19429108155",
        "17429139120",
        "05419107273",
        "23419103306",
        "25510151472",
        "11510175907",
        "28420256681",
        "30520157057",
        "16430285231",
        "20430295099",
        "08520188070",
        "06450266866",
        "13440262575",
        "15520163031",
        "11440185659",
        "10440190605",
    )

}