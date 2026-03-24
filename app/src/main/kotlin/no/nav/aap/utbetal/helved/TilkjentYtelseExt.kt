package no.nav.aap.utbetal.helved

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvvent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingIdMap
import no.nav.aap.utbetaling.helved.toBase64

fun TilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap: MeldeperiodeUtbetalingIdMap): UtbetalingMelding {
    val utbetalingMelding = UtbetalingMelding(
        sakId = this.saksnummer.toString(),
        behandlingId = this.behandlingsreferanse.toBase64(),
        ident = this.personIdent,
        utbetalinger = this.perioder.tilUtbetalinger(meldeperiodeUtbetalingMap),
        vedtakstidspunkt = this.vedtakstidspunkt,
        saksbehandling = this.saksbehandlerId,
        beslutter = this.beslutterId,
        avvent = this.avvent?.tilAvvent(),
    )
    return utbetalingMelding
}

private fun TilkjentYtelseAvvent.tilAvvent() =
    Avvent(
        fom = this.fom.toString(),
        tom = this.tom.toString(),
        overføres = this.overføres.toString(),
        årsak = this.årsak?.toString()
    )

private fun List<TilkjentYtelsePeriode>.tilUtbetalinger(meldeperiodeUtbetalingMap: MeldeperiodeUtbetalingIdMap) =
    this.map { tyPeriode ->
        val meldeperiode = tyPeriode.detaljer.meldeperiode
            ?: error("Meldeperiode må være satt for å kunne sende utbetaling. Skal være satt for alle nye tilkjent ytelse perioder.")
        val utbetalingId = meldeperiodeUtbetalingMap[meldeperiode]
            ?: error("Finner ikke utbetalingId for meldeperiode: $meldeperiode. UtbetalingId må være satt for å kunne sende utbetaling.")
        Utbetaling(
            id = utbetalingId.toString(),
            fom = tyPeriode.periode.fom.toString(),
            tom = tyPeriode.periode.tom.toString(),
            sats = tyPeriode.detaljer.dagsats.avrundet(),
            utbetaltBeløp = tyPeriode.detaljer.redusertDagsats.avrundet(),
        )
    }

//Siden Beløp her alltid er heltall, så holder det å trunkere til UInt.
private fun Beløp.avrundet() = this.verdi.toInt().toUInt()