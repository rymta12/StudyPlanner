package com.studyplanner.app.feature.subject

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.core.data.local.entity.ChapterEntity
import com.studyplanner.app.core.data.local.entity.TopicEntity
import com.studyplanner.app.ui.components.LoadingScreen

@Composable
fun SubjectDetailScreen(
    onBack: () -> Unit,
    viewModel: SubjectViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }
    var showAddChapter by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<ChapterEntity?>(null) }

    if (state.isLoading) { LoadingScreen(); return }

    if (isEditing) {
        SubjectEditScreen(
            state = state,
            onSave = { name, color, priority, pages, speed, videoMin ->
                viewModel.saveSubject(name, color, priority, pages, speed, videoMin)
                isEditing = false
            },
            onBack = { isEditing = false }
        )
        return
    }

    val subject = state.subject ?: run { onBack(); return }
    val subjectColor = runCatching {
        Color(android.graphics.Color.parseColor(subject.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SubjectDetailHeader(
                subject = subject,
                subjectColor = subjectColor,
                totalChapters = state.chapters.size,
                completedChapters = state.chapters.count { it.completionPercent == 1f },
                onBack = onBack,
                onEdit = { isEditing = true }
            )
        }

        item {
            ProgressBar(
                percent = if (subject.estimatedTotalMinutes == 0) 0f
                else subject.completedMinutes.toFloat() / subject.estimatedTotalMinutes,
                color = subjectColor
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chapters (${state.chapters.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddChapter = true }) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (showAddChapter) {
            item {
                ChapterFormCard(
                    chapter = null,
                    onSave = { name, ps, pe ->
                        viewModel.addChapter(name, ps, pe)
                        showAddChapter = false
                    },
                    onCancel = { showAddChapter = false }
                )
            }
        }

        editingChapter?.let { editing ->
            item {
                ChapterFormCard(
                    chapter = editing,
                    onSave = { name, ps, pe ->
                        viewModel.updateChapter(editing, name, ps, pe)
                        editingChapter = null
                    },
                    onCancel = { editingChapter = null }
                )
            }
        }

        if (state.chapters.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📖", fontSize = 40.sp)
                        Text("No chapters added",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { showAddChapter = true }) {
                            Text("Add first chapter")
                        }
                    }
                }
            }
        } else {
            items(state.chapters) { cwt ->
                ChapterCard(
                    cwt = cwt,
                    subjectColor = subjectColor,
                    onEdit = { editingChapter = cwt.chapter },
                    onDelete = { viewModel.deleteChapter(cwt.chapter) },
                    onTopicToggle = { viewModel.markTopicComplete(it) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun SubjectDetailHeader(
    subject: com.studyplanner.app.core.data.local.entity.SubjectEntity,
    subjectColor: Color,
    totalChapters: Int,
    completedChapters: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(subjectColor.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .systemBarsPadding()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = subjectColor)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                        .background(subjectColor.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(subject.name.first().uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold, color = subjectColor)
                }
                Column {
                    Text(subject.name, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Priority: ${subject.priority}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip("📖", "$completedChapters/$totalChapters chapters", subjectColor)
                InfoChip("⏱️", "${subject.estimatedTotalMinutes / 60}h total", subjectColor)
                InfoChip("✅", "${subject.completedMinutes / 60}h done", subjectColor)
            }
        }
    }
}

@Composable
private fun InfoChip(emoji: String, text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProgressBar(percent: Float, color: Color) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Overall Progress", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(percent * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color, trackColor = color.copy(0.15f)
        )
    }
}

@Composable
private fun ChapterCard(
    cwt: ChapterWithTopics,
    subjectColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTopicToggle: (TopicEntity) -> Unit,
    viewModel: SubjectViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Chapter?") },
            text = { Text("\"${cwt.chapter.name}\" delete ho jayega. Ye undo nahi ho sakta.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(
                            if (cwt.completionPercent == 1f) subjectColor.copy(0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (cwt.completionPercent == 1f) "✅" else "${cwt.chapter.orderIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cwt.completionPercent == 1f) subjectColor
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(cwt.chapter.name, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Pg ${cwt.chapter.pageStart}–${cwt.chapter.pageEnd} • ${cwt.chapter.totalPages} pages • ${cwt.chapter.estimatedMinutes}min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp))
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (cwt.completionPercent > 0f || cwt.topics.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { cwt.completionPercent },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = subjectColor, trackColor = subjectColor.copy(0.1f)
                )
            }

            AnimatedVisibility(visible = expanded && cwt.topics.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                    Text("Topics", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                    cwt.topics.forEach { topic ->
                        TopicRow(
                            topic = topic,
                            subjectColor = subjectColor,
                            onToggle = { onTopicToggle(topic) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(topic: TopicEntity, subjectColor: Color, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (topic.status == "COMPLETED") Icons.Default.CheckCircle
            else Icons.Default.RadioButtonUnchecked,
            null,
            tint = if (topic.status == "COMPLETED") subjectColor
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(topic.name, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (topic.status == "COMPLETED")
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface)
        Text("${topic.estimatedMinutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChapterFormCard(
    chapter: ChapterEntity?,
    onSave: (String, Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(chapter?.name ?: "") }
    var pageStart by remember { mutableStateOf(chapter?.pageStart?.toString() ?: "") }
    var pageEnd by remember { mutableStateOf(chapter?.pageEnd?.toString() ?: "") }

    val valid = name.isNotBlank() &&
            pageStart.toIntOrNull() != null && pageEnd.toIntOrNull() != null &&
            (pageEnd.toIntOrNull() ?: 0) >= (pageStart.toIntOrNull() ?: 0)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (chapter == null) "Add Chapter" else "Edit Chapter",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Chapter Name *") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pageStart, onValueChange = { pageStart = it },
                    label = { Text("Page Start") }, modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(10.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = pageEnd, onValueChange = { pageEnd = it },
                    label = { Text("Page End") }, modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(10.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
            if (valid) {
                val pages = (pageEnd.toInt() - pageStart.toInt() + 1)
                Text("$pages pages", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    onSave(name, pageStart.toInt(), pageEnd.toInt())
                }, enabled = valid, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)) {
                    Text(if (chapter == null) "Add" else "Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectEditScreen(
    state: SubjectDetailUiState,
    onSave: (String, String, String, Int, Float, Int) -> Unit,
    onBack: () -> Unit,
) {
    val subject = state.subject ?: return
    var name by remember { mutableStateOf(subject.name) }
    var colorHex by remember { mutableStateOf(subject.colorHex) }
    var priority by remember { mutableStateOf(subject.priority) }
    var totalPages by remember { mutableStateOf(subject.totalPages.toString()) }
    var readingSpeed by remember { mutableStateOf(subject.readingSpeedMinPerPage.toString()) }
    var videoMinutes by remember { mutableStateOf(subject.totalVideoMinutes.toString()) }

    val colors = listOf(
        "#1565C0", "#2E7D32", "#C62828", "#F57F17",
        "#6A1B9A", "#00838F", "#AD1457", "#4E342E"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Subject") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                name, colorHex, priority,
                                totalPages.toIntOrNull() ?: 0,
                                readingSpeed.toFloatOrNull() ?: 2f,
                                videoMinutes.toIntOrNull() ?: 0
                            )
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Subject Name *") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(12.dp))

            Text("Priority", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p },
                        label = { Text(p) })
                }
            }

            Text("Color", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { hex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                        .getOrDefault(Color.Blue)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (colorHex == hex) 3.dp else 0.dp,
                                MaterialTheme.colorScheme.onSurface, CircleShape
                            )
                            .clickable { colorHex = hex }
                    )
                }
            }

            HorizontalDivider()
            Text("Study Material", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)

            OutlinedTextField(value = totalPages, onValueChange = { totalPages = it },
                label = { Text("Total Pages") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))

            OutlinedTextField(value = readingSpeed, onValueChange = { readingSpeed = it },
                label = { Text("Reading Speed (min/page)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))

            OutlinedTextField(value = videoMinutes, onValueChange = { videoMinutes = it },
                label = { Text("Video Duration (minutes)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))

            val est = (totalPages.toIntOrNull() ?: 0) *
                    (readingSpeed.toFloatOrNull() ?: 2f) +
                    (videoMinutes.toIntOrNull() ?: 0)
            if (est > 0) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("Estimated total: ${est.toInt() / 60}h ${est.toInt() % 60}m",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
