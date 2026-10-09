package com.scarface.code
import com.scarface.code.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
class RecoveryTest {
    @Test fun preservesUnicodeAndUnsavedText() {
        val dir = Files.createTempDirectory("recovery").toFile()
        val w = WorkspaceController(); val a = w.open("main.py", "line\r\n🐺\n", "content://file/1")
        w.update(a.id, "edited\r\n🐺\n")
        val store = RecoveryStore(dir); store.save(w.buffers, a.id)
        val restored = store.load()
        assertEquals(a.id, restored.activeId)
        assertEquals("edited\r\n🐺\n", restored.buffers.single().text)
        assertEquals("line\r\n🐺\n", restored.buffers.single().savedText)
        assertTrue(restored.buffers.single().dirty)
        dir.deleteRecursively()
    }
    @Test fun corruptAndMissingRecoveryDoNotCrash() {
        val dir = Files.createTempDirectory("recovery").toFile()
        val store = RecoveryStore(dir); assertTrue(store.load().buffers.isEmpty())
        java.io.File(dir, "recovery.json").writeText("broken")
        assertTrue(store.load().buffers.isEmpty()); dir.deleteRecursively()
    }
    @Test fun differentStoreInstancesSerializeWrites() {
        val dir = Files.createTempDirectory("parallel-recovery").toFile()
        val a = RecoveryStore(dir); val b = RecoveryStore(dir)
        val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        val gate = java.util.concurrent.CountDownLatch(1)
        val one = Thread { gate.await(); repeat(40) { try { a.save(listOf(EditorBuffer(name="one", text="a".repeat(4096))), null) } catch(e: Throwable) { failures.add(e) } } }
        val two = Thread { gate.await(); repeat(40) { try { b.save(listOf(EditorBuffer(name="two", text="b".repeat(4096))), null) } catch(e: Throwable) { failures.add(e) } } }
        one.start(); two.start(); gate.countDown(); one.join(); two.join()
        assertTrue(failures.toString(), failures.isEmpty()); assertEquals(1, a.load().buffers.size)
        dir.deleteRecursively()
    }
    @Test fun olderQueuedSnapshotCannotReplaceNewerDraft() {
        val dir = Files.createTempDirectory("ordered-recovery").toFile()
        val store = RecoveryStore(dir)
        store.save(listOf(EditorBuffer(name="new", text="current")), null, 20)
        RecoveryStore(dir).save(listOf(EditorBuffer(name="old", text="obsolete")), null, 10)
        assertEquals("current", store.load().buffers.single().text)
        dir.deleteRecursively()
    }
    @Test fun oversizedSnapshotKeepsLastGoodRecovery() {
        val dir = Files.createTempDirectory("bounded-recovery").toFile()
        val store = RecoveryStore(dir, 1024)
        store.save(listOf(EditorBuffer(name="safe", text="kept")), null)
        assertThrows(IllegalArgumentException::class.java) { store.save(listOf(EditorBuffer(name="large", text="x".repeat(2048))), null) }
        assertEquals("kept", store.load().buffers.single().text)
        dir.deleteRecursively()
    }
}
