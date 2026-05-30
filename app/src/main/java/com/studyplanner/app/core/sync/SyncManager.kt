package com.studyplanner.app.core.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore by preferencesDataStore("sync_settings")

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // ════════════════════════════════════════════════
        //  RELEASE TIME PE YE FALSE KAR DENA
        //  true  = har save Firestore pe bhi (testing)
        //  false = sirf manual sync (hybrid, kam data use)
        // ════════════════════════════════════════════════
        const val ALWAYS_SYNC = true
    }

    private val autoSyncKey = booleanPreferencesKey("auto_sync_enabled")

    val isAutoSyncEnabled: Flow<Boolean> = context.syncDataStore.data.map { prefs ->
        if (ALWAYS_SYNC) true else (prefs[autoSyncKey] ?: false)
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.syncDataStore.edit { it[autoSyncKey] = enabled }
    }

    fun shouldSyncNow(): Boolean = ALWAYS_SYNC
}
