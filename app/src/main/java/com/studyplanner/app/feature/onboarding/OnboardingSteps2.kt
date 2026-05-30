package com.studyplanner.app.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StepCurrentAffairs(
    state: OnboardingState,
    onNext: (frequency: String, durationMin: Int) -> Unit
) {
    var frequency by remember { mutableStateOf(state.currentAffairsFrequency) }
    var duration by remember { mutableIntStateOf(state.currentAffairsDurationMin) }

    OnboardingStepContainer(
        title = "Current Affairs 📰",
        subtitle = "Kitni baar current affairs cover karna chahte ho?",
        onNext = { onNext(frequency, duration) },
        nextEnabled = true,
        infoText = "Current affairs UPSC/SSC me bahut important hai. App aapke timetable me iske liye dedicated slots add kar dega."
    ) {
        FieldLabel("Frequency", required = true)
        listOf("DAILY" to "📅 Daily", "WEEKLY" to "📆 Weekly", "MONTHLY" to "🗓️ Monthly").forEach { (id, label) ->
            SelectionChip(text = label, selected = frequency == id,
                onClick = { frequency = id }, modifier = Modifier.fillMaxWidth())
        }
        FieldLabel("Duration per session", infoText = "Har current affairs session kitne minute ka hoga.")
        StepperRow("Duration", duration, "min", 15, 120, 15) { duration = it }
    }
}

