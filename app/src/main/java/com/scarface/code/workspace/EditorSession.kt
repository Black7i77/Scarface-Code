package com.scarface.code.workspace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
/** Survives rotation, including writes and document-picker requests in flight. */
class EditorSession : ViewModel() {
    val workspace = WorkspaceController()
    var loaded = false
    var saveAsBuffer: String? = null
    val updates = MutableLiveData(0)
    fun changed() { updates.value = (updates.value ?: 0) + 1 }
}
