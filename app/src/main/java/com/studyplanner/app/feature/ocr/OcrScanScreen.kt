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

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScanScreen(
    onConfirm: (List<String>) -> Unit,
    onBack: () -> Unit,
    viewModel: OcrScanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Index Scan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.lines.isEmpty()) {
                Text(
                    "Book ke index/contents page ko camera me dikhao, fir Scan dabao.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasCamPermission) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        CameraPreview(imageCapture, lifecycleOwner, Modifier.fillMaxSize())
                        if (state.isProcessing) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    PrimaryButton(
                        text = "📷 Scan Index Page",
                        onClick = { captureAndRecognize(context, imageCapture, viewModel) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isProcessing,
                        leadingIcon = { Icon(Icons.Default.CameraAlt, null) }
                    )
                } else {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Camera permission chahiye scan ke liye")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Allow Camera")
                            }
                        }
                    }
                }
                state.error?.let {
                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "${state.selected.size} topics selected — jo nahi chahiye unhe untick karo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(state.lines) { i, line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = i in state.selected,
                                onCheckedChange = { viewModel.toggle(i) }
                            )
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.rescan() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Rescan") }
                    PrimaryButton(
                        text = "Add ${state.selected.size} Topics",
                        onClick = { onConfirm(viewModel.selectedTopics()) },
                        modifier = Modifier.weight(1f),
                        enabled = state.selected.isNotEmpty()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun captureAndRecognize(
    context: Context,
    imageCapture: ImageCapture,
    viewModel: OcrScanViewModel
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(proxy: ImageProxy) {
                val mediaImage = proxy.image
                if (mediaImage != null) {
                    val img = InputImage.fromMediaImage(
                        mediaImage, proxy.imageInfo.rotationDegrees
                    )
                    viewModel.process(img)
                }
                proxy.close()
            }

            override fun onError(exception: ImageCaptureException) {
                // capture fail — user dobara try karega
            }
        }
    )
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
