package no.nav.aap.utbetal.trekk

import java.time.LocalDate
import java.util.UUID

data class TrekkResponsDto(val trekkListe: List<TrekkDto>)

data class TrekkDto(val saksnummer: String,
                    val behandlingsreferanse: UUID,
                    val dato: LocalDate,
                    val beløp: Int,
                    val aktiv: Boolean,
                    val posteringer: List<TrekkPosteringDto>)

data class TrekkPosteringDto(
    val dato: LocalDate,
    val beløp: Int
)





