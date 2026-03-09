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

    private val whitelisteMigrerteFødselsnummer = setOf<String>(
        //TODO: Hent liste fra dolly
    )

}