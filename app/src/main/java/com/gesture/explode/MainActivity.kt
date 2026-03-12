package com.gesture.explode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================================================
// 就是下面这 3 行！刚才网络断开时，它们极有可能被软件自动抹除了
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
// =========================================================

import com.gesture.explode.ui.theme.GestureExplodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestureExplodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GestureBoardWithAI()
                }
            }
        }
    }
}

@Composable
fun GestureBoardWithAI() {
    var recognizedText by remember { mutableStateOf("正在准备 AI 引擎...") }
    var isReady by remember { mutableStateOf(false) }

    // 初始化 AI 引擎 (英文模型)
    val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")!!
    val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
    val recognizer = remember { DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build()) }

    // 后台下载模型
    LaunchedEffect(Unit) {
        val remoteModelManager = RemoteModelManager.getInstance()
        val conditions = DownloadConditions.Builder().build()
        remoteModelManager.download(model, conditions)
            .addOnSuccessListener {
                isReady = true
                recognizedText = "准备就绪！请在屏幕上写字母"
            }
            .addOnFailureListener {
                recognizedText = "模型下载失败，请检查网络"
            }
    }

    val paths = remember { mutableStateListOf<Path>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    var inkBuilder = remember { Ink.builder() }
    var strokeBuilder: Ink.Stroke.Builder? = null

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Text(
            text = recognizedText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(24.dp)
                .align(Alignment.CenterHorizontally)
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isReady) {
                    if (!isReady) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath.clear()
                            currentPath.add(offset)
                            strokeBuilder = Ink.Stroke.builder()
                            strokeBuilder?.addPoint(Ink.Point.create(offset.x, offset.y, System.currentTimeMillis()))
                        },
                        onDrag = { change, _ ->
                            currentPath.add(change.position)
                            strokeBuilder?.addPoint(Ink.Point.create(change.position.x, change.position.y, System.currentTimeMillis()))
                        },
                        onDragEnd = {
                            val path = Path()
                            if (currentPath.isNotEmpty()) {
                                path.moveTo(currentPath.first().x, currentPath.first().y)
                                for (i in 1 until currentPath.size) {
                                    path.lineTo(currentPath[i].x, currentPath[i].y)
                                }
                                paths.add(path)
                            }

                            strokeBuilder?.let {
                                inkBuilder.addStroke(it.build())
                                val ink = inkBuilder.build()
                                recognizer.recognize(ink)
                                    .addOnSuccessListener { result ->
                                        if (result.candidates.isNotEmpty()) {
                                            recognizedText = result.candidates[0].text
                                        }
                                    }
                                    .addOnFailureListener {
                                        recognizedText = "识别失败"
                                    }
                            }
                            currentPath.clear()
                        }
                    )
                }
        ) {
            val brushColor = Color(0xFF007AFF)
            val strokeWidth = 20f

            for (path in paths) {
                drawPath(path = path, color = brushColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val currentTempPath = Path()
            if (currentPath.isNotEmpty()) {
                currentTempPath.moveTo(currentPath.first().x, currentPath.first().y)
                for (i in 1 until currentPath.size) {
                    currentTempPath.lineTo(currentPath[i].x, currentPath[i].y)
                }
                drawPath(path = currentTempPath, color = brushColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}