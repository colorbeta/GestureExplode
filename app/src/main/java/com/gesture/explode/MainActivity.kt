package com.gesture.explode

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.gesture.explode.ui.theme.GestureExplodeTheme
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale
import kotlin.math.abs

// --- 【升级为 88：引入中英文双语动态支持】 ---
const val BUILD_VERSION = 88

enum class SearchItemType { APP, CONTACT, SETTINGS, SYSTEM_ACTION }

data class SearchItem(
    val title: String,
    val subtitle: String,
    val initials: String,
    val initialIndices: List<Int>,
    val launchData: String,
    val type: SearchItemType,
    val contactId: String? = null
)

fun getSearchInitialsInfo(text: String): Pair<String, List<Int>> {
    val initials = StringBuilder()
    val indices = mutableListOf<Int>()
    var isNewWord = true
    for (i in text.indices) {
        val c = text[i]
        val pinyinArray: Array<String>? = PinyinHelper.toHanyuPinyinStringArray(c)
        if (!pinyinArray.isNullOrEmpty()) {
            initials.append(pinyinArray[0][0]); indices.add(i); isNewWord = false
        } else if (c.isLetterOrDigit()) {
            if (isNewWord) { initials.append(c.lowercaseChar()); indices.add(i); isNewWord = false }
        } else isNewWord = true
    }
    return Pair(initials.toString(), indices)
}

@Composable
fun HighlightedText(text: String, query: String, initials: String, initialIndices: List<Int>) {
    val annotatedString = remember(text, query, initials, initialIndices) {
        buildAnnotatedString {
            val lowerT = text.lowercase(Locale.getDefault())
            val lowerQ = query.lowercase(Locale.getDefault())
            val lowerQFallback = lowerQ.replace('z', '2').replace('l', '1')

            val matchStyle = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)
            val dimStyle = SpanStyle(color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Normal)

            if (query.isEmpty()) { withStyle(matchStyle) { append(text) }; return@buildAnnotatedString }

            var directIdx = lowerT.indexOf(lowerQ)
            var matchedQ = lowerQ
            if (directIdx == -1 && lowerQ != lowerQFallback) {
                directIdx = lowerT.indexOf(lowerQFallback)
                matchedQ = lowerQFallback
            }

            var acronymIdx = initials.indexOf(lowerQ)
            var matchedAcronymQ = lowerQ
            if (acronymIdx == -1 && lowerQ != lowerQFallback) {
                acronymIdx = initials.indexOf(lowerQFallback)
                matchedAcronymQ = lowerQFallback
            }

            when {
                directIdx != -1 -> {
                    withStyle(dimStyle) { append(text.substring(0, directIdx)) }
                    withStyle(matchStyle) { append(text.substring(directIdx, directIdx + matchedQ.length)) }
                    withStyle(dimStyle) { append(text.substring(directIdx + matchedQ.length)) }
                }
                acronymIdx != -1 -> {
                    val matchedIndicesSet = mutableSetOf<Int>()
                    for (i in matchedAcronymQ.indices) { matchedIndicesSet.add(initialIndices[acronymIdx + i]) }
                    for (i in text.indices) {
                        if (i in matchedIndicesSet) withStyle(matchStyle) { append(text[i]) }
                        else withStyle(dimStyle) { append(text[i]) }
                    }
                }
                else -> { withStyle(dimStyle) { append(text) } }
            }
        }
    }
    Text(text = annotatedString, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White)
}

