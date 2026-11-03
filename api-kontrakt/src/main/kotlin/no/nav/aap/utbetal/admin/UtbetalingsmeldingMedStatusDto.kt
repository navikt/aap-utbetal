package no.nav.aap.utbetal.admin

import no.nav.aap.utbetal.kodeverk.Status
import no.nav.aap.utbetal.kodeverk.UtbetalingsmeldingType
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingsmeldingMedStatusDto(
    val referanse: UUID,
    val utbetalingsmeldingType: UtbetalingsmeldingType,
    val melding: String,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val status: Status,
)
