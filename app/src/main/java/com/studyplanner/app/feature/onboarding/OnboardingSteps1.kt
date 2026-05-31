package com.studyplanner.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyplanner.app.ui.components.*
import com.studyplanner.app.ui.theme.LocalAppTheme

@Composable
fun StepProfile(
    state: OnboardingState,
    onNext: (name: String, nickname: String, gender: String, city: String, state: String, pincode: String) -> Unit
) {
    var name by remember { mutableStateOf(state.name) }
    var nickname by remember { mutableStateOf(state.nickname) }
    var gender by remember { mutableStateOf(state.gender.ifEmpty { "MALE" }) }
    var userState by remember { mutableStateOf(state.state) }
    var city by remember { mutableStateOf(state.city) }
    var pincode by remember { mutableStateOf(state.pincode) }

    val pincodeValid = pincode.isEmpty() || pincode.length == 6
    val canProceed = name.isNotBlank() && userState.isNotBlank() && city.isNotBlank() && pincodeValid

    OnboardingStepContainer(
        title = "Tell us about yourself 👋",
        subtitle = "Yeh details leaderboard aur personalization ke liye hain",
        onNext = { onNext(name, nickname, gender, city, userState, pincode) },
        nextEnabled = canProceed,
        infoText = "Hum aapka naam, sheher aur state isliye lete hain taaki aapko aapke area ke students ke saath leaderboard pe rank de sakein. Pincode optional hai."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Full Name", required = true,
                infoText = "Aapka asli naam — yeh certificates aur reports pe dikhega.")
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("Your name", color = Color.White.copy(0.4f)) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Nickname",
                infoText = "App aapko is naam se bulayega, jaise 'Uth ja Rahul!'. Khaali chhod do toh full name use hoga.")
            OutlinedTextField(
                value = nickname, onValueChange = { nickname = it },
                placeholder = { Text("What should we call you?", color = Color.White.copy(0.4f)) },
                leadingIcon = { Icon(Icons.Default.Face, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = outlinedTextFieldWhiteColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Gender", required = true,
                infoText = "Female select karne pe period tracking ka optional feature unlock hoga.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("MALE" to "👨 Male", "FEMALE" to "👩 Female", "OTHER" to "🧑 Other").forEach { (id, label) ->
                    SelectionChip(text = label, selected = gender == id,
                        onClick = { gender = id }, modifier = Modifier.weight(1f))
                }
            }
        }

        SearchableDropdown(
            label = "State", selected = userState, options = IndiaData.states,
            onSelect = { userState = it; city = "" }, required = true,
            placeholder = "Select your state"
        )

        SearchableDropdown(
            label = "City / District", selected = city,
            options = IndiaData.districtsFor(userState),
            onSelect = { city = it }, required = true,
            enabled = userState.isNotBlank(),
            placeholder = if (userState.isBlank()) "Pehle state select karo" else "Select your city"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Pincode", infoText = "6-digit area pincode — aur accurate local ranking ke liye.")
            OutlinedTextField(
                value = pincode,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pincode = it },
                placeholder = { Text("6-digit pincode", color = Color.White.copy(0.4f)) },
                leadingIcon = { Icon(Icons.Default.PinDrop, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = !pincodeValid,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                colors = outlinedTextFieldWhiteColors(),
                shape = RoundedCornerShape(12.dp)
            )
            if (!pincodeValid) ValidationBanner("Pincode 6 digits ka hona chahiye")
        }
    }
}

