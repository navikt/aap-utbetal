package no.nav.aap.utbetal.trekk

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class TrekkRepositoryTest {

    private val iDag = LocalDate.now()

    @Test
    fun `lagre og hente trekk`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val trekk = TrekkRepository(connection).lagre(lagTrekk())

            val trekkListe = TrekkRepository(connection).hentTrekk(trekk.saksnummer)

            assertEquals(1, trekkListe.size)
            assertThat(trekkListe.first().behandlingsreferanse).isEqualTo(trekk.behandlingsreferanse)
            assertThat(trekkListe.first().dato).isEqualTo(trekk.dato)
            assertThat(trekkListe.first().beløp).isEqualTo(trekk.beløp)
        }
    }

    @Test
    fun `lagre og hente trekkposteringer`() {

        InitTestDatabase.freshDatabase().transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val trekk = trekkRepo.lagre(lagTrekk())

            trekkRepo.lagre(trekk.id!!, listOf(
                TrekkPostering(trekkId = trekk.id, dato = iDag.plusDays(10), beløp = 600),
                TrekkPostering(trekkId = trekk.id, dato = iDag.plusDays(11), beløp = 500),
            ))

            val trekkListe = trekkRepo.hentTrekk(trekk.saksnummer)

            assertEquals(1, trekkListe.size)
            assertThat(trekkListe.first().behandlingsreferanse).isEqualTo(trekk.behandlingsreferanse)
            assertThat(trekkListe.first().dato).isEqualTo(trekk.dato)
            assertThat(trekkListe.first().beløp).isEqualTo(trekk.beløp)
            val posteringer = trekkListe.first().posteringer
            assertThat(posteringer).hasSize(2)
            assertThat(posteringer[0].beløp).isEqualTo(600)
            assertThat(posteringer[0].dato).isEqualTo(iDag.plusDays(10))
            assertThat(posteringer[1].beløp).isEqualTo(500)
            assertThat(posteringer[1].dato).isEqualTo(iDag.plusDays(11))
        }
    }

    @Test
    fun `slett trekk`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val trekkRepo = TrekkRepository(connection)
            val trekk = trekkRepo.lagre(lagTrekk())

            trekkRepo.lagre(trekk.id!!, listOf(
                TrekkPostering(trekkId = trekk.id, dato = iDag.plusDays(10), beløp = 600),
                TrekkPostering(trekkId = trekk.id, dato = iDag.plusDays(11), beløp = 500),
            ))

            val trekkListe = trekkRepo.hentTrekk(trekk.saksnummer)

            assertThat(trekkListe).hasSize(1)

            trekkRepo.slett(trekkListe.first().id!!)

            val trekkListeEtterSletting = trekkRepo.hentTrekk(trekk.saksnummer)

            assertThat(trekkListeEtterSletting).hasSize(0)
        }
    }

    private fun lagTrekk(): Trekk {
        return Trekk(
            saksnummer = Saksnummer("123"),
            behandlingsreferanse = UUID.randomUUID(),
            dato = iDag,
            beløp = 1100,
        )
    }



}