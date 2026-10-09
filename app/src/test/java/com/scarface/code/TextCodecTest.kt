package com.scarface.code
import com.scarface.code.workspace.TextCodec
import org.junit.Assert.*
import org.junit.Test
class TextCodecTest {
    @Test fun preservesUnicodeCrLfAndFinalNewline() {
        val text = "🐺 café\r\nsecond\n"
        assertEquals(text, TextCodec.decode(text.toByteArray(Charsets.UTF_8)))
    }
    @Test fun rejectsBinaryAndMalformedUtf8() {
        assertThrows(IllegalArgumentException::class.java) { TextCodec.decode(byteArrayOf(65,0,66)) }
        assertThrows(java.nio.charset.CharacterCodingException::class.java) { TextCodec.decode(byteArrayOf(0xc3.toByte(), 0x28)) }
    }
    @Test fun rejectsOversizedFile() {
        assertThrows(IllegalArgumentException::class.java) { TextCodec.decode(ByteArray(2097153) { 65 }) }
    }
}
