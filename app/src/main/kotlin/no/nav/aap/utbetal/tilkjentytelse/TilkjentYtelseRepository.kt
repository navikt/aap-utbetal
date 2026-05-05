package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AvventPeriode (
    val tilkjentYtelseId: Long,
    val behandlingRef: UUID,
    val avvent: Periode,
    val overføres: LocalDate?,
    val årsak: AvventÅrsak?,
    val feilregistrering: Boolean,
    val vedtakstidspunkt: LocalDateTime,
)

class TilkjentYtelseRepository(private val connection: DBConnection) {

    fun lagreTilkjentYtelse(tilkjentYtelse: TilkjentYtelse): Long {
        val sqlInsertTilkjentYtelse = """
            INSERT INTO TILKJENT_YTELSE 
                (SAKSNUMMER, BEHANDLING_REF, FORRIGE_BEHANDLING_REF, PERSON_IDENT, VEDTAKSTIDSPUNKT, BESLUTTER_ID, SAKSBEHANDLER_ID, NY_MELDEPERIODE)
                VALUES(?, ? ,?, ?, ?, ?, ?, ?::daterange)
        """.trimIndent()

        val tilkjentYtelseId = connection.executeReturnKey(sqlInsertTilkjentYtelse) {
            setParams {
                setString(1, tilkjentYtelse.saksnummer.toString())
                setUUID(2, tilkjentYtelse.behandlingsreferanse)
                setUUID(3, tilkjentYtelse.forrigeBehandlingsreferanse)
                setString(4, tilkjentYtelse.personIdent)
                setLocalDateTime(5, tilkjentYtelse.vedtakstidspunkt)
                setString(6, tilkjentYtelse.beslutterId)
                setString(7, tilkjentYtelse.saksbehandlerId)
                setPeriode(8, tilkjentYtelse.nyMeldeperiode)
            }
        }

        lagreTilkjentYtelsePerioder(tilkjentYtelseId, tilkjentYtelse.perioder)
        tilkjentYtelse.avvent?.let { lagreTilkjentYtelseAvvent(tilkjentYtelseId, it) }
        lagreTilkjentYtelseTrekk(tilkjentYtelseId, tilkjentYtelse.trekk)

        return tilkjentYtelseId
    }

    fun hentTilkjentYtelseAvventHistorikk(saksnummer: Saksnummer): List<AvventPeriode> {
        val sql = """
            SELECT 
                TILKJENT_YTELSE.ID AS TILKJENT_YTELSE_ID,
                BEHANDLING_REF,
                PERIODE,
                OVERFORES,
                ARSAK,
                FEILREGISTRERING,
                VEDTAKSTIDSPUNKT
            FROM TILKJENT_YTELSE_AVVENT
            JOIN TILKJENT_YTELSE ON TILKJENT_YTELSE.ID = TILKJENT_YTELSE_AVVENT.TILKJENT_YTELSE_ID
            WHERE SAKSNUMMER = ?
            ORDER BY VEDTAKSTIDSPUNKT
        """.trimIndent()

        return connection.queryList(sql) {
            setParams {
                setString(1, saksnummer.toString())
            }

            setRowMapper {
                AvventPeriode(
                    tilkjentYtelseId = it.getLong("tilkjent_ytelse_id"),
                    behandlingRef = it.getUUID("BEHANDLING_REF"),
                    avvent = it.getPeriode("PERIODE"),
                    overføres = it.getLocalDateOrNull("OVERFORES"),
                    årsak = AvventÅrsak.valueOf(it.getString("ARSAK")),
                    feilregistrering = it.getBoolean("FEILREGISTRERING"),
                    vedtakstidspunkt = it.getLocalDateTime("VEDTAKSTIDSPUNKT"),
                )
            }
        }

    }