@Composable
fun StepRevision(
    state: OnboardingState,
    onNext: (style: String) -> Unit
) {
    var style by remember { mutableStateOf(state.revisionStyle) }
    var manualDays by remember { mutableStateOf(setOf(1, 7, 30)) }

    OnboardingStepContainer(
        title = "Revision Settings 🔄",
        subtitle = "Kaise revise karna pasand karte ho?",
        onNext = { onNext(style) },
        nextEnabled = true,
        infoText = "Revision yaad rakhne ke liye sabse zaroori hai. Spaced repetition science-backed method hai jisme aap fixed gaps pe revise karte ho."
    ) {
        listOf(
            Triple("SPACED", "🧠 Spaced Repetition", "Best! 1, 7, 30, 90 din baad auto revision schedule hoga"),
            Triple("WEEKLY", "📅 Weekly Revision", "Har hafte ek baar sab kuch review"),
            Triple("MANUAL", "✏️ Manual", "Aap khud revision din choose karoge"),
        ).forEach { (id, label, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { style = id },
                colors = CardDefaults.cardColors(
                    containerColor = if (style == id) Color.White.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.08f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall)
                        Text(desc, color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (style == id) Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                }
            }
        }

        if (style == "MANUAL") {
            FieldLabel("Choose revision gaps (days after study)",
                infoText = "Topic padhne ke kitne din baad revise karna hai — multiple choose kar sakte ho.")
            val options = listOf(1, 3, 7, 14, 30, 60, 90)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                options.take(4).forEach { day ->
                    SelectionChip(text = "${day}d", selected = day in manualDays,
                        onClick = {
                            manualDays = if (day in manualDays) manualDays - day else manualDays + day
                        }, modifier = Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                options.drop(4).forEach { day ->
                    SelectionChip(text = "${day}d", selected = day in manualDays,
                        onClick = {
                            manualDays = if (day in manualDays) manualDays - day else manualDays + day
                        }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.weight(1f))
            }
            if (manualDays.isNotEmpty()) {
                ValidationBanner("Selected: ${manualDays.sorted().joinToString(", ") { "${it}d" }}", isError = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepDeadline(
    state: OnboardingState,
    onNext: (targetDate: Long) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(state.targetDate) }
    var showPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val daysRemaining = if (selectedDate > 0)
        ((selectedDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt() else 0

    val totalStudyMin = state.subjects.sumOf {
        (it.totalPages * it.readingSpeedMinPerPage).toInt() + it.totalVideoMinutes
    }
    val dailyMin = (state.dailyStudyHours * 60).toInt()
    val daysNeeded = if (dailyMin > 0) (totalStudyMin + dailyMin - 1) / dailyMin else 0
    val feasible = daysRemaining >= daysNeeded

    OnboardingStepContainer(
        title = "Set Your Deadline 🎯",
        subtitle = "Kab tak syllabus complete karna hai?",
        onNext = { onNext(selectedDate) },
        nextEnabled = selectedDate > System.currentTimeMillis(),
        infoText = "Yeh aapki target date hai jab tak poora syllabus khatam karna hai. App is date tak ka realistic timetable banayega."
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📅", style = MaterialTheme.typography.displaySmall)
                if (selectedDate > 0) {
                    Text(dateFormat.format(Date(selectedDate)),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("$daysRemaining days remaining",
                        style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.8f))
                } else {
                    Text("No date selected", color = Color.White.copy(0.5f))
                }
            }
        }

        FieldLabel("Quick select")
        listOf("3 months" to 90, "6 months" to 180, "1 year" to 365, "2 years" to 730)
            .chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, days) ->
                        SelectionChip(text = label, selected = daysRemaining == days,
                            onClick = { selectedDate = System.currentTimeMillis() + days.toLong() * 86400000 },
                            modifier = Modifier.weight(1f))
                    }
                }
            }

        OutlinedButton(
            onClick = { showPicker = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.EditCalendar, null)
            Spacer(Modifier.width(8.dp))
            Text("Pick Custom Date")
        }

        if (selectedDate > 0 && totalStudyMin > 0) {
            if (!feasible) {
                ValidationBanner(
                    "⚠️ Itna syllabus (${totalStudyMin / 60}h) is deadline tak finish karna mushkil hai. Aapko $daysNeeded din chahiye but sirf $daysRemaining din hain. Daily hours badhao ya deadline aage karo.",
                    isError = true
                )
            } else {
                ValidationBanner(
                    "✅ Perfect! ${state.dailyStudyHours}h daily se aap is deadline tak syllabus complete kar loge.",
                    isError = false
                )
            }
        }
    }

    if (showPicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = if (selectedDate > 0) selectedDate
            else System.currentTimeMillis() + 180L * 86400000,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis > System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { selectedDate = it }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}

@Composable
fun StepDailySchedule(
    state: OnboardingState,
    onNext: (hours: Float, slots: List<StudySlotDraft>) -> Unit
) {
    var totalHours by remember { mutableFloatStateOf(state.dailyStudyHours) }
    var slots by remember { mutableStateOf(state.studySlots.toList()) }
    var showAddSlot by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Sun" to 1, "Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 7)

    val totalSlotMinutes = slots.sumOf {
        minutesBetween(it.startHour, it.startMinute, it.endHour, it.endMinute).coerceAtLeast(0)
    }
    val targetMinutes = (totalHours * 60).toInt()
    val exceedsTarget = totalSlotMinutes > targetMinutes
    val meetsTarget = totalSlotMinutes >= targetMinutes && slots.isNotEmpty()

    val lastSlotEnd = slots.maxByOrNull { it.endHour * 60 + it.endMinute }

    OnboardingStepContainer(
        title = "Daily Study Schedule ⏰",
        subtitle = "Din me kab kab padhna chahte ho?",
        onNext = { onNext(totalHours, slots) },
        nextEnabled = slots.isNotEmpty() && !exceedsTarget,
        infoText = "Daily target = aap roz kitne ghante padhna chahte ho. Time slots us target ke andar fit hone chahiye. Slots ka total time daily target se zyada nahi ho sakta."
    ) {
        FieldLabel("Daily Target", required = true,
            infoText = "Roz kitne ghante padhoge. Niche ke slots ka total time isse zyada nahi hona chahiye.")
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = { if (totalHours > 1f) totalHours -= 0.5f }) {
                Icon(Icons.Default.Remove, null, tint = Color.White)
            }
            Text("$totalHours hrs / day", style = MaterialTheme.typography.titleLarge,
                color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (totalHours < 16f) totalHours += 0.5f }) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }

        val usedH = totalSlotMinutes / 60
        val usedM = totalSlotMinutes % 60
        ValidationBanner(
            "Slots total: ${usedH}h ${usedM}m / ${totalHours}h target",
            isError = exceedsTarget
        )

        FieldLabel("Study Time Slots", required = true)

        slots.sortedBy { it.startHour * 60 + it.startMinute }.forEach { slot ->
            val day = daysOfWeek.find { it.second == slot.dayOfWeek }?.first ?: ""
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f)),
                shape = MaterialTheme.shapes.medium) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("$day  ${formatTime12(slot.startHour, slot.startMinute)} - ${formatTime12(slot.endHour, slot.endMinute)}",
                        color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { slots = slots - slot }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (showAddSlot) {
            AddSlotInline(
                daysOfWeek = daysOfWeek,
                defaultStartHour = lastSlotEnd?.endHour ?: 9,
                defaultStartMin = lastSlotEnd?.endMinute ?: 0,
                onAdd = { slot -> slots = slots + slot; showAddSlot = false },
                onCancel = { showAddSlot = false }
            )
        } else if (!exceedsTarget) {
            OutlinedButton(onClick = { showAddSlot = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Time Slot")
            }
        }
    }
}

