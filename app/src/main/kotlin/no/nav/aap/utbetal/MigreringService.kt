package no.nav.aap.utbetal

import no.nav.aap.komponenter.miljo.Miljø

class MigreringService {

    fun skalTilNyttGrensesnitt(fnr: String): Boolean {
        if (Miljø.erProd()) {
            //Safe-guard slik at disse ikke slipper ut i prod enda.
            return false
        }
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
    )

}