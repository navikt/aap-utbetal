package no.nav.aap.utbetal.trekk

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class Trekk(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val behandlingsreferanse: UUID,
    val dato: LocalDate,
    val beløp: Int,
    val aktiv: Boolean,
    val posteringer: List<TrekkPostering> = emptyList(),
) {
    fun erOppgjort() = restBeløp() == 0

    fun restBeløp() = beløp - posteringer.sumOf { it.beløp }

    fun finnTrekkPosteringUtenDekning(tilkjentYtelse: TilkjentYtelse): List<TrekkPostering>  {
        val utbetalinger = mutableMapOf<LocalDate, Int>()
         tilkjentYtelse.perioder.forEach { tyPeriode ->
            tyPeriode.periode.dager()
                .filter {it.dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY }
                .forEach {utbetalinger[it] =  tyPeriode.detaljer.redusertDagsats.verdi.toInt() }
        }
        return posteringer.filterNot {it.beløp <= (utbetalinger[it.dato]?:0)}
    }

}

data class TrekkPostering(
    val id: Long? = null,
    val trekkId: Long,
    val dato: LocalDate,
    val beløp: Int,
)
