package no.nav.aap.utbetal.helved

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvvent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingIdMap
import no.nav.aap.utbetaling.helved.toBase64
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDate
import java.util.UUID

fun TilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap: MeldeperiodeUtbetalingIdMap): UtbetalingMelding {
    val utbetalingMelding = UtbetalingMelding(
        sakId = this.saksnummer.toString(),
        behandlingId = this.behandlingsreferanse.toBase64(),
        ident = this.personIdent,
        utbetalinger = this.perioder.tilUtbetalinger(meldeperiodeUtbetalingMap, this.saksnummer),
        vedtakstidspunktet = this.vedtakstidspunkt,
        saksbehandler = this.saksbehandlerId,
        beslutter = this.beslutterId,
        avvent = this.avvent?.tilAvvent(),
    )
    return utbetalingMelding
}

@VisibleForTesting
internal fun TilkjentYtelseAvvent.tilAvvent() =
    Avvent(
        fom = this.fom.toString(),
        tom = this.tom.toString(),
        overføres = this.overføres?.toString(),
        årsak = this.årsak?.toString(),
        feilregistrering = this.feilregistrering,
    )

private fun List<TilkjentYtelsePeriode>.tilUtbetalinger(meldeperiodeUtbetalingMap: MeldeperiodeUtbetalingIdMap, saksnummer: Saksnummer): List<Utbetaling> {
    val iDag = LocalDate.now()
    return this
        .filter {
            //Bare send over perioder som har beløp større enn 0, og som har utbetalingdato som er i dag eller tidligere.
            it.detaljer.redusertDagsats.avrundet() > 0u && it.detaljer.utbetalingsdato <= iDag
        }
        .map { tyPeriode ->
            val meldeperiode = tyPeriode.detaljer.meldeperiode
                ?: error("Meldeperiode må være satt for å kunne sende utbetaling. Skal være satt for alle nye tilkjent ytelse perioder.")
            val utbetalingId = meldeperiodeUtbetalingMap.finnMatchendeUtbetalingsreferanse(meldeperiode, saksnummer)
            Utbetaling(
                id = utbetalingId.toString(),
                fom = tyPeriode.periode.fom.toString(),
                tom = tyPeriode.periode.tom.toString(),
                sats = tyPeriode.detaljer.dagsatsMedBarnetillegg().avrundet(),
                utbetaltBeløp = tyPeriode.detaljer.redusertDagsats.avrundet(),
            )
        }
}

private fun MeldeperiodeUtbetalingIdMap.finnMatchendeUtbetalingsreferanse(periode: Periode, saksnummer: Saksnummer): UUID {
    val overlappendePerioder = this.entries.filter {   (meldeperiode, _) -> meldeperiode.overlapper(periode) }
    if (overlappendePerioder.isEmpty()) {
        error("Ingen overlappende meldeperiode for periode: $periode. UtbetalingId må være satt for å kunne sende utbetaling. Gjelder sak: $saksnummer")
    }
    if (overlappendePerioder.size > 1) {
        error("Flere overlappende meldeperioder for periode: $periode. Gjelder sak: $saksnummer.")
    }
    return overlappendePerioder.first().value
}

//Siden Beløp her alltid er heltall, så holder det å trunkere til UInt.
private fun Beløp.avrundet() = this.verdi.toInt().toUInt()