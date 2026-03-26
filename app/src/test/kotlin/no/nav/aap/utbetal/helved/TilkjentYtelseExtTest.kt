package no.nav.aap.utbetal.helved

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingIdMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilkjentYtelseExtTest {

    private val saksnummer = Saksnummer("sak123")
    private val behandlingsreferanse = UUID.randomUUID()
    private val personIdent = "01017012345"
    private val vedtakstidspunkt = LocalDateTime.now()

    @Test
    fun `perioder med redusertDagsats over 0 skal inkluderes`() {
        val meldeperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(1))
        val tilkjentYtelse = lagTilkjentYtelse(
            listOf(
                lagPeriode(
                    fom = meldeperiode.fom,
                    tom = meldeperiode.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(500),
                    utbetalingsdato = LocalDate.now().minusDays(1),
                    meldeperiode = meldeperiode,
                )
            )
        )
        val meldeperiodeUtbetalingMap = lagMeldeperiodeMap(meldeperiode)

        val melding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        assertThat(melding.utbetalinger).hasSize(1)
        assertThat(melding.utbetalinger.first().utbetaltBeløp).isEqualTo(500u)
        assertThat(melding.utbetalinger.first().sats).isEqualTo(1000u)
    }

    @Test
    fun `perioder med redusertDagsats lik 0 skal ikke inkluderes`() {
        val meldeperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(1))
        val tilkjentYtelse = lagTilkjentYtelse(
            listOf(
                lagPeriode(
                    fom = meldeperiode.fom,
                    tom = meldeperiode.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(0),
                    utbetalingsdato = LocalDate.now().minusDays(1),
                    meldeperiode = meldeperiode,
                )
            )
        )
        val meldeperiodeUtbetalingMap = lagMeldeperiodeMap(meldeperiode)

        val melding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        assertThat(melding.utbetalinger).isEmpty()
    }

    @Test
    fun `perioder med utbetalingsdato frem i tid skal ikke inkluderes`() {
        val meldeperiode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(14))
        val tilkjentYtelse = lagTilkjentYtelse(
            listOf(
                lagPeriode(
                    fom = meldeperiode.fom,
                    tom = meldeperiode.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(500),
                    utbetalingsdato = LocalDate.now().plusDays(1),
                    meldeperiode = meldeperiode,
                )
            )
        )
        val meldeperiodeUtbetalingMap = lagMeldeperiodeMap(meldeperiode)

        val melding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        assertThat(melding.utbetalinger).isEmpty()
    }

    @Test
    fun `perioder med utbetalingsdato i dag skal inkluderes`() {
        val meldeperiode = Periode(LocalDate.now().minusDays(13), LocalDate.now())
        val tilkjentYtelse = lagTilkjentYtelse(
            listOf(
                lagPeriode(
                    fom = meldeperiode.fom,
                    tom = meldeperiode.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(800),
                    utbetalingsdato = LocalDate.now(),
                    meldeperiode = meldeperiode,
                )
            )
        )
        val meldeperiodeUtbetalingMap = lagMeldeperiodeMap(meldeperiode)

        val melding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        assertThat(melding.utbetalinger).hasSize(1)
        assertThat(melding.utbetalinger.first().utbetaltBeløp).isEqualTo(800u)
    }

    @Test
    fun `bare perioder som oppfyller begge krav skal inkluderes`() {
        val meldeperiodeFortidig = Periode(LocalDate.now().minusDays(28), LocalDate.now().minusDays(15))
        val meldeperiodeNylig = Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(1))
        val meldeperiodeFremtidig = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(14))

        val tilkjentYtelse = lagTilkjentYtelse(
            listOf(
                // Skal med: redusertDagsats > 0 og utbetalingsdato i fortid
                lagPeriode(
                    fom = meldeperiodeFortidig.fom,
                    tom = meldeperiodeFortidig.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(500),
                    utbetalingsdato = LocalDate.now().minusDays(1),
                    meldeperiode = meldeperiodeFortidig,
                ),
                // Skal IKKE med: redusertDagsats = 0
                lagPeriode(
                    fom = meldeperiodeNylig.fom,
                    tom = meldeperiodeNylig.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(0),
                    utbetalingsdato = LocalDate.now().minusDays(1),
                    meldeperiode = meldeperiodeNylig,
                ),
                // Skal IKKE med: utbetalingsdato i fremtiden
                lagPeriode(
                    fom = meldeperiodeFremtidig.fom,
                    tom = meldeperiodeFremtidig.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(500),
                    utbetalingsdato = LocalDate.now().plusDays(1),
                    meldeperiode = meldeperiodeFremtidig,
                ),
            )
        )

        val meldeperiodeUtbetalingMap = mapOf(
            meldeperiodeFortidig to UUID.randomUUID(),
            meldeperiodeNylig to UUID.randomUUID(),
            meldeperiodeFremtidig to UUID.randomUUID(),
        )

        val melding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        assertThat(melding.utbetalinger).hasSize(1)
        assertThat(melding.utbetalinger.first().fom).isEqualTo(meldeperiodeFortidig.fom.toString())
        assertThat(melding.utbetalinger.first().tom).isEqualTo(meldeperiodeFortidig.tom.toString())
    }

    @Test
    fun `utbetalingMelding mapper felter korrekt`() {
        val meldeperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(1))
        val tilkjentYtelse = lagTilkjentYtelse(
            listOf(
                lagPeriode(
                    fom = meldeperiode.fom,
                    tom = meldeperiode.tom,
                    dagsats = Beløp(1000),
                    redusertDagsats = Beløp(800),
                    utbetalingsdato = LocalDate.now().minusDays(1),
                    meldeperiode = meldeperiode,
                )
            )
        )
        val meldeperiodeUtbetalingMap = lagMeldeperiodeMap(meldeperiode)

        val melding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        assertThat(melding.sakId).isEqualTo(saksnummer.toString())
        assertThat(melding.ident).isEqualTo(personIdent)
        assertThat(melding.saksbehandler).isEqualTo("saksbehandler1")
        assertThat(melding.beslutter).isEqualTo("beslutter1")
        assertThat(melding.vedtakstidspunktet).isEqualTo(vedtakstidspunkt)
        assertThat(melding.avvent).isNull()
    }

    private fun lagTilkjentYtelse(perioder: List<TilkjentYtelsePeriode>): TilkjentYtelse {
        return TilkjentYtelse(
            id = 1L,
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingsreferanse,
            forrigeBehandlingsreferanse = null,
            personIdent = personIdent,
            vedtakstidspunkt = vedtakstidspunkt,
            beslutterId = "beslutter1",
            saksbehandlerId = "saksbehandler1",
            perioder = perioder,
            avvent = null,
        )
    }

    private fun lagPeriode(
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Beløp,
        redusertDagsats: Beløp,
        utbetalingsdato: LocalDate,
        meldeperiode: Periode,
    ): TilkjentYtelsePeriode {
        return TilkjentYtelsePeriode(
            periode = Periode(fom, tom),
            detaljer = YtelseDetaljer(
                redusertDagsats = redusertDagsats,
                gradering = Prosent(100),
                dagsats = dagsats,
                grunnlag = dagsats,
                grunnlagsfaktor = GUnit(6),
                grunnbeløp = Beløp(100000),
                antallBarn = 0,
                barnetilleggsats = Beløp(0),
                barnetillegg = Beløp(0),
                utbetalingsdato = utbetalingsdato,
                meldeperiode = meldeperiode,
                barnepensjonDagsats = Beløp(0),
            )
        )
    }

    private fun lagMeldeperiodeMap(meldeperiode: Periode): MeldeperiodeUtbetalingIdMap {
        return mapOf(meldeperiode to UUID.randomUUID())
    }
}

