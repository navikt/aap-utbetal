package no.nav.aap.utbetal.klient.helved

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.klienter.helved.HelvedUtbetalingOppretter
import no.nav.aap.utbetal.klienter.helved.Utbetalingsperiode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class HelvedUtbetalingOppretterTest {

    @Test
    fun `Vanlig 14 dagers utbetaling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(
            saksnummer = Saksnummer("123"),
            behandlingRef = UUID.randomUUID(),
            forrigeBehandlingRef = null,
            beløp = Beløp(1000),
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        )

        val utbetaling = HelvedUtbetalingOppretter().opprettUtbetaling(
            tilkjentYtelse = tilkjentYtelse,
            periode = Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 21))
        )

        Assertions.assertThat(utbetaling.perioder).hasSize(10)
        val perioder = utbetaling.perioder
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
        Assertions.assertThat(periode.fom).isEqualTo(dato)
        Assertions.assertThat(periode.tom).isEqualTo(dato)
        Assertions.assertThat(this[index].beløp).isEqualTo(beløp)
        Assertions.assertThat(this[index].fastsattDagsats).isEqualTo(beløp)
    }

    private fun opprettTilkjentYtelse(
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID?,
        beløp: Beløp,
        periode: Periode
    ): TilkjentYtelse {
        val periode = TilkjentYtelsePeriode(
            periode = periode,
            YtelseDetaljer(
                gradering = Prosent.Companion.`0_PROSENT`,
                dagsats = beløp,
                grunnlag = beløp,
                grunnbeløp = Beløp(100000L),
                antallBarn = 0,
                barnetillegg = Beløp(0L),
                grunnlagsfaktor = GUnit("0.008"),
                barnetilleggsats = Beløp(36L),
                redusertDagsats = beløp,
                ventedagerSamordning = false,
            )
        )
        return TilkjentYtelse(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingRef,
            forrigeBehandlingsreferanse = forrigeBehandlingRef,
            personIdent = "12345612345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = listOf(periode)
        )
    }

}