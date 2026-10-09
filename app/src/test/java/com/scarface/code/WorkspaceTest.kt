package com.scarface.code
import com.scarface.code.workspace.*
import org.junit.Assert.*
import org.junit.Test
class WorkspaceTest {
    @Test fun tabsRetainIndependentEdits() {
        val w = WorkspaceController()
        val a = w.open("a.py", "print(1)")
        val b = w.open("b.py", "print(2)")
        w.update(a.id, "changed")
        w.select(b.id)
        assertEquals("print(2)", w.active!!.text)
        w.select(a.id)
        assertEquals("changed", w.active!!.text)
    }
    @Test fun revertingEditClearsDirty() {
        val w = WorkspaceController(); val b = w.open("a", "original")
        w.update(b.id, "other"); assertTrue(b.dirty)
        w.update(b.id, "original"); assertFalse(b.dirty)
    }
    @Test fun dirtyCloseRequiresDiscard() {
        val w = WorkspaceController(); val b = w.open("a", "")
        w.update(b.id, "unsaved")
        assertFalse(w.close(b.id)); assertEquals("unsaved", w.active!!.text)
        assertTrue(w.close(b.id, true)); assertNull(w.active)
    }
    @Test fun onlySuccessfulSaveMarksClean() {
        val w = WorkspaceController(); val b = w.open("a", "")
        w.update(b.id, "new")
        assertTrue(b.dirty)
        w.markSaved(b.id, "new"); assertFalse(b.dirty)
        w.update(b.id, "later"); w.markSaved(b.id, "new"); assertTrue(b.dirty)
    }
    @Test fun sameNamesRemainDistinct() {
        val w = WorkspaceController(); val a = w.open("a", "1"); val b = w.open("a", "2")
        assertNotEquals(a.id, b.id); assertEquals(2, w.buffers.size)
    }
}