@Composable
private fun AddSlotInline(
    daysOfWeek: List<Pair<String, Int>>,
    defaultStartHour: Int,
    defaultStartMin: Int,
    onAdd: (StudySlotDraft) -> Unit,
    onCancel: () -> Unit
) {
    var selectedDays by remember { mutableStateOf(setOf(2, 3, 4, 5, 6)) }
    var startHour by remember { mutableIntStateOf(defaultStartHour) }
    var startMin by remember { mutableIntStateOf(defaultStartMin) }
    var endHour by remember { mutableIntStateOf((defaultStartHour + 3).coerceAtMost(23)) }
    var endMin by remember { mutableIntStateOf(defaultStartMin) }

    val durationMin = minutesBetween(startHour, startMin, endHour, endMin)
    val validTime = durationMin > 0

    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f)),
        shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New Slot", style = MaterialTheme.typography.titleSmall,
                color = Color.White, fontWeight = FontWeight.Bold)

            FieldLabel("Days", infoText = "Konse din ye slot repeat hoga. Multiple din select kar sakte ho.")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                daysOfWeek.forEach { (label, id) ->
                    Box(modifier = Modifier.weight(1f)) {
                        SelectionChip(text = label, selected = id in selectedDays,
                            onClick = {
                                selectedDays = if (id in selectedDays) selectedDays - id else selectedDays + id
                            }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TimePicker12Hr("Start", startHour, startMin,
                    onTimeChange = { h, m ->
                        startHour = h; startMin = m
                        if (endHour * 60 + endMin <= h * 60 + m) {
                            endHour = (h + 1).coerceAtMost(23); endMin = m
                        }
                    }, modifier = Modifier.weight(1f))
                Text("→", color = Color.White)
                TimePicker12Hr("End", endHour, endMin,
                    onTimeChange = { h, m -> endHour = h; endMin = m }, modifier = Modifier.weight(1f))
            }

            if (!validTime) {
                ValidationBanner("End time start time se baad ka hona chahiye")
            } else {
                ValidationBanner("Duration: ${durationMin / 60}h ${durationMin % 60}m", isError = false)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = Color.White.copy(0.7f))
                }
                Button(
                    onClick = {
                        selectedDays.forEach { day ->
                            onAdd(StudySlotDraft(day, startHour, startMin, endHour, endMin))
                        }
                    },
                    enabled = validTime && selectedDays.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Add", color = com.studyplanner.app.ui.theme.LocalAppTheme.current.colors.primary,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun StepperRow(label: String, value: Int, unit: String, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > min) onChange(value - step) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Remove, null, tint = Color.White)
            }
            Text("$value $unit", color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 60.dp))
            IconButton(onClick = { if (value < max) onChange(value + step) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    }
}
