package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import kotlin.test.Test

class GjeldendeAvventPeriodeRepositoryTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `lagre og hente gjeldende avvent periode`() {
        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection)
            val repo = GjeldendeAvventPeriodeRepository(connection)

            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            repo.lagre(GjeldendeAvventPeriode(sakUtbetalingId, periode))

            val hentet = repo.hentGjeldendeAvventPeriode(sakUtbetalingId)

            assertThat(hentet).isNotNull()
            assertThat(hentet!!.sakUtbetalingId).isEqualTo(sakUtbetalingId)
            assertThat(hentet.periode.fom).isEqualTo(periode.fom)
            assertThat(hentet.periode.tom).isEqualTo(periode.tom)
        }
    }

    @Test
    fun `hente gjeldende avvent periode som ikke finnes returnerer null`() {
        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection)
            val repo = GjeldendeAvventPeriodeRepository(connection)

            val hentet = repo.hentGjeldendeAvventPeriode(sakUtbetalingId)

            assertThat(hentet).isNull()
        }
    }

    @Test
    fun `lagre ny periode deaktiverer eksisterende periode for samme sak`() {
        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection)
            val repo = GjeldendeAvventPeriodeRepository(connection)

            val førstePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            repo.lagre(GjeldendeAvventPeriode(sakUtbetalingId, førstePeriode))

            val nyPeriode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28))
            repo.lagre(GjeldendeAvventPeriode(sakUtbetalingId, nyPeriode))

            val hentet = repo.hentGjeldendeAvventPeriode(sakUtbetalingId)

            assertThat(hentet).isNotNull()
            assertThat(hentet!!.periode.fom).isEqualTo(nyPeriode.fom)
            assertThat(hentet.periode.tom).isEqualTo(nyPeriode.tom)
        }
    }

    private fun opprettSakUtbetaling(connection: DBConnection): Long {
        return SakUtbetalingRepository(connection).lagre(Saksnummer("123"), true)
    }
}
