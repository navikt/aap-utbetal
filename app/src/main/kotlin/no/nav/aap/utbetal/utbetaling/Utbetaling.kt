package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.klienter.helved.Avvent
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDateTime
import java.util.UUID

data class Utbetaling(
    val id: Long? = null,
    val utbetalingRef: UUID,
    val sakUtbetalingId: Long,
    val tilkjentYtelseId: Long,
    val utbetalingOversendt: LocalDateTime,
    val utbetalingBekreftet: LocalDateTime? = null,
    val utbetalingStatus: UtbetalingStatus,
    val perioder: List<Utbetalingsperiode>,
    val avvent: Avvent? = null,
)

data class Utbetalingsperiode(
    val id: Long? = null,
    val periode: Periode,
    val beløp: UInt,
    val fastsattDagsats: UInt,
    val utbetalingsperiodeType: UtbetalingsperiodeType
)
