package com.gesture.explode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.sourceforge.pinyin4j.PinyinHelper
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import com.gesture.explode.ui.theme.GestureExplodeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. 统一的数据结构：既能存 App，也能存联系人
data class SearchItem(
    val title: String,
    val subtitle: String,
    val pinyinInitials: String,
    val isApp: Boolean, // true代表是App，false代表是联系人
    val launchData: String // 如果是App存包名，如果是联系人存电话号码
)

// 2. 拼音转换引擎 (Pinyin4j)
fun getPinyinInitials(text: String): String {
    val sb = StringBuilder()
    for (c in text) {
        try {
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                sb.append(pinyinArray[0][0])
            } else {
                sb.append(c)
            }
        } catch (e: Exception) {
            sb.append(c)
        }
    }
    return sb.toString().lowercase()
}

// 3. 读取手机应用引擎
fun getInstalledApps(context: Context): List<SearchItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolveInfos.map {
        val appName = it.loadLabel(pm).toString()
        SearchItem(
            title = appName,
            subtitle = it.activityInfo.packageName,
            pinyinInitials = getPinyinInitials(appName),
            isApp = true,
            launchData = it.activityInfo.packageName
        )
    }
}

// 4. 读取通讯录引擎
fun getContacts(context: Context): List<SearchItem> {
    val contacts = mutableListOf<SearchItem>()
    // 再次确认是否拥有权限，没有则直接返回空列表
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return contacts
    }
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex) ?: ""
            val number = cursor.getString(numberIndex) ?: ""
            if (name.isNotEmpty()) {
                contacts.add(
                    SearchItem(
                        title = name,
                        subtitle = number,
                        pinyinInitials = getPinyinInitials(name),
                        isApp = false,
                        launchData = number
                    )
                )
            }
        }
    }
    // 去重并返回
    return contacts.distinctBy { it.title }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestureExplodeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GestureSearchApp()
                }
            }
        }
    }
}

@Composable
fun GestureSearchApp() {
    val context = LocalContext.current
    // ↓↓↓ 就是加上下面这一行！聘请后台任务管家 ↓↓↓
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("正在准备 AI...") }

    // --- 权限申请逻辑 ---
    var hasContactPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasContactPermission = isGranted
    }
    LaunchedEffect(Unit) {
        if (!hasContactPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS) // 启动时弹窗请求权限
        }
    }

    // --- 获取数据流 ---
    val allApps = remember { getInstalledApps(context) }
    // 只要权限被同意，立刻去读取通讯录
    val allContacts = remember(hasContactPermission) {
        if (hasContactPermission) getContacts(context) else emptyList()
    }
    // 将 App 和 联系人 混合在一起，并按拼音排序
    val allItems = remember(allApps, allContacts) {
        (allApps + allContacts).sortedBy { it.pinyinInitials }
    }

    // --- 实时过滤逻辑 ---
    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isEmpty()) allItems else {
            val queryLower = searchQuery.lowercase()
            allItems.filter {
                it.title.lowercase().contains(queryLower) ||
                        it.pinyinInitials.contains(queryLower)
            }
        }
    }

    val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")!!
    val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
    val recognizer = remember { DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build()) }

    LaunchedEffect(Unit) {
        RemoteModelManager.getInstance().download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                isReady = true
                statusText = "写字母搜应用/联系人"
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isNotEmpty()) "搜索: $searchQuery" else statusText,
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
            )
            if (searchQuery.isNotEmpty()) {
                Button(onClick = { searchQuery = "" }) { Text("退格") }
            }
        }

        Box(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
            val paths = remember { mutableStateListOf<Path>() }
            val currentPath = remember { mutableStateListOf<Offset>() }
            var inkBuilder = remember { Ink.builder() }
            var strokeBuilder: Ink.Stroke.Builder? = null
            var recognizeJob by remember { mutableStateOf<Job?>(null) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isReady) {
                        if (!isReady) return@pointerInput

                        detectDragGestures(
                            onDragStart = { offset ->
                                recognizeJob?.cancel()
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
                                    for (i in 1 until currentPath.size) { path.lineTo(currentPath[i].x, currentPath[i].y) }
                                    paths.add(path)
                                }
                                strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                                currentPath.clear()

                                recognizeJob = coroutineScope.launch {
                                    delay(400)
                                    val ink = inkBuilder.build()
                                    recognizer.recognize(ink).addOnSuccessListener { result ->
                                        if (result.candidates.isNotEmpty()) {
                                            val recognizedChar = result.candidates[0].text
                                            if (recognizedChar.isNotEmpty()) {
                                                searchQuery += recognizedChar.substring(0, 1).lowercase()
                                            }
                                        }
                                        paths.clear()
                                        inkBuilder = Ink.builder()
                                    }
                                }
                            }
                        )
                    }
            ) {
                drawRect(color = Color.LightGray.copy(alpha = 0.1f))
                val brushColor = Color(0xFF007AFF)
                val strokeWidth = 20f

                for (path in paths) {
                    drawPath(path = path, color = brushColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                val currentTempPath = Path()
                if (currentPath.isNotEmpty()) {
                    currentTempPath.moveTo(currentPath.first().x, currentPath.first().y)
                    for (i in 1 until currentPath.size) { currentTempPath.lineTo(currentPath[i].x, currentPath[i].y) }
                    drawPath(path = currentTempPath, color = brushColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            items(filteredItems) { item ->
                // 用 Emoji 区分一下外观
                val icon = if (item.isApp) "📱" else "👤"

                ListItem(
                    headlineContent = { Text("$icon ${item.title}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(item.subtitle) },
                    modifier = Modifier.clickable {
                        // 【核心改动】点击执行逻辑
                        try {
                            if (item.isApp) {
                                // 启动 App
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(item.launchData)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            } else {
                                // 拨打联系人电话 (跳转到拨号盘)
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.launchData}"))
                                context.startActivity(dialIntent)
                            }
                        } catch (e: Exception) {
                            // 忽略找不到页面的极小概率崩溃
                        }
                    }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        }
    }
}