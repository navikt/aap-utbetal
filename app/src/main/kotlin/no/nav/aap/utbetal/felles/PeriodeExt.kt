package no.nav.aap.utbetal.felles

import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek

fun Periode.finnHelger(): List<Periode> {
    val helger = mutableListOf<Periode>()
    var dato = fom
    while (dato.isBefore(tom) || dato.isEqual(tom)) {
        when (dato.dayOfWeek) {
            DayOfWeek.SATURDAY -> {
                if (dato == tom) {
                    helger.add(Periode(dato, dato))
                } else {
                    helger.add(Periode(dato, dato.plusDays(1)))
                }
                dato = dato.plusDays(7)
            }
            DayOfWeek.SUNDAY -> {
                helger.add(Periode(dato, dato))
                dato = dato.plusDays(6)
            }
            else -> {
                // Finn neste lørdag
                dato = dato.plusDays(6L - dato.dayOfWeek.value)
            }
        }
    }
    return helger
}