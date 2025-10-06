package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer

fun TilkjentYtelseDto.tilTilkjentYtelse(): TilkjentYtelse {
    val perioder = perioder.tilTilkjentYtelsePeriode()
    val avvent = avvent?.tilAvvent()
    return TilkjentYtelse(
        saksnummer = Saksnummer(this.saksnummer),
        behandlingsreferanse = this.behandlingsreferanse,
        forrigeBehandlingsreferanse = this.forrigeBehandlingsreferanse,
        personIdent = this.personIdent,
        vedtakstidspunkt = this.vedtakstidspunkt,
        beslutterId = this.beslutterId,
        saksbehandlerId = this.saksbehandlerId,
        perioder = perioder,
        avvent = avvent,
        nyMeldeperiode = nyMeldeperiode?.tilPeriode(),
        trekk = trekk.tilTrekk()
    )
}

private fun List<TilkjentYtelsePeriodeDto>.tilTilkjentYtelsePeriode(): List<TilkjentYtelsePeriode> {
    return this.map { periodeDto ->
        val detaljerDto = periodeDto.detaljer
        TilkjentYtelsePeriode(
            periode = Periode(periodeDto.fom, periodeDto.tom),
            detaljer = YtelseDetaljer(
                redusertDagsats = Beløp(detaljerDto.redusertDagsats),
                gradering = Prosent(detaljerDto.gradering),
                dagsats = Beløp(detaljerDto.dagsats),
                grunnlag = Beløp(detaljerDto.grunnlag),
                grunnlagsfaktor = GUnit(detaljerDto.grunnlagsfaktor),
                grunnbeløp = Beløp(detaljerDto.grunnbeløp),
                antallBarn = detaljerDto.antallBarn,
                barnetilleggsats = Beløp(detaljerDto.barnetilleggsats),
                barnetillegg = Beløp(detaljerDto.barnetillegg),
                utbetalingsdato = detaljerDto.utbetalingsdato,
            )
        )
    }
}

private fun TilkjentYtelseAvventDto.tilAvvent(): TilkjentYtelseAvvent {
    return TilkjentYtelseAvvent(
        fom = this.fom,
        tom = this.tom,
        overføres = this.overføres,
        årsak = this.årsak,
        feilregistrering = this.feilregistrering
    )

}

private fun MeldeperiodeDto.tilPeriode(): Periode {
    return Periode(fom = fom , tom = tom)
}

private fun List<TilkjentYtelseTrekkDto>.tilTrekk(): List<TilkjentYtelseTrekk> {
    return map {
        TilkjentYtelseTrekk(
            dato = it.dato,
            beløp = it.beløp,
        )
    }
}

