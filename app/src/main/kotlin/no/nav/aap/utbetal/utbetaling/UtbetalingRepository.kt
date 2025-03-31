package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingLight(
    val id: Long,
    val utbetalingRef: UUID,
)

class UtbetalingRepository(private val connection: DBConnection) {

    fun lagre(utbetaling: Utbetaling): Long {
        slettTidligereUtbetaling(utbetaling.utbetalingRef)
        var insertUtbetalingSql = """
            INSERT INTO UTBETALING
                (
                    SAKSNUMMER,
                    BEHANDLING_REF,
                    SAK_UTBETALING_ID,
                    TILKJENT_YTELSE_ID,
                    PERSON_IDENT,
                    VEDTAKSTIDSPUNKT,
                    BESLUTTER_IDENT,
                    SAKSBEHANDLER_IDENT,
                    UTBETALING_OPPRETTET,
                    UTBETALING_STATUS,
                    UTBETALING_REF
                ) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val utbetalingId = connection.executeReturnKey(insertUtbetalingSql) {
            setParams {
                setString(1, utbetaling.saksnummer.toString())
                setUUID(2, utbetaling.behandlingsreferanse)
                setLong(3, utbetaling.sakUtbetalingId)
                setLong(4, utbetaling.tilkjentYtelseId)
                setString(5, utbetaling.personIdent)
                setLocalDateTime(6, utbetaling.vedtakstidspunkt)
                setString(7, utbetaling.beslutterId)
                setString(8, utbetaling.saksbehandlerId)
                setLocalDateTime(9, LocalDateTime.now())
                setString(10, utbetaling.utbetalingStatus.name)
                setUUID(11, utbetaling.utbetalingRef)
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
                    UTBETALINGSPERIODE_TYPE,
                    UTBETALINGSDATO
                )
                VALUES (?, ?::daterange, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(insertUtbetalingsperiodeSql, utbetalingsperioder) {
            setParams {
                setLong(1, utbetalingId)
                setPeriode(2, it.periode)
                setInt(3, it.beløp.toInt())
                setInt(4, it.fastsattDagsats.toInt())
                setString(5, it.utbetalingsperiodeType.name)
                setLocalDate(6, it.utbetalingsdato)
            }
        }

    }

    fun hent(saksnummer: Saksnummer): List<Utbetaling> {
        val hentUtbetalingerSql = """
            SELECT 
                ID,
                SAKSNUMMER,
                BEHANDLING_REF,
                UTBETALING_REF,
                SAK_UTBETALING_ID,
                TILKJENT_YTELSE_ID,
                PERSON_IDENT,
                VEDTAKSTIDSPUNKT,
                BESLUTTER_IDENT,
                SAKSBEHANDLER_IDENT,
                UTBETALING_OPPRETTET,
                UTBETALING_ENDRET,
                UTBETALING_STATUS
            FROM 
                UTBETALING
            WHERE
                SAKSNUMMER = ? AND
                SLETTET = FALSE
                
        """.trimIndent()

        return connection.queryList(hentUtbetalingerSql) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { mapUtbetaling(it) }
        }
    }

    fun hent(behandlingsreferanse: UUID): List<Utbetaling> {
        val hentUtbetalingerSql = """
            SELECT 
                U.ID,
                U.SAKSNUMMER,
                U.BEHANDLING_REF,
                U.UTBETALING_REF,
                U.SAK_UTBETALING_ID,
                U.TILKJENT_YTELSE_ID,
                U.PERSON_IDENT,
                U.VEDTAKSTIDSPUNKT,
                U.BESLUTTER_IDENT,
                U.SAKSBEHANDLER_IDENT,
                U.UTBETALING_OPPRETTET,
                U.UTBETALING_ENDRET,
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

    fun hentAlleSendteUtbetalinger(): List<UtbetalingLight> {
        val hentAlleSendteUtbetalingerSql = """
            SELECT 
                ID,
                UTBETALING_REF
            FROM 
                UTBETALING
            WHERE
                UTBETALING_STATUS = 'SENDT' AND
                SLETTET = FALSE
        """.trimIndent()

        return connection.queryList(hentAlleSendteUtbetalingerSql) {
            setRowMapper { row ->
                UtbetalingLight(
                    id = row.getLong("ID"),
                    utbetalingRef = row.getUUID("UTBETALING_REF"),
                )
            }
        }
    }

    fun hentUtbetaling(utbetalingId: Long): Utbetaling {
        val hentUtbetalingSql = """
            SELECT 
                ID,
                SAKSNUMMER,
                BEHANDLING_REF,
                SAK_UTBETALING_ID,
                TILKJENT_YTELSE_ID,
                PERSON_IDENT,
                VEDTAKSTIDSPUNKT,
                BESLUTTER_IDENT,
                SAKSBEHANDLER_IDENT,
                UTBETALING_OPPRETTET,
                UTBETALING_ENDRET,
                UTBETALING_STATUS,
                UTBETALING_REF
            FROM 
                UTBETALING
            WHERE
                ID = ? AND
                SLETTET = FALSE
        """.trimIndent()

        return connection.queryFirst(hentUtbetalingSql) {
            setParams { setLong(1, utbetalingId) }
            setRowMapper { mapUtbetaling(it) }
        }
    }

    private fun mapUtbetaling(row: Row, medPeriode: Boolean = true): Utbetaling {
        val utbetaling = Utbetaling(
            id = row.getLong("ID"),
            saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
            behandlingsreferanse = row.getUUID("BEHANDLING_REF"), sakUtbetalingId = row.getLong("SAK_UTBETALING_ID"),
            tilkjentYtelseId = row.getLong("TILKJENT_YTELSE_ID"),
            personIdent = row.getString("PERSON_IDENT"),
            vedtakstidspunkt = row.getLocalDateTime("VEDTAKSTIDSPUNKT"),
            beslutterId = row.getString("BESLUTTER_IDENT"),
            saksbehandlerId = row.getString("SAKSBEHANDLER_IDENT"),
            utbetalingOversendt = row.getLocalDateTime("UTBETALING_OPPRETTET"),
            utbetalingEndret = row.getLocalDateTimeOrNull("UTBETALING_ENDRET"),
            utbetalingStatus = UtbetalingStatus.valueOf(row.getString("UTBETALING_STATUS")),
            perioder = listOf(),
            utbetalingRef = row.getUUID("UTBETALING_REF")
        )
        if (medPeriode) {
            return utbetaling.copy(perioder = hentUtbetalingsperioder(utbetaling.id!!))
        }
        return utbetaling
    }

    private fun hentUtbetalingsperioder(utbetalingId: Long): List<Utbetalingsperiode> {
        val hentUtbetalingsperioderSql = """
            SELECT
                ID,
                PERIODE,
                BELOP,
                FASTSATT_DAGSATS,
                UTBETALINGSPERIODE_TYPE,
                UTBETALINGSDATO
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
                    utbetalingsperiodeType = UtbetalingsperiodeType.valueOf(row.getString("UTBETALINGSPERIODE_TYPE")),
                    utbetalingsdato = row.getLocalDate("UTBETALINGSDATO")

                )
            }
        }
    }

    private fun slettTidligereUtbetaling(utbetalingRef: UUID) {
        val logiskSlettGammelUtbetaling = """
            UPDATE 
                UTBETALING
            SET 
                SLETTET = TRUE,
                UTBETALING_ENDRET = CURRENT_TIMESTAMP
            WHERE 
                UTBETALING_REF = ?
        """.trimIndent()

        connection.execute(logiskSlettGammelUtbetaling) {
            setParams {
                setUUID(1, utbetalingRef)
            }
        }
    }

    fun oppdaterStatus(utbetalingId: Long, status: UtbetalingStatus) {
        val oppdaterStatusSql = """
            UPDATE 
                UTBETALING 
            SET 
                UTBETALING_STATUS = ?, 
                UTBETALING_ENDRET = CURRENT_TIMESTAMP
            WHERE 
                ID = ?
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

