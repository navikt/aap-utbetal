package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class UtbetalingRepositoryTest {

    @Test
    fun `Lagre utbetaling`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val saksnummer = Saksnummer("001")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600001"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            val utbetalingId = opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)

            val utbetalingFraId = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            assertThat(utbetalingFraId.saksnummer).isEqualTo(saksnummer)
            assertThat(utbetalingFraId.behandlingsreferanse).isEqualTo(behandlingRef)

            val utbetalingFraSaksnummer = UtbetalingRepository(connection).hent(saksnummer).first()
            assertThat(utbetalingFraSaksnummer.saksnummer).isEqualTo(saksnummer)
            assertThat(utbetalingFraSaksnummer.behandlingsreferanse).isEqualTo(behandlingRef)

            val utbetalingFraBehandlingRef = UtbetalingRepository(connection).hent(behandlingRef).first()
            assertThat(utbetalingFraBehandlingRef.saksnummer).isEqualTo(saksnummer)
            assertThat(utbetalingFraBehandlingRef.behandlingsreferanse).isEqualTo(behandlingRef)
        }
    }

    @Test
    fun `Lagre utbetaling med avvent utbetaling`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val saksnummer = Saksnummer("001-1")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600001"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            val utbetalingAvvent = UtbetalingAvvent(
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-01-31"),
                LocalDate.parse("2025-01-31"),
                AvventÅrsak.AVVENT_AVREGNING
            )

            val utbetalingId = opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId, utbetalingAvvent)

            val utbetalingFraId = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            assertThat(utbetalingFraId.saksnummer).isEqualTo(saksnummer)
            assertThat(utbetalingFraId.behandlingsreferanse).isEqualTo(behandlingRef)

            val avvent = utbetalingFraId.avvent!!
            assertThat(avvent.fom).isEqualTo(utbetalingAvvent.fom)
            assertThat(avvent.tom).isEqualTo(utbetalingAvvent.tom)
            assertThat(avvent.overføres).isEqualTo(utbetalingAvvent.overføres)
            assertThat(avvent.årsak).isEqualTo(utbetalingAvvent.årsak)
        }
    }

    @Test
    fun `Lagre utbetaling med avvent utbetaling uten overføresdato`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val saksnummer = Saksnummer("001-2")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600001"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            val utbetalingAvvent = UtbetalingAvvent(
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-01-31"),
                null,
                AvventÅrsak.AVVENT_AVREGNING
            )

            val utbetalingId = opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId, utbetalingAvvent)

            val utbetalingFraId = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            assertThat(utbetalingFraId.saksnummer).isEqualTo(saksnummer)
            assertThat(utbetalingFraId.behandlingsreferanse).isEqualTo(behandlingRef)

            val avvent = utbetalingFraId.avvent!!
            assertThat(avvent.fom).isEqualTo(utbetalingAvvent.fom)
            assertThat(avvent.tom).isEqualTo(utbetalingAvvent.tom)
            assertThat(avvent.overføres).isEqualTo(utbetalingAvvent.overføres)
            assertThat(avvent.årsak).isEqualTo(utbetalingAvvent.årsak)
        }
    }


    @Test
    fun `Oppdater status på utbetaling`() {
        val saksnummer = Saksnummer("002")
        val behandlingRef = UUID.randomUUID()
        val personIdent = "12345600002"

        val dataSource = InitTestDatabase.freshDatabase()

        val utbetalingId = dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)
        }

        dataSource.transaction { connection ->
            val utbetalingRepo = UtbetalingRepository(connection)
            val utbetaling = utbetalingRepo.hentUtbetaling(utbetalingId)
            assertThat(utbetaling.utbetalingStatus).isEqualTo(UtbetalingStatus.OPPRETTET)
            utbetalingRepo.oppdaterStatus(utbetaling.id!!, utbetaling.versjon, UtbetalingStatus.SENDT)
        }

        dataSource.transaction { connection ->
            val utbetalingRepo = UtbetalingRepository(connection)
            val utbetaling = utbetalingRepo.hentUtbetaling(utbetalingId)
            assertThat(utbetaling.utbetalingStatus).isEqualTo(UtbetalingStatus.SENDT)
        }
    }

    @Test
    fun `Optimistisk låsing på utbetaling`() {
        val saksnummer = Saksnummer("003")
        val behandlingRef = UUID.randomUUID()
        val personIdent = "12345600003"

        val dataSource = InitTestDatabase.freshDatabase()

        val utbetalingId = dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)
        }

        dataSource.transaction { connection ->
            val utbetalingRepo = UtbetalingRepository(connection)
            val utbetaling = utbetalingRepo.hentUtbetaling(utbetalingId)

            // En annen transaksjon kommer i mellom og oppdaterer til BEKREFTET
            dataSource.transaction { innerConnection ->
                val utbetalingRepo2 = UtbetalingRepository(connection)
                val utbetaling = utbetalingRepo2.hentUtbetaling(utbetalingId)
                utbetalingRepo2.oppdaterStatus(utbetaling.id!!, utbetaling.versjon, UtbetalingStatus.BEKREFTET)

            }

            // Oppdatering av etter at annen transaksjon er ferdig skal feile pga feil versjon av utbetalingen
            assertThat(utbetaling.utbetalingStatus).isEqualTo(UtbetalingStatus.OPPRETTET)
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy {
                    utbetalingRepo.oppdaterStatus(utbetaling.id!!, utbetaling.versjon, UtbetalingStatus.SENDT)
                }
        }

        //Verifiserer riktig status
        dataSource.transaction { connection ->
            val utbetalingRepo = UtbetalingRepository(connection)
            val utbetaling = utbetalingRepo.hentUtbetaling(utbetalingId)
            assertThat(utbetaling.utbetalingStatus).isEqualTo(UtbetalingStatus.BEKREFTET)
        }

    }

    @Test
    fun `Skal hente sendte utbetalinger`() {
        val dataSource = InitTestDatabase.freshDatabase()

        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("004")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600004"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)
        }

        val utbetalingId2 = dataSource.transaction { connection ->
            val saksnummer = Saksnummer("005")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600005"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            val utbetalingId = opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)
            val utbetaling = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            UtbetalingRepository(connection).oppdaterStatus(utbetaling.id!!, utbetaling.versjon, UtbetalingStatus.SENDT )
            utbetaling.id
        }

        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("006")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600006"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            val utbetalingId = opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)
            val utbetaling = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            UtbetalingRepository(connection).oppdaterStatus(utbetaling.id!!, utbetaling.versjon, UtbetalingStatus.BEKREFTET)
        }

        dataSource.transaction(readOnly = true) { connection ->
            val alleSendteUtbetalinger = UtbetalingRepository(connection).hentUtbetalingerSomManglerKvittering()
            assertThat(alleSendteUtbetalinger).hasSize(1)
            assertThat(alleSendteUtbetalinger.first().id).isEqualTo(utbetalingId2)
        }
    }


    @Test
    fun `Skal også hente feilede utbetalinger`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val utbetalingId = dataSource.transaction { connection ->
            val saksnummer = Saksnummer("005")
            val behandlingRef = UUID.randomUUID()
            val personIdent = "12345600005"

            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef, personIdent)
            val utbetalingId = opprettUtbetaling(connection, saksnummer, behandlingRef, personIdent, sakUtbetalingId, tyId)

            val utbetaling = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            UtbetalingRepository(connection).oppdaterStatus(utbetaling.id!!, utbetaling.versjon, UtbetalingStatus.FEILET )
            utbetaling.id
        }

        dataSource.transaction(readOnly = true) { connection ->
            val alleSendteUtbetalinger = UtbetalingRepository(connection).hentUtbetalingerSomManglerKvittering()
            assertThat(alleSendteUtbetalinger).hasSize(1)
            assertThat(alleSendteUtbetalinger.first().id).isEqualTo(utbetalingId)
        }
    }

    private fun opprettUtbetaling(connection: DBConnection, saksnummer: Saksnummer, behandlingRef: UUID, personIdent: String, sakUtbetalingId: Long, tilkjentYtelseId: Long, utbetalingAvvent: UtbetalingAvvent? = null): Long {
        val utbetaling = Utbetaling(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingRef,
            tilkjentYtelseId = tilkjentYtelseId,
            personIdent = personIdent,
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "x12345",
            saksbehandlerId = "y54321",
            utbetalingOversendt = LocalDateTime.now(),
            utbetalingStatus = UtbetalingStatus.OPPRETTET,
            perioder = listOf(),
            avvent = utbetalingAvvent,
            utbetalingRef = UUID.randomUUID(),
        )
        return UtbetalingRepository(connection).lagre(sakUtbetalingId, utbetaling)
    }

    private fun opprettTilkjentYtelse(connection: DBConnection, saksnummer: Saksnummer, behandlingRef: UUID, personIdent: String): Long {
        return TilkjentYtelseRepository(connection).lagre(
            TilkjentYtelse(
                saksnummer = saksnummer,
                behandlingsreferanse = behandlingRef,
                forrigeBehandlingsreferanse = null,
                personIdent = personIdent,
                vedtakstidspunkt = LocalDateTime.now(),
                beslutterId = "x12345",
                saksbehandlerId = "y54321",
                perioder = listOf()
            )
        )
    }

    private fun opprettSakUtbetaling(connection: DBConnection, saksnummer: Saksnummer): Long {
        return SakUtbetalingRepository(connection).lagre(
            SakUtbetaling(
                saksnummer = saksnummer,
                opprettetTidspunkt = LocalDateTime.now()
            )
        )
    }

}