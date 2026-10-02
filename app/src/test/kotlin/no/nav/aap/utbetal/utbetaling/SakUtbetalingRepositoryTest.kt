package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import kotlin.test.Test

class SakUtbetalingRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `lagre og hente sakutbetaling`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("sak-001")
            val repository = SakUtbetalingRepository(connection)

            val id = repository.lagre(saksnummer, migrertTilKafka = false)
            val hentet = repository.hent(saksnummer)

            assertThat(id).isPositive()
            assertThat(hentet).isNotNull
            assertThat(hentet!!.id).isEqualTo(id)
            assertThat(hentet.saksnummer).isEqualTo(saksnummer)
            assertThat(hentet.migrertTilKafka).isNull()
        }
    }

    @Test
    fun `lagre med migrertTilKafka true setter migreringstidspunkt`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("sak-002")
            val repository = SakUtbetalingRepository(connection)

            repository.lagre(saksnummer, migrertTilKafka = true)
            val hentet = repository.hent(saksnummer)

            assertThat(hentet).isNotNull
            assertThat(hentet!!.migrertTilKafka).isNotNull()
        }
    }

    @Test
    fun `settMigrertTilKafka oppdaterer eksisterende sakutbetaling`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("sak-003")
            val repository = SakUtbetalingRepository(connection)

            repository.lagre(saksnummer, migrertTilKafka = false)
            repository.settMigrertTilKafka(saksnummer)
            val hentet = repository.hent(saksnummer)

            assertThat(hentet).isNotNull
            assertThat(hentet!!.migrertTilKafka).isNotNull()
        }
    }

    @Test
    fun `flere inaktive rader og en aktiv rad kan ha samme saksnummer`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("sak-008")
            val repository = SakUtbetalingRepository(connection)

            repeat(2) {
                val id = repository.lagre(saksnummer, migrertTilKafka = false)
                connection.execute("UPDATE SAK_UTBETALING SET AKTIV = FALSE WHERE ID = ?") {
                    setParams { setLong(1, id) }
                }
            }
            val aktivId = repository.lagre(saksnummer, migrertTilKafka = false)

            assertThat(repository.hent(saksnummer)?.id).isEqualTo(aktivId)
            assertThat(repository.hent(aktivId).id).isEqualTo(aktivId)

            repository.settMigrertTilKafka(saksnummer)
            assertThat(repository.hent(saksnummer)?.migrertTilKafka).isNotNull()

            val inaktiveUtenMigreringstidspunkt = connection.queryList(
                "SELECT ID FROM SAK_UTBETALING WHERE SAKSNUMMER = ? AND AKTIV = FALSE AND MIGRERT_TIL_KAFKA IS NULL"
            ) {
                setParams { setString(1, saksnummer.toString()) }
                setRowMapper { it.getLong("ID") }
            }
            assertThat(inaktiveUtenMigreringstidspunkt).hasSize(2)
        }
    }

    @Test
    fun `to aktive rader kan ikke ha samme saksnummer`() {
        val saksnummer = Saksnummer("sak-009")
        dataSource.transaction { connection ->
            SakUtbetalingRepository(connection).lagre(saksnummer, migrertTilKafka = false)
        }

        assertUnikhetsbrudd {
            dataSource.transaction { connection ->
                SakUtbetalingRepository(connection).lagre(saksnummer, migrertTilKafka = false)
            }
        }
    }

    @Test
    fun `inaktiv rad kan ikke reaktiveres når aktiv rad har samme saksnummer`() {
        val saksnummer = Saksnummer("sak-010")
        val inaktivId = dataSource.transaction { connection ->
            val repository = SakUtbetalingRepository(connection)
            val id = repository.lagre(saksnummer, migrertTilKafka = false)
            connection.execute("UPDATE SAK_UTBETALING SET AKTIV = FALSE WHERE ID = ?") {
                setParams { setLong(1, id) }
            }
            repository.lagre(saksnummer, migrertTilKafka = false)
            id
        }

        assertUnikhetsbrudd {
            dataSource.transaction { connection ->
                connection.execute("UPDATE SAK_UTBETALING SET AKTIV = TRUE WHERE ID = ?") {
                    setParams { setLong(1, inaktivId) }
                }
            }
        }
    }

    private fun assertUnikhetsbrudd(block: () -> Unit) {
        val exception = assertThrows<Exception>(block)
        val sqlState = generateSequence(exception as Throwable) { it.cause }
            .filterIsInstance<SQLException>()
            .firstOrNull()
            ?.sqlState
        assertThat(sqlState).isEqualTo("23505")
    }

    @Test
    fun `hentSakerForMigrering returnerer kun ikke migrerte saker opp til limit`() {
        dataSource.transaction { connection ->
            val repository = SakUtbetalingRepository(connection)
            val ikkeMigrert1 = Saksnummer("sak-004")
            val ikkeMigrert2 = Saksnummer("sak-005")
            val migrert = Saksnummer("sak-006")

            repository.lagre(ikkeMigrert1, migrertTilKafka = false)
            repository.lagre(ikkeMigrert2, migrertTilKafka = false)
            repository.lagre(migrert, migrertTilKafka = true)

            val sakerForMigrering = repository.hentSakerForMigrering(1)

            assertThat(sakerForMigrering).hasSize(1)
            assertThat(sakerForMigrering.single().migrertTilKafka).isNull()
            assertThat(sakerForMigrering.single().saksnummer)
                .isIn(ikkeMigrert1, ikkeMigrert2)
        }
    }

    @Test
    fun `hentMedLås låser raden for oppdatering`() {
        val saksnummer = Saksnummer("sak-007")
        dataSource.transaction { connection ->
            val repository = SakUtbetalingRepository(connection)
            repository.lagre(saksnummer, migrertTilKafka = false)
        }

        dataSource.transaction { connection ->
            val repository = SakUtbetalingRepository(connection)
            val hentet = repository.hentMedLås(saksnummer)

            assertThat(hentet).isNotNull
            assertThat(hentet!!.saksnummer).isEqualTo(saksnummer)

            dataSource.transaction { innerConnection ->
                val innerRepository = SakUtbetalingRepository(innerConnection)
                try {
                    innerRepository.hentMedLås(saksnummer)
                    assert(false) { "Forventet at låsingen skulle feile, men den gjorde det ikke." }
                } catch (e: Exception) {
                    assert(true) // Forventet feil
                }
            }
        }
    }

}