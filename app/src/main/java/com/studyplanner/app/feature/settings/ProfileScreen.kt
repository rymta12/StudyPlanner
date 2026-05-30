package com.studyplanner.app.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.feature.onboarding.IndiaData
import com.studyplanner.app.feature.onboarding.SearchableDropdown
import com.studyplanner.app.feature.onboarding.ValidationBanner
import com.studyplanner.app.ui.components.LoadingScreen
import com.studyplanner.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }

    if (state.isLoading) { LoadingScreen(); return }

    if (isEditing) {
        ProfileEditScreen(
            state = state,
            onSave = { name, nick, gender, city, st, pin, date ->
                viewModel.saveProfile(name, nick, gender, city, st, pin, date)
                isEditing = false
            },
            onCancel = { isEditing = false }
        )
    } else {
        ProfileViewScreen(
            state = state,
            onEdit = { isEditing = true },
            onSignOut = { viewModel.signOut(); onSignOut() }
        )
    }
}

@Composable
private fun ProfileViewScreen(
    state: ProfileUiState,
    onEdit: () -> Unit,
    onSignOut: () -> Unit
) {
    val theme = LocalAppTheme.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(theme.gradients.backgroundStart, theme.gradients.backgroundMid)
                    )
                )
                .padding(24.dp)
                .systemBarsPadding()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.name.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(state.name, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(state.email, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f))
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isPro) PremiumBadge("👑 Pro", Color(0xFFFFD700))
                    else if (state.isPremium) PremiumBadge("⭐ Premium", Color(0xFF64B5F6))
                    else PremiumBadge("Free", Color.White.copy(0.5f))
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat("🔥", "${state.currentStreak}", "Streak")
                    ProfileStat("⭐", "${state.points}", "Points")
                    ProfileStat("🛡️", "${state.authenticityScore}%", "Trust")
                    ProfileStat("🏆", "${state.longestStreak}", "Best")
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Edit, null, tint = Color.White)
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileSection("Personal Info") {
                ProfileInfoRow(Icons.Default.Person, "Name", state.name)
                ProfileInfoRow(Icons.Default.Wc, "Gender", state.gender.lowercase().replaceFirstChar { it.uppercase() })
                ProfileInfoRow(Icons.Default.LocationOn, "Location", "${state.city}, ${state.state}")
            }

            ProfileSection("Exam Info") {
                ProfileInfoRow(Icons.Default.School, "Exam", "${state.examType} — ${state.examSubType}")
                ProfileInfoRow(Icons.Default.CalendarToday, "Target Date",
                    if (state.targetDate > 0) dateFormat.format(Date(state.targetDate)) else "Not set")
                val daysLeft = if (state.targetDate > 0)
                    ((state.targetDate - System.currentTimeMillis()) / 86400000).toInt() else 0
                if (daysLeft > 0)
                    ProfileInfoRow(Icons.Default.Timer, "Days Remaining", "$daysLeft days")
            }

            ProfileSection("Account") {
                ProfileInfoRow(Icons.Default.Email, "Email", state.email)
                ProfileInfoRow(Icons.Default.VpnKey, "Parent Monitor Code", state.parentCode)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null,
                        tint = MaterialTheme.colorScheme.error)
                    Text("Sign Out", color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditScreen(
    state: ProfileUiState,
    onSave: (String, String, String, String, String, String, Long) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(state.name) }
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(state.gender) }
    var userState by remember { mutableStateOf(state.state) }
    var city by remember { mutableStateOf(state.city) }
    var pincode by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var targetDate by remember { mutableLongStateOf(state.targetDate) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .systemBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Text("Edit Profile", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            TextButton(
                onClick = { onSave(name, nickname, gender, city, userState, pincode, targetDate) },
                enabled = name.isNotBlank() && city.isNotBlank()
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name *") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = nickname, onValueChange = { nickname = it },
                label = { Text("Nickname") },
                leadingIcon = { Icon(Icons.Default.Face, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Text("Gender *", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MALE" to "👨 Male", "FEMALE" to "👩 Female", "OTHER" to "🧑 Other").forEach { (id, label) ->
                    FilterChip(
                        selected = gender == id,
                        onClick = { gender = id },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SearchableDropdownLight("State *", userState, IndiaData.states,
                onSelect = { userState = it; city = "" })
            SearchableDropdownLight("City / District *", city,
                IndiaData.districtsFor(userState), onSelect = { city = it },
                enabled = userState.isNotBlank())

            OutlinedTextField(
                value = pincode,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pincode = it },
                label = { Text("Pincode") },
                leadingIcon = { Icon(Icons.Default.PinDrop, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, null,
                        tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Target Date", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (targetDate > 0) dateFormat.format(Date(targetDate)) else "Tap to set",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = if (targetDate > 0) targetDate
            else System.currentTimeMillis() + 180L * 86400000,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis > System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { targetDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}

@Composable
private fun SearchableDropdownLight(
    label: String, selected: String, options: List<String>,
    onSelect: (String) -> Unit, enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { expanded = true; query = "" },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected.ifEmpty { if (enabled) "Select" else "Select state first" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.Default.ArrowDropDown, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (expanded) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expanded = false }) {
            Card(shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxHeight(0.7f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select $label", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = options.filter { it.contains(query, ignoreCase = true) }
                    androidx.compose.foundation.lazy.LazyColumn {
                       items(filtered) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(option); expanded = false }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(option, style = MaterialTheme.typography.bodyLarge)
                                if (option == selected)
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ProfileStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White.copy(0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PremiumBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold)
    }
}
