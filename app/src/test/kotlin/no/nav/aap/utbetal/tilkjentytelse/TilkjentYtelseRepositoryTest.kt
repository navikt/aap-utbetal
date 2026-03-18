package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertNull

class TilkjentYtelseRepositoryTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()
    @Test
    fun `Finner ingen tilkjentYtelse dersom ingen er lagret`() {
        dataSource.transaction { connection ->
            val tilkjentYtelseId = TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(Saksnummer("123"))
            assertNull(tilkjentYtelseId)
        }
    }

    @Test
    fun `Finner siste tilkjentYtelse dersom bare førstegangsbehandling`() {
        val saksnummer = Saksnummer("123")
        val tilkjentYtelseId = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
                TilkjentYtelseTestUtil.opprettTilkjentYtelse(
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

        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef2, behandlingRef1)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef3, behandlingRef2)
        val tilkjentYtelseId4 = TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef4, behandlingRef3)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer2, behandlingRef5, null)

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

        // Fem tilkjent ytelse i litt random rekkefølge
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef4, behandlingRef3)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef5, behandlingRef4)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef2, behandlingRef1)
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef3, behandlingRef2)

        val tilkjentYtelseListe = dataSource.transaction { connection ->
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

        val avvent = TilkjentYtelseAvvent(
            fom = LocalDate.parse("2025-01-01"),
            tom = LocalDate.parse("2025-01-31"),
            overføres = LocalDate.parse("2025-01-31"),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null, avvent)

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

        val avvent = TilkjentYtelseAvvent(
            fom = LocalDate.parse("2025-01-01"),
            tom = LocalDate.parse("2025-01-31"),
            overføres = null,
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        TilkjentYtelseTestUtil.lagreTilkjentYtelse(dataSource, saksnummer1, behandlingRef1, null, avvent)

        val tilkjentYtelseMedAvvent = dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).hent(behandlingRef1)
        }
        assertThat(tilkjentYtelseMedAvvent!!.avvent).isNotNull()
        assertThat(tilkjentYtelseMedAvvent.avvent!!.fom).isEqualTo(avvent.fom)
        assertThat(tilkjentYtelseMedAvvent.avvent.tom).isEqualTo(avvent.tom)
        assertThat(tilkjentYtelseMedAvvent.avvent.overføres).isNull()
        assertThat(tilkjentYtelseMedAvvent.avvent.årsak).isEqualTo(avvent.årsak)

    }

}