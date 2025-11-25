package no.nav.aap.utbetal.admin

import no.nav.aap.utbetaling.UtbetalingStatus
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingStatusDto(
    val utbetalingerSomManglerKvittering: List<UtbetalingInfoDto>,
    val utbetalingerMedFeiletStatus: List<UtbetalingInfoDto>  ,
)

data  class UtbetalingInfoDto(
    val utbetalingRef: UUID,
    val saksnummer: String,
    val behandlingsreferanse: UUID,
    val utbetalingStatus: UtbetalingStatus,
    val utbetalingOpprettet: LocalDateTime,
    val utbetalingEndret: LocalDateTime?,

    )
