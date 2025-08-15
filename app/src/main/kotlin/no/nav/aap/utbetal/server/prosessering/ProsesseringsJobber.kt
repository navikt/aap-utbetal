package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.motor.JobbSpesifikasjon

object ProsesseringsJobber {

    fun alle(): List<JobbSpesifikasjon> {
        // Legger her alle oppgavene som skal utføres i systemet
        return listOf(
            OpprettUtbetalingUtfører,
            OverførTilØkonomiJobbUtfører,
            SjekkKvitteringFraØkonomiUtfører,
        )
    }
}