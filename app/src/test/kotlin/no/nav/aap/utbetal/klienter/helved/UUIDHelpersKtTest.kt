package no.nav.aap.utbetal.klienter.helved

import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

class UUIDHelpersKtTest {

    @Test
    fun base64OgTilbakeTilUuid() {
        val uuid = UUID.randomUUID()
        val base64 = uuid.toBase64()
        val tilbakeTilUUID = base64.base64ToUUID()
        assertThat(uuid).isEqualTo(tilbakeTilUUID)
    }

}
