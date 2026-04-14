package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*
import kotlin.test.Test

class TilkjentYtelseRepositoryTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

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