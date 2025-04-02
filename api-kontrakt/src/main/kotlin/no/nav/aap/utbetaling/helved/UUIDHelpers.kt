package no.nav.aap.utbetaling.helved

import java.util.Base64
import java.util.UUID

fun UUID.toBase64(): String {
    val bytes = this.toString()
        .replace("-", "")
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
    return Base64.getEncoder().encodeToString(bytes)
}

fun String.base64ToUUID(): UUID {
    val bytes = Base64.getDecoder().decode(this)
    val uuidString = listOf(
        bytes.slice(0..3).toHex(),
        bytes.slice(4..5).toHex(),
        bytes.slice(6..7).toHex(),
        bytes.slice(8..9).toHex(),
        bytes.slice(10..15).toHex(),
    ).joinToString(separator = "-")
    return UUID.fromString(uuidString)
}

private fun List<Byte>.toHex() = joinToString(separator = "") { String.format("%02X".lowercase(), it) }
