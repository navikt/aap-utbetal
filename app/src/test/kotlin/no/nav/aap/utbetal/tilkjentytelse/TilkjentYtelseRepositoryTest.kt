package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull

class TilkjentYtelseRepositoryTest {

    @AfterTest
    fun tearDown() {
        @Suppress("SqlWithoutWhere")
        InitTestDatabase.dataSource.transaction {
            it.execute("DELETE FROM TILKJENT_PERIODE")
            it.execute("DELETE FROM TILKJENT_YTELSE")
            it.execute("DELETE FROM SAK_UTBETALING")
        }
    }

    @Test
    fun `Finner ingen tilkjentYtelse dersom ingen er lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val tilkjentYtelseId = TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(Saksnummer("123"))
            assertNull(tilkjentYtelseId)
        }
    }

    @Test
    fun `Finner siste tilkjentYtelse dersom bare førstegangsbehandling`() {
        val saksnummer = Saksnummer("123")
        val tilkjentYtelseId = InitTestDatabase.dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(
                opprettTilkjentYtelse(
                    saksnummer = saksnummer,
                    behandlingRef = UUID.randomUUID(),
                    forrigeBehandlingRef = null,
                    antallPerioder = 3,
                    beløp = Beløp(1000L),
                    startDato = LocalDate.now()
                )
            )
        }
        val funnetTilkjentYtelseId = InitTestDatabase.dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(saksnummer)
        }
        assertThat(funnetTilkjentYtelseId).isEqualTo(tilkjentYtelseId)
    }


    @Test
    fun `Finner siste tilkjentYtelse dersom 4 behandler på samme saksnummer`() {
        val behandlingRef1 = UUID.randomUUID()
        val behandlingRef2 = UUID.randomUUID()
        val behandlingRef3 = UUID.randomUUID()
        val behandlingRef4 = UUID.randomUUID()
        val behandlingRef5 = UUID.randomUUID()
        val saksnummer1 = Saksnummer("123")
        val saksnummer2 = Saksnummer("456")

        lagreTilkjentYtelse(saksnummer1, behandlingRef1, null)
        lagreTilkjentYtelse(saksnummer1, behandlingRef2, behandlingRef1)
        lagreTilkjentYtelse(saksnummer1, behandlingRef3, behandlingRef2)
        val tilkjentYtelseId4 = lagreTilkjentYtelse(saksnummer1, behandlingRef4, behandlingRef3)
        lagreTilkjentYtelse(saksnummer2, behandlingRef5, null)

        val funnetTilkjentYtelseId = InitTestDatabase.dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).finnSisteTilkjentYtelse(saksnummer1)
        }
        assertThat(funnetTilkjentYtelseId).isEqualTo(tilkjentYtelseId4)
    }

    private fun lagreTilkjentYtelse(saksnummer: Saksnummer, behandlingRef: UUID, forrigeBehandlingRef: UUID?): Long {
        return InitTestDatabase.dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(
                opprettTilkjentYtelse(
                    saksnummer = saksnummer,
                    behandlingRef = behandlingRef,
                    forrigeBehandlingRef = forrigeBehandlingRef,
                    antallPerioder = 5,
                    beløp = Beløp(1000L),
                    startDato = LocalDate.now()
                )
            )
        }
    }

    private fun opprettTilkjentYtelse(
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID?,
        antallPerioder: Int,
        beløp: Beløp,
        startDato: LocalDate
    ): TilkjentYtelse {
        val perioder = (0 until antallPerioder).map {
            TilkjentYtelsePeriode(
                periode = Periode(startDato.plusWeeks(it * 2L), startDato.plusWeeks(it * 2L).plusDays(13)),
                YtelseDetaljer(
                    gradering = Prosent.`0_PROSENT`,
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = Beløp(100000L) ,
                    antallBarn = 0,
                    barnetillegg = Beløp(0L),
                    grunnlagsfaktor = GUnit("0.008"),
                    barnetilleggsats = Beløp(36L),
                    redusertDagsats = beløp,
                    ventedagerSamordning = false,
                )
            )
        }
        return TilkjentYtelse(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingRef,
            forrigeBehandlingsreferanse = forrigeBehandlingRef,
            personIdent = "12345123456",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder)
    }

}