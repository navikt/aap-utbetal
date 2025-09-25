package no.nav.aap.utbetal.trekk

import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseTrekk
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class TrekkService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val trekkRepository: TrekkRepository
) {

    fun oppdaterTrekk(behandlingRef: UUID) {
        val tilkjentYtelse = tilkjentYtelseRepository.hent(behandlingRef) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingRef")
        oppdaterTrekk(tilkjentYtelse)
        var eksisterendeTrekk = trekkRepository.hentTrekk(tilkjentYtelse.saksnummer)
        kreditterPosteringerSomIkkeHarDekning(tilkjentYtelse, eksisterendeTrekk)
        eksisterendeTrekk = trekkRepository.hentTrekk(tilkjentYtelse.saksnummer)
        oppdaterMedNyeTrekkPosteringer(tilkjentYtelse, eksisterendeTrekk)
    }

    private fun oppdaterTrekk(
        tilkjentYtelse: TilkjentYtelse,
    ) {
        val eksisterendeTrekk = trekkRepository.hentTrekk(tilkjentYtelse.saksnummer)
        val eksisterendeMap = eksisterendeTrekk.tilDatoMap()
        tilkjentYtelse.trekk.forEach {
            val eksisterende = eksisterendeMap[it.dato]
            if (eksisterende == null) {
                // Lag nye trekk
                trekkRepository.lagre(it.tilTrekk(tilkjentYtelse))
            } else if (it.beløp != eksisterende.beløp) {
                // Slettet endret trekk(med posteringer) og opprett nytt trekk
                trekkRepository.slett(eksisterende.id!!)
                trekkRepository.lagre(it.tilTrekk(tilkjentYtelse))
            }
        }
    }


    private fun oppdaterMedNyeTrekkPosteringer(tilkjentYtelse: TilkjentYtelse, eksisterendeTrekk: List<Trekk>) {
        eksisterendeTrekk.forEach { trekk ->
            if (!trekk.erOppgjort()) {
                val nyeTrekkPosteringer = finnNyeTrekkPosteringer(tilkjentYtelse, trekk)
                trekkRepository.lagre(trekk.id!!, nyeTrekkPosteringer)
            }
        }
    }

    private fun kreditterPosteringerSomIkkeHarDekning(tilkjentYtelse: TilkjentYtelse, eksisterendeTrekk: List<Trekk>) {
        eksisterendeTrekk.forEach { trekk ->
            val posteringerUtenDekning = trekk.finnTrekkPosteringUtenDekning(tilkjentYtelse)
            val kreditteringsPosteringer = posteringerUtenDekning
                .map {postering -> postering.copy(id = null, beløp = -postering.beløp)}

            trekkRepository.lagre(trekk.id!!,kreditteringsPosteringer)
        }
    }

    private fun finnNyeTrekkPosteringer(tilkjentYtelse: TilkjentYtelse, trekk: Trekk): List<TrekkPostering> {
        var restTrekk = trekk.beløp
        val muligeDatoerForTrekk = tilkjentYtelse.finnMuligeDatoerForTrekk()

        val nyePosteringer = mutableListOf<TrekkPostering>()

        for (muligDato in muligeDatoerForTrekk) {
            if (restTrekk == 0) break
            if (muligDato.beløp >= restTrekk) {
                nyePosteringer.add(TrekkPostering(trekkId = trekk.id!!, dato = muligDato.dato, beløp = restTrekk))
                restTrekk = 0
            } else if (muligDato.beløp in 1..<restTrekk) {
                nyePosteringer.add(TrekkPostering(trekkId = trekk.id!!, dato = muligDato.dato, beløp = muligDato.beløp))
                restTrekk -= muligDato.beløp
            }
        }
        return nyePosteringer
    }

    private fun TilkjentYtelse.finnMuligeDatoerForTrekk(): List<DatoOgBeløp> {
        if (nyMeldeperiode == null) return emptyList()
        return perioder
            .filter {it.periode.fom >= nyMeldeperiode.fom && it.periode.tom <= nyMeldeperiode.tom}
            .map {arbeidsdagerMedBeløp(it)}
            .flatten()
    }

    private fun arbeidsdagerMedBeløp(tyPeriode: TilkjentYtelsePeriode): List<DatoOgBeløp> {
        var dato = tyPeriode.periode.fom
        val arbeidsdager = mutableListOf<DatoOgBeløp>()
        while (dato <= tyPeriode.periode.tom) {
            if (dato.dayOfWeek in listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
                arbeidsdager.add(DatoOgBeløp(dato, tyPeriode.detaljer.redusertDagsats.verdi.toInt()))
            }
            dato = dato.plusDays(1)
        }
        return arbeidsdager
    }

    private fun List<Trekk>.tilDatoMap() = associateBy {it.dato}


    private fun TilkjentYtelseTrekk.tilTrekk(tilkjentYtelse: TilkjentYtelse): Trekk {
        return Trekk(
            saksnummer = tilkjentYtelse.saksnummer,
            behandlingsreferanse = tilkjentYtelse.behandlingsreferanse,
            dato = this.dato,
            beløp = this.beløp,
            posteringer = emptyList(),
        )
    }

}

private data class DatoOgBeløp(
    val dato: LocalDate,
    val beløp: Int,
)

