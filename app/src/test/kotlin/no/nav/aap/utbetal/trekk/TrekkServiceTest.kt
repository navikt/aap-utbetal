package no.nav.aap.utbetal.trekk

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseTrekk
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class TrekkServiceTest {


    @Test
    fun `ingen nye og ingen eksisterende trekk`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val tyRepo = TilkjentYtelseRepository(connection)
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(tyRepo, trekkRepo)

            val ty = lagTilkjentYtelse()
            tyRepo.lagreTilkjentYtelse(ty)

            service.oppdaterTrekk(ty.behandlingsreferanse)

            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(0)
        }
    }

    @Test
    fun `ingen ny meldeperiode, skal føre til ingen nye trekk-posteringer men trekk skal lagres`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val tyRepo = TilkjentYtelseRepository(connection)
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(tyRepo, trekkRepo)

            val ty = lagTilkjentYtelse(trekk = listOf(
                TilkjentYtelseTrekk(
                    dato = LocalDate.parse("2025-01-01"),
                    beløp = 2000
                )
            ))
            tyRepo.lagreTilkjentYtelse(ty)

            service.oppdaterTrekk(ty.behandlingsreferanse)

            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(1)
            assertThat(trekkListe.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListe.first().beløp).isEqualTo(2000)
            assertThat(trekkListe.first().posteringer).hasSize(0)
        }
    }

    @Test
    fun `trekk og meldeperiode skal føre til posteringer`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val tyRepo = TilkjentYtelseRepository(connection)
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(tyRepo, trekkRepo)

            val ty = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 1800
                    )
                ),
            )
            tyRepo.lagreTilkjentYtelse(ty)

            service.oppdaterTrekk(ty.behandlingsreferanse)

            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(1)
            assertThat(trekkListe.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListe.first().beløp).isEqualTo(1800)
            assertThat(trekkListe.first().posteringer).hasSize(2)
            val posteringer = trekkListe.first().posteringer
            assertThat(posteringer[0].dato).isEqualTo(LocalDate.parse("2025-01-13"))
            assertThat(posteringer[0].beløp).isEqualTo(1000)
            assertThat(posteringer[1].dato).isEqualTo(LocalDate.parse("2025-01-14"))
            assertThat(posteringer[1].beløp).isEqualTo(800)
        }
    }

    private fun lagTilkjentYtelse(meldeperiode: Periode? = null, trekk: List<TilkjentYtelseTrekk> = emptyList()): TilkjentYtelse {
        return TilkjentYtelse(
            id = 123L,
            saksnummer = Saksnummer("sak1"),
            behandlingsreferanse = UUID.randomUUID(),
            forrigeBehandlingsreferanse = UUID.randomUUID(),
            personIdent = "01017012345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "saksbehandler2",
            saksbehandlerId = "saksbehandler1",
            perioder = listOf(
                TilkjentYtelsePeriode(
                    periode = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31")),
                    detaljer = YtelseDetaljer(
                        redusertDagsats = Beløp(1000),
                        gradering = Prosent(100),
                        dagsats = Beløp(1000),
                        grunnlag = Beløp(1000),
                        grunnlagsfaktor = GUnit(6),
                        grunnbeløp = Beløp(1000),
                        antallBarn = 0,
                        barnetilleggsats = Beløp(0),
                        barnetillegg = Beløp(0),
                        utbetalingsdato = LocalDate.now()
                    )
                )
            ),
            avvent = null,
            nyMeldeperiode = meldeperiode,
            trekk = trekk
        )
    }

}