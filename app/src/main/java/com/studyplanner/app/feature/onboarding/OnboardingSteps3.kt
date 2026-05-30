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

@Composable
fun StepBreakSettings(
    state: OnboardingState,
    onNext: (studyMin: Int, breakMin: Int, longAfter: Int, longMin: Int) -> Unit
) {
    var studyMin by remember { mutableIntStateOf(state.breakStudyMinutes) }
    var breakMin by remember { mutableIntStateOf(state.breakMinutes) }
    var longAfter by remember { mutableIntStateOf(state.longBreakAfterSessions) }
    var longMin by remember { mutableIntStateOf(state.longBreakMinutes) }

    OnboardingStepContainer(
        title = "Break Settings ☕",
        subtitle = "Sahi break cycle se focus banaye rakhte ho",
        onNext = { onNext(studyMin, breakMin, longAfter, longMin) },
        nextEnabled = true,
        infoText = "Pomodoro technique: thodi der padho, chhota break lo. Kuch sessions ke baad ek bada (long) break — dimaag fresh rehta hai."
    ) {
        StepperRow("Study duration", studyMin, "min", 25, 90, 5) { studyMin = it }

        FieldLabel("Short Break", infoText = "Har study session ke turant baad ka chhota break. Paani piyo, stretch karo.")
        StepperRow("Short break", breakMin, "min", 5, 30, 5) { breakMin = it }

        FieldLabel("Long Break", infoText = "Yeh BADA break hai jo kuch sessions ke baad aata hai — jaise 4 sessions ke baad. Isme aap properly relax kar sakte ho, khana kha sakte ho. Short break choti hoti hai (har session ke baad), long break badi (kai sessions ke baad).")
        StepperRow("Long break after", longAfter, "sessions", 2, 8, 1) { longAfter = it }
        StepperRow("Long break", longMin, "min", 15, 60, 5) { longMin = it }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
            shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📋 Aapka cycle aisa chalega:", color = Color.White,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("• Padho ${studyMin}min → Short break ${breakMin}min",
                    color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodySmall)
                Text("• Ye $longAfter baar repeat hoga",
                    color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodySmall)
                Text("• Phir LONG break ${longMin}min (lamba aaram) 🛋️",
                    color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StepPersonalRoutine(
    state: OnboardingState,
    onNext: (List<RoutineDraft>) -> Unit
) {
    var routines by remember { mutableStateOf(state.personalRoutines.toList()) }
    var showAdd by remember { mutableStateOf(false) }

    OnboardingStepContainer(
        title = "Personal Routine 🗓️",
        subtitle = "Aapki daily activities (in pe study slot nahi aayega)",
        onNext = { onNext(routines) },
        nextEnabled = true,
        nextText = if (routines.isEmpty()) "Skip" else "Continue",
        infoText = "Namaz, exercise, yoga, khana jaise fixed kaam. App in times pe study session nahi lagayega. Flexible = app thoda adjust kar sakta hai zaroorat pe; Fixed = bilkul nahi chhedega."
    ) {
        routines.forEach { r ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f)),
                shape = MaterialTheme.shapes.medium) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.name, color = Color.White, fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium)
                        Text("${formatTime12(r.startHour, r.startMinute)} - ${formatTime12(r.endHour, r.endMinute)} • ${if (r.isFlexible) "Flexible" else "Fixed"}",
                            color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { routines = routines - r }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (showAdd) {
            AddRoutineInline(
                onAdd = { routines = routines + it; showAdd = false },
                onCancel = { showAdd = false }
            )
        } else {
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Routine")
            }
        }
    }
}

