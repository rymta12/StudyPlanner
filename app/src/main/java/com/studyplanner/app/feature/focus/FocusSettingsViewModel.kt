package com.studyplanner.app.feature.focus

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyplanner.app.core.service.FocusMonitorService
import com.studyplanner.app.core.util.BlockingMode
import com.studyplanner.app.core.util.DistractionBlocker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.focusDataStore by preferencesDataStore("focus_settings")

data class FocusUiState(
    val blockingMode: BlockingMode = BlockingMode.YOUTUBE_WHITELIST,
    val whitelistedChannels: List<String> = emptyList(),
    val hasUsageStatsPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isFocusActive: Boolean = false,
)

@HiltViewModel
class FocusSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val distractionBlocker: DistractionBlocker,
) : ViewModel() {

    private val modeKey = stringPreferencesKey("blocking_mode")
    private val channelsKey = stringSetPreferencesKey("whitelisted_channels")

    private val _state = MutableStateFlow(FocusUiState())
    val state = _state.asStateFlow()

    init {
        loadSettings()
        checkPermissions()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            context.focusDataStore.data.collect { prefs ->
                val modeId = prefs[modeKey] ?: BlockingMode.YOUTUBE_WHITELIST.id
                val mode = BlockingMode.entries.find { it.id == modeId } ?: BlockingMode.YOUTUBE_WHITELIST
                val channels = prefs[channelsKey]?.toList() ?: emptyList()
                _state.update { it.copy(blockingMode = mode, whitelistedChannels = channels) }
            }
        }
    }

    fun checkPermissions() {
        _state.update {
            it.copy(
                hasUsageStatsPermission = distractionBlocker.hasUsageStatsPermission(),
                hasOverlayPermission = distractionBlocker.hasOverlayPermission(),
            )
        }
    }

    fun setBlockingMode(mode: BlockingMode) {
        viewModelScope.launch {
            context.focusDataStore.edit { it[modeKey] = mode.id }
            _state.update { it.copy(blockingMode = mode) }
        }
    }

    fun addChannel(channel: String) {
        viewModelScope.launch {
            val updated = (_state.value.whitelistedChannels + channel).distinct()
            context.focusDataStore.edit { it[channelsKey] = updated.toSet() }
            _state.update { it.copy(whitelistedChannels = updated) }
        }
    }

    fun removeChannel(channel: String) {
        viewModelScope.launch {
            val updated = _state.value.whitelistedChannels - channel
            context.focusDataStore.edit { it[channelsKey] = updated.toSet() }
            _state.update { it.copy(whitelistedChannels = updated) }
        }
    }

    fun startFocus(sessionId: Long) {
        val s = _state.value
        if (!s.hasUsageStatsPermission || !s.hasOverlayPermission) return
        val intent = Intent(context, FocusMonitorService::class.java).apply {
            action = FocusMonitorService.ACTION_START
            putExtra(FocusMonitorService.EXTRA_MODE, s.blockingMode.id)
            putExtra(FocusMonitorService.EXTRA_SESSION_ID, sessionId)
            putStringArrayListExtra(FocusMonitorService.EXTRA_WHITELIST, ArrayList(s.whitelistedChannels))
        }
        context.startForegroundService(intent)
        _state.update { it.copy(isFocusActive = true) }
    }

    fun stopFocus() {
        context.startService(Intent(context, FocusMonitorService::class.java).apply {
            action = FocusMonitorService.ACTION_STOP
        })
        _state.update { it.copy(isFocusActive = false) }
    }

    fun openUsageStatsSettings() = distractionBlocker.openUsageStatsSettings()
    fun openOverlaySettings() = distractionBlocker.openOverlaySettings()
}
