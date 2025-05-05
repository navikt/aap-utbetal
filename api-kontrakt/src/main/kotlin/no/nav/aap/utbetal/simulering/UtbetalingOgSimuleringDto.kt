package no.nav.aap.utbetal.simulering

import no.nav.aap.utbetaling.UtbetalingDto

data class UtbetalingOgSimuleringDto(
    val utbetalingDto: UtbetalingDto,
    val simuleringDto: SimuleringDto,
)