@Composable
fun StepExamType(
    state: OnboardingState,
    onNext: (examType: String, examSubType: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(state.examType) }
    var selectedSubType by remember { mutableStateOf(state.examSubType) }

    val examTypes = listOf(
        "SCHOOL_10" to "🏫 School (Class 5-10)",
        "SCHOOL_12" to "🏫 School (Class 11-12)",
        "COMPETITIVE" to "🎯 Competitive Exams",
        "GENERAL" to "🎓 General / Graduation",
    )

    val competitiveSubTypes = listOf(
        "UPSC_PRELIMS" to "UPSC Prelims",
        "UPSC_MAINS" to "UPSC Mains",
        "SSC_CGL" to "SSC CGL",
        "SSC_CHSL" to "SSC CHSL",
        "BANKING_IBPS" to "Banking (IBPS)",
        "BANKING_SBI" to "Banking (SBI)",
        "RAILWAY_NTPC" to "Railway NTPC",
        "RAILWAY_GROUP_D" to "Railway Group D",
        "STATE_PCS_UP" to "State PCS (UP)",
        "STATE_PCS_BIHAR" to "State PCS (Bihar)",
        "STATE_PCS_MP" to "State PCS (MP)",
        "STATE_PCS_RAJ" to "State PCS (Rajasthan)",
    )

    OnboardingStepContainer(
        title = "What are you preparing for? 🎯",
        subtitle = "We'll load the right syllabus for you",
        onNext = { onNext(selectedType, selectedSubType) },
        nextEnabled = selectedType.isNotEmpty() && (selectedType != "COMPETITIVE" || selectedSubType.isNotEmpty())
    ) {
        examTypes.forEach { (id, label) ->
            SelectionChip(
                text = label,
                selected = selectedType == id,
                onClick = { selectedType = id; selectedSubType = "" },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (selectedType == "COMPETITIVE") {
            Spacer(Modifier.height(8.dp))
            Text("Select Exam", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            competitiveSubTypes.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (id, label) ->
                        SelectionChip(
                            text = label,
                            selected = selectedSubType == id,
                            onClick = { selectedSubType = id },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StepStudyMaterial(
    state: OnboardingState,
    onNext: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(state.studyMaterials.toMutableSet()) }

    val materials = listOf(
        "BOOK" to "📚" to "Hard Copy / Book",
        "YOUTUBE" to "🎥" to "YouTube Videos",
        "INSTITUTE" to "🏫" to "Institute / Coaching",
        "NOTES_PDF" to "📝" to "Notes / PDF",
    )

    OnboardingStepContainer(
        title = "How do you study? 📖",
        subtitle = "Select all that apply",
        onNext = { onNext(selected.toList()) },
        nextEnabled = selected.isNotEmpty()
    ) {
        materials.forEach { (ids, label) ->
            val (id, emoji) = ids
            val isSelected = id in selected
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    selected = selected.toMutableSet().also {
                        if (id in it) it.remove(id) else it.add(id)
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color.White.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(emoji, style = MaterialTheme.typography.headlineSmall)
                    Text(label, style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun StepSubjects(
    state: OnboardingState,
    viewModel: OnboardingViewModel,
    ocrResult: List<ChapterDraft>?,
    onScanRequest: () -> Unit,
    onClearOcrResult: () -> Unit,
    onNext: () -> Unit
) {
    var showAddSubject by remember { mutableStateOf(false) }

    // Agar OCR scan se data lekar user wapas aata hai, toh sheet automatically open ho jayegi
    LaunchedEffect(ocrResult) {
        if (ocrResult != null) {
            showAddSubject = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        if (showAddSubject) {
            AddSubjectSheet(
                studyMaterials = state.studyMaterials,
                initialChapters = ocrResult ?: emptyList(),
                onOcrScan = onScanRequest,
                onAdd = { subject ->
                    viewModel.addSubject(subject)
                    showAddSubject = false
                    onClearOcrResult() // Data consume hone ke baad clear karein
                },
                onDismiss = {
                    showAddSubject = false
                    onClearOcrResult() // Cancel karne par bhi clean-up
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Add Your Subjects 📚",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Add all subjects you want to study",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                if (state.subjects.isEmpty()) {
                    item {
                        EmptyState(
                            emoji = "📚",
                            title = "No subjects added",
                            subtitle = "Tap + to add your first subject",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                itemsIndexed(state.subjects) { index, subject ->
                    SubjectSummaryCard(
                        subject = subject,
                        onRemove = { viewModel.removeSubject(index) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showAddSubject = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Subject")
                    }
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 0.dp),
                enabled = state.subjects.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    text = "Continue",
                    color = LocalAppTheme.current.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SubjectSummaryCard(subject: SubjectDraft, onRemove: () -> Unit) {
    val totalMin = (subject.totalPages * subject.readingSpeedMinPerPage).toInt() + subject.totalVideoMinutes
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color(android.graphics.Color.parseColor(subject.colorHex)))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${subject.chapters.size} chapters • ${subject.totalPages} pages • ~${totalMin / 60}h ${totalMin % 60}m",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AddSubjectSheet(
    studyMaterials: List<String>,
    initialChapters: List<ChapterDraft>,
    onOcrScan: () -> Unit,
    onAdd: (SubjectDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#1565C0") }
    var priority by remember { mutableStateOf("HIGH") }
    var totalPages by remember { mutableStateOf("") }
    var readingSpeed by remember { mutableStateOf("2") }
    var videoMinutes by remember { mutableStateOf("") }

    // Chapters list ko state mein hold kiya hai aur initial OCR lists se initialize kiya hai
    var chapters by remember { mutableStateOf(initialChapters) }

    // Manual chapter add karne ke liye fields
    var chapterName by remember { mutableStateOf("") }
    var chapterStart by remember { mutableStateOf("") }
    var chapterEnd by remember { mutableStateOf("") }

    // Agar sheet open rehte hue bhi fresh OCR data mile, toh list append ho jaye
    LaunchedEffect(initialChapters) {
        if (initialChapters.isNotEmpty()) {
            // Sirf wahi chapters append karein jo pehle se list mein na hon (Duplicate handling)
            val newChapters = initialChapters.filter { fresh ->
                chapters.none { existing -> existing.name == fresh.name }
            }
            chapters = chapters + newChapters
        }
    }

    val colors = listOf("#1565C0", "#E65100", "#2E7D32", "#6A1B9A", "#C62828", "#FF8F00", "#00838F")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP BAR WITH BACK AND SCAN INDEX
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(
                text = "Add Subject",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Scan Index Button
            OutlinedButton(
                onClick = onOcrScan,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.DocumentScanner, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Scan Index", style = MaterialTheme.typography.labelMedium)
            }
        }

        // SUBJECT NAME
        OutlinedTextField(
            value = subjectName,
            onValueChange = { subjectName = it },
            label = { Text("Subject Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = outlinedTextFieldWhiteColors()
        )

        // COLOR PICKER
        Text("Color", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                        .clickable { selectedColor = colorHex }
                        .then(
                            if (selectedColor == colorHex)
                                Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                )
            }
        }

        // PRIORITY PICKER
        Text("Priority", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("HIGH" to "🔴 High", "MEDIUM" to "🟡 Medium", "LOW" to "🟢 Low").forEach { (id, label) ->
                SelectionChip(
                    text = label,
                    selected = priority == id,
                    onClick = { priority = id },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // STUDY MATERIAL CONDITIONAL FIELDS
        if ("BOOK" in studyMaterials || "NOTES_PDF" in studyMaterials) {
            OutlinedTextField(
                value = totalPages, onValueChange = { totalPages = it },
                label = { Text("Total Pages") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outlinedTextFieldWhiteColors()
            )
            OutlinedTextField(
                value = readingSpeed, onValueChange = { readingSpeed = it },
                label = { Text("Reading Speed (min/page)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outlinedTextFieldWhiteColors()
            )
        }

        if ("YOUTUBE" in studyMaterials || "INSTITUTE" in studyMaterials) {
            OutlinedTextField(
                value = videoMinutes, onValueChange = { videoMinutes = it },
                label = { Text("Total Video Duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outlinedTextFieldWhiteColors()
            )
        }

        // DYNAMIC CHAPTERS LIST (Shows mixed: OCR + Manual items)
        Text(
            text = "Chapters (${chapters.size} added)",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )

        chapters.forEachIndexed { i, ch ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${i + 1}. ${ch.name} (pg ${ch.pageStart}-${ch.pageEnd})",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Individual delete button (Chahe scan se aya ho ya manual se)
                    IconButton(
                        onClick = {
                            chapters = chapters.toMutableList().also { it.removeAt(i) }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // MANUAL CHAPTER INPUT SECTION
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = chapterName, onValueChange = { chapterName = it },
                label = { Text("Chapter name") }, modifier = Modifier.weight(2f), singleLine = true,
                colors = outlinedTextFieldWhiteColors()
            )
            OutlinedTextField(
                value = chapterStart, onValueChange = { chapterStart = it },
                label = { Text("From") }, modifier = Modifier.weight(1f), singleLine = true,
                colors = outlinedTextFieldWhiteColors()
            )
            OutlinedTextField(
                value = chapterEnd, onValueChange = { chapterEnd = it },
                label = { Text("To") }, modifier = Modifier.weight(1f), singleLine = true,
                colors = outlinedTextFieldWhiteColors()
            )
        }

        TextButton(
            onClick = {
                if (chapterName.isNotBlank()) {
                    chapters = chapters + ChapterDraft(
                        name = chapterName,
                        pageStart = chapterStart.toIntOrNull() ?: 1,
                        pageEnd = chapterEnd.toIntOrNull() ?: 1,
                        orderIndex = chapters.size
                    )
                    chapterName = ""; chapterStart = ""; chapterEnd = ""
                }
            }
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
            Spacer(Modifier.width(4.dp))
            Text("Add Chapter Manually", color = Color.White)
        }

        // FINAL SAVE BUTTON
        Button(
            onClick = {
                onAdd(SubjectDraft(
                    name = subjectName,
                    colorHex = selectedColor,
                    priority = priority,
                    totalPages = totalPages.toIntOrNull() ?: 0,
                    readingSpeedMinPerPage = readingSpeed.toFloatOrNull() ?: 2f,
                    totalVideoMinutes = videoMinutes.toIntOrNull() ?: 0,
                    chapters = chapters // Final mixed list ViewModel me jayegi
                ))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = subjectName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text(
                text = "Add Subject",
                color = LocalAppTheme.current.colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
