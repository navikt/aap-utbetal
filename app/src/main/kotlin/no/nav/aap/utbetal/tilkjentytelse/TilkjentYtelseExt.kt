package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent

fun TilkjentYtelseDto.tilTilkjentYtelse(): TilkjentYtelse {
    val perioder = this.perioder.map { periodeDto ->
        val detaljerDto = periodeDto.detaljer
        TilkjentYtelsePeriode(
            periode = Periode(periodeDto.fom, periodeDto.tom),
            detaljer = TilkjentYtelseDetaljer(
                redusertDagsats = Beløp(detaljerDto.redusertDagsats),
                gradering = Prosent.fraDesimal(detaljerDto.gradering),
                dagsats = Beløp(detaljerDto.dagsats),
                grunnlag = Beløp(detaljerDto.grunnlag),
                grunnlagsfaktor = GUnit(detaljerDto.grunnlagsfaktor) ,
                grunnbeløp = Beløp(detaljerDto.grunnbeløp),
                antallBarn = detaljerDto.antallBarn,
                barnetilleggsats = Beløp(detaljerDto.barnetilleggsats),
                barnetillegg = Beløp(detaljerDto.barnetillegg),
            )
        )
    }
    return TilkjentYtelse(
        behandlingsreferanse = this.behandlingsreferanse,
        forrigeBehandlingsreferanse = this.forrigeBehandlingsreferanse,
        perioder = perioder
    )
}