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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import net.sourceforge.pinyin4j.PinyinHelper
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import com.gesture.explode.ui.theme.GestureExplodeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// --- 【编译计数器：请手动在此处修改数字】 ---
const val BUILD_VERSION = 1

enum class SearchItemType { APP, CONTACT, SETTINGS }

data class SearchItem(
    val title: String,
    val subtitle: String,
    val pinyinInitials: String,
    val launchData: String,
    val type: SearchItemType,
    val contactId: String? = null // 增加 ID 以提高联系人跳转成功率
)

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
        } catch (e: Exception) { sb.append(c) }
    }
    return sb.toString().lowercase(Locale.getDefault())
}

fun getInstalledApps(context: Context): List<SearchItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

    return resolveInfos.map {
        val appName = it.loadLabel(pm).toString()
        val packageName = it.activityInfo.packageName
        val type = if (packageName == "com.android.settings" || packageName.contains("settings", ignoreCase = true)) {
            SearchItemType.SETTINGS
        } else {
            SearchItemType.APP
        }
        SearchItem(appName, if (type == SearchItemType.SETTINGS) "System" else "Application", getPinyinInitials(appName), packageName, type)
    }
}

fun getContacts(context: Context): List<SearchItem> {
    val contacts = mutableListOf<SearchItem>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return contacts
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID // 获取 ID
    )
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex) ?: ""
            val number = cursor.getString(numberIndex) ?: ""
            val id = cursor.getString(idIndex) ?: ""
            if (name.isNotEmpty()) {
                contacts.add(SearchItem(name, number, getPinyinInitials(name), number, SearchItemType.CONTACT, id))
            }
        }
    }
    return contacts.distinctBy { it.title }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(100, 100)
                imageBitmap = bitmap.asImageBitmap()
            } catch (e: Exception) { }
        }
    }
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap!!, contentDescription = null, modifier = modifier)
    } else {
        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray, modifier = modifier)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestureExplodeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    GestureSearchApp()
                }
            }
        }
    }
}

@Composable
fun GestureSearchApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }

    var hasContactPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasContactPermission = isGranted
    }
    LaunchedEffect(Unit) {
        if (!hasContactPermission) permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    val allApps = remember { getInstalledApps(context) }
    val allContacts = remember(hasContactPermission) { if (hasContactPermission) getContacts(context) else emptyList() }
    val allItems = remember(allApps, allContacts) { (allApps + allContacts).sortedBy { it.pinyinInitials } }
    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isEmpty()) allItems else {
            val queryLower = searchQuery.lowercase(Locale.getDefault())
            allItems.filter { it.title.lowercase(Locale.getDefault()).contains(queryLower) || it.pinyinInitials.contains(queryLower) }
        }
    }

    val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")!!
    val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
    val recognizer = remember { DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build()) }

    LaunchedEffect(Unit) {
        RemoteModelManager.getInstance().download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { isReady = true }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.statusBarsPadding())

        // --- 1. 标题栏 (包含计数器) ---
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF000000)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.logobeta), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            // 这里显示计数器
            Text("Gesture Explode $BUILD_VERSION", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        HorizontalDivider(color = Color(0xFF00BCD4), thickness = 2.dp)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val paths = remember { mutableStateListOf<Path>() }
            val currentPath = remember { mutableStateListOf<Offset>() }
            var inkBuilder = remember { Ink.builder() }
            var strokeBuilder: Ink.Stroke.Builder? = null
            var recognizeJob by remember { mutableStateOf<Job?>(null) }

            // --- 2. 列表与跳转逻辑修正 ---
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredItems) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                try {
                                    when (item.type) {
                                        SearchItemType.APP -> {
                                            val intent = context.packageManager.getLaunchIntentForPackage(item.launchData)
                                            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            intent?.let { context.startActivity(it) }
                                        }
                                        SearchItemType.SETTINGS -> {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                        SearchItemType.CONTACT -> {
                                            // 使用更稳健的联系人详情跳转方式
                                            val intent = Intent(Intent.ACTION_VIEW)
                                            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, item.contactId)
                                            intent.data = uri
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            when (item.type) {
                                SearchItemType.CONTACT -> Icon(Icons.Default.Phone, null, tint = Color(0xFF8BC34A), modifier = Modifier.size(28.dp))
                                SearchItemType.SETTINGS -> Icon(Icons.Default.Settings, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(28.dp))
                                SearchItemType.APP -> AppIcon(item.launchData, Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(item.title, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.W500)
                            Text(item.subtitle, fontSize = 14.sp, color = Color(0xFFAAAAAA))
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                }
            }

            // 画板交互逻辑 (保持不变)
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(isReady) {
                if (!isReady) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        recognizeJob?.cancel()
                        currentPath.clear(); currentPath.add(offset)
                        strokeBuilder = Ink.Stroke.builder().apply { addPoint(Ink.Point.create(offset.x, offset.y, System.currentTimeMillis())) }
                    },
                    onDrag = { change, _ ->
                        currentPath.add(change.position)
                        strokeBuilder?.addPoint(Ink.Point.create(change.position.x, change.position.y, System.currentTimeMillis()))
                    },
                    onDragEnd = {
                        val path = Path()
                        if (currentPath.isNotEmpty()) {
                            path.moveTo(currentPath.first().x, currentPath.first().y)
                            for (i in 1 until currentPath.size) path.lineTo(currentPath[i].x, currentPath[i].y)
                            paths.add(path)
                        }
                        strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                        if (currentPath.isNotEmpty()) {
                            val dx = currentPath.last().x - currentPath.first().x
                            val dy = currentPath.maxOf { it.y } - currentPath.minOf { it.y }
                            if (dx < -150f && dy < 150f) {
                                if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
                                paths.clear(); inkBuilder = Ink.builder(); currentPath.clear()
                                return@detectDragGestures
                            }
                        }
                        currentPath.clear()
                        recognizeJob = coroutineScope.launch {
                            delay(400)
                            recognizer.recognize(inkBuilder.build()).addOnSuccessListener { result ->
                                if (result.candidates.isNotEmpty()) searchQuery += result.candidates[0].text.substring(0, 1).lowercase()
                                paths.clear(); inkBuilder = Ink.builder()
                            }
                        }
                    }
                )
            }) {
                for (path in paths) drawPath(path, Color(0xFFFFEB3B), style = Stroke(15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (currentPath.isNotEmpty()) {
                    val p = Path().apply {
                        moveTo(currentPath.first().x, currentPath.first().y)
                        for (i in 1 until currentPath.size) lineTo(currentPath[i].x, currentPath[i].y)
                    }
                    drawPath(p, Color(0xFFFFEB3B), style = Stroke(15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }

        // 底部 MD3 悬浮框 (保持不变)
        val gradientBrush = Brush.horizontalGradient(listOf(Color(0xFF141E30), Color(0xFF35577D)))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).navigationBarsPadding()
                .shadow(12.dp, RoundedCornerShape(50)).background(gradientBrush).height(64.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(searchQuery, fontSize = 28.sp, color = Color(0xFFFFEB3B), fontWeight = FontWeight.Medium)
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(28.dp)) }
            }
        }
    }
}