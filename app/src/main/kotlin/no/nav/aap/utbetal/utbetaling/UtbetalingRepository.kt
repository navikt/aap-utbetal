package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingRepository(private val connection: DBConnection) {

    fun lagre(utbetaling: Utbetaling): Long {
        var insertUtbetalingSql = """
            INSERT INTO UTBETALING
                (UTBETALING_REF, SAK_UTBETALING_ID, TILKJENT_YTELSE_ID, UTBETALING_OVERSENDT, UTBETALING_STATUS) 
                VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        val utbetalingId = connection.executeReturnKey(insertUtbetalingSql) {
            setParams {
                setUUID(1, utbetaling.utbetalingRef)
                setLong(2, utbetaling.sakUtbetalingId)
                setLong(3, utbetaling.tilkjentYtelseId)
                setLocalDateTime(4, LocalDateTime.now())
                setString(5, "SENDT")
            }
        }

        lagre(utbetalingId, utbetaling.perioder)
        return utbetalingId
    }

    private fun lagre(utbetalingId: Long, utbetalingsperioder: List<Utbetalingsperiode>) {
        val insertUtbetalingsperiodeSql = """
            INSERT INTO UTBETALINGSPERIODE
                (
                    UTBETALING_ID,
                    PERIODE,
                    BELOP,           
                    FASTSATT_DAGSATS,
                    UTBETALINGSPERIODE_TYPE
                )
                VALUES (?, ?::daterange, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(insertUtbetalingsperiodeSql, utbetalingsperioder) {
            setParams {
                setLong(1, utbetalingId)
                setPeriode(2, it.periode)
                setInt(3, it.beløp.toInt())
                setInt(4, it.fastsattDagsats.toInt())
                setString(5, it.utbetalingsperiodeType.name)
            }
        }

    }

    fun hent(behandlingsreferanse: UUID): List<Utbetaling> {
        val hentUtbetalingerSql = """
            SELECT 
                U.ID,
                U.UTBETALING_REF,
                U.SAK_UTBETALING_ID,
                U.TILKJENT_YTELSE_ID,
                U.UTBETALING_OVERSENDT,
                U.UTBETALING_BEKREFTET,
                U.UTBETALING_STATUS
            FROM 
                UTBETALING U,
                TILKJENT_YTELSE TY
            WHERE
                TY.BEHANDLING_REF = ? AND
                TY.ID = U.TILKJENT_YTELSE_ID
        """.trimIndent()
        
        return connection.queryList(hentUtbetalingerSql) {
            setParams { 
                setUUID(1, behandlingsreferanse)
            }
            setRowMapper { mapUtbetaling(it) }
        }
    }

    fun hentUtbetaling(utbetalingId: Long): Utbetaling {
        val hentUtbetalingSql = """
            SELECT 
                ID,
                UTBETALING_REF,
                SAK_UTBETALING_ID,
                TILKJENT_YTELSE_ID,
                UTBETALING_OVERSENDT,
                UTBETALING_BEKREFTET,
                UTBETALING_STATUS
            FROM 
                UTBETALING
            WHERE
                ID = ?
        """.trimIndent()

        return connection.queryFirst(hentUtbetalingSql) {
            setParams { setLong(1, utbetalingId) }
            setRowMapper { mapUtbetaling(it) }
        }
    }

    private fun mapUtbetaling(row: Row): Utbetaling {
        val utbetaling = Utbetaling(
            id = row.getLong("ID"),
            utbetalingRef = row.getUUID("UTBETALING_REF"),
            sakUtbetalingId = row.getLong("SAK_UTBETALING_ID"),
            tilkjentYtelseId = row.getLong("TILKJENT_YTELSE_ID"),
            utbetalingOversendt = row.getLocalDateTime("UTBETALING_OVERSENDT"),
            utbetalingBekreftet = row.getLocalDateTimeOrNull("UTBETALING_BEKREFTET"),
            utbetalingStatus = UtbetalingStatus.valueOf(row.getString("UTBETALING_STATUS")),
            perioder = listOf()
        )
        utbetaling.copy(perioder = hentUtbetalingsperioder(utbetaling.id!!))
        return utbetaling
    }

    private fun hentUtbetalingsperioder(utbetalingId: Long): List<Utbetalingsperiode> {
        val hentUtbetalingsperioderSql = """
            SELECT
                ID,
                PERIODE,
                BELOP,
                FASTSATT_DAGSATS,
                UTBETALINGSPERIODE_TYPE
            FROM
                UTBETALINGSPERIODE
            WHERE
                UTBETALING_ID = ?
              
        """.trimIndent()

        return connection.queryList(hentUtbetalingsperioderSql) {
            setParams {
                setLong(1, utbetalingId)
            }
            setRowMapper { row ->
                Utbetalingsperiode(
                    id = row.getLong("ID"),
                    periode = row.getPeriode("PERIODE"),
                    beløp = row.getInt("BELOP").toUInt(),
                    fastsattDagsats = row.getInt("FASTSATT_DAGSATS").toUInt(),
                    utbetalingsperiodeType = UtbetalingsperiodeType.valueOf(row.getString("UTBETALINGSPERIODE_TYPE"))
                )
            }
        }
    }

    fun oppdaterStatus(utbetalingId: Long, status: UtbetalingStatus) {
        val oppdaterStatusSql = """
            UPDATE 
                UTBETALING 
            SET 
                UTBETALING_STATUS = ? 
            WHERE 
                id = ?
        """

        connection.execute(oppdaterStatusSql) {
            setParams {
                setString(1, status.name)
                setLong(2, utbetalingId)
            }
            setResultValidator { require(it == 1) }
        }
    }

}

