package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*
import kotlin.test.Test

class MeldeperiodeUtbetalingMappingRepositoryTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `hent returnerer null når mapping ikke finnes`() {
        val sakUtbetalingId = 123L

        val mapping = dataSource.transaction { connection ->
            MeldeperiodeUtbetalingMappingRepository(connection).hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
        }

        assertThat(mapping).isEmpty()
    }

    @Test
    fun `lagre og hent lagrer og henter mapping`() {
        val sakUtbetalingId = 123L
        val periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        val utbetalingId = UUID.randomUUID()

        dataSource.transaction { connection ->
            MeldeperiodeUtbetalingMappingRepository(connection).lagreMeldeperiodeUtbetalingMapping(sakUtbetalingId, periode, utbetalingId)
        }

        val lagret = dataSource.transaction { connection ->
            MeldeperiodeUtbetalingMappingRepository(connection).hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
        }

        assertThat(lagret[periode]).isEqualTo(utbetalingId)
    }
}