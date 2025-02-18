package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.math.BigDecimal
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
                    DAGSATS,           
                    GRUNNLAG,          
                    GRADERING,         
                    GRUNNBELOP,        
                    ANTALL_BARN,
                    BARNETILLEGG,  
                    GRUNNLAGSFAKTOR,
                    BARNETILLEGGSATS,
                    REDUSERT_DAGSATS,
                    UTBETALINGSPERIODE_TYPE,
                    VENTEDAGER_SAMORDNING
                )
                VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(insertUtbetalingsperiodeSql, utbetalingsperioder) {
            setParams {
                setLong(1, utbetalingId)
                setPeriode(2, it.periode)
                setBigDecimal(3, it.detaljer.dagsats.verdi())
                setBigDecimal(4, it.detaljer.grunnlag.verdi())
                setBigDecimal(5, BigDecimal.valueOf(it.detaljer.gradering.prosentverdi().toLong()))
                setBigDecimal(6, it.detaljer.grunnbeløp.verdi())
                setInt(7, it.detaljer.antallBarn)
                setBigDecimal(8, it.detaljer.barnetillegg.verdi())
                setBigDecimal(9, it.detaljer.grunnlagsfaktor.verdi())
                setBigDecimal(10, it.detaljer.barnetilleggsats.verdi())
                setBigDecimal(11, it.detaljer.redusertDagsats.verdi())
                setString(12, it.utbetalingsperiodeType.name)
                setBoolean(13, it.detaljer.ventedagerSamordning)
            }
        }

    }


    fun hent(behandlingsreferanse: UUID): List<Utbetaling> {
        val hentUtbetalingSql = """
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
        
        return connection.queryList(hentUtbetalingSql) {
            setParams { 
                setUUID(1, behandlingsreferanse)
            }
            setRowMapper { row ->
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
            }
        }
    }

    private fun hentUtbetalingsperioder(utbetalingId: Long): List<Utbetalingsperiode> {
        val hentUtbetalingsperioderSql = """
            SELECT
                ID,
                PERIODE,
                DAGSATS,           
                GRUNNLAG,          
                GRADERING,         
                GRUNNBELOP,        
                ANTALL_BARN,
                BARNETILLEGG,  
                GRUNNLAGSFAKTOR,
                BARNETILLEGGSATS,
                REDUSERT_DAGSATS,
                VENTEDAGER_SAMORDNING,
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
                    utbetalingsperiodeType = UtbetalingsperiodeType.valueOf(row.getString("UTBETALINGSPERIODE_TYPE")),
                    detaljer = YtelseDetaljer(
                        dagsats = Beløp(row.getBigDecimal("DAGSATS")),
                        grunnlag = Beløp(row.getBigDecimal("GRUNNLAG")),
                        gradering = Prosent.fraDesimal(row.getBigDecimal("GRADERING")),
                        grunnbeløp = Beløp(row.getBigDecimal("GRUNNBELOP")),
                        antallBarn = row.getInt("ANTALL_BARN"),
                        barnetillegg = Beløp(row.getBigDecimal("BARNETILLEGG")),
                        grunnlagsfaktor = GUnit(row.getBigDecimal("GRUNNLAGSFAKTOR")),
                        barnetilleggsats = Beløp(row.getBigDecimal("BARNETILLEGGSATS")),
                        redusertDagsats = Beløp(row.getBigDecimal("REDUSERT_DAGSATS")),
                        ventedagerSamordning = row.getBoolean(("VENTEDAGER_SAMORDNING")),
                    )
                )
            }
        }
    }

    fun oppdaterStatus(utbetalingId: Long, status: UtbetalingStatus) {
        val oppdaterStatusSql = "UPDATE UTBETALING SET UTBETALING_STATUS = ? WHERE id = ?"
        connection.execute(oppdaterStatusSql) {
            setParams {
                setString(1, status.name)
                setLong(2, utbetalingId)
            }
        }
    }

}

