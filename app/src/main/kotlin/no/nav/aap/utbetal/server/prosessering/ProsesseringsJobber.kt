package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.motor.JobbSpesifikasjon
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.OpprettUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.SjekkKvitteringFraØkonomiUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SjekkStatusForUtbetalingerUtfører

object ProsesseringsJobber {

    fun alle(): List<JobbSpesifikasjon> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            OpprettUtbetalingUtfører,
            OverførTilØkonomiJobbUtfører,
            SjekkKvitteringFraØkonomiUtfører,
            SendUtbetalingUtfører,
            SjekkStatusForUtbetalingerUtfører,
        )
    }
}