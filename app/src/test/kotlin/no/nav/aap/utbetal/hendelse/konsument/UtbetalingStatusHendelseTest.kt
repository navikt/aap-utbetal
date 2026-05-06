package no.nav.aap.utbetal.hendelse.konsument

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class UtbetalingStatusHendelseTest {

    private val hendelseJSON = """
        {
            "status":"OK",
            "detaljer": {
                "ytelse":"AAP",
                "linjer":[
                    {
                        "behandlingId":"abHiwFUwTwGiKOGQCRpEaA==",
                        "fom":"2026-01-19",
                        "tom":"2026-02-27",
                        "vedtakssats":1982,
                        "beløp":1982,
                        "klassekode":"AAPOR"
                    }
                ]
            },
            "error":null,
            "simulering":false
        }        
    """.trimIndent()

    @Test
    fun `Parse eksempel på hendelse`() {
        val utbetalingStatusHendelse = DefaultJsonMapper.fromJson<UtbetalingStatusHendelse>(hendelseJSON)
        assertThat(utbetalingStatusHendelse.status).isEqualTo(Status.OK)
        assertThat(utbetalingStatusHendelse.detaljer.linjer.first().fom).isEqualTo(LocalDate.parse("2026-01-19"))
        assertThat(utbetalingStatusHendelse.detaljer.linjer.first().tom).isEqualTo(LocalDate.parse("2026-02-27"))
    }

}