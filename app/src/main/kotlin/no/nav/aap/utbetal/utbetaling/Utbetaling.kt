package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetaling.UtbetalingDto
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Utbetaling(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val behandlingsreferanse: UUID,
    val tilkjentYtelseId: Long,
    val personIdent: String,
    val vedtakstidspunkt: LocalDateTime,
    val beslutterId: String,
    val saksbehandlerId: String,
    val utbetalingOversendt: LocalDateTime,
    val utbetalingEndret: LocalDateTime? = null,
    val utbetalingStatus: UtbetalingStatus,
    val perioder: List<Utbetalingsperiode>,
    val avvent: UtbetalingAvvent? = null,
    val utbetalingRef: UUID,
    val versjon: Long = 0L
) {
    fun tilUtbetalingDto(): UtbetalingDto {
        return UtbetalingDto(
            utbetalingOversendt = this.utbetalingOversendt,
            utbetalingBekreftet = this.utbetalingEndret,
            utbetalingStatus = this.utbetalingStatus,
            perioder = this.perioder.map { it.tilUtbetalingsperiodeDto() },
        )
    }
}

data class Utbetalingsperiode(
    val id: Long? = null,
    val periode: Periode,
    val beløp: UInt,
    val fastsattDagsats: UInt,
    val utbetalingsperiodeType: UtbetalingsperiodeType,
    val utbetalingsdato: LocalDate,
) {
    fun tilUtbetalingsperiodeDto() =
        UtbetalingsperiodeDto(
            fom = this.periode.fom,
            tom = this.periode.tom,
            beløp = this.beløp.toInt(),
            fastsattDagsats = this.fastsattDagsats.toInt(),
            utbetalingsperiodeType = this.utbetalingsperiodeType,
            utbetalingsdato = this.utbetalingsdato,
        )
}

data class UtbetalingAvvent(
    val fom: LocalDate,
    val tom: LocalDate,
    val overføres: LocalDate,
    val årsak: AvventÅrsak? = null,
    val feilregistrering: Boolean = false,
)