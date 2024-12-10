package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.math.BigDecimal
import java.util.UUID

data class TilkjentYtelse(
    val saksnummer: Saksnummer,
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,

    val perioder: List<TilkjentYtelsePeriode>
)

data class TilkjentYtelsePeriode(
    val periode: Periode,
    val detaljer: TilkjentYtelseDetaljer
)

data class TilkjentYtelseDetaljer(
    val redusertDagsats: Beløp,
    val gradering: Prosent,
    val dagsats: Beløp,
    val grunnlag: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp
)

class TilkjentYtelseRepository(private val connection: DBConnection) {

    fun lagre(tilkjentYtelse: TilkjentYtelse) {
        val sqlInsertTilkjentYtelse = """
            INSERT INTO TILKJENT_YTELSE 
                (SAKSNUMMER, BEHANDLING_REF, FORRIGE_BEHANDLING_REF)
                VALUES(?, ? ,?)
        """.trimIndent()

        val tilkjentYtelseId = connection.executeReturnKey(sqlInsertTilkjentYtelse) {
            setParams {
                setString(1, tilkjentYtelse.saksnummer.toString())
                setUUID(2, tilkjentYtelse.behandlingsreferanse)
                setUUID(3, tilkjentYtelse.forrigeBehandlingsreferanse)
            }
        }

        lagre(tilkjentYtelseId, tilkjentYtelse.perioder)
    }


    private fun lagre(tilkjentYtelseId: Long, tilkjentPerioder: List<TilkjentYtelsePeriode>) {
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
                    TILKJENT_YTELSE_ID
                )
                VALUES (?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()


        connection.executeBatch(sqlInsertTilkjentPeriode, tilkjentPerioder) {
            setParams {
                setPeriode(1, it.periode)
                setBigDecimal(2, it.detaljer.dagsats.verdi())
                setBigDecimal(3, it.detaljer.grunnlag.verdi())
                setBigDecimal(4, BigDecimal.valueOf(it.detaljer.gradering.prosentverdi().toLong()))
                setBigDecimal(5, it.detaljer.grunnbeløp.verdi())
                setInt(6, it.detaljer.antallBarn)
                setBigDecimal(7, it.detaljer.barnetillegg.verdi())
                setBigDecimal(8, it.detaljer.grunnlagsfaktor.verdi())
                setBigDecimal(9, it.detaljer.barnetilleggsats.verdi())
                setBigDecimal(10, it.detaljer.redusertDagsats.verdi())
                setLong(11, tilkjentYtelseId)
            }
        }
    }


    fun hent(behandlingReferanse: UUID): TilkjentYtelse? {
        val selectTilkjentYtelse = """
            SELECT 
                ID,
                SAKSNUMMER,
                BEHANDLING_REF,
                FORRIGE_BEHANDLING_REF
            FROM 
                TILKJENT_YTELSE
            WHERE
                BEHANDLING_REF = ?
        """.trimIndent()


        val idOgTilkjentYtelse = connection.queryFirstOrNull<Pair<Long, TilkjentYtelse>>(selectTilkjentYtelse) {
            setParams {
                setUUID(1, behandlingReferanse)
            }
            setRowMapper { row ->
                row.getLong("ID") to TilkjentYtelse(
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    behandlingsreferanse = row.getUUID("BEHANDLING_REF"),
                    forrigeBehandlingsreferanse = row.getUUIDOrNull("FORRIGE_BEHANDLING_REF"),
                    listOf()
                )
            }
        }
        if (idOgTilkjentYtelse != null) {
            val (tilkjentYtelseId, tilkjentYtelse) = idOgTilkjentYtelse
            return tilkjentYtelse.copy(perioder = hentTilkjentePerioder(tilkjentYtelseId))
        } else {
            return null
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
                REDUSERT_DAGSATS
            FROM TILKJENT_PERIODE
            WHERE TILKJENT_YTELSE_ID = ? 
        """.trimIndent()

        return connection.queryList<TilkjentYtelsePeriode>(selectTilkjentePerioder) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->

                val periode = row.getPeriode("PERIODE")
                TilkjentYtelsePeriode(
                    periode = periode,
                    detaljer = TilkjentYtelseDetaljer(
                        dagsats = Beløp(row.getBigDecimal("DAGSATS")),
                        grunnlag = Beløp(row.getBigDecimal("GRUNNLAG")),
                        gradering = Prosent.fraDesimal(row.getBigDecimal("GRADERING")),
                        grunnbeløp = Beløp(row.getBigDecimal("GRUNNBELOP")),
                        antallBarn = row.getInt("ANTALL_BARN"),
                        barnetillegg = Beløp(row.getBigDecimal("BARNETILLEGG")),
                        grunnlagsfaktor = GUnit(row.getBigDecimal("GRUNNLAGSFAKTOR")),
                        barnetilleggsats = Beløp(row.getBigDecimal("BARNETILLEGGSATS")),
                        redusertDagsats = Beløp(row.getBigDecimal("REDUSERT_DAGSATS")),
                    )
                )
            }
        }
    }

}