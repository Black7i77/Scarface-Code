package com.scarface.code.workspace
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
object TextCodec {
    const val MAX_BYTES = 2 * 1024 * 1024
    fun decode(bytes: ByteArray): String {
        require(bytes.size <= MAX_BYTES) { "File exceeds the 2 MiB editor limit" }
        require(bytes.none { it == 0.toByte() }) { "Binary files cannot be edited as text" }
        return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    }
}
