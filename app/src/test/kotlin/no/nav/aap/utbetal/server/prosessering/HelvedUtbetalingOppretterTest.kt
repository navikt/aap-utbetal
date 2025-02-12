package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class HelvedUtbetalingOppretterTest {

    @Test
    fun test1() {

        val tilkjentYtelse = opprettTilkjentYtelse(
            saksnummer = Saksnummer("123"),
            behandlingRef = UUID.randomUUID(),
            forrigeBehandlingRef = null,
            beløp = Beløp(1000),
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025,1, 31))
        )
        val utbetaling = HelvedUtbetalingOppretter().opprettUtbetaling(1L, tilkjentYtelse, Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 22)))

        println(utbetaling)

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
                gradering = Prosent.`0_PROSENT`,
                dagsats = beløp,
                grunnlag = beløp,
                grunnbeløp = Beløp(100000L) ,
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
            perioder = listOf(periode)
        )
    }


}