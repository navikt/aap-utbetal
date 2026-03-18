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

    companion object {
        private val whitelisteMigrerteFødselsnummer = setOf(
            "29509000997",
            "01509033583",
            "30519028723",
            "15419137747",
            "13429149309",
            "04509024311",
            "31529003245",
            "06509025930",
            "14529037059",
            "26529003764",
        )
    }

}