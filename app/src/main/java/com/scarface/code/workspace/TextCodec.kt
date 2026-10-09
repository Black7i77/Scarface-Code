package com.scarface.code.workspace

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.CharacterCodingException

object TextCodec {
    const val MAX_BYTES = 2 * 1024 * 1024

    fun decode(bytes: ByteArray): String {
        require(bytes.size <= MAX_BYTES) {
            "File exceeds the 2 MiB editor limit"
        }

        val charset = when {
            bytes.size >= 2 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE

            bytes.size >= 2 &&
                bytes[0] == 0xFE.toByte() &&
                bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE

            else -> Charsets.UTF_8
        }

        val offset = when {
            charset == Charsets.UTF_16LE ||
                charset == Charsets.UTF_16BE -> 2

            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte() -> 3

            else -> 0
        }

        if (charset == Charsets.UTF_8) {
            require(bytes.none { it == 0.toByte() }) {
                "Binary files cannot be edited as text"
            }
        }

        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        val text = decoder.decode(
            ByteBuffer.wrap(bytes, offset, bytes.size - offset)
        ).toString()

        require('\u0000' !in text) {
            "Binary files cannot be edited as text"
        }

        return text
    }
}