    private fun lagreTilkjentYtelsePerioder(tilkjentYtelseId: Long, tilkjentPerioder: List<TilkjentYtelsePeriode>) {
        val sqlInsertTilkjentPeriode = """
            INSERT INTO TILKJENT_PERIODE
                (
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
                    TILKJENT_YTELSE_ID,
                    UTBETALINGSDATO,
                    TREKK_POSTERING_ID,
                    MELDEPERIODE,
                    BARNEPENSJON_DAGSATS
                )
                VALUES (?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange, ?)
        """.trimIndent()


        connection.executeBatch(sqlInsertTilkjentPeriode, tilkjentPerioder) {
            setParams {
                setPeriode(1, it.periode)
                setBigDecimal(2, it.detaljer.dagsats.verdi())
                setBigDecimal(3, it.detaljer.grunnlag.verdi())
                setInt(4, it.detaljer.gradering.prosentverdi())
                setBigDecimal(5, it.detaljer.grunnbeløp.verdi())
                setInt(6, it.detaljer.antallBarn)
                setBigDecimal(7, it.detaljer.barnetillegg.verdi())
                setBigDecimal(8, it.detaljer.grunnlagsfaktor.verdi())
                setBigDecimal(9, it.detaljer.barnetilleggsats.verdi())
                setBigDecimal(10, it.detaljer.redusertDagsats.verdi())
                setLong(11, tilkjentYtelseId)
                setLocalDate(12, it.detaljer.utbetalingsdato)
                setLong(13, it.detaljer.trekkPosteringId)
                setPeriode(14, it.detaljer.meldeperiode)
                setBigDecimal(15, it.detaljer.barnepensjonDagsats.verdi())
            }
        }
    }

    private fun lagreTilkjentYtelseAvvent(tilkjentYtelseId: Long, avvent: TilkjentYtelseAvvent) {
        val sqlInsertAvvent = """
            INSERT INTO TILKJENT_YTELSE_AVVENT 
                (
                    TILKJENT_YTELSE_ID,
                    PERIODE,
                    OVERFORES,
                    ARSAK,
                    FEILREGISTRERING
                )
                VALUES (?, ?::daterange, ?, ?, ?)
        """.trimIndent()

        connection.execute(sqlInsertAvvent) {
            setParams {
                setLong(1, tilkjentYtelseId)
                setPeriode(2, Periode(avvent.fom, avvent.tom))
                setLocalDate(3, avvent.overføres)
                setString(4, avvent.årsak?.name)
                setBoolean(5, avvent.feilregistrering)
            }
        }
    }

    private fun lagreTilkjentYtelseTrekk(tilkjentYtelseId: Long, tilkjentYtelseTrekk: List<TilkjentYtelseTrekk>) {
        val sqlInsertTrekk = """
            INSERT INTO TILKJENT_YTELSE_TREKK
                (
                   TILKJENT_YTELSE_ID,
                   DATO,
                   BELOP
               )
               VALUES (?, ?, ?)
        """.trimIndent()

        connection.executeBatch(sqlInsertTrekk, tilkjentYtelseTrekk) {
            setParams {
                setLong(1, tilkjentYtelseId)
                setLocalDate(2, it.dato)
                setInt(3, it.beløp)
            }
        }
    }


    fun hent(behandlingReferanse: UUID): TilkjentYtelse? {
        val selectTilkjentYtelse = """
            SELECT 
                ID,
                SAKSNUMMER,
                BEHANDLING_REF,
                FORRIGE_BEHANDLING_REF,
                PERSON_IDENT,
                VEDTAKSTIDSPUNKT,
                BESLUTTER_ID,
                SAKSBEHANDLER_ID,
                NY_MELDEPERIODE
            FROM 
                TILKJENT_YTELSE
            WHERE
                BEHANDLING_REF = ?
        """.trimIndent()


        val tilkjentYtelse = connection.queryFirstOrNull(selectTilkjentYtelse) {
            setParams {
                setUUID(1, behandlingReferanse)
            }
            setRowMapper { row ->
                TilkjentYtelse(
                    id = row.getLong("ID"),
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    behandlingsreferanse = row.getUUID("BEHANDLING_REF"),
                    forrigeBehandlingsreferanse = row.getUUIDOrNull("FORRIGE_BEHANDLING_REF"),
                    personIdent = row.getString("PERSON_IDENT"),
                    vedtakstidspunkt = row.getLocalDateTime("VEDTAKSTIDSPUNKT"),
                    beslutterId = row.getString("BESLUTTER_ID"),
                    saksbehandlerId = row.getString("SAKSBEHANDLER_ID"),
                    nyMeldeperiode = row.getPeriodeOrNull("NY_MELDEPERIODE"),
                    perioder = listOf(),
                )
            }
        }
        return tilkjentYtelse?.copy(
            perioder = hentTilkjentePerioder(tilkjentYtelse.id!!),
            avvent = hentAvvent(tilkjentYtelse.id),
            trekk = hentTrekk(tilkjentYtelse.id),
        )
    }


