package no.nav.aap.utbetal.admin

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class SaksnummerParameter(@param:PathParam("saksnummer") val saksnummer: String)