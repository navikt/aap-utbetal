package no.nav.aap.utbetal.admin

import no.nav.aap.utbetal.kodeverk.Status

data class UtbetalingStatusAntallDto(
    val antallPerStatus: Map<Status, Int>,
)