package com.studyplanner.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.studyplanner.app.ui.theme.LocalAppTheme

@Composable
fun OnboardingStepContainer(
    title: String,
    subtitle: String,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextText: String = "Continue",
    isLoading: Boolean = false,
    infoText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                if (infoText != null) InfoTooltip(infoText)
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f))
        }

        content()

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = nextEnabled && !isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = LocalAppTheme.current.colors.primary, strokeWidth = 2.dp)
            } else {
                Text(nextText,
                    color = if (nextEnabled) LocalAppTheme.current.colors.primary else Color.White.copy(0.6f),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun InfoTooltip(text: String, label: String = "") {
    var show by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable { show = true },
        contentAlignment = Alignment.Center
    ) {
        Text("?", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    if (show) {
        Dialog(onDismissRequest = { show = false }) {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center) {
                            Text("💡", fontSize = 14.sp)
                        }
                        Text(if (label.isNotEmpty()) label else "Info",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { show = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Samajh gaya 👍")
                    }
                }
            }
        }
    }
}

@Composable
fun FieldLabel(text: String, required: Boolean = false, infoText: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White,
                fontWeight = FontWeight.Medium)
            if (required) Text(" *", color = Color(0xFFEF5350),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        if (infoText != null) InfoTooltip(infoText, text)
    }
}

@Composable
fun SelectionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(if (selected) Modifier.border(1.5.dp, Color.White.copy(0.8f), RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun outlinedTextFieldWhiteColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
    focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
    cursorColor = Color.White,
    focusedLeadingIconColor = Color.White, unfocusedLeadingIconColor = Color.White.copy(0.7f),
    focusedTrailingIconColor = Color.White, unfocusedTrailingIconColor = Color.White.copy(0.7f),
)

@Composable
fun SearchableDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    required: Boolean = false,
    enabled: Boolean = true,
    placeholder: String = "Select",
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label, required)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable(enabled = enabled) { expanded = true; query = "" }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text(
                    selected.ifEmpty { placeholder },
                    color = if (selected.isEmpty()) Color.White.copy(0.4f) else Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(Icons.Default.ArrowDropDown, null,
                    tint = Color.White.copy(if (enabled) 0.7f else 0.3f))
            }
        }
    }

    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxHeight(0.75f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select $label", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = options.filter { it.contains(query, ignoreCase = true) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(option); expanded = false }
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option, style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                                if (option == selected) Icon(Icons.Default.Check, null,
                                    tint = MaterialTheme.colorScheme.primary)
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
fun TimePicker12Hr(
    label: String,
    hour24: Int,
    minute: Int,
    onTimeChange: (hour24: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var show by remember { mutableStateOf(false) }
    val display = formatTime12(hour24, minute)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty())
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(if (enabled) 0.1f else 0.04f))
                .clickable(enabled = enabled) { show = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(display, color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (show) {
        var tempHour by remember { mutableIntStateOf(hour24) }
        var tempMin by remember { mutableIntStateOf(minute) }
        Dialog(onDismissRequest = { show = false }) {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(if (label.isNotEmpty()) label else "Select Time",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)

                    Text(formatTime12(tempHour, tempMin),
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        SpinnerColumn("Hour", if (tempHour % 12 == 0) 12 else tempHour % 12, 1, 12) { h12 ->
                            val isPm = tempHour >= 12
                            tempHour = to24(h12, isPm)
                        }
                        Text(":", style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        SpinnerColumn("Min", tempMin, 0, 55, 5) { tempMin = it }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("AM", "PM").forEach { period ->
                                val isSelected = (period == "PM") == (tempHour >= 12)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            val h12 = if (tempHour % 12 == 0) 12 else tempHour % 12
                                            tempHour = to24(h12, period == "PM")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(period, fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { show = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(onClick = { onTimeChange(tempHour, tempMin); show = false },
                            modifier = Modifier.weight(1f)) {
                            Text("Set", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinnerColumn(label: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {
            val next = value + step
            onChange(if (next > max) min else next)
        }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.primary) }
        Text(value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = {
            val prev = value - step
            onChange(if (prev < min) max else prev)
        }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun ValidationBanner(message: String, isError: Boolean = true) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) Color(0xFFB71C1C).copy(0.3f) else Color(0xFFFF6D00).copy(0.25f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isError) Icons.Default.Error else Icons.Default.Warning, null,
                tint = if (isError) Color(0xFFEF5350) else Color(0xFFFFB74D),
                modifier = Modifier.size(18.dp))
            Text(message,
                color = if (isError) Color(0xFFEF9A9A) else Color(0xFFFFCC80),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatTime12(hour24: Int, minute: Int): String {
    val period = if (hour24 >= 12) "PM" else "AM"
    val h12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "${h12.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $period"
}

fun to24(hour12: Int, isPm: Boolean): Int = when {
    isPm && hour12 == 12 -> 12
    isPm -> hour12 + 12
    !isPm && hour12 == 12 -> 0
    else -> hour12
}

fun minutesBetween(startH: Int, startM: Int, endH: Int, endM: Int): Int =
    (endH * 60 + endM) - (startH * 60 + startM)
