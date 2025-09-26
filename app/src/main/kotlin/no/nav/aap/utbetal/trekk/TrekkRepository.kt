package no.nav.aap.utbetal.trekk

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection

class TrekkRepository(private val connection: DBConnection) {

    fun lagre(trekk: Trekk): Trekk {
        val insertTrekkSql = """
            INSERT INTO TREKK (SAKSNUMMER, BEHANDLINGSREFERANSE, DATO, BELOP)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        val trekkId =  connection.executeReturnKey(insertTrekkSql) {
            setParams {
                setString(1, trekk.saksnummer.toString())
                setUUID(2, trekk.behandlingsreferanse)
                setLocalDate(3, trekk.dato)
                setInt(4, trekk.beløp)
            }
        }

        return trekk.copy(id = trekkId)
    }

    fun lagre(trekkId: Long, trekkPosteringer: List<TrekkPostering>) {
        val insertTrekkPosteringerSql = """
            INSERT INTO TREKK_POSTERING (TREKK_ID, DATO, BELOP)
            VALUES (?, ?, ?)
        """.trimIndent()

        connection.executeBatch(insertTrekkPosteringerSql, trekkPosteringer) {
            setParams {
                setLong(1, trekkId)
                setLocalDate(2, it.dato)
                setInt(3, it.beløp)
            }
        }
    }

    fun hentTrekk(saksnummer: Saksnummer): List<Trekk> {
        val selectTrekkSql = """
            SELECT 
                ID, SAKSNUMMER, BEHANDLINGSREFERANSE, DATO, BELOP 
            FROM 
                TREKK 
            WHERE 
                SAKSNUMMER = ? AND AKTIV = TRUE
        """.trimIndent()

        return  connection.queryList(selectTrekkSql) {
            setParams {
                setString(1, saksnummer.toString())

            }

            setRowMapper { row ->
                Trekk(
                    id = row.getLong("ID"),
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    behandlingsreferanse = row.getUUID("BEHANDLINGSREFERANSE"),
                    dato = row.getLocalDate("DATO"),
                    beløp = row.getInt("BELOP"),
                    posteringer = hentTrekkPosteringer(row.getLong("ID"))
                )
            }
        }
    }

    fun slett(trekkId: Long) {
        val deleteTrekkSql =
            "UPDATE TREKK SET AKTIV = FALSE, OPPDATERT_TIDSPUNKT = CURRENT_TIMESTAMP WHERE ID = ?"
        connection.execute(deleteTrekkSql) {
            setParams {
                setLong(1, trekkId)
            }
        }
    }

    private fun hentTrekkPosteringer(trekkId: Long): List<TrekkPostering> {
        val selectTrekkPosteringerSql = """
            SELECT 
                ID, TREKK_ID, DATO, BELOP 
            FROM 
                TREKK_POSTERING
            WHERE
                TREKK_ID = ?
            ORDER BY ID
        """.trimIndent()
        return connection.queryList(selectTrekkPosteringerSql) {
            setParams {
                setLong(1, trekkId)
            }
            setRowMapper { row ->
                TrekkPostering(
                    id = row.getLong("ID"),
                    trekkId = row.getLong("TREKK_ID"),
                    dato = row.getLocalDate("DATO"),
                    beløp = row.getInt("BELOP"),
                )
            }
        }
    }

}