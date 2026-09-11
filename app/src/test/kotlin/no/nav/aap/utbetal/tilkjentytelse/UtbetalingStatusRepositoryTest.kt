package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingError
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingLinje
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.utbetaling.Utbetalingsmelding
import no.nav.aap.utbetal.utbetaling.UtbetalingsmeldingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingsmeldingType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
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
        val tilkjentYtelse = opprettTilkjentYtelse(behandlingRef)
        opprettUtbetalingsmelding(tilkjentYtelse.id!!, behandlingRef)

        dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)
                ?: throw IllegalStateException("Finner ikke tilkjent ytelse for behandling: $behandlingRef")

            UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatusV2(
                tilkjentYtelseId = tilkjentYtelse.id!!,
                referanse = behandlingRef,
                utbetalingStatusHendelse = lagUtbetalingStatusHendelse(Status.HOS_OPPDRAG)
            )

            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus!!.status).isEqualTo(Status.HOS_OPPDRAG)
        }
    }


    @Test
    fun `oppdatere utbetalingstatus`() {
        val behandlingRef = UUID.randomUUID()
        val tilkjentYtelse = opprettTilkjentYtelse(behandlingRef)
        opprettUtbetalingsmelding(tilkjentYtelse.id!!, behandlingRef)

        dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)
                ?: throw IllegalStateException("Finner ikke tilkjent ytelse for behandling: $behandlingRef")

            UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatusV2(
                tilkjentYtelseId = tilkjentYtelse.id!!,
                referanse = behandlingRef,
                utbetalingStatusHendelse = lagUtbetalingStatusHendelse(Status.HOS_OPPDRAG)
            )

            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus!!.status).isEqualTo(Status.HOS_OPPDRAG)

            UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatusV2(
                tilkjentYtelseId = tilkjentYtelse.id,
                referanse = behandlingRef,
                utbetalingStatusHendelse = lagUtbetalingStatusHendelse(Status.OK)
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
        val tilkjentYtelse = opprettTilkjentYtelse(behandlingRef)
        opprettUtbetalingsmelding(tilkjentYtelse.id!!, behandlingRef)

        dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)
                ?: throw IllegalStateException("Finner ikke tilkjent ytelse for behandling: $behandlingRef")

            UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatusV2(
                tilkjentYtelseId = tilkjentYtelse.id!!,
                referanse = behandlingRef,
                utbetalingStatusHendelse = lagUtbetalingStatusHendelse(Status.FEILET)
            )

            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(tilkjentYtelse.behandlingsreferanse)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus!!.status).isEqualTo(Status.FEILET)
            assertThat(utbetalingStatus.httpStatusKode).isEqualTo(404)
            assertThat(utbetalingStatus.feilMelding).isEqualTo("Bug")
            assertThat(utbetalingStatus.dokumentasjonReferanse).isEqualTo("RTFM")
        }
    }

    @Test
    fun `finn antall utbetalinger per status`() {
        val ty1 = opprettTilkjentYtelse(UUID.randomUUID())
        val ty2 = opprettTilkjentYtelse(UUID.randomUUID())
        val ty3 = opprettTilkjentYtelse(UUID.randomUUID())
        val ty4 = opprettTilkjentYtelse(UUID.randomUUID())
        val ty5 = opprettTilkjentYtelse(UUID.randomUUID())
        opprettUtbetalingsmelding(ty1.id!!, ty1.behandlingsreferanse,)
        opprettUtbetalingsmelding(ty2.id!!, ty2.behandlingsreferanse,)
        opprettUtbetalingsmelding(ty3.id!!, ty3.behandlingsreferanse,)
        opprettUtbetalingsmelding(ty4.id!!, ty4.behandlingsreferanse,)
        opprettUtbetalingsmelding(ty5.id!!, ty5.behandlingsreferanse,)

        val oppdaterStatus = fun (tilkjentYtelse: TilkjentYtelse, status: Status) {
            dataSource.transaction { connection ->
                UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatusV2(
                    tilkjentYtelseId = tilkjentYtelse.id!!,
                    referanse = tilkjentYtelse.behandlingsreferanse,
                    utbetalingStatusHendelse = lagUtbetalingStatusHendelse(status)
                )
            }
        }

        oppdaterStatus(ty1, Status.MOTTATT)

        oppdaterStatus(ty2, Status.MOTTATT)
        oppdaterStatus(ty2, Status.OK)

        oppdaterStatus(ty3, Status.FEILET)
        oppdaterStatus(ty4, Status.FEILET)
        oppdaterStatus(ty5, Status.FEILET)

        val antallPerStatus = dataSource.transaction { connection ->
            UtbetalingStatusRepository(connection).antallUtbetalingerPerStatus()
        }

        assertThat(antallPerStatus.keys).hasSize(3)
        assertThat(antallPerStatus[Status.MOTTATT]).isEqualTo(1)
        assertThat(antallPerStatus[Status.OK]).isEqualTo(1)
        assertThat(antallPerStatus[Status.FEILET]).isEqualTo(3)
    }


    private fun lagUtbetalingStatusHendelse(status: Status): UtbetalingStatusHendelse {
        return UtbetalingStatusHendelse(
            status = status,
            detaljer = UtbetalingDetaljer(
                ytelse = "AAP",
                linjer = listOf(
                    UtbetalingLinje(
                        behandlingId = UUID.randomUUID().toString(),
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusMonths(1),
                        vedtakssats = 1000u,
                        beløp = 1000u,
                        klassekode = "AAP"
                    )
                )
            ),
            error = if (status == Status.FEILET) UtbetalingError(404, "Bug", "RTFM") else null
        )
    }

    private fun opprettTilkjentYtelse(behandlingRef: UUID): TilkjentYtelse {
        val saksnummer = Saksnummer("123")
        return dataSource.transaction { connection ->
            val tilkjentYtelse = TilkjentYtelseTestUtil.opprettTilkjentYtelse(
                saksnummer = saksnummer,
                behandlingRef = behandlingRef,
                forrigeBehandlingRef = null,
                antallPerioder = 3,
                beløp = Beløp(1000L),
                startDato = LocalDate.now()
            )
            val tilkjentYtelseId = TilkjentYtelseRepository(connection).lagreTilkjentYtelse(tilkjentYtelse)
            tilkjentYtelse.copy(id = tilkjentYtelseId)
        }
    }

    private fun opprettUtbetalingsmelding(tilkjentYtelseId: Long, referanse: UUID) {
        return dataSource.transaction { connection ->

            UtbetalingsmeldingRepository(connection).lagre(
                Utbetalingsmelding(
                    sakUtbetalingId = 123,
                    tilkjentYtelseId = tilkjentYtelseId,
                    referanse = referanse,
                    utbetalingsmeldingType = UtbetalingsmeldingType.UTBETALING,
                    melding = "{}",
                    opprettet = LocalDateTime.now(),
                )
            )

        }
    }

}