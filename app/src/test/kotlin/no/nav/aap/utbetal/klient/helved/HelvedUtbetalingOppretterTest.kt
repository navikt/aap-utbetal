package no.nav.aap.utbetal.klient.helved

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.klienter.helved.HelvedUtbetalingOppretter
import no.nav.aap.utbetal.klienter.helved.Utbetalingsperiode
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class HelvedUtbetalingOppretterTest {

    @Test
    fun `Vanlig 14 dagers utbetaling`() {
        val utbetaling = Utbetaling(
            saksnummer = Saksnummer("123"),
            behandlingsreferanse = UUID.randomUUID(),
            utbetalingRef = UUID.randomUUID(),
            sakUtbetalingId = 123L,
            tilkjentYtelseId = 456L,
            personIdent = "12345612345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "abc1234",
            saksbehandlerId = "abc4321",
            utbetalingOversendt = LocalDateTime.now(),
            utbetalingStatus = UtbetalingStatus.OPPRETTET,
            perioder = listOf(
                no.nav.aap.utbetal.utbetaling.Utbetalingsperiode(
                    periode = Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 10)),
                    beløp = 1000.toUInt(),
                    fastsattDagsats = 1000.toUInt(),
                    utbetalingsperiodeType = UtbetalingsperiodeType.NY
                ),
                no.nav.aap.utbetal.utbetaling.Utbetalingsperiode(
                    periode = Periode(LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 17)),
                    beløp = 1000.toUInt(),
                    fastsattDagsats = 1000.toUInt(),
                    utbetalingsperiodeType = UtbetalingsperiodeType.NY
                ),
                no.nav.aap.utbetal.utbetaling.Utbetalingsperiode(
                    periode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 1, 21)),
                    beløp = 1000.toUInt(),
                    fastsattDagsats = 1000.toUInt(),
                    utbetalingsperiodeType = UtbetalingsperiodeType.NY
                )
            )
        )

        val helvedUtbetaling = HelvedUtbetalingOppretter().opprettUtbetaling(utbetaling)

        assertThat(helvedUtbetaling.perioder).hasSize(10)
        val perioder = helvedUtbetaling.perioder
        perioder.sjekkBeløp(0, LocalDate.of(2025, 1, 8), 1000.toUInt())
        perioder.sjekkBeløp(1, LocalDate.of(2025, 1, 9), 1000.toUInt())
        perioder.sjekkBeløp(2, LocalDate.of(2025, 1, 10), 1000.toUInt())
        perioder.sjekkBeløp(3, LocalDate.of(2025, 1, 13), 1000.toUInt())
        perioder.sjekkBeløp(4, LocalDate.of(2025, 1, 14), 1000.toUInt())
        perioder.sjekkBeløp(5, LocalDate.of(2025, 1, 15), 1000.toUInt())
        perioder.sjekkBeløp(6, LocalDate.of(2025, 1, 16), 1000.toUInt())
        perioder.sjekkBeløp(7, LocalDate.of(2025, 1, 17), 1000.toUInt())
        perioder.sjekkBeløp(8, LocalDate.of(2025, 1, 20), 1000.toUInt())
        perioder.sjekkBeløp(9, LocalDate.of(2025, 1, 21), 1000.toUInt())
    }

    fun List<Utbetalingsperiode>.sjekkBeløp(index: Int, dato: LocalDate, beløp: UInt) {
        val periode = this[index]
        assertThat(periode.fom).isEqualTo(dato)
        assertThat(periode.tom).isEqualTo(dato)
        assertThat(this[index].beløp).isEqualTo(beløp)
        assertThat(this[index].fastsattDagsats).isEqualTo(beløp)
    }

}