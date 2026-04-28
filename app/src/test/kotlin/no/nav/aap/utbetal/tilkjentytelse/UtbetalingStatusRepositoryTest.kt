package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingError
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingLinje
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*
import kotlin.test.Test

class UtbetalingStatusRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()


    @Test
    fun `lagre og henter utbetalingstatus`() {
        val behandlingRef = UUID.randomUUID()
        opprettTilkjentYtelse(behandlingRef)


        dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)

            UtbetalingStatusRepository(connection).lagre(
                tilkjentYtelse!!,
                lagUtbetalingStatusHendelse(Status.HOS_OPPDRAG)
            )

            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus!!.status).isEqualTo(Status.HOS_OPPDRAG)
        }
    }


    @Test
    fun `oppdatere utbetalingstatus`() {
        val behandlingRef = UUID.randomUUID()
        opprettTilkjentYtelse(behandlingRef)

        dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)

            UtbetalingStatusRepository(connection).lagre(
                tilkjentYtelse!!,
                lagUtbetalingStatusHendelse(Status.HOS_OPPDRAG)
            )

            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus!!.status).isEqualTo(Status.HOS_OPPDRAG)

            UtbetalingStatusRepository(connection).lagre(
                tilkjentYtelse,
                lagUtbetalingStatusHendelse(Status.OK)
            )

            val oppdatertUtbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(oppdatertUtbetalingStatus).isNotNull()
            assertThat(utbetalingStatus.id).isNotEqualTo(oppdatertUtbetalingStatus!!.id)
            assertThat(oppdatertUtbetalingStatus.status).isEqualTo(Status.OK)
        }
    }

    @Test
    fun `utbetalingstatus med feilet status`() {
        val behandlingRef = UUID.randomUUID()
        opprettTilkjentYtelse(behandlingRef)

        dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)

            UtbetalingStatusRepository(connection).lagre(
                tilkjentYtelse!!,
                lagUtbetalingStatusHendelse(Status.FEILET)
            )

            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus!!.status).isEqualTo(Status.FEILET)
            assertThat(utbetalingStatus.httpStatusKode).isEqualTo(404)
            assertThat(utbetalingStatus.feilMelding).isEqualTo("Bug")
            assertThat(utbetalingStatus.dokumentasjonReferanse).isEqualTo("RTFM")
        }
    }

    private fun lagUtbetalingStatusHendelse(status: Status): UtbetalingStatusHendelse {
        return UtbetalingStatusHendelse(
            status = status,
            detaljer = UtbetalingDetaljer(
                ytelse = "AAP",
                linjer = listOf(
                    UtbetalingLinje(
                        behandlingId = UUID.randomUUID().toString(),
                        periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1)),
                        vedtakssats = 1000u,
                        beløp = 1000u,
                        klassekode = "AAP"
                    )
                )
            ),
            error = if (status == Status.FEILET) UtbetalingError(404, "Bug", "RTFM") else null
        )
    }

    private fun opprettTilkjentYtelse(behandlingRef: UUID): Long {
        val saksnummer = Saksnummer("123")
        return dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
                TilkjentYtelseTestUtil.opprettTilkjentYtelse(
                    saksnummer = saksnummer,
                    behandlingRef = behandlingRef,
                    forrigeBehandlingRef = null,
                    antallPerioder = 3,
                    beløp = Beløp(1000L),
                    startDato = LocalDate.now()
                )
            )
        }
    }

}