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
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty("NAIS_CLUSTER_NAME")
        dataSource.close()
    }

    @Test
    fun `Returnerer alltid false i produksjonsmiljø`() {
        System.setProperty("NAIS_CLUSTER_NAME", "prod-gcp")
        val fnrIWhitelist = "29509000997"
        val saksnummer = Saksnummer("SAK-001")

        val resultat = dataSource.transaction { connection ->
            SjekkMigreringService(connection).skalTilNyttGrensesnitt(fnrIWhitelist, saksnummer)
        }

        assertThat(resultat).isFalse()
    }

    @Test
    fun `Returnerer true for fnr i whitelist`() {
        val fnrIWhitelist = "29509000997"
        val saksnummer = Saksnummer("SAK-002")

        val resultat = dataSource.transaction { connection ->
            SjekkMigreringService(connection).skalTilNyttGrensesnitt(fnrIWhitelist, saksnummer)
        }

        assertThat(resultat).isTrue()
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
    fun `Returnerer false når sak ikke finnes i databasen`() {
        val fnr = "12345678901"
        val saksnummer = Saksnummer("SAK-005")

        val resultat = dataSource.transaction { connection ->
            SjekkMigreringService(connection).skalTilNyttGrensesnitt(fnr, saksnummer)
        }

        assertThat(resultat).isFalse()
    }
}