    fun hentTilkjentYtelseLight(behandlingReferanse: UUID): TilkjentYtelseLight? {
        val selectTilkjentYtelse = """
            SELECT 
                ID,
                SAKSNUMMER,
                BEHANDLING_REF
            FROM 
                TILKJENT_YTELSE
            WHERE
                BEHANDLING_REF = ?
        """.trimIndent()


        return connection.queryFirstOrNull(selectTilkjentYtelse) {
            setParams {
                setUUID(1, behandlingReferanse)
            }
            setRowMapper { row ->
                TilkjentYtelseLight(
                    id = row.getLong("ID"),
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    behandlingsreferanse = row.getUUID("BEHANDLING_REF"),
                )
            }
        }
    }


    private fun hentAvvent(tilkjentYtelseId: Long): TilkjentYtelseAvvent? {
        val sqlHentAvvent = """
            SELECT
                TILKJENT_YTELSE_ID,
                PERIODE,
                OVERFORES,
                ARSAK,
                FEILREGISTRERING
            FROM TILKJENT_YTELSE_AVVENT
            WHERE TILKJENT_YTELSE_ID = ?
        """.trimIndent()

        return connection.queryFirstOrNull(sqlHentAvvent) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->
                val periode = row.getPeriode("PERIODE")
                TilkjentYtelseAvvent(
                    fom = periode.fom,
                    tom = periode.tom,
                    overføres = row.getLocalDateOrNull("OVERFORES"),
                    årsak = AvventÅrsak.valueOf(row.getString("ARSAK")),
                    feilregistrering = row.getBoolean("FEILREGISTRERING"),
                )
            }
        }
    }

    private fun hentTilkjentePerioder(tilkjentYtelseId: Long): List<TilkjentYtelsePeriode> {
        val selectTilkjentePerioder = """
            SELECT 
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
                UTBETALINGSDATO,
                TREKK_POSTERING_ID,
                MELDEPERIODE,
                BARNEPENSJON_DAGSATS
            FROM TILKJENT_PERIODE
            WHERE TILKJENT_YTELSE_ID = ? 
        """.trimIndent()

        return connection.queryList(selectTilkjentePerioder) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->

                val periode = row.getPeriode("PERIODE")
                TilkjentYtelsePeriode(
                    periode = periode,
                    detaljer = YtelseDetaljer(
                        dagsats = Beløp(row.getBigDecimal("DAGSATS")),
                        grunnlag = Beløp(row.getBigDecimal("GRUNNLAG")),
                        gradering = Prosent(row.getInt("GRADERING")),
                        grunnbeløp = Beløp(row.getBigDecimal("GRUNNBELOP")),
                        barnepensjonDagsats = Beløp(row.getBigDecimal("BARNEPENSJON_DAGSATS")),
                        antallBarn = row.getInt("ANTALL_BARN"),
                        barnetillegg = Beløp(row.getBigDecimal("BARNETILLEGG")),
                        grunnlagsfaktor = GUnit(row.getBigDecimal("GRUNNLAGSFAKTOR")),
                        barnetilleggsats = Beløp(row.getBigDecimal("BARNETILLEGGSATS")),
                        redusertDagsats = Beløp(row.getBigDecimal("REDUSERT_DAGSATS")),
                        utbetalingsdato = row.getLocalDate("UTBETALINGSDATO"),
                        trekkPosteringId = row.getLongOrNull("TREKK_POSTERING_ID"),
                        meldeperiode = row.getPeriodeOrNull("MELDEPERIODE"),
                    )
                )
            }
        }
    }

    private fun hentTrekk(tilkjentYtelseId: Long): List<TilkjentYtelseTrekk> {
        val sqlHentTrekk = """
            SELECT 
                DATO, BELOP
            FROM TILKJENT_YTELSE_TREKK
            WHERE TILKJENT_YTELSE_ID = ?
        """.trimIndent()

        return connection.queryList(sqlHentTrekk) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->
                TilkjentYtelseTrekk(
                    dato = row.getLocalDate("DATO"),
                    beløp = row.getInt("BELOP"),
                )

            }
        }
    }

}
