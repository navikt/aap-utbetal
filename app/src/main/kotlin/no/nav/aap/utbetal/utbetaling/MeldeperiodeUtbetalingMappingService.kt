package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import java.util.UUID

class MeldeperiodeUtbetalingMappingService(val connection: DBConnection) {

    fun oppdatereMeldeperiodeUtbetalingMapping(sakUtbetalingId: Long, tilkjentYtelse: TilkjentYtelse): MeldeperiodeUtbetalingIdMap {
        val repo = MeldeperiodeUtbetalingMappingRepository(connection)
        val meldeperiodeUtbetalingMapping = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)

        val mappingTidslinje = Tidslinje(meldeperiodeUtbetalingMapping.map { Segment(it.key, it.value) })
        val tyTidslinje = Tidslinje(tilkjentYtelse.perioder.map { Segment(it.periode, it) })

        val tyPerioderSomManglerMapping = tyTidslinje.kombiner(mappingTidslinje, StandardSammenslåere.minus())

        tyPerioderSomManglerMapping
            .perioder()
            .forEach { repo.lagreMeldeperiodeUtbetalingMapping(sakUtbetalingId, it, UUID.randomUUID()) }

        val oppdatertMeldeperiodeUtbetalingMapping =  repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)

        // Oppretter tidslinje for å sjekke at det ikke finnes overlappende perioder.
        Tidslinje(meldeperiodeUtbetalingMapping.map { Segment(it.key, it.value) })

        return oppdatertMeldeperiodeUtbetalingMapping
    }

}