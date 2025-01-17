package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID


class UtbetalingsplanRepository(private val connection: DBConnection) {

    fun lagre(utbetalingsplan: Utbetalingsplan) {
        var insertUtbetalingsplanSql = """
            INSERT INTO UTBETALINGSPLAN
                (SAK_UTBETALING_ID, TILKJENT_YTELSE_ID, UTBETALING_OVERSENDT) 
                VALUES (?, ?, ?, 'SENDT')
        """.trimIndent()

        val utbetalingsplanId = connection.executeReturnKey(insertUtbetalingsplanSql) {
            setParams {
                setLong(1, utbetalingsplan.sakUtbetalingId)
                setLong(2, utbetalingsplan.tilkjentYtelseId)
                setLocalDateTime(3, LocalDateTime.now())
            }
        }

        lagre(utbetalingsplanId, utbetalingsplan.perioder)
    }

    private fun lagre(utbetalingsplanId: Long, utbetalingsperioder: List<Utbetalingsperiode>) {
        val insertUtbetalingsperiodeSql = """
            INSERT INTO UTBETALINGSPERIODE
                (
                    PERIODE,
                    UTBETALINGSPLAN_ID,
                    DAGSATS,           
                    GRUNNLAG,          
                    GRADERING,         
                    GRUNNBELOP,        
                    ANTALL_BARN,
                    BARNETILLEGG,  
                    GRUNNLAGSFAKTOR,
                    BARNETILLEGGSATS,
                    REDUSERT_DAGSATS
                )
                VALUES (?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(insertUtbetalingsperiodeSql, utbetalingsperioder) {
            setParams {
                setLong(1, utbetalingsplanId)
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
            }
        }

    }


    fun hent(behandlingsreferanse: UUID): Utbetalingsplan? {
        TODO()
    }

    //TODO: metode for utbetalingsplan

}

