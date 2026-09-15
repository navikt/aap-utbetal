package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.motor.JobbSpesifikasjon
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.OpprettUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.SjekkKvitteringFraØkonomiUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendSlettAvventPeriodeUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.OpprettUtbetalingsmeldingUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SjekkStatusForUtbetalingerUtfører

object ProsesseringsJobber {

    fun alle(): List<JobbSpesifikasjon> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            // Gammelt grensesnitt for utbetalinger
            OpprettUtbetalingUtfører,
            OverførTilØkonomiJobbUtfører,
            SjekkKvitteringFraØkonomiUtfører,
            //Nytt grensesnitt for utbetalinger
            OpprettUtbetalingsmeldingUtfører,
            SendUtbetalingsmeldingUtfører,
            SendSlettAvventPeriodeUtfører,
            SjekkStatusForUtbetalingerUtfører,
        )
    }
}