@Composable
fun HighlightedSubtitle(text: String, query: String) {
    val annotatedString = remember(text, query) {
        buildAnnotatedString {
            val lowerT = text.lowercase(Locale.getDefault())
            val lowerQ = query.lowercase(Locale.getDefault())
            val lowerQFallback = lowerQ.replace('z', '2').replace('l', '1')

            val matchStyle = SpanStyle(color = Color(0xFFFFEB3B), fontWeight = FontWeight.Bold)
            val dimStyle = SpanStyle(color = Color.Gray, fontWeight = FontWeight.Normal)

            if (query.isEmpty()) { withStyle(dimStyle) { append(text) }; return@buildAnnotatedString }

            var directIdx = lowerT.indexOf(lowerQ)
            var matchedQ = lowerQ
            if (directIdx == -1 && lowerQ != lowerQFallback) {
                directIdx = lowerT.indexOf(lowerQFallback)
                matchedQ = lowerQFallback
            }

            if (directIdx != -1) {
                withStyle(dimStyle) { append(text.substring(0, directIdx)) }
                withStyle(matchStyle) { append(text.substring(directIdx, directIdx + matchedQ.length)) }
                withStyle(dimStyle) { append(text.substring(directIdx + matchedQ.length)) }
            } else {
                withStyle(dimStyle) { append(text) }
            }
        }
    }
    Text(text = annotatedString, fontSize = 12.sp)
}

// 核心升级：系统设置名称全面实现双语映射
@SuppressLint("InlinedApi")
fun getSystemSettingsItems(isChinese: Boolean): List<SearchItem> {
    val settings = listOf(
        (if(isChinese) "WLAN / 无线网络" else "Wi-Fi") to Settings.ACTION_WIFI_SETTINGS,
        (if(isChinese) "蓝牙设置" else "Bluetooth") to Settings.ACTION_BLUETOOTH_SETTINGS,
        (if(isChinese) "移动网络 / 蜂窝" else "Mobile Network") to Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
        (if(isChinese) "数据使用 / 流量管理" else "Data Usage") to Settings.ACTION_DATA_USAGE_SETTINGS,
        (if(isChinese) "个人热点 / 共享" else "Hotspot & Tethering") to Settings.ACTION_WIRELESS_SETTINGS,
        (if(isChinese) "NFC / 触碰付款" else "NFC") to Settings.ACTION_NFC_SETTINGS,
        (if(isChinese) "飞行模式" else "Airplane Mode") to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
        (if(isChinese) "投屏 / 多屏互动" else "Cast") to Settings.ACTION_CAST_SETTINGS,
        (if(isChinese) "VPN 设置" else "VPN") to Settings.ACTION_VPN_SETTINGS,
        (if(isChinese) "显示与亮度 / 屏幕" else "Display") to Settings.ACTION_DISPLAY_SETTINGS,
        (if(isChinese) "深色模式 / 护眼" else "Dark Mode") to Settings.ACTION_NIGHT_DISPLAY_SETTINGS,
        (if(isChinese) "声音与振动 / 音量" else "Sound & Vibration") to Settings.ACTION_SOUND_SETTINGS,
        (if(isChinese) "壁纸与个性化" else "Wallpaper") to Intent.ACTION_SET_WALLPAPER,
        (if(isChinese) "日期和时间" else "Date & Time") to Settings.ACTION_DATE_SETTINGS,
        (if(isChinese) "语言与地区" else "Language & Region") to Settings.ACTION_LOCALE_SETTINGS,
        (if(isChinese) "输入法与键盘" else "Keyboard & Input") to Settings.ACTION_INPUT_METHOD_SETTINGS,
        (if(isChinese) "位置信息 / 定位" else "Location") to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        (if(isChinese) "密码与安全 / 隐私" else "Security & Privacy") to Settings.ACTION_SECURITY_SETTINGS,
        (if(isChinese) "生物识别 / 指纹面部" else "Biometrics") to Settings.ACTION_BIOMETRIC_ENROLL,
        (if(isChinese) "锁屏与息屏" else "Lock Screen") to "android.settings.LOCK_SCREEN_SETTINGS",
        (if(isChinese) "应用管理 / 列表" else "Apps") to Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
        (if(isChinese) "默认应用设置" else "Default Apps") to Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
        (if(isChinese) "通知管理 / 状态栏" else "Notifications") to Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS,
        (if(isChinese) "悬浮窗权限" else "Display Over Other Apps") to Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        (if(isChinese) "未知来源安装" else "Install Unknown Apps") to Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        (if(isChinese) "画中画权限" else "Picture-in-Picture") to "android.settings.PICTURE_IN_PICTURE_SETTINGS",
        (if(isChinese) "后台运行 / 优化限制" else "Battery Optimization") to Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
        (if(isChinese) "存储空间 / 内存" else "Storage") to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        (if(isChinese) "账号与同步" else "Accounts & Sync") to Settings.ACTION_SYNC_SETTINGS,
        (if(isChinese) "关于手机 / 设备信息" else "About Phone") to Settings.ACTION_DEVICE_INFO_SETTINGS,
        (if(isChinese) "系统更新 / 软件升级" else "System Update") to "android.settings.SYSTEM_UPDATE_SETTINGS",
        (if(isChinese) "开发者选项" else "Developer Options") to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
        (if(isChinese) "电池与省电" else "Battery Saver") to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        (if(isChinese) "耗电详情 / 电量" else "Battery Usage") to Intent.ACTION_POWER_USAGE_SUMMARY,
        (if(isChinese) "无障碍 / 辅助功能" else "Accessibility") to Settings.ACTION_ACCESSIBILITY_SETTINGS,
        (if(isChinese) "搜索设置" else "Search Settings") to Settings.ACTION_SEARCH_SETTINGS,
        (if(isChinese) "勿扰模式" else "Do Not Disturb") to Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
        (if(isChinese) "默认桌面 / 主屏幕" else "Home Screen") to Settings.ACTION_HOME_SETTINGS,
        (if(isChinese) "单手操作模式" else "One-handed Mode") to "android.settings.action.ONE_HANDED_SETTINGS"
    )

    return settings.map { (name, action) ->
        val (initials, indices) = getSearchInitialsInfo(name)
        SearchItem(name, "", initials, indices, action, SearchItemType.SYSTEM_ACTION)
    }
}

