package com.studyplanner.app.core.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class PremiumTier(val id: String, val displayName: String, val price: String) {
    FREE("free", "Free", "₹0"),
    PREMIUM("premium", "Premium", "₹199/month"),
    PRO("pro", "Pro", "₹399/month"),
}

data class PremiumStatus(
    val tier: PremiumTier = PremiumTier.FREE,
    val expiresAt: Long = 0L,
    val isActive: Boolean = false,
    val razorpayOrderId: String = "",
    val transactionId: String = "",
)

@Singleton
class PremiumManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _status = MutableStateFlow(PremiumStatus())
    val status: Flow<PremiumStatus> = _status.asStateFlow()

    suspend fun refreshStatus() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val doc = firestore.collection("subscriptions").document(uid).get().await()
            val tier = PremiumTier.entries.find { it.id == doc.getString("tier") } ?: PremiumTier.FREE
            val expiresAt = doc.getLong("expiresAt") ?: 0L
            val isActive = expiresAt > System.currentTimeMillis()
            _status.value = PremiumStatus(
                tier = if (isActive) tier else PremiumTier.FREE,
                expiresAt = expiresAt,
                isActive = isActive,
                razorpayOrderId = doc.getString("razorpayOrderId") ?: "",
                transactionId = doc.getString("transactionId") ?: "",
            )
        }
    }

    suspend fun activatePremium(
        tier: PremiumTier,
        razorpayOrderId: String,
        transactionId: String,
    ) {
        val uid = auth.currentUser?.uid ?: return
        val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        val data = mapOf(
            "uid" to uid,
            "tier" to tier.id,
            "expiresAt" to expiresAt,
            "razorpayOrderId" to razorpayOrderId,
            "transactionId" to transactionId,
            "activatedAt" to System.currentTimeMillis(),
        )
        firestore.collection("subscriptions").document(uid).set(data).await()
        _status.value = PremiumStatus(tier, expiresAt, true, razorpayOrderId, transactionId)
    }

    fun canAccessFeature(feature: PremiumFeature): Boolean {
        val tier = _status.value.tier
        val isActive = _status.value.isActive
        return when (feature) {
            PremiumFeature.UNLIMITED_SUBJECTS -> isActive && tier != PremiumTier.FREE
            PremiumFeature.NATIONAL_LEADERBOARD -> isActive && tier != PremiumTier.FREE
            PremiumFeature.AI_COACH -> isActive && tier != PremiumTier.FREE
            PremiumFeature.DETAILED_ANALYTICS -> isActive && tier != PremiumTier.FREE
            PremiumFeature.MOCK_TESTS -> isActive && tier == PremiumTier.PRO
            PremiumFeature.MENTOR_ACCESS -> isActive && tier == PremiumTier.PRO
            PremiumFeature.PARENT_DASHBOARD -> isActive && tier == PremiumTier.PRO
            PremiumFeature.REMOVE_ADS -> isActive && tier != PremiumTier.FREE
            PremiumFeature.ALL_THEMES -> isActive && tier != PremiumTier.FREE
            PremiumFeature.CUSTOM_BACKGROUNDS -> isActive && tier != PremiumTier.FREE
        }
    }
}

enum class PremiumFeature {
    UNLIMITED_SUBJECTS,
    NATIONAL_LEADERBOARD,
    AI_COACH,
    DETAILED_ANALYTICS,
    MOCK_TESTS,
    MENTOR_ACCESS,
    PARENT_DASHBOARD,
    REMOVE_ADS,
    ALL_THEMES,
    CUSTOM_BACKGROUNDS,
}

data class PremiumPlan(
    val tier: PremiumTier,
    val features: List<String>,
    val highlightFeature: String,
)

val premiumPlans = listOf(
    PremiumPlan(
        tier = PremiumTier.FREE,
        features = listOf(
            "2 subjects",
            "Basic timetable",
            "City leaderboard",
            "Basic streaks",
            "Standard themes",
        ),
        highlightFeature = "Free forever"
    ),
    PremiumPlan(
        tier = PremiumTier.PREMIUM,
        features = listOf(
            "Unlimited subjects",
            "AI study coach",
            "National leaderboard",
            "Detailed analytics",
            "All themes & backgrounds",
            "No ads",
        ),
        highlightFeature = "Most Popular 🔥"
    ),
    PremiumPlan(
        tier = PremiumTier.PRO,
        features = listOf(
            "Everything in Premium",
            "Mock tests & PYQs",
            "Mentor access",
            "Parent dashboard",
            "Priority support",
        ),
        highlightFeature = "Best Value 👑"
    ),
)
