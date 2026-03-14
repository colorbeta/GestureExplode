package com.gesture.explode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import com.gesture.explode.ui.theme.GestureExplodeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale
import kotlin.math.abs

// --- 【版本号更新为 37】 ---
const val BUILD_VERSION = 37

enum class SearchItemType { APP, CONTACT, SETTINGS }
data class SearchItem(val title: String, val subtitle: String, val pinyinInitials: String, val launchData: String, val type: SearchItemType, val contactId: String? = null)

// 数据处理逻辑
fun getPinyinInitials(text: String): String {
    val sb = StringBuilder()
    for (c in text) {
        try {
            val pinyinArray: Array<String>? = PinyinHelper.toHanyuPinyinStringArray(c)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) sb.append(pinyinArray[0][0]) else sb.append(c)
        } catch (e: Exception) { sb.append(c) }
    }
    return sb.toString().lowercase(Locale.getDefault())
}

@Composable
fun HighlightedText(text: String, query: String, pinyinInitials: String) {
    val annotatedString = remember(text, query) {
        buildAnnotatedString {
            if (query.isEmpty()) append(text) else {
                val lowerT = text.lowercase(Locale.getDefault()); val lowerQ = query.lowercase(Locale.getDefault())
                val directIdx = lowerT.indexOf(lowerQ); val pinyinIdx = pinyinInitials.indexOf(lowerQ)
                val highlightStyle = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)
                when {
                    directIdx != -1 -> {
                        append(text.substring(0, directIdx))
                        withStyle(highlightStyle) { append(text.substring(directIdx, directIdx + query.length)) }
                        append(text.substring(directIdx + query.length))
                    }
                    pinyinIdx != -1 && pinyinIdx + query.length <= text.length -> {
                        append(text.substring(0, pinyinIdx))
                        withStyle(highlightStyle) { append(text.substring(pinyinIdx, pinyinIdx + query.length)) }
                        append(text.substring(pinyinIdx + query.length))
                    }
                    else -> append(text)
                }
            }
        }
    }
    Text(text = annotatedString, fontSize = 17.sp, color = if (query.isEmpty()) Color.White else Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Medium)
}

fun getInstalledApps(context: Context): List<SearchItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolveInfos.map {
        val name = it.loadLabel(pm).toString()
        val pkg = it.activityInfo.packageName
        val type = if (pkg == "com.android.settings" || pkg.contains("settings", true)) SearchItemType.SETTINGS else SearchItemType.APP
        SearchItem(name, "Application", getPinyinInitials(name), pkg, type)
    }
}

