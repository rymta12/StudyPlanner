package com.studyplanner.app.feature.auth

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: (isNewUser: Boolean) -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onAuthSuccess(state.isNewUser)
    }

    if (state.showForgotPassword) {
        ForgotPasswordSheet(
            isLoading = state.isLoading,
            isSent = state.forgotPasswordSent,
            error = state.error,
            onSend = { viewModel.sendForgotPassword(it) },
            onDismiss = { viewModel.showForgotPassword(false) }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060B18), Color(0xFF0D1B3E), Color(0xFF1A2A5E))))
    ) {
        TextButton(
            onClick = {
                Log.d("AuthScreenUI", "Skip TextButton actually clicked inside Composable!")
                onSkip() // Lambda fire hoga
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .zIndex(1f)
        ) {
            Text(
                text = "Skip →",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            AuthHeader()


            Spacer(Modifier.height(32.dp))

            AuthTabRow(selectedTab = state.tab, onTabChange = { viewModel.setTab(it) })

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = state.tab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "authTab"
            ) { tab ->
                when (tab) {
                    AuthTab.LOGIN -> LoginForm(
                        state = state,
                        onLogin = { email, pass -> viewModel.login(email, pass) },
                        onForgotPassword = { viewModel.showForgotPassword(true) },
                        onGoogleSignIn = { viewModel.signInWithGoogle(it) },
                        onClearError = { viewModel.clearError() }
                    )
                    AuthTab.REGISTER -> RegisterForm(
                        state = state,
                        onRegister = { name, email, pass, phone -> viewModel.register(name, email, pass, phone) },
                        onGoogleSignIn = { viewModel.signInWithGoogle(it) },
                        onClearError = { viewModel.clearError() }
                    )
                    AuthTab.PHONE -> PhoneCollectForm(
                        onSwitchToRegister = { viewModel.setTab(AuthTab.REGISTER) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AuthHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1)))),
            contentAlignment = Alignment.Center
        ) {
            Text("📚", fontSize = 36.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("StudyPlanner", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text("UPSC • SSC • Banking • Railway",
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun AuthTabRow(selectedTab: AuthTab, onTabChange: (AuthTab) -> Unit) {
    val tabs = listOf(AuthTab.LOGIN to "Login", AuthTab.REGISTER to "Register", AuthTab.PHONE to "📱 Phone")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onTabChange(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color(0xFF1565C0) else Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: (String) -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AuthTextField(
            value = email, onValueChange = { email = it; onClearError() },
            label = "Email address",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        AuthTextField(
            value = password, onValueChange = { password = it; onClearError() },
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onLogin(email, password) })
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword, contentPadding = PaddingValues(0.dp)) {
                Text("Forgot Password?", color = Color(0xFF64B5F6),
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }

        state.error?.let { AuthErrorCard(it) }

        AuthPrimaryButton(
            text = "Login",
            isLoading = state.isLoading,
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = { onLogin(email, password) }
        )

        AuthDivider()

        GoogleSignInButton(isLoading = state.isLoading, onToken = { onGoogleSignIn(it) })
    }
}

@Composable
private fun RegisterForm(
    state: AuthUiState,
    onRegister: (String, String, String, String) -> Unit,
    onGoogleSignIn: (String) -> Unit,
    onClearError: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val passwordMatch = confirmPassword.isEmpty() || password == confirmPassword

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AuthTextField(
            value = name, onValueChange = { name = it; onClearError() },
            label = "Full Name", leadingIcon = Icons.Default.Person,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        AuthTextField(
            value = email, onValueChange = { email = it; onClearError() },
            label = "Email address", leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Phone, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Box(modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("🇮🇳 +91", color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            }
            BasicTextField(
                value = phone,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) { phone = it; onClearError() } },
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                decorationBox = { inner ->
                    if (phone.isEmpty()) Text("Mobile number (optional)", color = Color.White.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.bodyLarge)
                    inner()
                }
            )
        }
        AuthTextField(
            value = password, onValueChange = { password = it; onClearError() },
            label = "Password (min 6 characters)", leadingIcon = Icons.Default.Lock,
            isPassword = true, passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        AuthTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it; onClearError() },
            label = "Confirm Password", leadingIcon = Icons.Default.LockOpen,
            isPassword = true, passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible },
            isError = !passwordMatch,
            errorText = if (!passwordMatch) "Passwords don't match" else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (passwordMatch) onRegister(name, email, password, if (phone.isNotBlank()) "+91$phone" else "")
            })
        )

        state.error?.let { AuthErrorCard(it) }

        AuthPrimaryButton(
            text = "Create Account",
            isLoading = state.isLoading,
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && passwordMatch,
            onClick = { onRegister(name, email, password, if (phone.isNotBlank()) "+91$phone" else "") }
        )

        AuthDivider()

        GoogleSignInButton(isLoading = state.isLoading, onToken = { onGoogleSignIn(it) })
    }
}

@Composable
private fun PhoneCollectForm(onSwitchToRegister: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📱", fontSize = 40.sp)
                Text("OTP Login Coming Soon",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    "Phone OTP login will be available in a future update. " +
                            "You can still save your phone number during registration.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        AuthPrimaryButton(
            text = "Register with Email + Phone",
            isLoading = false,
            enabled = true,
            onClick = onSwitchToRegister
        )
    }
}

@Composable
private fun ForgotPasswordSheet(
    isLoading: Boolean,
    isSent: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060B18), Color(0xFF0D1B3E), Color(0xFF1A2A5E)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                .background(Color(0xFF1565C0).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LockReset, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(36.dp))
            }

            Text("Reset Password", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color.White)

            if (isSent) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✅", fontSize = 32.sp)
                        Text("Reset link sent!", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Check your email inbox and follow the instructions",
                            color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center)
                    }
                }
            } else {
                Text("Enter your registered email. We'll send a password reset link.",
                    color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)

                AuthTextField(value = email, onValueChange = { email = it },
                    label = "Email address", leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done))

                error?.let { AuthErrorCard(it) }

                AuthPrimaryButton(text = "Send Reset Link", isLoading = isLoading,
                    enabled = email.isNotBlank(), onClick = { onSend(email) })
            }

            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back to Login", color = Color(0xFF64B5F6))
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    if (isError) Color(0xFFEF5350) else Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(leadingIcon, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(label, color = Color.White.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.bodyLarge)
                    inner()
                }
            )
            if (isPassword && onPasswordToggle != null) {
                IconButton(onClick = onPasswordToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (isError && errorText != null) {
            Text(errorText, color = Color(0xFFEF5350), style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun AuthPrimaryButton(text: String, isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1565C0),
            disabledContainerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun GoogleSignInButton(isLoading: Boolean, onToken: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var googleLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            googleLoading = true
            scope.launch {
                getGoogleIdToken(context)
                    .onSuccess { token -> onToken(token) }
                    .onFailure { googleLoading = false }
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = !isLoading && !googleLoading,
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.White.copy(alpha = 0.2f))),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        if (googleLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("G", fontWeight = FontWeight.ExtraBold, color = Color(0xFF4285F4), fontSize = 18.sp)
                Text("Continue with Google", fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

@Composable
private fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
        Text("or", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
    }
}

@Composable
private fun AuthErrorCard(error: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFB71C1C).copy(alpha = 0.25f))
            .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
        Text(error, color = Color(0xFFEF9A9A), style = MaterialTheme.typography.bodySmall)
    }
}