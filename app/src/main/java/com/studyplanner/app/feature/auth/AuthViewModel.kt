package com.studyplanner.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class AuthTab { LOGIN, REGISTER, PHONE }

data class AuthUiState(
    val tab: AuthTab = AuthTab.LOGIN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isNewUser: Boolean = false,
    val showForgotPassword: Boolean = false,
    val forgotPasswordSent: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun setTab(tab: AuthTab) = _state.update { it.copy(tab = tab, error = null) }
    fun clearError() = _state.update { it.copy(error = null) }
    fun showForgotPassword(show: Boolean) = _state.update {
        it.copy(showForgotPassword = show, error = null, forgotPasswordSent = false)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { auth.signInWithEmailAndPassword(email, password).await() }
                .onSuccess { _state.update { it.copy(isLoading = false, isSuccess = true, isNewUser = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = friendlyError(e)) } }
        }
    }

    fun register(name: String, email: String, password: String, phone: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("UID null")
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )?.await()
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "createdAt" to System.currentTimeMillis(),
                        "isOnboardingComplete" to false
                    )
                ).await()
            }
                .onSuccess { _state.update { it.copy(isLoading = false, isSuccess = true, isNewUser = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = friendlyError(e)) } }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val isNew = result.additionalUserInfo?.isNewUser == true
                if (isNew) {
                    val uid = result.user?.uid ?: throw Exception("UID null")
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "uid" to uid,
                            "name" to (result.user?.displayName ?: ""),
                            "email" to (result.user?.email ?: ""),
                            "phone" to "",
                            "createdAt" to System.currentTimeMillis(),
                            "isOnboardingComplete" to false
                        )
                    ).await()
                }
                isNew
            }
                .onSuccess { isNew -> _state.update { it.copy(isLoading = false, isSuccess = true, isNewUser = isNew) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = friendlyError(e)) } }
        }
    }

    fun sendForgotPassword(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { auth.sendPasswordResetEmail(email).await() }
                .onSuccess { _state.update { it.copy(isLoading = false, forgotPasswordSent = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = friendlyError(e)) } }
        }
    }

    private fun friendlyError(e: Throwable): String = when {
        e.message?.contains("INVALID_EMAIL") == true -> "Invalid email address"
        e.message?.contains("WRONG_PASSWORD") == true || e.message?.contains("INVALID_CREDENTIAL") == true -> "Incorrect email or password"
        e.message?.contains("EMAIL_NOT_FOUND") == true -> "No account with this email"
        e.message?.contains("EMAIL_EXISTS") == true -> "Email already registered"
        e.message?.contains("WEAK_PASSWORD") == true -> "Password too weak (min 6 chars)"
        e.message?.contains("network") == true -> "No internet connection"
        else -> e.localizedMessage ?: "Something went wrong"
    }
}
