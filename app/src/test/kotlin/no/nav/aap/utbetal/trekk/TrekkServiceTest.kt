package no.nav.aap.utbetal.trekk

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseTrekk
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class TrekkServiceTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `ingen nye og ingen eksisterende trekk`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse()

            service.oppdaterTrekk(ty)

            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(0)
        }
    }

    @Test
    fun `ingen ny meldeperiode, skal føre til ingen nye trekk-posteringer men trekk skal lagres`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse(trekk = listOf(
                TilkjentYtelseTrekk(
                    dato = LocalDate.parse("2025-01-01"),
                    beløp = 2000
                )
            ))

            service.oppdaterTrekk(ty)

            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(1)
            assertThat(trekkListe.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListe.first().beløp).isEqualTo(2000)
            assertThat(trekkListe.first().posteringer).hasSize(0)
        }
    }

    @Test
    fun `trekk og meldeperiode skal føre til posteringer`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 1800
                    )
                ),
            )

            service.oppdaterTrekk(ty)

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

    @Test
    fun `flere trekk og meldeperiode skal føre til posteringer på forskjellige datoer`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 1800
                    ),
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-02"),
                        beløp = 1800
                    ),
                ),
            )

            service.oppdaterTrekk(ty)

            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer).sortedBy {it.dato}

            assertThat(trekkListe).hasSize(2)
            assertThat(trekkListe.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListe.first().beløp).isEqualTo(1800)
            assertThat(trekkListe.first().posteringer).hasSize(2)
            assertThat(trekkListe.last().dato).isEqualTo(LocalDate.parse("2025-01-02"))
            assertThat(trekkListe.last().beløp).isEqualTo(1800)
            assertThat(trekkListe.last().posteringer).hasSize(2)

            val posteringer1 = trekkListe.first().posteringer
            assertThat(posteringer1[0].dato).isEqualTo(LocalDate.parse("2025-01-13"))
            assertThat(posteringer1[0].beløp).isEqualTo(1000)
            assertThat(posteringer1[1].dato).isEqualTo(LocalDate.parse("2025-01-14"))
            assertThat(posteringer1[1].beløp).isEqualTo(800)

            val posteringer2 = trekkListe.last().posteringer
            assertThat(posteringer2[0].dato).isEqualTo(LocalDate.parse("2025-01-15"))
            assertThat(posteringer2[0].beløp).isEqualTo(1000)
            assertThat(posteringer2[1].dato).isEqualTo(LocalDate.parse("2025-01-16"))
            assertThat(posteringer2[1].beløp).isEqualTo(800)
        }
    }




    @Test
    fun `tilbaketrekking av trekk skal føre til at posteringene fjernes`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 1800
                    )
                ),
            )

            service.oppdaterTrekk(ty)
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

            val oppdatertTilkjentYtelseMedEndretTrekk = ty.copy(
                behandlingsreferanse = UUID.randomUUID(),
                nyMeldeperiode = Periode(fom = LocalDate.parse("2025-01-27"), tom = LocalDate.parse("2025-02-09")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 0
                    )
                ),
            )

            service.oppdaterTrekk(oppdatertTilkjentYtelseMedEndretTrekk)
            val trekkListeEtterEndretTrekk = trekkRepo.hentTrekk(oppdatertTilkjentYtelseMedEndretTrekk.saksnummer)

            assertThat(trekkListeEtterEndretTrekk).hasSize(1)
            assertThat(trekkListeEtterEndretTrekk.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListeEtterEndretTrekk.first().beløp).isEqualTo(0)
            assertThat(trekkListeEtterEndretTrekk.first().posteringer).hasSize(0)
        }
    }

    @Test
    fun `ingen endring av trekk skal ikke påvirke trekk tabellene`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse(
                meldeperiode = null,
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 1800
                    )
                ),
            )

            service.oppdaterTrekk(ty)
            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(1)
            assertThat(trekkListe.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListe.first().beløp).isEqualTo(1800)
            assertThat(trekkListe.first().posteringer).hasSize(0)

            val oppdatertTilkjentYtelseMedEndretTrekk = ty.copy(
                behandlingsreferanse = UUID.randomUUID(),
            )

            service.oppdaterTrekk(oppdatertTilkjentYtelseMedEndretTrekk)
            val trekkListeEtterEndretTrekk = trekkRepo.hentTrekk(oppdatertTilkjentYtelseMedEndretTrekk.saksnummer)

            assertThat(trekkListeEtterEndretTrekk).hasSize(1)
            assertThat(trekkListeEtterEndretTrekk.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListeEtterEndretTrekk.first().beløp).isEqualTo(1800)
            assertThat(trekkListeEtterEndretTrekk.first().posteringer).hasSize(0)
            assertThat(trekkListeEtterEndretTrekk.first().id).isEqualTo(trekkListe.first().id)
        }
    }

    @Test
    fun `trekk som har rest fører til flere posteringer ved neste meldekort`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val trekk = TilkjentYtelseTrekk(
                dato = LocalDate.parse("2025-01-01"),
                beløp = 11000
            )

            val ty = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(trekk),
            )

            service.oppdaterTrekk(ty)
            val trekkListe = trekkRepo.hentTrekk(ty.saksnummer)

            assertThat(trekkListe).hasSize(1)
            val posteringerTrekk1 = trekkListe.first().posteringer
            assertThat(posteringerTrekk1).hasSize(10)
            posteringerTrekk1.forEach {
                assertThat(it.beløp).isEqualTo(1000)
            }

            val ty2 = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-27"), tom = LocalDate.parse("2025-02-10")),
                trekk = listOf(trekk),
            )

            service.oppdaterTrekk(ty2)
            val trekkListeOppdatert = trekkRepo.hentTrekk(ty2.saksnummer)

            assertThat(trekkListeOppdatert).hasSize(1)
            val posteringerTrekk2 = trekkListeOppdatert.last().posteringer
            assertThat(posteringerTrekk2).hasSize(11)
            posteringerTrekk2.forEach {
                assertThat(it.beløp).isEqualTo(1000)
            }
        }
    }

    @Test
    fun `endring av trekk skal føre til at posteringene justeres`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val ty = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 1800
                    )
                ),
            )

            service.oppdaterTrekk(ty)
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

            val oppdatertTilkjentYtelseMedEndretTrekk = ty.copy(
                behandlingsreferanse = UUID.randomUUID(),
                nyMeldeperiode = Periode(fom = LocalDate.parse("2025-01-27"), tom = LocalDate.parse("2025-02-09")),
                trekk = listOf(
                    TilkjentYtelseTrekk(
                        dato = LocalDate.parse("2025-01-01"),
                        beløp = 999
                    )
                ),
            )

            service.oppdaterTrekk(oppdatertTilkjentYtelseMedEndretTrekk)
            val trekkListeEtterEndretTrekk = trekkRepo.hentTrekk(oppdatertTilkjentYtelseMedEndretTrekk.saksnummer)

            assertThat(trekkListeEtterEndretTrekk).hasSize(1)
            assertThat(trekkListeEtterEndretTrekk.first().dato).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(trekkListeEtterEndretTrekk.first().beløp).isEqualTo(999)
            assertThat(trekkListeEtterEndretTrekk.first().posteringer).hasSize(1)
            val posteringerEtterEndring = trekkListeEtterEndretTrekk.first().posteringer
            assertThat(posteringerEtterEndring[0].dato).isEqualTo(LocalDate.parse("2025-01-27"))
            assertThat(posteringerEtterEndring[0].beløp).isEqualTo(999)
        }
    }

    @Test
    fun `skal støtte flere trekk på påfølgende tilkjente ytelser`() {
        dataSource.transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val service = TrekkService(trekkRepo)

            val trekk1 = TilkjentYtelseTrekk(
                dato = LocalDate.parse("2025-01-01"),
                beløp = 1800
            )
            val trekk2 = TilkjentYtelseTrekk(
                dato = LocalDate.parse("2025-01-02"),
                beløp = 700
            )

            val ty1 = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-13"), tom = LocalDate.parse("2025-01-26")),
                trekk = listOf(
                    trekk1
                ),
            )

            service.oppdaterTrekk(ty1)

            val trekkListe = trekkRepo.hentTrekk(ty1.saksnummer).sortedBy {it.dato}

            assertThat(trekkListe).hasSize(1)
            val posteringerTrekk1 = trekkListe.first().posteringer
            assertThat(posteringerTrekk1[0].dato).isEqualTo(LocalDate.parse("2025-01-13"))
            assertThat(posteringerTrekk1[0].beløp).isEqualTo(1000)
            assertThat(posteringerTrekk1[1].dato).isEqualTo(LocalDate.parse("2025-01-14"))
            assertThat(posteringerTrekk1[1].beløp).isEqualTo(800)

            val ty2 = lagTilkjentYtelse(
                meldeperiode = Periode(fom = LocalDate.parse("2025-01-27"), tom = LocalDate.parse("2025-02-10")),
                trekk = listOf(
                    trekk1,
                    trekk2
                ),
            )

            service.oppdaterTrekk(ty2)
            val trekkListeOppdatert = trekkRepo.hentTrekk(ty2.saksnummer)

            assertThat(trekkListeOppdatert).hasSize(2)
            val posteringerTrekk2 = trekkListeOppdatert.last().posteringer
            assertThat(posteringerTrekk2[0].dato).isEqualTo(LocalDate.parse("2025-01-27"))
            assertThat(posteringerTrekk2[0].beløp).isEqualTo(700)



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
                    periode = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-02-10")),
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
                        utbetalingsdato = LocalDate.now(),
                        meldeperiode = meldeperiode,
                    )
                )
            ),
            avvent = null,
            nyMeldeperiode = meldeperiode,
            trekk = trekk
        )
    }

}