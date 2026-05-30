package com.studyplanner.app.core.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    private val premiumManager: PremiumManager
) {
    fun shouldShowAd(placement: AdPlacement): Boolean {
        if (premiumManager.canAccessFeature(PremiumFeature.REMOVE_ADS)) return false
        return when (placement) {
            AdPlacement.HOME_BANNER -> true
            AdPlacement.LEADERBOARD_BANNER -> true
            AdPlacement.SESSION_COMPLETE_INTERSTITIAL -> true
            AdPlacement.TIMETABLE_NATIVE -> true
            AdPlacement.PROGRESS_BANNER -> true
            AdPlacement.ANALYTICS_BANNER -> true
            AdPlacement.SETTINGS_BANNER -> false
            AdPlacement.SESSION_ACTIVE -> false
            AdPlacement.ONBOARDING -> false
        }
    }
}

enum class AdPlacement {
    HOME_BANNER,
    LEADERBOARD_BANNER,
    SESSION_COMPLETE_INTERSTITIAL,
    TIMETABLE_NATIVE,
    PROGRESS_BANNER,
    ANALYTICS_BANNER,
    SETTINGS_BANNER,
    SESSION_ACTIVE,
    ONBOARDING,
}

@Composable
fun BannerAdPlaceholder(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    adManager: AdManager? = null
) {
    if (adManager?.shouldShowAd(placement) == false) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ad",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Text(
                "Remove ads with Premium ✨",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PremiumUpsellBanner(
    message: String,
    ctaText: String = "Upgrade to Premium",
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("✨ Premium Feature", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onUpgradeClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(ctaText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
