package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertNull

class TilkjentYtelseRepositoryTest {

    @Test
    fun `Finner ingen tilkjentYtelse dersom ingen er lagret`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val tilkjentYtelseId = TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(Saksnummer("123"))
            assertNull(tilkjentYtelseId)
        }
    }

    @Test
    fun `Finner siste tilkjentYtelse dersom bare førstegangsbehandling`() {
        val saksnummer = Saksnummer("123")
        val dataSource = InitTestDatabase.freshDatabase()
        val tilkjentYtelseId = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(
                opprettTilkjentYtelse(
                    saksnummer = saksnummer,
                    behandlingRef = UUID.randomUUID(),
                    forrigeBehandlingRef = null,
                    antallPerioder = 3,
                    beløp = Beløp(1000L),
                    startDato = LocalDate.now()
                )
            )
        }
        val funnetTilkjentYtelseId = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(saksnummer)
        }
        assertThat(funnetTilkjentYtelseId).isEqualTo(tilkjentYtelseId)
    }


    @Test
    fun `Finner siste tilkjentYtelse dersom 4 behandler på samme saksnummer`() {
        val behandlingRef1 = UUID.randomUUID()
        val behandlingRef2 = UUID.randomUUID()
        val behandlingRef3 = UUID.randomUUID()
        val behandlingRef4 = UUID.randomUUID()
        val behandlingRef5 = UUID.randomUUID()
        val saksnummer1 = Saksnummer("123")
        val saksnummer2 = Saksnummer("456")

        val dataSource = InitTestDatabase.freshDatabase()

        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null)
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef2, behandlingRef1)
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef3, behandlingRef2)
        val tilkjentYtelseId4 = lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef4, behandlingRef3)
        lagreTilkjentYtelse(dataSource, saksnummer2, behandlingRef5, null)

        val funnetTilkjentYtelseId = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(saksnummer1)
        }
        assertThat(funnetTilkjentYtelseId).isEqualTo(tilkjentYtelseId4)
    }

    @Test
    fun `Kan hente tilkjent ytelse i riktig rekkefølge`() {
        val saksnummer1 = Saksnummer("123")
        val behandlingRef1 = UUID.randomUUID()
        val behandlingRef2 = UUID.randomUUID()
        val behandlingRef3 = UUID.randomUUID()
        val behandlingRef4 = UUID.randomUUID()
        val behandlingRef5 = UUID.randomUUID()

        val dataSource = InitTestDatabase.freshDatabase()
        // Fem tilkjent ytelse i litt random rekkefølge
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null)
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef4, behandlingRef3)
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef5, behandlingRef4)
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef2, behandlingRef1)
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef3, behandlingRef2)

        val tilkjentYtelseListe = InitTestDatabase.freshDatabase().transaction { connection ->
            TilkjentYtelseRepository(connection).finnRekkefølgeTilkjentYtelse(saksnummer1)
        }

        tilkjentYtelseListe.forEachIndexed {index, ty ->
            if (index == 0) {
                assertThat(ty.forrigeBehandlingRef).isNull()
            } else {
                assertThat(ty.forrigeBehandlingRef).isNotNull()
                assertThat(ty.forrigeBehandlingRef).isEqualTo(tilkjentYtelseListe[index-1].behandlingRef)
            }
        }
    }

    @Test
    fun `Lagre tilkjent ytelse med avvent utbetaling`() {
        val behandlingRef1 = UUID.randomUUID()
        val saksnummer1 = Saksnummer("123")

        val dataSource = InitTestDatabase.freshDatabase()

        val avvent = TilkjentYtelseAvvent(
            fom = LocalDate.parse("2025-01-01"),
            tom = LocalDate.parse("2025-01-31"),
            overføres = LocalDate.parse("2025-01-31"),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null, avvent)

        val tilkjentYtelseMedAvvent = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).hent(behandlingRef1)
        }
        assertThat(tilkjentYtelseMedAvvent!!.avvent).isNotNull()
        assertThat(tilkjentYtelseMedAvvent.avvent!!.fom).isEqualTo(avvent.fom)
        assertThat(tilkjentYtelseMedAvvent.avvent.tom).isEqualTo(avvent.tom)
        assertThat(tilkjentYtelseMedAvvent.avvent.overføres).isEqualTo(avvent.overføres)
        assertThat(tilkjentYtelseMedAvvent.avvent.årsak).isEqualTo(avvent.årsak)
    }

    @Test
    fun `Lagre tilkjent ytelse med avvent utbetaling uten overføresdato`() {
        val behandlingRef1 = UUID.randomUUID()
        val saksnummer1 = Saksnummer("123")

        val dataSource = InitTestDatabase.freshDatabase()

        val avvent = TilkjentYtelseAvvent(
            fom = LocalDate.parse("2025-01-01"),
            tom = LocalDate.parse("2025-01-31"),
            overføres = null,
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null, avvent)

        val tilkjentYtelseMedAvvent = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).hent(behandlingRef1)
        }
        assertThat(tilkjentYtelseMedAvvent!!.avvent).isNotNull()
        assertThat(tilkjentYtelseMedAvvent.avvent!!.fom).isEqualTo(avvent.fom)
        assertThat(tilkjentYtelseMedAvvent.avvent.tom).isEqualTo(avvent.tom)
        assertThat(tilkjentYtelseMedAvvent.avvent.overføres).isNull()
        assertThat(tilkjentYtelseMedAvvent.avvent.årsak).isEqualTo(avvent.årsak)

    }


    private fun lagreTilkjentYtelse(
        dataSource: DataSource,
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID?,
        avvent: TilkjentYtelseAvvent? = null,
    ): Long {
        return dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(
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

    private fun opprettTilkjentYtelse(
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID?,
        antallPerioder: Int,
        beløp: Beløp,
        startDato: LocalDate,
        avvent: TilkjentYtelseAvvent? = null,
    ): TilkjentYtelse {
        val perioder = (0 until antallPerioder).map {
            TilkjentYtelsePeriode(
                periode = Periode(startDato.plusWeeks(it * 2L), startDato.plusWeeks(it * 2L).plusDays(13)),
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
                    utbetalingsdato = startDato.plusWeeks(it * 2L).plusDays(14)
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