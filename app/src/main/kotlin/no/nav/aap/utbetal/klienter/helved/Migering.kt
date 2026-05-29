package no.nav.aap.utbetal.klienter.helved

import java.util.UUID


data class MigreringRequest(val items: List<Migrering>)


data class Migrering(val uid: UUID, val id: UUID)