fun getContacts(context: Context): List<SearchItem> {
    val contacts = mutableListOf<SearchItem>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return contacts
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val p = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
    context.contentResolver.query(uri, p, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.getString(0) ?: ""; val num = cursor.getString(1) ?: ""; val id = cursor.getString(2) ?: ""
            if (name.isNotEmpty()) contacts.add(SearchItem(name, num, getPinyinInitials(name), num, SearchItemType.CONTACT, id))
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
                imageBitmap = drawable.toBitmap(120, 120).asImageBitmap()
            } catch (e: Exception) { }
        }
    }
    if (imageBitmap != null) Image(bitmap = imageBitmap!!, contentDescription = null, modifier = modifier)
    else Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = modifier)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSearchApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE) }

    var searchQuery by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var searchAppsEnabled by remember { mutableStateOf(prefs.getBoolean("search_apps", true)) }
    var searchContactsEnabled by remember { mutableStateOf(prefs.getBoolean("search_contacts", true)) }
    var searchSettingsEnabled by remember { mutableStateOf(prefs.getBoolean("search_settings", true)) }
    var writingSpeedDelay by remember { mutableStateOf(prefs.getLong("writing_delay", 500L)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) searchQuery = "" }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val density = LocalDensity.current
    val itemHeightPx = with(density) { 96.dp.toPx() }

    var hasContactPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasContactPermission = it }
    LaunchedEffect(Unit) { if (!hasContactPermission) permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }

    val allApps = remember { getInstalledApps(context) }
    val allContacts = remember(hasContactPermission) { if (hasContactPermission) getContacts(context) else emptyList() }
    val allItems = remember(allApps, allContacts, searchAppsEnabled, searchContactsEnabled, searchSettingsEnabled) {
        val list = mutableListOf<SearchItem>()
        if (searchAppsEnabled) list.addAll(allApps.filter { it.type == SearchItemType.APP })
        if (searchSettingsEnabled) list.addAll(allApps.filter { it.type == SearchItemType.SETTINGS })
        if (searchContactsEnabled) list.addAll(allContacts)
        list.sortedBy { it.pinyinInitials }
    }

    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isEmpty()) allItems else {
            val q = searchQuery.lowercase(Locale.getDefault()); allItems.filter { it.title.lowercase(Locale.getDefault()).contains(q) || it.pinyinInitials.contains(q) }
        }
    }

    val recognizer = remember {
        val m = DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")!!).build()
        DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(m).build())
    }

    LaunchedEffect(Unit) {
        val m = DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")!!).build()
        RemoteModelManager.getInstance().download(m, DownloadConditions.Builder().build()).addOnSuccessListener { isReady = true }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        Scaffold(
            containerColor = Color(0xFF121212),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("设置", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = { IconButton(onClick = { showSettings = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF121212))
                )
            }
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
                item {
                    Text(text = "搜索范围", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00BCD4), modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(24.dp)) {
                        Column {
                            SettingM3Row("联系人", Icons.Default.Phone, searchContactsEnabled) { searchContactsEnabled = it; prefs.edit().putBoolean("search_contacts", it).apply() }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                            SettingM3Row("应用软件", Icons.Default.Face, searchAppsEnabled) { searchAppsEnabled = it; prefs.edit().putBoolean("search_apps", it).apply() }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                            SettingM3Row("系统设置", Icons.Default.Settings, searchSettingsEnabled) { searchSettingsEnabled = it; prefs.edit().putBoolean("search_settings", it).apply() }
                        }
                    }
                }
                item {
                    Text(text = "写入速度", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00BCD4), modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.selectableGroup()) {
                            val speedOptions = listOf("快 (300ms)" to 300L, "中等 (500ms)" to 500L, "慢 (800ms)" to 800L)
                            speedOptions.forEach { option ->
                                Row(Modifier.fillMaxWidth().height(56.dp).selectable(selected = (writingSpeedDelay == option.second), onClick = { writingSpeedDelay = option.second; prefs.edit().putLong("writing_delay", option.second).apply() }, role = Role.RadioButton).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = (writingSpeedDelay == option.second), onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00BCD4)))
                                    Text(text = option.first, color = Color.White, modifier = Modifier.padding(start = 16.dp))
                                }
                                if (option != speedOptions.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logobeta), null, Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp)); Text("Gesture Explode", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.weight(1f)); IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings", tint = if (isReady) Color.White else Color.Gray) }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val paths = remember { mutableStateListOf<Path>() }
                val currentPath = remember { mutableStateListOf<Offset>() }
                var inkBuilder by remember { mutableStateOf(Ink.builder()) }
                var strokeBuilder: Ink.Stroke.Builder? by remember { mutableStateOf(null) }
                var recognizeJob by remember { mutableStateOf<Job?>(null) }

                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp)) {
                    itemsIndexed(filteredItems) { _, item ->
                        // --- 【显示框圆角加大至 28.dp】 ---
                        Card(
                            modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp), Alignment.Center) {
                                    when (item.type) {
                                        SearchItemType.CONTACT -> Icon(Icons.Default.Phone, null, tint = Color(0xFF8BC34A), modifier = Modifier.size(26.dp))
                                        SearchItemType.SETTINGS -> Icon(Icons.Default.Settings, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(26.dp))
                                        SearchItemType.APP -> AppIcon(item.launchData, Modifier.size(40.dp))
                                    }
                                }
                                Spacer(Modifier.width(16.dp)); Column { HighlightedText(item.title, searchQuery, item.pinyinInitials); Text(item.subtitle, fontSize = 12.sp, color = Color.Gray) }
                            }
                        }
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()
                    .pointerInput(isReady, filteredItems) { detectTapGestures { offset ->
                        val index = (offset.y / itemHeightPx).toInt()
                        if (index >= 0 && index < filteredItems.size) {
                            val item = filteredItems[index]
                            try {
                                val intent = when (item.type) {
                                    SearchItemType.APP -> context.packageManager.getLaunchIntentForPackage(item.launchData)
                                    SearchItemType.SETTINGS -> Intent(android.provider.Settings.ACTION_SETTINGS)
                                    SearchItemType.CONTACT -> Intent(Intent.ACTION_VIEW).apply { data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, item.contactId) }
                                }
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); intent?.let { context.startActivity(it); searchQuery = "" }
                            } catch (e: Exception) { }
                        }
                    } }
                    .pointerInput(isReady) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                recognizeJob?.cancel(); currentPath.clear();
                                strokeBuilder = Ink.Stroke.builder().apply { addPoint(Ink.Point.create(offset.x, offset.y, System.currentTimeMillis())) }
                            },
                            onDrag = { change, _ ->
                                currentPath.add(change.position)
                                strokeBuilder?.addPoint(Ink.Point.create(change.position.x, change.position.y, System.currentTimeMillis()))
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    val p = Path().apply { moveTo(currentPath.first().x, currentPath.first().y); for (i in 1 until currentPath.size) lineTo(currentPath[i].x, currentPath[i].y) }
                                    paths.add(p); strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                                }
                                // 方向拦截判定
                                if (currentPath.isNotEmpty()) {
                                    val dx = currentPath.last().x - currentPath.first().x
                                    val dy = currentPath.last().y - currentPath.first().y
                                    if (dx < -80f && abs(dx) > abs(dy)) {
                                        if (dx < -250f && abs(dx) > abs(dy) * 2f) { if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1) }
                                        paths.clear(); inkBuilder = Ink.builder(); currentPath.clear()
                                        recognizeJob?.cancel(); return@detectDragGestures
                                    }
                                }
                                currentPath.clear()
                                recognizeJob = coroutineScope.launch {
                                    delay(writingSpeedDelay)
                                    val finalInk = inkBuilder.build(); inkBuilder = Ink.builder(); paths.clear()
                                    if (finalInk.strokes.isNotEmpty()) {
                                        recognizer.recognize(finalInk).addOnSuccessListener { result ->
                                            for (c in result.candidates.take(10)) {
                                                val t = c.text.trim(); if (t.isNotEmpty() && t[0].isLetter()) { searchQuery += t[0].toString().lowercase(); break }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                ) {
                    for (path in paths) drawPath(path, Color(0xFFFFEB3B), style = Stroke(15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    if (currentPath.isNotEmpty()) {
                        val p = Path().apply { moveTo(currentPath.first().x, currentPath.first().y); for (i in 1 until currentPath.size) lineTo(currentPath[i].x, currentPath[i].y) }
                        drawPath(p, Color(0xFFFFEB3B), style = Stroke(15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }

            // --- 【搜索框修改：左右半圆形 Pixel 风格】 ---
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).navigationBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        // 【修改点】：使用 CircleShape 实现左右半圆（药丸状）
                        .background(Color(0xFF222222), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "画一个字母开始搜索" else searchQuery,
                        fontSize = 18.sp,
                        color = if (searchQuery.isEmpty()) Color.Gray else Color(0xFFFFEB3B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEB3B))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingM3Row(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = Color.White, fontWeight = FontWeight.Medium) },
        leadingContent = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BCD4), checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.3f))) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}