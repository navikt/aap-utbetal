package no.nav.aap.utbetal.utbetaling

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.kodeverk.Status
import no.nav.aap.utbetal.kodeverk.UtbetalingsmeldingType
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
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
            UtbetalingsmeldingRepository(connection).hent(utbetalingsmelding.referanse)
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


    @Test
    fun `hent utbetalingsmeldinger for saksnummer`() {
        val lagUtbetalingsmelding = fun (connection: DBConnection, sakUtbetalingId: Long) {
            val behandlingRef = java.util.UUID.randomUUID()
            val sakUtbetaling = SakUtbetalingRepository(connection).hent(sakUtbetalingId)
            val tilkjentYtelseId = TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
                TilkjentYtelse(
                    saksnummer = sakUtbetaling.saksnummer,
                    behandlingsreferanse = behandlingRef,
                    personIdent = sakUtbetaling.saksnummer.toString() + "_fnr",
                    vedtakstidspunkt = LocalDateTime.now(),
                    beslutterId = "abc",
                    saksbehandlerId = "abc",
                    perioder = emptyList(),
                )
            )
            val utbetalingsmelding = Utbetalingsmelding(
                sakUtbetalingId = sakUtbetalingId,
                tilkjentYtelseId = tilkjentYtelseId,
                referanse = behandlingRef,
                utbetalingsmeldingType = UtbetalingsmeldingType.UTBETALING,
                melding = """{"status":"SENDT","antall":1}""",
                opprettet = LocalDateTime.now(),
            )

            UtbetalingsmeldingRepository(connection).lagre(utbetalingsmelding)
            UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatus(
                tilkjentYtelseId = tilkjentYtelseId,
                referanse = behandlingRef,
                utbetalingStatusHendelse = UtbetalingStatusHendelse(Status.OK),
                statusEndringTidspunkt = LocalDateTime.now(),
            )
        }

        dataSource.transaction { connection ->
            val sakUtbetalingId1 = SakUtbetalingRepository(connection).lagre(Saksnummer("ABC123"), true)
            val sakUtbetalingId2 = SakUtbetalingRepository(connection).lagre(Saksnummer("ABC456"), true)

            lagUtbetalingsmelding(connection, sakUtbetalingId1)
            lagUtbetalingsmelding(connection, sakUtbetalingId1)
            lagUtbetalingsmelding(connection, sakUtbetalingId2)
            lagUtbetalingsmelding(connection, sakUtbetalingId1)
        }

        dataSource.transaction(readOnly = true) { connection ->
            val repo =UtbetalingsmeldingRepository(connection)
            val utbetalingsmeldingerSak1 = repo.hent(Saksnummer("ABC123"))
            val utbetalingsmeldingerSak2 = repo.hent(Saksnummer("ABC456"))
            assertThat(utbetalingsmeldingerSak1).hasSize(3)
            assertThat(utbetalingsmeldingerSak2).hasSize(1)
        }
    }

}
