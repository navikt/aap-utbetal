package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class SjekkMigreringServiceTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `Returnerer true når sak er migrert til Kafka`() {
        val fnr = "12345678901"
        val saksnummer = Saksnummer("SAK-003")
        dataSource.transaction { connection ->
            SakUtbetalingRepository(connection).lagre(saksnummer, migrertTilKafka = true)
        }

        val resultat = dataSource.transaction { connection ->
            SjekkMigreringService(connection).skalTilNyttGrensesnitt(fnr, saksnummer)
        }

        assertThat(resultat).isTrue()
    }

    @Test
    fun `Returnerer false når sak ikke er migrert til Kafka`() {
        val fnr = "12345678901"
        val saksnummer = Saksnummer("SAK-004")
        dataSource.transaction { connection ->
            SakUtbetalingRepository(connection).lagre(saksnummer, migrertTilKafka = false)
        }

        val resultat = dataSource.transaction { connection ->
            SjekkMigreringService(connection).skalTilNyttGrensesnitt(fnr, saksnummer)
        }

        assertThat(resultat).isFalse()
    }

    @Test
    fun `Returnerer true når sak ikke finnes i databasen`() {
        val fnr = "12345678901"
        val saksnummer = Saksnummer("SAK-005")

        val resultat = dataSource.transaction { connection ->
            SjekkMigreringService(connection).skalTilNyttGrensesnitt(fnr, saksnummer)
        }

        assertThat(resultat).isTrue()
    }

}
