package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import java.util.UUID
import javax.sql.DataSource

class TilkjentYtelseService {

    fun lagre(dataSource: DataSource, tilkjentYtelse: TilkjentYtelseDto) {
        dataSource.transaction { connection ->
            TilkjentYtelseRepository(connection).lagre(tilkjentYtelse)
        }
    }


    fun simulerUtbetaling(dataSource: DataSource, nyTilkjentYtelse: TilkjentYtelseDto): Utbetalingsplan {
        TODO()
    }



}
/*
internal fun List<TilkjentYtelseDto>.finnGjeldende(periode: Periode): List<TilkjentYtelsePeriodeDto> {
    var tidslinje = Tidslinje<Beløp>()
    this.forEach { tilkjentYtelse ->
        tidslinje = tidslinje.kombiner(tilkjentYtelse.perioder.tilTidslinje(), StandardSammenslåere.prioriterHøyreSide())
    }
    return tidslinje.disjoint(periode).segmenter().map { TilkjentYtelsePeriodeDto(it.fom(), it.tom(), it.verdi.verdi().toInt()) }
}

private fun List<TilkjentYtelsePeriodeDto>.tilTidslinje(): Tidslinje<Beløp> {
    val segments = this.map { Segment(Periode(it.fom, it.tom), Beløp(it.beløp)) }
    return Tidslinje<Beløp>(segments)
}


internal fun TilkjentYtelseDto.finnDifferans(nyTilkjentYtelse:  TilkjentYtelseDto): List<TilkjentYtelsePeriodeDto> {
    this.perioder.tilTidslinje()
    TODO()

}

 */