package no.nav.aap.utbetal.utbetaling

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test

class UtbetalingsmeldingRepositoryTest {

    private val objectMapper = ObjectMapper()
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `lagre og hente utbetalingsmelding`() {
        val utbetalingsmelding = Utbetalingsmelding(
            sakUtbetalingId = 17,
            tilkjentYtelseId = 42,
            referanse = java.util.UUID.randomUUID(),
            utbetalingsmeldingType = UtbetalingsmeldingType.UTBETALING,
            melding = """{"status":"SENDT","antall":1}""",
            opprettet = LocalDateTime.parse("2026-08-18T15:00:00"),
        )

        val lagretId = dataSource.transaction { connection ->
            UtbetalingsmeldingRepository(connection).lagre(utbetalingsmelding)
        }

        val hentet = dataSource.transaction { connection ->
            UtbetalingsmeldingRepository(connection).hent(utbetalingsmelding.referanse,)
        }

        assertThat(lagretId).isPositive
        assertThat(hentet!!.id).isEqualTo(lagretId)
        assertThat(hentet.sakUtbetalingId).isEqualTo(utbetalingsmelding.sakUtbetalingId)
        assertThat(hentet.tilkjentYtelseId).isEqualTo(utbetalingsmelding.tilkjentYtelseId)
        assertThat(hentet.referanse).isEqualTo(utbetalingsmelding.referanse)
        assertThat(hentet.utbetalingsmeldingType).isEqualTo(utbetalingsmelding.utbetalingsmeldingType)
        assertThat(objectMapper.readTree(hentet.melding)).isEqualTo(objectMapper.readTree(utbetalingsmelding.melding))
        assertThat(hentet.opprettet).isEqualTo(utbetalingsmelding.opprettet)
    }
}