fun getInstalledApps(context: Context): List<SearchItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolveInfos.map {
        val name = it.loadLabel(pm).toString()
        val pkg = it.activityInfo.packageName
        val type = if (pkg == "com.android.settings" || pkg.contains("settings", true)) SearchItemType.SETTINGS else SearchItemType.APP
        val (initials, indices) = getSearchInitialsInfo(name)
        SearchItem(name, "", initials, indices, pkg, type)
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
            if (name.isNotEmpty()) {
                val (initials, indices) = getSearchInitialsInfo(name)
                contacts.add(SearchItem(name, num, initials, indices, num, SearchItemType.CONTACT, id))
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
                imageBitmap = drawable.toBitmap(120, 120).asImageBitmap()
            } catch (_: Exception) { }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GestureSearchApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val prefs = remember { context.getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE) }

    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val paths = remember { mutableStateListOf<Path>() }
    val currentPath = remember { mutableStateListOf<Offset>() }
    var inkBuilder by remember { mutableStateOf(Ink.builder()) }
    var strokeBuilder: Ink.Stroke.Builder? by remember { mutableStateOf(null) }
    var recognizeJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(searchQuery) { listState.scrollToItem(0) }

    // --- 【新增：状态管理，语言控制】 ---
    var languagePref by remember { mutableIntStateOf(prefs.getInt("app_language", 0)) } // 0:系统, 1:中文, 2:英文
    val isChinese = remember(languagePref) {
        when (languagePref) {
            1 -> true
            2 -> false
            else -> Locale.getDefault().language == "zh"
        }
    }

    var searchAppsEnabled by remember { mutableStateOf(prefs.getBoolean("search_apps", true)) }
    var searchContactsEnabled by remember { mutableStateOf(prefs.getBoolean("search_contacts", true)) }
    var searchSettingsEnabled by remember { mutableStateOf(prefs.getBoolean("search_settings", true)) }
    var writingSpeedDelay by remember { mutableLongStateOf(prefs.getLong("writing_delay", 500L)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                searchQuery = ""
                paths.clear()
                currentPath.clear()
                inkBuilder = Ink.builder()
                recognizeJob?.cancel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val density = LocalDensity.current

    var hasContactPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasContactPermission = it }
    LaunchedEffect(Unit) { if (!hasContactPermission) permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }

    val allApps = remember { getInstalledApps(context) }
    val allContacts = remember(hasContactPermission) { if (hasContactPermission) getContacts(context) else emptyList() }

    // 系统设置列表现在会根据语言状态自动重载
    val allSystemSettings = remember(isChinese) { getSystemSettingsItems(isChinese) }

    val allItems = remember(allApps, allContacts, allSystemSettings, searchAppsEnabled, searchContactsEnabled, searchSettingsEnabled) {
        val list = mutableListOf<SearchItem>()
        if (searchAppsEnabled) list.addAll(allApps.filter { it.type == SearchItemType.APP })
        if (searchSettingsEnabled) {
            list.addAll(allApps.filter { it.type == SearchItemType.SETTINGS })
            list.addAll(allSystemSettings)
        }
        if (searchContactsEnabled) list.addAll(allContacts)
        list.sortedBy { it.title.lowercase() }
    }

    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isEmpty()) return@remember allItems
        val q = searchQuery.lowercase(Locale.getDefault())
        val qFallback = q.replace('z', '2').replace('l', '1')

        allItems.mapNotNull { item ->
            val t = item.title.lowercase(Locale.getDefault())
            val i = item.initials
            val sub = item.subtitle.lowercase(Locale.getDefault())

            val tIdxPrimary = t.indexOf(q)
            val iIdxPrimary = i.indexOf(q)

            val tIdxFallback = if (q != qFallback) t.indexOf(qFallback) else -1
            val iIdxFallback = if (q != qFallback) i.indexOf(qFallback) else -1

            val subIdxPrimary = if (item.type == SearchItemType.CONTACT) sub.indexOf(q) else -1
            val subIdxFallback = if (item.type == SearchItemType.CONTACT && q != qFallback) sub.indexOf(qFallback) else -1

            val isMatchPrimary = tIdxPrimary != -1 || iIdxPrimary != -1
            val isMatchFallback = tIdxFallback != -1 || iIdxFallback != -1
            val isMatchSubPrimary = subIdxPrimary != -1
            val isMatchSubFallback = subIdxFallback != -1

            if (isMatchPrimary || isMatchFallback || isMatchSubPrimary || isMatchSubFallback) {
                var baseScore = 0
                var effectiveIdx = -1
                var matchedLength = 1
                var targetLen = t.length

                if (isMatchPrimary) {
                    effectiveIdx = when {
                        iIdxPrimary != -1 && tIdxPrimary != -1 -> minOf(iIdxPrimary, tIdxPrimary)
                        iIdxPrimary != -1 -> iIdxPrimary
                        else -> tIdxPrimary
                    }
                    if (t == q || (i == q && t.length == q.length)) baseScore = 100000
                    else if (t.startsWith("$q ") || t.startsWith("$q(") || t.startsWith("$q-") || t.startsWith("${q}（")) baseScore = 90000
                    else if (effectiveIdx == 0) baseScore = 70000
                    else baseScore = 40000
                    matchedLength = q.length
                }
                else if (isMatchFallback) {
                    effectiveIdx = when {
                        iIdxFallback != -1 && tIdxFallback != -1 -> minOf(iIdxFallback, tIdxFallback)
                        iIdxFallback != -1 -> iIdxFallback
                        else -> tIdxFallback
                    }
                    if (t == qFallback || (i == qFallback && t.length == qFallback.length)) baseScore = 95000
                    else if (t.startsWith("$qFallback ") || t.startsWith("$qFallback(") || t.startsWith("$qFallback-") || t.startsWith("${qFallback}（")) baseScore = 85000
                    else if (effectiveIdx == 0) baseScore = 65000
                    else baseScore = 35000
                    matchedLength = qFallback.length
                }
                else if (isMatchSubPrimary || isMatchSubFallback) {
                    effectiveIdx = if (isMatchSubPrimary) subIdxPrimary else subIdxFallback
                    baseScore = if (isMatchSubPrimary) 20000 else 15000
                    matchedLength = if (isMatchSubPrimary) q.length else qFallback.length
                    targetLen = sub.length
                }

                val ratioBonus = ((matchedLength.toFloat() / targetLen.coerceAtLeast(1).toFloat()) * 10000).toInt()
                val positionPenalty = effectiveIdx * 500
                val finalScore = baseScore + ratioBonus - positionPenalty - targetLen

                Pair(item, finalScore)
            } else {
                null
            }
        }
            .sortedByDescending { it.second }
            .map { it.first }
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

    val onItemClick: (SearchItem) -> Unit = { item ->
        try {
            val intent = when (item.type) {
                SearchItemType.APP -> context.packageManager.getLaunchIntentForPackage(item.launchData)
                SearchItemType.SETTINGS, SearchItemType.SYSTEM_ACTION -> Intent(item.launchData)
                SearchItemType.CONTACT -> Intent(Intent.ACTION_VIEW).apply { data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, item.contactId) }
            }
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let {
                context.startActivity(it)
                searchQuery = ""
            }
        } catch (_: Exception) { }
    }

    if (showSettings) {
        Scaffold(
            containerColor = Color(0xFF121212),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (isChinese) "设置" else "Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = { IconButton(onClick = { showSettings = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF121212))
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    // --- 语言设置 ---
                    item {
                        Text(text = if (isChinese) "语言 / Language" else "Language", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00BCD4), modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.selectableGroup()) {
                                val langOptions = listOf((if (isChinese) "跟随系统" else "System Default") to 0, "中文" to 1, "English" to 2)
                                langOptions.forEach { option ->
                                    Row(Modifier.fillMaxWidth().height(56.dp).selectable(selected = (languagePref == option.second), onClick = { languagePref = option.second; prefs.edit { putInt("app_language", option.second) } }, role = Role.RadioButton).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = (languagePref == option.second), onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00BCD4)))
                                        Text(text = option.first, color = Color.White, modifier = Modifier.padding(start = 16.dp))
                                    }
                                    if (option != langOptions.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }

                    // --- 搜索范围 ---
                    item {
                        Text(text = if (isChinese) "搜索范围" else "Search Scope", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00BCD4), modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(24.dp)) {
                            Column {
                                SettingM3Row(if (isChinese) "联系人" else "Contacts", Icons.Default.Phone, searchContactsEnabled) { searchContactsEnabled = it; prefs.edit { putBoolean("search_contacts", it) } }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                                SettingM3Row(if (isChinese) "应用软件" else "Applications", Icons.Default.Face, searchAppsEnabled) { searchAppsEnabled = it; prefs.edit { putBoolean("search_apps", it) } }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                                SettingM3Row(if (isChinese) "系统设置" else "System Settings", Icons.Default.Settings, searchSettingsEnabled) { searchSettingsEnabled = it; prefs.edit { putBoolean("search_settings", it) } }
                            }
                        }
                    }

                    // --- 写入速度 ---
                    item {
                        Text(text = if (isChinese) "写入速度" else "Writing Speed", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00BCD4), modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.selectableGroup()) {
                                val speedOptions = listOf(
                                    (if (isChinese) "快 (300ms)" else "Fast (300ms)") to 300L,
                                    (if (isChinese) "中等 (500ms)" else "Medium (500ms)") to 500L,
                                    (if (isChinese) "慢 (800ms)" else "Slow (800ms)") to 800L
                                )
                                speedOptions.forEach { option ->
                                    Row(Modifier.fillMaxWidth().height(56.dp).selectable(selected = (writingSpeedDelay == option.second), onClick = { writingSpeedDelay = option.second; prefs.edit { putLong("writing_delay", option.second) } }, role = Role.RadioButton).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = (writingSpeedDelay == option.second), onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00BCD4)))
                                        Text(text = option.first, color = Color.White, modifier = Modifier.padding(start = 16.dp))
                                    }
                                    if (option != speedOptions.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }

                val packageInfo = remember {
                    try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (e: Exception) { null }
                }
                val vName = packageInfo?.versionName ?: "1.4"
                val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: BUILD_VERSION.toLong()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toLong() ?: BUILD_VERSION.toLong()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri("https://github.com/colorbeta/GestureExplode") }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.github),
                        contentDescription = "GitHub Repository",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Gesture Explode v$vName (Build $vCode)",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
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

            var showIndicatorOnRight by remember { mutableStateOf(true) }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull()
                                if (change != null && change.pressed && !change.previousPressed) {
                                    showIndicatorOnRight = change.position.x < size.width * 0.5f
                                }
                            }
                        }
                    }
            ) {
                val trackWidthPx = with(density) { maxWidth.toPx() }
                val trackHeightPx = with(density) { maxHeight.toPx() }
                val offsetXShift = trackWidthPx * 0.15f

                androidx.compose.animation.AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { 60 }, animationSpec = tween(250))
                ) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                        itemsIndexed(filteredItems) { _, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { onItemClick(item) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                            ) {
                                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(44.dp), Alignment.Center) {
                                        when (item.type) {
                                            SearchItemType.CONTACT -> Icon(Icons.Default.Phone, null, tint = Color(0xFF8BC34A), modifier = Modifier.size(26.dp))
                                            SearchItemType.SETTINGS, SearchItemType.SYSTEM_ACTION -> Icon(Icons.Default.Settings, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(26.dp))
                                            SearchItemType.APP -> AppIcon(item.launchData, Modifier.size(40.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        HighlightedText(item.title, searchQuery, item.initials, item.initialIndices)

                                        // 动态匹配副标题语言
                                        val displaySubtitle = when (item.type) {
                                            SearchItemType.APP -> if (isChinese) "应用软件" else "Application"
                                            SearchItemType.SETTINGS, SearchItemType.SYSTEM_ACTION -> if (isChinese) "系统设置" else "System Settings"
                                            SearchItemType.CONTACT -> item.subtitle // 联系人展示号码，不翻译
                                        }
                                        HighlightedSubtitle(displaySubtitle, searchQuery)
                                    }
                                }
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = searchQuery.isEmpty(),
                    enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = -120.dp)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            // 主屏幕的双语提示词
                            text = if (isChinese) "画一个字符开始搜索！" else "Draw a character to search!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E),
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (path in paths) drawPath(path, Color(0xFFFFEB3B), style = Stroke(15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    if (currentPath.isNotEmpty()) {
                        val p = Path().apply { moveTo(currentPath.first().x, currentPath.first().y); for (i in 1 until currentPath.size) lineTo(currentPath[i].x, currentPath[i].y) }
                        drawPath(p, Color(0xFFFFEB3B), style = Stroke(15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(0.15f).fillMaxHeight())

                    Box(modifier = Modifier
                        .weight(0.70f)
                        .fillMaxHeight()
                        .pointerInput(isReady, filteredItems) {
                            detectTapGestures { offset ->
                                if (searchQuery.isNotEmpty()) {
                                    val tappedItem = listState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                                        offset.y.toInt() in itemInfo.offset until (itemInfo.offset + itemInfo.size)
                                    }
                                    tappedItem?.let { itemInfo ->
                                        if (itemInfo.index in filteredItems.indices) {
                                            onItemClick(filteredItems[itemInfo.index])
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(isReady) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    recognizeJob?.cancel()
                                    currentPath.clear()
                                    strokeBuilder = Ink.Stroke.builder().apply { addPoint(Ink.Point.create(offset.x + offsetXShift, offset.y, System.currentTimeMillis())) }
                                },
                                onDragCancel = {
                                    recognizeJob?.cancel()
                                    currentPath.clear()
                                    paths.clear()
                                    inkBuilder = Ink.builder()
                                },
                                onDrag = { change, _ ->
                                    val timeOffset = System.currentTimeMillis() - change.uptimeMillis
                                    change.historical.forEach { hist ->
                                        currentPath.add(Offset(hist.position.x + offsetXShift, hist.position.y))
                                        strokeBuilder?.addPoint(Ink.Point.create(hist.position.x + offsetXShift, hist.position.y, hist.uptimeMillis + timeOffset))
                                    }
                                    currentPath.add(Offset(change.position.x + offsetXShift, change.position.y))
                                    strokeBuilder?.addPoint(Ink.Point.create(change.position.x + offsetXShift, change.position.y, change.uptimeMillis + timeOffset))
                                },
                                onDragEnd = {
                                    if (currentPath.isNotEmpty()) {
                                        val p = Path().apply { moveTo(currentPath.first().x, currentPath.first().y); for (i in 1 until currentPath.size) lineTo(currentPath[i].x, currentPath[i].y) }
                                        paths.add(p); strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                                    }
                                    if (currentPath.isNotEmpty()) {
                                        val dx = currentPath.last().x - currentPath.first().x
                                        val dy = currentPath.last().y - currentPath.first().y

                                        if (dx < -200f && abs(dx) > abs(dy) * 1.5f) {
                                            if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
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

                                                val candidates = result.candidates.take(10).mapNotNull {
                                                    val tStr = it.text.trim()
                                                    if (tStr.isNotEmpty() && tStr.first().isLetterOrDigit()) {
                                                        var char = tStr.first().toString().lowercase()
                                                        if (char == "2") char = "z"
                                                        if (char == "1") char = "l"
                                                        char
                                                    } else null
                                                }.distinct()

                                                if (candidates.isNotEmpty()) {
                                                    var bestChar: String? = null
                                                    for (char in candidates) {
                                                        val testQuery = searchQuery + char
                                                        val testQueryFallback = testQuery.replace('z', '2').replace('l', '1')

                                                        val hasMatch = filteredItems.any { item ->
                                                            val t = item.title.lowercase()
                                                            val i = item.initials
                                                            val sub = item.subtitle.lowercase() // 这里匹配真实的固定文字

                                                            t.contains(testQuery) || i.contains(testQuery) ||
                                                                    t.contains(testQueryFallback) || i.contains(testQueryFallback) ||
                                                                    (item.type == SearchItemType.CONTACT && (sub.contains(testQuery) || sub.contains(testQueryFallback)))
                                                        }
                                                        if (hasMatch) {
                                                            bestChar = char
                                                            break
                                                        }
                                                    }
                                                    if (bestChar == null) {
                                                        bestChar = candidates.first()
                                                    }
                                                    searchQuery += bestChar
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.weight(0.15f).fillMaxHeight())
                }

                if (searchQuery.isNotEmpty() && listState.isScrollInProgress) {
                    val totalItems = filteredItems.size
                    val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                    if (totalItems > 0 && visibleItemsInfo.isNotEmpty()) {
                        val visibleItems = visibleItemsInfo.size.toFloat()
                        val firstItemInfo = visibleItemsInfo.first()
                        val exactScrollIndex = firstItemInfo.index.toFloat() + (abs(firstItemInfo.offset).toFloat() / firstItemInfo.size.coerceAtLeast(1))

                        val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
                        val searchBoxBottomMarginPx = with(density) { 24.dp.toPx() } + navBarBottomPx

                        val effectiveTrackHeightPx = trackHeightPx - searchBoxBottomMarginPx

                        val thumbHeight = (effectiveTrackHeightPx * (visibleItems / totalItems)).coerceIn(80f, effectiveTrackHeightPx)
                        val scrollProportion = exactScrollIndex / (totalItems - visibleItems).coerceAtLeast(1f)
                        val scrollOffset = (effectiveTrackHeightPx - thumbHeight) * scrollProportion

                        val alignModifier = if (showIndicatorOnRight) Alignment.TopEnd else Alignment.TopStart
                        val paddingModifier = if (showIndicatorOnRight) Modifier.padding(end = 24.dp) else Modifier.padding(start = 24.dp)

                        Box(
                            modifier = Modifier
                                .align(alignModifier)
                                .then(paddingModifier)
                                .offset(y = with(density) { scrollOffset.toDp() })
                                .width(12.dp)
                                .height(with(density) { thumbHeight.toDp() })
                                .background(Color(0xFFFFEB3B).copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    ) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).navigationBarsPadding()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .background(Color(0xFF222222), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = searchQuery,
                                    fontSize = 24.sp,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFFFFEB3B),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFEB3B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
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