package no.nav.aap.utbetal.trekk

import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class TrekkRequestDto(@JsonValue @param:PathParam("saksnummer") val saksnummer: String) {
    override fun toString(): String {
        return saksnummer
    }
}