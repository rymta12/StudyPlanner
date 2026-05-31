package com.studyplanner.app.feature.ocr


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.studyplanner.app.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OcrScanViewModel @Inject constructor() : ViewModel() {

    data class UiState(
        val isProcessing: Boolean = false,
        val lines: List<String> = emptyList(),
        val selected: Set<Int> = emptySet(),
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun process(image: InputImage) {
        _state.update { it.copy(isProcessing = true, error = null) }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val lines = result.textBlocks
                    .flatMap { it.lines }
                    .map { it.text.trim() }
                    .filter { it.length in 2..80 }
                    .distinct()
                _state.update {
                    it.copy(isProcessing = false, lines = lines,
                        selected = lines.indices.toSet())
                }
            }
            .addOnFailureListener { e ->
                _state.update { it.copy(isProcessing = false, error = e.localizedMessage) }
            }
    }

    fun toggle(i: Int) = _state.update {
        it.copy(selected = if (i in it.selected) it.selected - i else it.selected + i)
    }

    fun selectedTopics(): List<String> {
        val s = _state.value
        return s.lines.filterIndexed { i, _ -> i in s.selected }
    }

    fun rescan() = _state.update { UiState() }
}