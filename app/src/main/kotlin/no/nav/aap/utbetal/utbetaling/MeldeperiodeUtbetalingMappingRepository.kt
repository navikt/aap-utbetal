package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import java.util.UUID

private data class MeldperiodeUtbetalingMapping(
    val meldeperiode: Periode,
    val utbetalingId: UUID
)

typealias MeldeperiodeUtbetalingIdMap = Map<Periode, UUID>

class MeldeperiodeUtbetalingMappingRepository(val connection: DBConnection) {

    fun hentMeldeperiodeUtbetalingMapping(sakUtbetalingId: Long): MeldeperiodeUtbetalingIdMap {
        val sql = """
            SELECT MELDEPERIODE, UTBETALING_ID 
            FROM MELDEPERIODE_UTBETALING_MAPPING
            WHERE SAK_UTBETALING_ID = ?
        """.trimIndent()

        val sakUtbetalingMappingList = connection.queryList(sql) {
            setParams {
                setLong(1, sakUtbetalingId)
            }
            setRowMapper { row ->
                MeldperiodeUtbetalingMapping(
                    row.getPeriode("MELDEPERIODE"),
                    row.getUUID("UTBETALING_ID"),
                )
            }
        }
        return sakUtbetalingMappingList.associate { it.meldeperiode to it.utbetalingId }
    }

    fun oppdatereMeldeperiodeUtbetalingMapping(sakUtbetalingId: Long, tilkjentYtelse: TilkjentYtelse, lagreResultat: Boolean): MeldeperiodeUtbetalingIdMap {
        val meldeperiodeUtbetalingMap = hentMeldeperiodeUtbetalingMapping(sakUtbetalingId).toMutableMap()

        tilkjentYtelse.perioder.forEach { tilkjentYtelsePeriode ->
            val periode = tilkjentYtelsePeriode.periode
            val lagretMeldeperiode = meldeperiodeUtbetalingMap
                .keys
                .firstOrNull { it.overlapper(periode) }
            if (lagretMeldeperiode == null) {
                val utbetalingId = UUID.randomUUID()
                if (lagreResultat) {
                    lagreMeldeperiodeUtbetalingMapping(sakUtbetalingId, periode, utbetalingId)
                }
                meldeperiodeUtbetalingMap[periode] = utbetalingId
            }
        }
        return meldeperiodeUtbetalingMap.toMap()
    }

    fun lagreMeldeperiodeUtbetalingMapping(sakUtbetalingId: Long, meldeperiode: Periode, utbetalingId: UUID) {
        connection.execute(
            """
            INSERT INTO MELDEPERIODE_UTBETALING_MAPPING (SAK_UTBETALING_ID, MELDEPERIODE, UTBETALING_ID) 
            VALUES (?, ?::daterange, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sakUtbetalingId)
                setPeriode(2, meldeperiode)
                setUUID(3, utbetalingId)
            }
        }
    }

}