@Composable
private fun AddRoutineInline(
    onAdd: (RoutineDraft) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🕌") }
    var startHour by remember { mutableIntStateOf(6) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(6) }
    var endMin by remember { mutableIntStateOf(30) }
    var flexible by remember { mutableStateOf(false) }

    val presets = listOf("🕌 Namaz", "🧘 Yoga", "🏃 Exercise", "🍳 Cooking", "🚿 Bath", "😴 Nap")
    val duration = minutesBetween(startHour, startMin, endHour, endMin)
    val valid = duration > 0 && name.isNotBlank()

    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f)),
        shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("Quick add")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.take(3).forEach { p ->
                    Box(modifier = Modifier.weight(1f)) {
                        SelectionChip(text = p, selected = name == p.substringAfter(" "),
                            onClick = { emoji = p.substringBefore(" "); name = p.substringAfter(" ") },
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.drop(3).forEach { p ->
                    Box(modifier = Modifier.weight(1f)) {
                        SelectionChip(text = p, selected = name == p.substringAfter(" "),
                            onClick = { emoji = p.substringBefore(" "); name = p.substringAfter(" ") },
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Activity Name") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, colors = outlinedTextFieldWhiteColors(),
                leadingIcon = { Text(emoji) })

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TimePicker12Hr("Start", startHour, startMin,
                    onTimeChange = { h, m ->
                        startHour = h; startMin = m
                        if (endHour * 60 + endMin <= h * 60 + m) { endHour = h; endMin = (m + 30) % 60; if (endMin < m) endHour = h + 1 }
                    }, modifier = Modifier.weight(1f))
                Text("→", color = Color.White)
                TimePicker12Hr("End", endHour, endMin,
                    onTimeChange = { h, m -> endHour = h; endMin = m }, modifier = Modifier.weight(1f))
            }

            if (duration <= 0) ValidationBanner("End time start ke baad hona chahiye")
            else ValidationBanner("Duration: ${duration} min", isError = false)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = flexible, onCheckedChange = { flexible = it })
                Column(modifier = Modifier.weight(1f)) {
                    Text("Flexible timing", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text(if (flexible) "App zaroorat pe thoda adjust kar sakta hai"
                        else "Bilkul fixed — app kabhi nahi chhedega",
                        color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                }
                InfoTooltip("Flexible ON: agar timetable me jagah kam ho toh app is activity ko 15-30 min aage-peeche kar sakta hai. OFF: ye time hamesha block rahega, koi study session yahan nahi aayega.", "Flexible")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = Color.White.copy(0.7f))
                }
                Button(onClick = {
                    onAdd(RoutineDraft(name, emoji, "1,2,3,4,5,6,7", startHour, startMin, endHour, endMin, flexible))
                }, enabled = valid, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                    Text("Add", color = com.studyplanner.app.ui.theme.LocalAppTheme.current.colors.primary,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StepPeriodTracking(
    state: OnboardingState,
    onNext: (enabled: Boolean, day: Int, cycle: Int, heavy: Int, schedule: String) -> Unit
) {
    var enabled by remember { mutableStateOf(state.periodEnabled) }
    var day by remember { mutableIntStateOf(state.periodDayOfMonth) }
    var cycle by remember { mutableIntStateOf(state.periodCycleDays) }
    var heavy by remember { mutableIntStateOf(state.periodHeavyDays) }
    var schedule by remember { mutableStateOf(state.periodHeavyDaySchedule) }

    OnboardingStepContainer(
        title = "Period Tracking 🌸",
        subtitle = "Optional — difficult days pe schedule adapt karta hai",
        onNext = { onNext(enabled, day, cycle, heavy, schedule) },
        nextEnabled = true,
        nextText = if (!enabled) "Skip" else "Continue",
        infoText = "Heavy pain wale dino me app aapka schedule halka kar dega taaki aap rest kar sako. Ye data sirf aapke phone pe rehta hai, private hai."
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()) {
            Switch(checked = enabled, onCheckedChange = { enabled = it })
            Column {
                Text("Enable Period Tracking", color = Color.White, fontWeight = FontWeight.Medium)
                Text("Heavy days pe schedule adapt hoga", color = Color.White.copy(0.6f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        if (enabled) {
            StepperRow("Expected start day", day, "of month", 1, 31, 1) { day = it }
            StepperRow("Cycle length", cycle, "days", 21, 35, 1) { cycle = it }
            StepperRow("Heavy pain days", heavy, "days", 1, 5, 1) { heavy = it }

            FieldLabel("Schedule on heavy days", infoText = "Heavy pain wale dino me kaisa schedule chahiye.")
            listOf(
                "COMPLETE_REST" to "😴 Complete rest — no sessions",
                "REDUCED" to "📉 Reduced — 50% sessions",
                "LIGHT_ONLY" to "📖 Light topics only — revision/reading",
            ).forEach { (id, label) ->
                SelectionChip(text = label, selected = schedule == id,
                    onClick = { schedule = id }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun StepFocusSettings(
    state: OnboardingState,
    onNext: (mode: String, channels: List<String>) -> Unit
) {
    var mode by remember { mutableStateOf(state.focusBlockingMode) }
    var channels by remember { mutableStateOf(state.youtubeWhitelistChannels.toList()) }
    var newChannel by remember { mutableStateOf("") }

    val suggested = when {
        state.examType.contains("UPSC") -> listOf("Drishti IAS", "Vision IAS", "StudyIQ IAS", "Unacademy UPSC")
        state.examType.contains("SSC") -> listOf("StudyIQ SSC", "Adda247", "SSC Adda")
        state.examType.contains("BANKING") -> listOf("Adda247 Banking", "Oliveboard", "Study Smart")
        else -> listOf("Khan Academy", "NCERT Official", "Vedantu")
    }

    val modes = listOf(
        Triple("COMPLETE_BLOCK", "🔴 Complete Block", "Saare social media + YouTube — sab block. Maximum focus."),
        Triple("YOUTUBE_BLOCK", "📵 YouTube Block", "Social media + YouTube 100% band. Books se padho."),
        Triple("YOUTUBE_WHITELIST", "✅ YouTube Whitelist", "Social block, lekin aapke chune hue study channels khulenge."),
        Triple("DISABLED", "🔓 Disabled", "Koi blocking nahi (recommended nahi)."),
    )

    OnboardingStepContainer(
        title = "Focus & Blocking 🔒",
        subtitle = "Study ke time distractions block karo",
        onNext = { onNext(mode, channels) },
        nextEnabled = true,
        infoText = "Session ke dauran distracting apps band ho jayenge. Har mode ke aage ? pe click karke detail dekho."
    ) {
        modes.forEach { (id, label, desc) ->
            Card(modifier = Modifier.fillMaxWidth(), onClick = { mode = id },
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == id) Color.White.copy(0.25f) else Color.White.copy(0.08f)
                ), shape = MaterialTheme.shapes.medium) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall)
                            InfoTooltip(desc, label)
                        }
                        Text(desc, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    if (mode == id) Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                }
            }
        }

        if (mode == "YOUTUBE_WHITELIST") {
            FieldLabel("Whitelisted Channels for ${state.examSubType}",
                infoText = "Sirf ye channels session ke time khulenge. Baaki YouTube block rahega.")
            suggested.forEach { ch ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = ch in channels, onCheckedChange = { checked ->
                        channels = if (checked) channels + ch else channels - ch
                    }, colors = CheckboxDefaults.colors(
                        checkmarkColor = com.studyplanner.app.ui.theme.LocalAppTheme.current.colors.primary,
                        uncheckedColor = Color.White))
                    Text(ch, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newChannel, onValueChange = { newChannel = it },
                    label = { Text("Add channel") }, modifier = Modifier.weight(1f),
                    singleLine = true, colors = outlinedTextFieldWhiteColors())
                IconButton(onClick = {
                    if (newChannel.isNotBlank()) { channels = channels + newChannel; newChannel = "" }
                }) { Icon(Icons.Default.Add, null, tint = Color.White) }
            }
        }
    }
}

@Composable
fun StepMorningAlarm(
    state: OnboardingState,
    onNext: (h: Int, m: Int, msg: String, music: String, wakeType: String, days: String) -> Unit
) {
    var hour by remember { mutableIntStateOf(state.morningAlarmHour) }
    var minute by remember { mutableIntStateOf(state.morningAlarmMinute) }
    var message by remember { mutableStateOf(state.morningAlarmMessage) }
    var soundType by remember { mutableStateOf("DEFAULT") }
    var musicUrl by remember { mutableStateOf(state.morningAlarmMusicUrl) }
    var wakeType by remember { mutableStateOf(state.morningAlarmWakeType) }

    OnboardingStepContainer(
        title = "Morning Alarm ⏰",
        subtitle = "Har din motivation ke saath shuru karo",
        onNext = {
            val music = if (soundType == "CUSTOM") musicUrl else "SYSTEM:$soundType"
            onNext(hour, minute, message, music, wakeType, "1,2,3,4,5,6,7")
        },
        nextEnabled = true,
        infoText = "Subah uthne ke liye alarm. Apna message, sound aur wake-up verification choose karo."
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f)),
            shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⏰", style = MaterialTheme.typography.displaySmall)
                TimePicker12Hr("", hour, minute, onTimeChange = { h, m -> hour = h; minute = m })
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Motivational Message", infoText = "Alarm bajne pe ye message dikhega aur bola jayega.")
            OutlinedTextField(value = message, onValueChange = { message = it },
                leadingIcon = { Icon(Icons.Default.EmojiEmotions, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors())
        }

        FieldLabel("Alarm Sound", infoText = "System ki ready sounds choose karo ya apna custom URL daalo.")
        listOf(
            "DEFAULT" to "🔔 Default Alarm",
            "RINGTONE" to "📞 Ringtone",
            "NOTIFICATION" to "🔉 Notification Tone",
            "CUSTOM" to "🔗 Custom URL",
        ).forEach { (id, label) ->
            SelectionChip(text = label, selected = soundType == id,
                onClick = { soundType = id }, modifier = Modifier.fillMaxWidth())
        }

        if (soundType == "CUSTOM") {
            OutlinedTextField(value = musicUrl, onValueChange = { musicUrl = it },
                label = { Text("Sound URL") }, leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors())
        }

        FieldLabel("Wake-up Verification", infoText = "Alarm band karne ke liye ye task karna padega — taaki aap dobara so na jao!")
        listOf("MATH" to "🧮 Solve math problem", "QR" to "📱 Scan QR code", "STEPS" to "🚶 Walk 10 steps").forEach { (id, label) ->
            SelectionChip(text = label, selected = wakeType == id,
                onClick = { wakeType = id }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StepVisionBoard(
    state: OnboardingState,
    isLoading: Boolean,
    onComplete: (photo: String, dream: String, img: String) -> Unit
) {
    var dreamPost by remember { mutableStateOf(state.visionBoardDreamPost) }
    var targetRank by remember { mutableStateOf("") }
    var targetYear by remember { mutableStateOf("2027") }

    val previewText = buildString {
        if (targetRank.isNotBlank()) append(targetRank)
        if (dreamPost.isNotBlank()) { if (isNotEmpty()) append(" "); append(dreamPost) }
        if (targetYear.isNotBlank()) { if (isNotEmpty()) append(" "); append(targetYear) }
    }.ifBlank { "Your dream goal" }

    OnboardingStepContainer(
        title = "Your Vision Board 🌟",
        subtitle = "Har session se pehle ye flash hoga — yaad dilane ke liye ki kyun padh rahe ho",
        onComplete@ { onComplete("", previewText, "") },
        nextEnabled = !isLoading,
        nextText = if (isLoading) "Setting up..." else "Start My Journey! 🚀",
        isLoading = isLoading,
        infoText = "Vision board aapka sapna hai. Session start hone se pehle 3 second ke liye ye screen pe aayega taaki aapko motivation mile."
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
            shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(28.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🎯", style = MaterialTheme.typography.displayMedium)
                Text("My Goal", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodyMedium)
                Text(previewText, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Target Post/Rank", infoText = "Aap kya banna chahte ho — jaise 'IAS Officer', 'PO', 'SSC Inspector'.")
            OutlinedTextField(value = targetRank, onValueChange = { targetRank = it },
                placeholder = { Text("e.g. IAS Officer", color = Color.White.copy(0.4f)) },
                leadingIcon = { Icon(Icons.Default.MilitaryTech, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors())
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Your Name in Dream")
            OutlinedTextField(value = dreamPost, onValueChange = { dreamPost = it },
                placeholder = { Text("e.g. Rahul Sharma", color = Color.White.copy(0.4f)) },
                leadingIcon = { Icon(Icons.Default.Star, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors())
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Target Year")
            OutlinedTextField(value = targetYear,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) targetYear = it },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors())
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f)),
            shape = MaterialTheme.shapes.medium) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700))
                Column {
                    Text("Almost done! 🎉", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Aapka personalized timetable ready ho jayega",
                        color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
