package com.subburn.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RenderState {
    data object Idle : RenderState
    data class Running(
        val progress: Float,          // 0f..1f, -1f when the duration is unknown
        val speed: Double,
        val fps: Double,
        val outputSizeBytes: Long,
        val etaSeconds: Long,
        val displayName: String
    ) : RenderState

    data class Done(val outputPath: String, val savedTo: String?, val sizeBytes: Long) : RenderState
    data class Failed(val message: String) : RenderState
    data object Cancelled : RenderState
}

/** Single source of truth shared by the foreground service and the UI. */
object BurnState {
    private val _state = MutableStateFlow<RenderState>(RenderState.Idle)
    val state: StateFlow<RenderState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    fun update(next: RenderState) {
        _state.value = next
    }

    fun appendLog(line: String) {
        _log.value = (_log.value + line).takeLast(200)
    }

    fun resetLog() {
        _log.value = emptyList()
    }
}
