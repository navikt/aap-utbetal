package no.nav.aap.utbetal.utbetaling

import no.nav.aap.utbetal.tilkjentytelse.AvventPeriode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetaling.UtbetalingStatus
import java.util.UUID

class UtbetalingMedSlettingAvAvventUtbetalingBeregninger {

    fun opprettUtbetalingMedSlettingAvAvventUtbetaling(
        utbetalingRef: UUID,
        forrigeAvventPeriode: AvventPeriode,
        nyTilkjentYtelse: TilkjentYtelse,
    ): Utbetaling? {
        if (nyTilkjentYtelse.avvent == null) return null

        val gammelFom = forrigeAvventPeriode.avvent.fom
        val gammelTom = forrigeAvventPeriode.avvent.tom
        val nyFom = nyTilkjentYtelse.avvent.fom
        if (nyFom < gammelFom) {
            return Utbetaling(
                saksnummer = nyTilkjentYtelse.saksnummer,
                behandlingsreferanse = forrigeAvventPeriode.behandlingRef,
                tilkjentYtelseId = forrigeAvventPeriode.tilkjentYtelseId,
                personIdent = nyTilkjentYtelse.personIdent,
                vedtakstidspunkt = nyTilkjentYtelse.vedtakstidspunkt,
                beslutterId = nyTilkjentYtelse.beslutterId,
                saksbehandlerId = nyTilkjentYtelse.saksbehandlerId,
                utbetalingOversendt = nyTilkjentYtelse.vedtakstidspunkt,
                utbetalingStatus = UtbetalingStatus.OPPRETTET,
                perioder = emptyList(), //Ikke send perioder ifm sletting av avvent utbetaling
                avvent = UtbetalingAvvent(
                    fom = gammelFom,
                    tom = gammelTom,
                    overføres = forrigeAvventPeriode.overføres,
                    årsak = forrigeAvventPeriode.årsak,
                    feilregistrering = true
                ),
                utbetalingRef = utbetalingRef //TODO hva skal denne være?
            )
        }
        return null
    }


}