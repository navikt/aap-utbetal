package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

object TilkjentYtelseTestUtil {

    fun lagreTilkjentYtelse(
        dataSource: DataSource,
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID?,
        avvent: TilkjentYtelseAvvent? = null,
    ): Long {
        return dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
                opprettTilkjentYtelse(
                    saksnummer = saksnummer,
                    behandlingRef = behandlingRef,
                    forrigeBehandlingRef = forrigeBehandlingRef,
                    antallPerioder = 5,
                    beløp = Beløp(1000L),
                    startDato = LocalDate.now(),
                    avvent = avvent,
                )
            )
        }
    }

    fun opprettTilkjentYtelse(
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID?,
        antallPerioder: Int,
        beløp: Beløp,
        startDato: LocalDate,
        avvent: TilkjentYtelseAvvent? = null,
    ): TilkjentYtelse {
        val perioder = (0 until antallPerioder).map {
            val periode = Periode(startDato.plusWeeks(it * 2L), startDato.plusWeeks(it * 2L).plusDays(13))
            TilkjentYtelsePeriode(
                periode = periode,
                YtelseDetaljer(
                    gradering = Prosent.`0_PROSENT`,
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = Beløp(100000L),
                    antallBarn = 0,
                    barnetillegg = Beløp(0L),
                    grunnlagsfaktor = GUnit("0.008"),
                    barnetilleggsats = Beløp(36L),
                    redusertDagsats = beløp,
                    utbetalingsdato = startDato.plusWeeks(it * 2L).plusDays(14),
                    meldeperiode = periode,
                    barnepensjonDagsats = Beløp(0)
                )
            )
        }
        return TilkjentYtelse(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingRef,
            forrigeBehandlingsreferanse = forrigeBehandlingRef,
            personIdent = "12345123456",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder,
            avvent = avvent,
        )
    }


}