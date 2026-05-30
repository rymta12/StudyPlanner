package com.studyplanner.app.core.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistractionBlocker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val BLOCKED_APPS = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.whatsapp",
            "com.twitter.android",
            "com.snapchat.android",
            "com.zhiliaoapp.musically",
            "com.reddit.frontpage",
            "org.telegram.messenger",
            "com.google.android.youtube",
            "com.pinterest",
            "com.linkedin.android",
        )
        val SOCIAL_APPS = BLOCKED_APPS - "com.google.android.youtube"
        const val YOUTUBE_PKG = "com.google.android.youtube"
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(context)

    fun openUsageStatsSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openOverlaySettings() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun getForegroundApp(): String? {
        if (!hasUsageStatsPermission()) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - 5000, now
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun shouldBlock(pkg: String, mode: BlockingMode, whitelistedChannels: List<String>): Boolean {
        return when (mode) {
            BlockingMode.COMPLETE_BLOCK -> pkg in BLOCKED_APPS
            BlockingMode.YOUTUBE_BLOCK -> pkg in BLOCKED_APPS
            BlockingMode.YOUTUBE_WHITELIST -> pkg in SOCIAL_APPS || pkg == YOUTUBE_PKG
            BlockingMode.DISABLED -> false
        }
    }

    fun isYoutubePartialBlock(pkg: String, mode: BlockingMode): Boolean =
        mode == BlockingMode.YOUTUBE_WHITELIST && pkg == YOUTUBE_PKG
}

enum class BlockingMode(val id: String, val displayName: String, val description: String) {
    COMPLETE_BLOCK("complete_block", "Complete Block", "All social + YouTube blocked"),
    YOUTUBE_BLOCK("youtube_block", "YouTube Block", "Social + YouTube fully blocked"),
    YOUTUBE_WHITELIST("youtube_whitelist", "YouTube Whitelist", "Social blocked, only selected YouTube allowed"),
    DISABLED("disabled", "Disabled", "No blocking"),
}
