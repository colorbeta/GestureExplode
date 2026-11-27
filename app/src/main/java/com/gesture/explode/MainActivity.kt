package com.gesture.explode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gesture.explode.data.DeviceBrand
import com.gesture.explode.data.SearchItem
import com.gesture.explode.data.SearchItemType
import com.gesture.explode.data.detectDeviceBrand
import com.gesture.explode.data.getContacts
import com.gesture.explode.data.getInstalledApps
import com.gesture.explode.data.getSystemSettingsItems
import com.gesture.explode.ui.components.AppsGridIcon
import com.gesture.explode.ui.components.BackspaceIcon
import com.gesture.explode.ui.components.CachedAppIcon
import com.gesture.explode.ui.components.CandidateCorrectionBar
import com.gesture.explode.ui.components.CategoryIconBadge
import com.gesture.explode.ui.components.ContactAvatar
import com.gesture.explode.ui.components.DraggableFastScroller
import com.gesture.explode.ui.components.EmptyGesturePrompt
import com.gesture.explode.ui.components.GestureGuideBottomSheet
import com.gesture.explode.ui.components.HapticCue
import com.gesture.explode.ui.components.HapticEngine
import com.gesture.explode.ui.components.HapticWaveIcon
import com.gesture.explode.ui.components.IconPackSymbol
import com.gesture.explode.ui.components.NoSearchResultsHint
import com.gesture.explode.ui.components.ScrollSessionBanner
import com.gesture.explode.ui.components.SettingSectionHeader
import com.gesture.explode.ui.components.SettingSwitchRow
import com.gesture.explode.ui.components.SmsBubbleIcon
import com.gesture.explode.ui.components.groupedCardShape
import com.gesture.explode.ui.theme.GestureExplodeTheme
import com.gesture.explode.ui.theme.ThemeMode
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.roundToInt

// --- 【升级为 97：拨号弹窗等宽双按钮（左拨打右取消）+ 自动识别图标包并支持独立开关（默认关闭）】 ---
const val BUILD_VERSION = 97

private enum class GestureCaptureMode { UNDECIDED, WRITING, SCROLLING }

private val CONFUSABLE_PAIRS: Map<String, List<String>> = mapOf(
    "t" to listOf("f", "l", "7"),
    "f" to listOf("t", "e", "p"),
    "d" to listOf("o", "a", "b"),
    "o" to listOf("d", "0", "q", "c"),
    "c" to listOf("e", "o", "g"),
    "v" to listOf("u", "y"),
    "u" to listOf("v", "n"),
    "n" to listOf("h", "m", "r"),
    "m" to listOf("n", "w"),
    "w" to listOf("m", "v"),
    "z" to listOf("2", "s"),
    "l" to listOf("1", "i", "t"),
    "i" to listOf("l", "j", "1"),
    "s" to listOf("5", "z")
)

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) {
        path.lineTo(points.first().x + 0.1f, points.first().y + 0.1f)
        return path
    }
    for (i in 1 until points.size - 1) {
        val midX = (points[i].x + points[i + 1].x) / 2f
        val midY = (points[i].y + points[i + 1].y) / 2f
        path.quadraticTo(points[i].x, points[i].y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

/**
 * Estimates whether a single stroke is already a complete curved/looped character
 * (like a, b, c, d, e, g, m, n, o, p, q, s, u, v, w, y, z, 2, 3, 5, 6, 8, 9, 0)
 * versus a straight/hook stem that may expect a second stroke (like t, f, x, k, i, H, E).
 */
private fun isLikelyCompleteSingleStroke(points: List<Offset>, density: Density): Boolean {
    if (points.size < 6) return false
    val minLengthPx = with(density) { 46.dp.toPx() }
    val sampleStepPx = with(density) { 6.dp.toPx() }

    var pathLength = 0f
    for (i in 1 until points.size) {
        pathLength += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    }
    if (pathLength < minLengthPx) return false

    val chordLength = hypot(points.last().x - points.first().x, points.last().y - points.first().y)
    val tortuosity = pathLength / chordLength.coerceAtLeast(1f)

    // Subsample by distance so micro finger jitter does not inflate turn angles
    val sampled = mutableListOf(points.first())
    var lastAdded = points.first()
    for (i in 1 until points.size) {
        val pt = points[i]
        if (hypot(pt.x - lastAdded.x, pt.y - lastAdded.y) >= sampleStepPx) {
            sampled.add(pt)
            lastAdded = pt
        }
    }
    if (sampled.last() != points.last()) sampled.add(points.last())

    var totalTurnRad = 0f
    for (i in 1 until sampled.size - 1) {
        val v1x = sampled[i].x - sampled[i - 1].x
        val v1y = sampled[i].y - sampled[i - 1].y
        val v2x = sampled[i + 1].x - sampled[i].x
        val v2y = sampled[i + 1].y - sampled[i].y
        val len1 = hypot(v1x, v1y)
        val len2 = hypot(v2x, v2y)
        if (len1 > 1f && len2 > 1f) {
            val cosTheta = ((v1x * v2x + v1y * v2y) / (len1 * len2)).coerceIn(-1f, 1f)
            totalTurnRad += acos(cosTheta)
        }
    }

    return totalTurnRad > 2.35f || tortuosity > 1.45f
}

private fun View.triggerHaptic(enabled: Boolean, feedbackConstant: Int, strength: Float) {
    val cue = when (feedbackConstant) {
        HapticFeedbackConstants.CONFIRM -> HapticCue.CONFIRM
        HapticFeedbackConstants.GESTURE_START -> HapticCue.SCROLL_START
        HapticFeedbackConstants.GESTURE_END,
        HapticFeedbackConstants.SEGMENT_TICK -> HapticCue.DELETE
        HapticFeedbackConstants.REJECT -> HapticCue.CLEAR
        else -> HapticCue.TICK
    }
    HapticEngine.trigger(context, this, enabled, strength, cue)
}

/**
 * Bidirectional handwritten lookalike equivalence:
 * - 'l', 'i', '1' can match each other (e.g. writing "ll" matches "lily")
 * - 'z', '2' can match each other
 */
private fun isFuzzyCharMatch(queryChar: Char, targetChar: Char): Boolean {
    if (queryChar == targetChar) return true
    return when (queryChar) {
        'l', 'i', '1' -> targetChar == 'l' || targetChar == 'i' || targetChar == '1'
        'z', '2' -> targetChar == 'z' || targetChar == '2'
        else -> false
    }
}

private fun indexOfFuzzy(haystack: String, needle: String): Int {
    if (needle.isEmpty()) return 0
    if (needle.length > haystack.length) return -1
    val maxStart = haystack.length - needle.length
    outer@ for (start in 0..maxStart) {
        for (j in needle.indices) {
            if (!isFuzzyCharMatch(needle[j], haystack[start + j])) {
                continue@outer
            }
        }
        return start
    }
    return -1
}

private fun startsWithFuzzy(text: String, prefix: String): Boolean {
    if (prefix.length > text.length) return false
    for (j in prefix.indices) {
        if (!isFuzzyCharMatch(prefix[j], text[j])) return false
    }
    return true
}

private fun equalsFuzzy(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    for (j in a.indices) {
        if (!isFuzzyCharMatch(a[j], b[j])) return false
    }
    return true
}

private fun startsWithWordBoundaryFuzzy(text: String, query: String): Boolean {
    if (text.length <= query.length || !startsWithFuzzy(text, query)) return false
    val next = text[query.length]
    return next == ' ' || next == '(' || next == '-' || next == '（'
}

private fun matchesAnyItem(items: List<SearchItem>, testQuery: String): Boolean {
    val q = testQuery.lowercase(Locale.getDefault())
    val allowPrefixAlias = q.length >= 2
    return items.any { item ->
        val t = item.title.lowercase(Locale.getDefault())
        val i = item.initials
        val sub = item.subtitle.lowercase(Locale.getDefault())
        val aliasHit = item.aliases.any { a ->
            if (allowPrefixAlias) startsWithFuzzy(a, q) else equalsFuzzy(a, q)
        }

        aliasHit || indexOfFuzzy(t, q) != -1 || indexOfFuzzy(i, q) != -1 ||
            (item.type == SearchItemType.CONTACT && indexOfFuzzy(sub, q) != -1)
    }
}

@Composable
fun HighlightedText(text: String, query: String, initials: String, initialIndices: List<Int>) {
    val colorScheme = MaterialTheme.colorScheme
    val matchColor = colorScheme.primary
    val normalColor = colorScheme.onSurface
    val dimColor = colorScheme.onSurface.copy(alpha = 0.68f)

    val annotatedString = remember(text, query, initials, initialIndices, matchColor, normalColor, dimColor) {
        buildAnnotatedString {
            val lowerT = text.lowercase(Locale.getDefault())
            val lowerQ = query.lowercase(Locale.getDefault())

            val matchStyle = SpanStyle(color = matchColor, fontWeight = FontWeight.ExtraBold)
            val dimStyle = SpanStyle(color = dimColor, fontWeight = FontWeight.Normal)

            if (query.isEmpty()) {
                withStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Medium)) { append(text) }
                return@buildAnnotatedString
            }

            var directIdx = lowerT.indexOf(lowerQ)
            if (directIdx == -1) {
                directIdx = indexOfFuzzy(lowerT, lowerQ)
            }

            var acronymIdx = initials.indexOf(lowerQ)
            if (acronymIdx == -1) {
                acronymIdx = indexOfFuzzy(initials, lowerQ)
            }

            when {
                directIdx != -1 -> {
                    withStyle(dimStyle) { append(text.substring(0, directIdx)) }
                    withStyle(matchStyle) { append(text.substring(directIdx, directIdx + lowerQ.length)) }
                    withStyle(dimStyle) { append(text.substring(directIdx + lowerQ.length)) }
                }
                acronymIdx != -1 -> {
                    val matchedIndicesSet = mutableSetOf<Int>()
                    for (i in lowerQ.indices) {
                        if (acronymIdx + i in initialIndices.indices) {
                            matchedIndicesSet.add(initialIndices[acronymIdx + i])
                        }
                    }
                    for (i in text.indices) {
                        if (i in matchedIndicesSet) withStyle(matchStyle) { append(text[i]) }
                        else withStyle(dimStyle) { append(text[i]) }
                    }
                }
                else -> {
                    // Alias-matched item: keep title crisp and readable
                    withStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Medium)) { append(text) }
                }
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun HighlightedSubtitle(text: String, query: String) {
    val colorScheme = MaterialTheme.colorScheme
    val matchColor = colorScheme.primary
    val dimColor = colorScheme.onSurfaceVariant

    val annotatedString = remember(text, query, matchColor, dimColor) {
        buildAnnotatedString {
            val lowerT = text.lowercase(Locale.getDefault())
            val lowerQ = query.lowercase(Locale.getDefault())

            val matchStyle = SpanStyle(color = matchColor, fontWeight = FontWeight.Bold)
            val dimStyle = SpanStyle(color = dimColor, fontWeight = FontWeight.Normal)

            if (query.isEmpty()) {
                withStyle(dimStyle) { append(text) }
                return@buildAnnotatedString
            }

            var directIdx = lowerT.indexOf(lowerQ)
            if (directIdx == -1) {
                directIdx = indexOfFuzzy(lowerT, lowerQ)
            }

            if (directIdx != -1) {
                withStyle(dimStyle) { append(text.substring(0, directIdx)) }
                withStyle(matchStyle) { append(text.substring(directIdx, directIdx + lowerQ.length)) }
                withStyle(dimStyle) { append(text.substring(directIdx + lowerQ.length)) }
            } else {
                withStyle(dimStyle) { append(text) }
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE) }

            var themeMode by remember {
                mutableStateOf(ThemeMode.fromPref(prefs.getString("theme_mode", ThemeMode.SYSTEM.prefValue)))
            }
            var dynamicColorEnabled by remember {
                mutableStateOf(prefs.getBoolean("dynamic_color", true))
            }
            var hapticsEnabled by remember {
                mutableStateOf(prefs.getBoolean("haptics_enabled", true))
            }
            var hapticStrength by remember {
                mutableFloatStateOf(prefs.getFloat("haptic_strength", 0.6f).coerceIn(0.2f, 1.0f))
            }

            GestureExplodeTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColorEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    GestureSearchApp(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            prefs.edit { putString("theme_mode", mode.prefValue) }
                        },
                        dynamicColorEnabled = dynamicColorEnabled,
                        onDynamicColorChange = { enabled ->
                            dynamicColorEnabled = enabled
                            prefs.edit { putBoolean("dynamic_color", enabled) }
                        },
                        hapticsEnabled = hapticsEnabled,
                        onHapticsChange = { enabled ->
                            hapticsEnabled = enabled
                            prefs.edit { putBoolean("haptics_enabled", enabled) }
                        },
                        hapticStrength = hapticStrength,
                        onHapticStrengthChange = { strength ->
                            val clamped = strength.coerceIn(0.2f, 1.0f)
                            hapticStrength = clamped
                            prefs.edit { putFloat("haptic_strength", clamped) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GestureSearchApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    hapticStrength: Float,
    onHapticStrengthChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val prefs = remember { context.getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE) }
    val flingDecay = rememberSplineBasedDecay<Float>()

    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var lastCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var isReady by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGuideSheet by remember { mutableStateOf(false) }
    var pendingCallContact by remember { mutableStateOf<SearchItem?>(null) }

    // Scroll session state: stays active during scroll + 1.2s after settling for bidirectional browsing
    var isInScrollSession by remember { mutableStateOf(false) }
    var isFastScrollerDragging by remember { mutableStateOf(false) }
    var scrollSessionTimeoutJob by remember { mutableStateOf<Job?>(null) }
    var flingJob by remember { mutableStateOf<Job?>(null) }

    val paths = remember { mutableStateListOf<Path>() }
    val fadingPaths = remember { mutableStateListOf<Path>() }
    val fadingAlpha = remember { Animatable(0f) }
    val commitProgress = remember { Animatable(0f) }
    val currentPath = remember { mutableStateListOf<Offset>() }
    var inkBuilder by remember { mutableStateOf(Ink.builder()) }
    var strokeBuilder: Ink.Stroke.Builder? by remember { mutableStateOf(null) }
    var recognizeJob by remember { mutableStateOf<Job?>(null) }

    val deviceBrand = remember { detectDeviceBrand() }
    val isChinese = Locale.getDefault().language == "zh"

    var searchAppsEnabled by remember { mutableStateOf(prefs.getBoolean("search_apps", true)) }
    var searchContactsEnabled by remember { mutableStateOf(prefs.getBoolean("search_contacts", true)) }
    var searchSettingsEnabled by remember { mutableStateOf(prefs.getBoolean("search_settings", true)) }
    var searchIconPacksEnabled by remember { mutableStateOf(prefs.getBoolean("search_icon_packs", false)) }
    var writingSpeedDelay by remember { mutableLongStateOf(prefs.getLong("writing_delay", 500L)) }
    var smartSingleStrokeBoost by remember { mutableStateOf(prefs.getBoolean("smart_single_stroke_boost", true)) }

    LaunchedEffect(searchQuery) {
        listState.scrollToItem(0)
        isInScrollSession = false
        scrollSessionTimeoutJob?.cancel()
        flingJob?.cancel()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                searchQuery = ""
                lastCandidates = emptyList()
                pendingCallContact = null
                isInScrollSession = false
                scrollSessionTimeoutJob?.cancel()
                flingJob?.cancel()
                paths.clear()
                fadingPaths.clear()
                currentPath.clear()
                inkBuilder = Ink.builder()
                recognizeJob?.cancel()
                coroutineScope.launch {
                    commitProgress.snapTo(0f)
                    fadingAlpha.snapTo(0f)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val density = LocalDensity.current

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasContactPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasContactPermission) permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    // --- 后台协程加载应用、品牌专属系统设置与联系人，确保冷启动秒开 ---
    var allApps by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var allContacts by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var allSystemSettings by remember { mutableStateOf<List<SearchItem>>(emptyList()) }

    LaunchedEffect(isChinese, deviceBrand) {
        withContext(Dispatchers.IO) {
            allApps = getInstalledApps(context)
            allSystemSettings = getSystemSettingsItems(context, isChinese, deviceBrand)
        }
    }

    LaunchedEffect(hasContactPermission, isChinese) {
        if (hasContactPermission) {
            withContext(Dispatchers.IO) {
                allContacts = getContacts(context, isChinese)
            }
        } else {
            allContacts = emptyList()
        }
    }

    val allItems = remember(
        allApps,
        allContacts,
        allSystemSettings,
        searchAppsEnabled,
        searchContactsEnabled,
        searchSettingsEnabled,
        searchIconPacksEnabled
    ) {
        val list = mutableListOf<SearchItem>()
        if (searchAppsEnabled) list.addAll(allApps.filter { it.type == SearchItemType.APP })
        if (searchIconPacksEnabled) list.addAll(allApps.filter { it.type == SearchItemType.ICON_PACK })
        if (searchSettingsEnabled) {
            list.addAll(allApps.filter { it.type == SearchItemType.SETTINGS })
            list.addAll(allSystemSettings)
        }
        if (searchContactsEnabled) list.addAll(allContacts)
        list.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isEmpty()) return@remember allItems
        val q = searchQuery.lowercase(Locale.getDefault())
        val allowPrefixAlias = q.length >= 2

        allItems.mapNotNull { item ->
            val t = item.title.lowercase(Locale.getDefault())
            val i = item.initials
            val sub = item.subtitle.lowercase(Locale.getDefault())

            val tIdxPrimary = t.indexOf(q)
            val iIdxPrimary = i.indexOf(q)
            val aliasExactPrimary = item.aliases.any { it == q }
            val aliasPrefixPrimary = allowPrefixAlias && item.aliases.any { it.startsWith(q) }

            val tIdxFallback = indexOfFuzzy(t, q)
            val iIdxFallback = indexOfFuzzy(i, q)
            val aliasExactFallback = item.aliases.any { equalsFuzzy(it, q) }
            val aliasPrefixFallback = allowPrefixAlias && item.aliases.any { startsWithFuzzy(it, q) }

            val subIdxPrimary = if (item.type == SearchItemType.CONTACT) sub.indexOf(q) else -1
            val subIdxFallback = if (item.type == SearchItemType.CONTACT) indexOfFuzzy(sub, q) else -1

            val isMatchPrimary = tIdxPrimary != -1 || iIdxPrimary != -1 || aliasExactPrimary || aliasPrefixPrimary
            val isMatchFallback = tIdxFallback != -1 || iIdxFallback != -1 || aliasExactFallback || aliasPrefixFallback
            val isMatchSubPrimary = subIdxPrimary != -1
            val isMatchSubFallback = subIdxFallback != -1

            if (isMatchPrimary || isMatchFallback || isMatchSubPrimary || isMatchSubFallback) {
                var baseScore = 0
                var effectiveIdx = 0
                val matchedLength = q.length
                var targetLen = t.length

                if (isMatchPrimary) {
                    val textOrInitialIdx = when {
                        iIdxPrimary != -1 && tIdxPrimary != -1 -> minOf(iIdxPrimary, tIdxPrimary)
                        iIdxPrimary != -1 -> iIdxPrimary
                        tIdxPrimary != -1 -> tIdxPrimary
                        else -> 0
                    }
                    effectiveIdx = if (aliasExactPrimary || aliasPrefixPrimary) 0 else textOrInitialIdx
                    baseScore = when {
                        t == q || (i == q && t.length == q.length) -> 100000
                        aliasExactPrimary -> 98000
                        t.startsWith("$q ") || t.startsWith("$q(") || t.startsWith("$q-") || t.startsWith("${q}（") -> 90000
                        i == q -> 86000
                        aliasPrefixPrimary -> 76000
                        textOrInitialIdx == 0 && (iIdxPrimary == 0 || tIdxPrimary == 0) -> 70000
                        else -> 40000
                    }
                } else if (isMatchFallback) {
                    val textOrInitialIdx = when {
                        iIdxFallback != -1 && tIdxFallback != -1 -> minOf(iIdxFallback, tIdxFallback)
                        iIdxFallback != -1 -> iIdxFallback
                        tIdxFallback != -1 -> tIdxFallback
                        else -> 0
                    }
                    effectiveIdx = if (aliasExactFallback || aliasPrefixFallback) 0 else textOrInitialIdx
                    baseScore = when {
                        equalsFuzzy(t, q) || (equalsFuzzy(i, q) && t.length == q.length) -> 95000
                        aliasExactFallback -> 93000
                        startsWithWordBoundaryFuzzy(t, q) -> 85000
                        equalsFuzzy(i, q) -> 81000
                        aliasPrefixFallback -> 71000
                        textOrInitialIdx == 0 && (iIdxFallback == 0 || tIdxFallback == 0) -> 65000
                        else -> 35000
                    }
                } else {
                    effectiveIdx = if (isMatchSubPrimary) subIdxPrimary else subIdxFallback
                    baseScore = if (isMatchSubPrimary) 20000 else 15000
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
        RemoteModelManager.getInstance()
            .download(m, DownloadConditions.Builder().build())
            .addOnSuccessListener { isReady = true }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    val currentHapticsEnabled by rememberUpdatedState(hapticsEnabled)
    val currentHapticStrength by rememberUpdatedState(hapticStrength)
    val currentWritingDelay by rememberUpdatedState(writingSpeedDelay)
    val currentSmartBoost by rememberUpdatedState(smartSingleStrokeBoost)
    val currentFilteredItems by rememberUpdatedState(filteredItems)
    val currentAllItems by rememberUpdatedState(allItems)
    val currentQuery by rememberUpdatedState(searchQuery)
    val currentInScrollSession by rememberUpdatedState(isInScrollSession)

    val scheduleScrollSessionTimeout: () -> Unit = {
        scrollSessionTimeoutJob?.cancel()
        scrollSessionTimeoutJob = coroutineScope.launch {
            delay(1200L)
            if (!isFastScrollerDragging) {
                isInScrollSession = false
            }
        }
    }

    val scheduleRecognition: (Long) -> Unit = { delayMs ->
        recognizeJob?.cancel()
        recognizeJob = coroutineScope.launch {
            commitProgress.snapTo(0f)
            commitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = delayMs.toInt().coerceAtLeast(120),
                    easing = LinearEasing
                )
            )
            commitProgress.snapTo(0f)

            val finalInk = inkBuilder.build()
            inkBuilder = Ink.builder()

            // Move strokes to fade-out layer so user can start next letter immediately
            val snapshotPaths = paths.toList()
            paths.clear()
            if (snapshotPaths.isNotEmpty()) {
                coroutineScope.launch {
                    fadingPaths.clear()
                    fadingPaths.addAll(snapshotPaths)
                    fadingAlpha.snapTo(1f)
                    fadingAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                    fadingPaths.clear()
                }
            }

            if (finalInk.strokes.isNotEmpty()) {
                recognizer.recognize(finalInk).addOnSuccessListener { result ->
                    val candidates = result.candidates.take(10).mapNotNull {
                        val tStr = it.text.trim()
                        if (tStr.isNotEmpty() && tStr.first().isLetterOrDigit()) {
                            var ch = tStr.first().toString().lowercase(Locale.getDefault())
                            if (ch == "2") ch = "z"
                            if (ch == "1") ch = "l"
                            ch
                        } else {
                            null
                        }
                    }.distinct()

                    if (candidates.isNotEmpty()) {
                        val baseQuery = currentQuery
                        val itemsForCheck = currentFilteredItems
                        var bestChar: String? = null
                        for (ch in candidates) {
                            if (matchesAnyItem(itemsForCheck, baseQuery + ch)) {
                                bestChar = ch
                                break
                            }
                        }
                        val chosen = bestChar ?: candidates.first()

                        // Build smart alternates for 1-tap correction (especially f/t, d/o)
                        val rawAlternates = (
                            candidates.filter { it != chosen } +
                                CONFUSABLE_PAIRS[chosen].orEmpty()
                            ).distinct().filter { it != chosen }

                        val sortedAlternates = rawAlternates
                            .sortedByDescending { alt ->
                                if (matchesAnyItem(itemsForCheck, baseQuery + alt)) 1 else 0
                            }
                            .take(4)

                        searchQuery = baseQuery + chosen
                        lastCandidates = sortedAlternates
                        view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CONFIRM, currentHapticStrength)
                    }
                }
            }
        }
    }

    val onItemClick: (SearchItem) -> Unit = { item ->
        view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CONFIRM, currentHapticStrength)
        try {
            val intent = when (item.type) {
                SearchItemType.APP, SearchItemType.ICON_PACK -> context.packageManager.getLaunchIntentForPackage(item.launchData)
                SearchItemType.SETTINGS -> context.packageManager.getLaunchIntentForPackage(item.launchData)
                    ?: Intent(Settings.ACTION_SETTINGS)
                SearchItemType.SYSTEM_ACTION -> item.resolvedIntent?.let { Intent(it) } ?: Intent(item.launchData)
                SearchItemType.CONTACT -> Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, item.contactId)
                }
            }
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                context.startActivity(intent)
                searchQuery = ""
                lastCandidates = emptyList()
                isInScrollSession = false
            }
        } catch (_: Exception) {
            // Safe fallback for OEM-restricted settings activity
            if (item.type == SearchItemType.SYSTEM_ACTION) {
                try {
                    val fallback = if (item.aliases.contains("dc")) {
                        Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                    } else {
                        Intent(Settings.ACTION_SETTINGS)
                    }
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                    searchQuery = ""
                    lastCandidates = emptyList()
                } catch (_: Exception) {
                }
            }
        }
    }

    val onRequestDialContact: (SearchItem) -> Unit = { item ->
        val phone = item.phoneNumber ?: item.launchData
        if (phone.isNotBlank()) {
            view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
            pendingCallContact = item
        }
    }

    val onDialContact: (SearchItem) -> Unit = { item ->
        val phone = item.phoneNumber ?: item.launchData
        if (phone.isNotBlank()) {
            view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CONFIRM, currentHapticStrength)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                searchQuery = ""
                lastCandidates = emptyList()
                isInScrollSession = false
            } catch (_: Exception) {
            }
        }
    }

    val onSmsContact: (SearchItem) -> Unit = { item ->
        val phone = item.phoneNumber ?: item.launchData
        if (phone.isNotBlank()) {
            view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CONFIRM, currentHapticStrength)
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                searchQuery = ""
                lastCandidates = emptyList()
                isInScrollSession = false
            } catch (_: Exception) {
            }
        }
    }

    // M3 Shared Axis X transition between main search screen and settings screen
    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(240)))
                    .togetherWith(slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 6 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 6 } + fadeIn(tween(240)))
                    .togetherWith(slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(180)))
            }
        },
        label = "screen_transition"
    ) { inSettings ->
        if (inSettings) {
            SettingsScreen(
                isChinese = isChinese,
                searchContactsEnabled = searchContactsEnabled,
                onSearchContactsChange = {
                    searchContactsEnabled = it
                    prefs.edit { putBoolean("search_contacts", it) }
                },
                searchAppsEnabled = searchAppsEnabled,
                onSearchAppsChange = {
                    searchAppsEnabled = it
                    prefs.edit { putBoolean("search_apps", it) }
                },
                searchSettingsEnabled = searchSettingsEnabled,
                onSearchSettingsChange = {
                    searchSettingsEnabled = it
                    prefs.edit { putBoolean("search_settings", it) }
                },
                searchIconPacksEnabled = searchIconPacksEnabled,
                onSearchIconPacksChange = {
                    searchIconPacksEnabled = it
                    prefs.edit { putBoolean("search_icon_packs", it) }
                },
                writingSpeedDelay = writingSpeedDelay,
                onWritingSpeedChange = {
                    writingSpeedDelay = it
                    prefs.edit { putLong("writing_delay", it) }
                    view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
                },
                smartSingleStrokeBoost = smartSingleStrokeBoost,
                onSmartSingleStrokeBoostChange = {
                    smartSingleStrokeBoost = it
                    prefs.edit { putBoolean("smart_single_stroke_boost", it) }
                },
                themeMode = themeMode,
                onThemeModeChange = {
                    onThemeModeChange(it)
                    view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
                },
                dynamicColorEnabled = dynamicColorEnabled,
                onDynamicColorChange = onDynamicColorChange,
                hapticsEnabled = hapticsEnabled,
                onHapticsChange = {
                    onHapticsChange(it)
                    if (it) HapticEngine.trigger(context, view, true, currentHapticStrength, HapticCue.CONFIRM)
                },
                hapticStrength = hapticStrength,
                onHapticStrengthChange = onHapticStrengthChange,
                onPreviewHapticStrength = { previewStrength ->
                    HapticEngine.trigger(context, view, true, previewStrength, HapticCue.CONFIRM)
                },
                onOpenGuide = { showGuideSheet = true },
                onBack = { showSettings = false },
                onOpenGithub = { uriHandler.openUri("https://github.com/colorbeta/GestureExplode") }
            )
        } else {
            val colorScheme = MaterialTheme.colorScheme
            val isListScrollable by remember {
                derivedStateOf {
                    searchQuery.isNotEmpty() && (listState.canScrollForward || listState.canScrollBackward)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surface)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.logobeta),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Gesture Explode",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showGuideSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = if (isChinese) "手势说明" else "Gesture Guide",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (isChinese) "设置" else "Settings",
                            tint = if (isReady) colorScheme.onSurface else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    val trackHeightPx = with(density) { maxHeight.toPx() }
                    val cardEndPaddingDp = if (isListScrollable) 24.dp else 16.dp
                    val cardEndPaddingPx = with(density) { cardEndPaddingDp.toPx() }
                    val currentContainerWidthPx by rememberUpdatedState(containerWidthPx)
                    val currentCardEndPaddingPx by rememberUpdatedState(cardEndPaddingPx)

                    val minSwipeDeletePx = with(density) { 80.dp.toPx() }
                    val upSwipeThresholdPx = with(density) { 18.dp.toPx() }
                    val writingLockDistancePx = with(density) { 24.dp.toPx() }
                    val inkStrokeWidthPx = with(density) { 5.dp.toPx() }
                    val inkGlowWidthPx = with(density) { 12.dp.toPx() }

                    // 1. Search Results Layer
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchQuery.isNotEmpty() && filteredItems.isNotEmpty(),
                        enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                            slideInVertically(initialOffsetY = { 44 }, animationSpec = tween(300, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(180)) +
                            slideOutVertically(targetOffsetY = { 36 }, animationSpec = tween(200))
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 196.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            itemsIndexed(
                                items = filteredItems,
                                key = { index, item -> "${item.type}_${item.launchData}_${item.contactId}_$index" }
                            ) { index, item ->
                                val cardShape = groupedCardShape(index, filteredItems.size)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .padding(start = 16.dp, end = cardEndPaddingDp)
                                        .clip(cardShape)
                                        .clickable { onItemClick(item) },
                                    shape = cardShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.surfaceContainerHigh
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 16.dp, end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when (item.type) {
                                            SearchItemType.CONTACT -> ContactAvatar(
                                                displayName = item.title,
                                                photoUri = item.photoUri,
                                                modifier = Modifier.size(42.dp)
                                            )
                                            SearchItemType.SETTINGS, SearchItemType.SYSTEM_ACTION -> CategoryIconBadge(
                                                icon = Icons.Default.Settings,
                                                containerColor = colorScheme.tertiaryContainer,
                                                contentColor = colorScheme.onTertiaryContainer
                                            )
                                            SearchItemType.APP, SearchItemType.ICON_PACK -> CachedAppIcon(
                                                packageName = item.launchData,
                                                modifier = Modifier.size(42.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            HighlightedText(
                                                text = item.title,
                                                query = searchQuery,
                                                initials = item.initials,
                                                initialIndices = item.initialIndices
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            val displaySubtitle = when (item.type) {
                                                SearchItemType.APP -> if (isChinese) "应用软件" else "Application"
                                                SearchItemType.ICON_PACK -> if (isChinese) "图标包" else "Icon Pack"
                                                SearchItemType.SETTINGS, SearchItemType.SYSTEM_ACTION -> if (isChinese) "系统设置" else "System Settings"
                                                SearchItemType.CONTACT -> item.subtitle
                                            }
                                            HighlightedSubtitle(displaySubtitle, searchQuery)
                                        }
                                        if (item.type == SearchItemType.CONTACT) {
                                            Spacer(Modifier.width(8.dp))
                                            // Direct Call button
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.secondaryContainer,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onRequestDialContact(item) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = if (isChinese) "拨打电话" else "Call",
                                                        tint = colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            // Direct SMS button
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.tertiaryContainer,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onSmsContact(item) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    SmsBubbleIcon(
                                                        tint = colorScheme.onTertiaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Empty Search Query Prompt
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchQuery.isEmpty(),
                        enter = fadeIn(tween(360)) + scaleIn(initialScale = 0.96f, animationSpec = tween(360, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-56).dp)
                    ) {
                        EmptyGesturePrompt(
                            isChinese = isChinese,
                            isModelReady = isReady
                        )
                    }

                    // 3. No Results State
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchQuery.isNotEmpty() && filteredItems.isEmpty(),
                        enter = fadeIn(tween(240)),
                        exit = fadeOut(tween(160)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-56).dp)
                    ) {
                        NoSearchResultsHint(
                            query = searchQuery,
                            isChinese = isChinese,
                            hasCandidates = lastCandidates.isNotEmpty()
                        )
                    }

                    // 4. Ink Canvas Layer (smooth quadratic strokes + soft glow + graceful fade-out)
                    val inkColor = colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val fade = fadingAlpha.value
                        if (fade > 0.01f && fadingPaths.isNotEmpty()) {
                            for (path in fadingPaths) {
                                drawPath(
                                    path = path,
                                    color = inkColor.copy(alpha = 0.18f * fade),
                                    style = Stroke(inkGlowWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                drawPath(
                                    path = path,
                                    color = inkColor.copy(alpha = fade),
                                    style = Stroke(inkStrokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        for (path in paths) {
                            drawPath(
                                path = path,
                                color = inkColor.copy(alpha = 0.22f),
                                style = Stroke(inkGlowWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = Stroke(inkStrokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        if (currentPath.isNotEmpty()) {
                            val livePath = buildSmoothPath(currentPath)
                            drawPath(
                                path = livePath,
                                color = inkColor.copy(alpha = 0.24f),
                                style = Stroke(inkGlowWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = livePath,
                                color = inkColor,
                                style = Stroke(inkStrokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    // 5. Full-Screen Unified Gesture Engine:
                    //    - 100% full-width handwriting
                    //    - Straight upward swipe anywhere -> instant scroll handoff + inertial fling
                    //    - 1.2s bidirectional scroll session window after scroll settles
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                val touchSlop = viewConfiguration.touchSlop
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downPos = down.position
                                    val downTime = System.currentTimeMillis()
                                    val downUptime = down.uptimeMillis
                                    val timeOffset = downTime - downUptime

                                    // Pause pending timers immediately on touch down
                                    recognizeJob?.cancel()
                                    flingJob?.cancel()
                                    scrollSessionTimeoutJob?.cancel()
                                    coroutineScope.launch { commitProgress.snapTo(0f) }

                                    val velocityTracker = VelocityTracker()
                                    velocityTracker.addPosition(down.uptimeMillis, downPos)

                                    val canTriggerUpScroll =
                                        currentQuery.isNotEmpty() &&
                                            currentFilteredItems.isNotEmpty() &&
                                            listState.canScrollForward &&
                                            paths.isEmpty()

                                    var mode = GestureCaptureMode.UNDECIDED
                                    var prevPos = downPos
                                    val pendingPoints = mutableListOf(Pair(downPos, downTime))

                                    fun enterWritingMode() {
                                        mode = GestureCaptureMode.WRITING
                                        isInScrollSession = false
                                        currentPath.clear()
                                        val sb = Ink.Stroke.builder()
                                        for ((pt, ts) in pendingPoints) {
                                            currentPath.add(pt)
                                            sb.addPoint(Ink.Point.create(pt.x, pt.y, ts))
                                        }
                                        strokeBuilder = sb
                                        view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
                                    }

                                    fun enterScrollMode(initialDeltaY: Float) {
                                        mode = GestureCaptureMode.SCROLLING
                                        isInScrollSession = true
                                        currentPath.clear()
                                        strokeBuilder = null
                                        view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.GESTURE_START, currentHapticStrength)
                                        if (abs(initialDeltaY) > 0.5f) {
                                            listState.dispatchRawDelta(-initialDeltaY)
                                        }
                                    }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                            ?: break

                                        if (!change.pressed) {
                                            break
                                        }

                                        val pos = change.position
                                        val stepDy = pos.y - prevPos.y
                                        val totalDx = pos.x - downPos.x
                                        val totalDy = pos.y - downPos.y
                                        val totalDist = hypot(totalDx, totalDy)

                                        when (mode) {
                                            GestureCaptureMode.UNDECIDED -> {
                                                velocityTracker.addPosition(change.uptimeMillis, pos)
                                                change.historical.forEach { hist ->
                                                    pendingPoints.add(Pair(hist.position, hist.uptimeMillis + timeOffset))
                                                }
                                                pendingPoints.add(Pair(pos, change.uptimeMillis + timeOffset))

                                                if (currentInScrollSession &&
                                                    currentQuery.isNotEmpty() &&
                                                    (listState.canScrollForward || listState.canScrollBackward)
                                                ) {
                                                    // Inside the 1.2s browse window: any vertical drag scrolls freely both up & down
                                                    if (totalDist > touchSlop) {
                                                        if (abs(totalDy) >= abs(totalDx) * 0.85f) {
                                                            enterScrollMode(totalDy)
                                                            change.consume()
                                                        } else {
                                                            // Distinct horizontal stroke exits browse window and writes/deletes immediately
                                                            enterWritingMode()
                                                            change.consume()
                                                        }
                                                    }
                                                } else if (canTriggerUpScroll) {
                                                    // Show subtle live ink while deciding so handwriting feels instant
                                                    if (totalDist > touchSlop * 0.5f) {
                                                        currentPath.clear()
                                                        for ((pt, _) in pendingPoints) currentPath.add(pt)
                                                    }
                                                    if (totalDy < -upSwipeThresholdPx && abs(totalDy) > abs(totalDx) * 2.4f) {
                                                        // Straight upward swipe -> instant scroll takeover
                                                        enterScrollMode(totalDy)
                                                        change.consume()
                                                    } else if (
                                                        totalDist > writingLockDistancePx ||
                                                        totalDy > touchSlop * 1.15f ||
                                                        abs(totalDx) > touchSlop * 1.15f
                                                    ) {
                                                        enterWritingMode()
                                                        change.consume()
                                                    }
                                                } else {
                                                    if (totalDist > touchSlop) {
                                                        enterWritingMode()
                                                        change.consume()
                                                    }
                                                }
                                            }

                                            GestureCaptureMode.SCROLLING -> {
                                                velocityTracker.addPosition(change.uptimeMillis, pos)
                                                if (abs(stepDy) > 0.1f) {
                                                    listState.dispatchRawDelta(-stepDy)
                                                }
                                                change.consume()
                                            }

                                            GestureCaptureMode.WRITING -> {
                                                val moveTimeOffset = System.currentTimeMillis() - change.uptimeMillis
                                                change.historical.forEach { hist ->
                                                    currentPath.add(hist.position)
                                                    strokeBuilder?.addPoint(
                                                        Ink.Point.create(
                                                            hist.position.x,
                                                            hist.position.y,
                                                            hist.uptimeMillis + moveTimeOffset
                                                        )
                                                    )
                                                }
                                                currentPath.add(pos)
                                                strokeBuilder?.addPoint(
                                                    Ink.Point.create(pos.x, pos.y, change.uptimeMillis + moveTimeOffset)
                                                )
                                                change.consume()
                                            }
                                        }

                                        prevPos = pos
                                    }

                                    // Pointer released (UP / CANCEL)
                                    when (mode) {
                                        GestureCaptureMode.UNDECIDED -> {
                                            currentPath.clear()
                                            strokeBuilder = null
                                            if (paths.isNotEmpty()) {
                                                // User tapped to draw the dot of a multi-stroke letter like 'i' or 'j'
                                                val dotEnd = if (pendingPoints.size > 1) {
                                                    pendingPoints.last().first
                                                } else {
                                                    Offset(downPos.x + 1.5f, downPos.y + 1.5f)
                                                }
                                                val dotPoints = listOf(downPos, dotEnd)
                                                paths.add(buildSmoothPath(dotPoints))
                                                val dotStroke = Ink.Stroke.builder().apply {
                                                    addPoint(Ink.Point.create(downPos.x, downPos.y, downTime))
                                                    addPoint(Ink.Point.create(dotEnd.x, dotEnd.y, System.currentTimeMillis().coerceAtLeast(downTime + 1L)))
                                                }.build()
                                                inkBuilder.addStroke(dotStroke)
                                                view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
                                                val dotDelay = if (currentSmartBoost) {
                                                    (currentWritingDelay * 0.72f).toLong().coerceAtLeast(220L)
                                                } else {
                                                    currentWritingDelay
                                                }
                                                scheduleRecognition(dotDelay)
                                            } else if (currentQuery.isNotEmpty()) {
                                                val tapY = downPos.y.toInt() - listState.layoutInfo.beforeContentPadding
                                                val tappedItem = listState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                                                    tapY in itemInfo.offset until (itemInfo.offset + itemInfo.size)
                                                }
                                                if (tappedItem != null && tappedItem.index in currentFilteredItems.indices) {
                                                    val item = currentFilteredItems[tappedItem.index]
                                                    if (item.type == SearchItemType.CONTACT) {
                                                        // Rightmost button: SMS (38dp + generous touch padding); Left of it: Call (38dp)
                                                        val cardRightPx = currentContainerWidthPx - currentCardEndPaddingPx
                                                        val smsRightPx = cardRightPx - with(density) { 8.dp.toPx() }
                                                        val smsLeftPx = smsRightPx - with(density) { 44.dp.toPx() }
                                                        val callRightPx = smsLeftPx - with(density) { 2.dp.toPx() }
                                                        val callLeftPx = callRightPx - with(density) { 44.dp.toPx() }

                                                        when {
                                                            downPos.x in smsLeftPx..smsRightPx -> onSmsContact(item)
                                                            downPos.x in callLeftPx..callRightPx -> onRequestDialContact(item)
                                                            else -> onItemClick(item)
                                                        }
                                                    } else {
                                                        onItemClick(item)
                                                    }
                                                } else if (currentInScrollSession) {
                                                    isInScrollSession = false
                                                }
                                            }
                                        }

                                        GestureCaptureMode.SCROLLING -> {
                                            val velocityY = velocityTracker.calculateVelocity().y
                                            if (abs(velocityY) > 120f) {
                                                flingJob = coroutineScope.launch {
                                                    try {
                                                        listState.scroll {
                                                            var prevValue = 0f
                                                            AnimationState(
                                                                initialValue = 0f,
                                                                initialVelocity = -velocityY
                                                            ).animateDecay(flingDecay) {
                                                                val delta = value - prevValue
                                                                prevValue = value
                                                                val consumed = scrollBy(delta)
                                                                if (abs(delta - consumed) > 1f) {
                                                                    cancelAnimation()
                                                                }
                                                            }
                                                        }
                                                    } catch (_: CancellationException) {
                                                        return@launch
                                                    }
                                                    scheduleScrollSessionTimeout()
                                                }
                                            } else {
                                                scheduleScrollSessionTimeout()
                                            }
                                        }

                                        GestureCaptureMode.WRITING -> {
                                            val strokePointsSnapshot = currentPath.toList()
                                            if (strokePointsSnapshot.isNotEmpty()) {
                                                paths.add(buildSmoothPath(strokePointsSnapshot))
                                                strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                                            }

                                            // dp-based single-stroke flat left-swipe delete
                                            if (paths.size == 1 && strokePointsSnapshot.size >= 2) {
                                                val firstPt = strokePointsSnapshot.first()
                                                val lastPt = strokePointsSnapshot.last()
                                                val dx = lastPt.x - firstPt.x
                                                val dy = lastPt.y - firstPt.y
                                                var minY = firstPt.y
                                                var maxY = firstPt.y
                                                for (pt in strokePointsSnapshot) {
                                                    if (pt.y < minY) minY = pt.y
                                                    if (pt.y > maxY) maxY = pt.y
                                                }
                                                val verticalSpan = maxY - minY

                                                if (dx < -minSwipeDeletePx &&
                                                    abs(dx) > abs(dy) * 1.8f &&
                                                    verticalSpan < abs(dx) * 0.45f
                                                ) {
                                                    if (currentQuery.isNotEmpty()) {
                                                        searchQuery = currentQuery.dropLast(1)
                                                        lastCandidates = emptyList()
                                                        view.triggerHaptic(
                                                            currentHapticsEnabled,
                                                            HapticFeedbackConstants.GESTURE_END,
                                                            currentHapticStrength
                                                        )
                                                    }
                                                    paths.clear()
                                                    inkBuilder = Ink.builder()
                                                    currentPath.clear()
                                                    strokeBuilder = null
                                                    recognizeJob?.cancel()
                                                    coroutineScope.launch { commitProgress.snapTo(0f) }
                                                    return@awaitEachGesture
                                                }
                                            }

                                            currentPath.clear()
                                            strokeBuilder = null

                                            // Adaptive smart delay: fast-track curved single-stroke letters, keep full delay for multi-stroke stems
                                            val effectiveDelay = if (currentSmartBoost) {
                                                when {
                                                    paths.size == 1 && isLikelyCompleteSingleStroke(strokePointsSnapshot, density) -> {
                                                        minOf(currentWritingDelay, 250L)
                                                    }
                                                    paths.size >= 2 -> {
                                                        (currentWritingDelay * 0.78f).toLong().coerceAtLeast(240L)
                                                    }
                                                    else -> currentWritingDelay
                                                }
                                            } else {
                                                currentWritingDelay
                                            }

                                            scheduleRecognition(effectiveDelay)
                                        }
                                    }
                                }
                            }
                    )

                    // 6. Right-Edge Draggable Fast Scroller Pill (sits above gesture pad on rightmost 28dp)
                    DraggableFastScroller(
                        listState = listState,
                        totalItems = filteredItems.size,
                        trackHeightPx = trackHeightPx,
                        visible = isListScrollable,
                        hapticsEnabled = hapticsEnabled,
                        hapticStrength = hapticStrength,
                        onDragStateChange = { dragging ->
                            isFastScrollerDragging = dragging
                            if (dragging) {
                                recognizeJob?.cancel()
                                flingJob?.cancel()
                                scrollSessionTimeoutJob?.cancel()
                                isInScrollSession = true
                            } else {
                                scheduleScrollSessionTimeout()
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    // 7. Floating Bottom Controls: Commit Progress + Scroll Session Banner / Candidate Chips + Search Pill
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val progress = commitProgress.value
                        AnimatedVisibility(
                            visible = progress > 0.01f && progress < 0.99f,
                            enter = fadeIn(tween(90)),
                            exit = fadeOut(tween(120))
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 10.dp)
                                    .width(96.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceContainerHighest)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(CircleShape)
                                        .background(colorScheme.primary)
                                )
                            }
                        }

                        // Scroll session status pill (shown during 1.2s free browse period)
                        ScrollSessionBanner(
                            visible = isInScrollSession && searchQuery.isNotEmpty(),
                            isChinese = isChinese,
                            onExitScrollSession = {
                                scrollSessionTimeoutJob?.cancel()
                                flingJob?.cancel()
                                isInScrollSession = false
                                view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
                            },
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // One-tap candidate correction chips (hidden temporarily while browsing banner is active)
                        CandidateCorrectionBar(
                            visible = searchQuery.isNotEmpty() && lastCandidates.isNotEmpty() && !isInScrollSession,
                            candidates = lastCandidates,
                            isChinese = isChinese,
                            onSelectCandidate = { replacement ->
                                if (searchQuery.isNotEmpty()) {
                                    val previousChar = searchQuery.last().toString()
                                    val newPrefix = searchQuery.dropLast(1)
                                    searchQuery = newPrefix + replacement
                                    val updatedPool = (listOf(previousChar) + lastCandidates.filter { it != replacement })
                                        .distinct()
                                        .sortedByDescending { alt ->
                                            if (alt == previousChar) 2
                                            else if (matchesAnyItem(currentAllItems, newPrefix + alt)) 1
                                            else 0
                                        }
                                        .take(4)
                                    lastCandidates = updatedPool
                                    view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.CLOCK_TICK, currentHapticStrength)
                                }
                            },
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Bottom Search Capsule + Clear Action
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(240, easing = FastOutLinearInEasing)
                            ) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = CircleShape,
                                    color = colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    tonalElevation = 6.dp,
                                    shadowElevation = 6.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    if (searchQuery.isNotEmpty()) {
                                                        searchQuery = searchQuery.dropLast(1)
                                                        lastCandidates = emptyList()
                                                        view.triggerHaptic(
                                                            currentHapticsEnabled,
                                                            HapticFeedbackConstants.SEGMENT_TICK,
                                                            currentHapticStrength
                                                        )
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BackspaceIcon(
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Text(
                                            text = searchQuery,
                                            fontSize = 23.sp,
                                            letterSpacing = 2.sp,
                                            color = colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                        ) {
                                            Text(
                                                text = "${filteredItems.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                Surface(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            searchQuery = ""
                                            lastCandidates = emptyList()
                                            isInScrollSession = false
                                            scrollSessionTimeoutJob?.cancel()
                                            flingJob?.cancel()
                                            paths.clear()
                                            currentPath.clear()
                                            inkBuilder = Ink.builder()
                                            recognizeJob?.cancel()
                                            coroutineScope.launch { commitProgress.snapTo(0f) }
                                            view.triggerHaptic(currentHapticsEnabled, HapticFeedbackConstants.REJECT, currentHapticStrength)
                                        },
                                    shape = CircleShape,
                                    color = colorScheme.primaryContainer,
                                    tonalElevation = 6.dp,
                                    shadowElevation = 6.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = if (isChinese) "清空" else "Clear",
                                            tint = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGuideSheet) {
        GestureGuideBottomSheet(
            isChinese = isChinese,
            onDismiss = { showGuideSheet = false }
        )
    }

    pendingCallContact?.let { contact ->
        val phone = contact.phoneNumber ?: contact.launchData
        AlertDialog(
            onDismissRequest = { pendingCallContact = null },
            icon = {
                ContactAvatar(
                    displayName = contact.title,
                    photoUri = contact.photoUri,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (isChinese) "确认拨打电话" else "Confirm Call",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = contact.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            pendingCallContact = null
                            onDialContact(contact)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isChinese) "拨打" else "Call")
                    }
                    FilledTonalButton(
                        onClick = { pendingCallContact = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(if (isChinese) "取消" else "Cancel")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    isChinese: Boolean,
    searchContactsEnabled: Boolean,
    onSearchContactsChange: (Boolean) -> Unit,
    searchAppsEnabled: Boolean,
    onSearchAppsChange: (Boolean) -> Unit,
    searchSettingsEnabled: Boolean,
    onSearchSettingsChange: (Boolean) -> Unit,
    searchIconPacksEnabled: Boolean,
    onSearchIconPacksChange: (Boolean) -> Unit,
    writingSpeedDelay: Long,
    onWritingSpeedChange: (Long) -> Unit,
    smartSingleStrokeBoost: Boolean,
    onSmartSingleStrokeBoostChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    hapticStrength: Float,
    onHapticStrengthChange: (Float) -> Unit,
    onPreviewHapticStrength: (Float) -> Unit,
    onOpenGuide: () -> Unit,
    onBack: () -> Unit,
    onOpenGithub: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isChinese) "设置" else "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isChinese) "返回" else "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Section 1: Search Scope
            item {
                SettingSectionHeader(if (isChinese) "搜索范围" else "Search Scope")
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        SettingSwitchRow(
                            label = if (isChinese) "联系人" else "Contacts",
                            checked = searchContactsEnabled,
                            onCheckedChange = onSearchContactsChange
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        SettingSwitchRow(
                            label = if (isChinese) "应用软件" else "Applications",
                            checked = searchAppsEnabled,
                            onCheckedChange = onSearchAppsChange
                        ) {
                            AppsGridIcon(
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        SettingSwitchRow(
                            label = if (isChinese) "系统设置" else "System Settings",
                            checked = searchSettingsEnabled,
                            onCheckedChange = onSearchSettingsChange
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        SettingSwitchRow(
                            label = if (isChinese) "图标包" else "Icon Packs",
                            checked = searchIconPacksEnabled,
                            onCheckedChange = onSearchIconPacksChange
                        ) {
                            IconPackSymbol(
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Section 2: Writing Recognition Speed + Smart Adaptive Boost
            item {
                SettingSectionHeader(if (isChinese) "写入识别速度" else "Recognition Speed")
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                            val speedOptions = listOf(
                                (if (isChinese) "快 300ms" else "Fast 300ms") to 300L,
                                (if (isChinese) "中 500ms" else "Mid 500ms") to 500L,
                                (if (isChinese) "慢 800ms" else "Slow 800ms") to 800L
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                speedOptions.forEachIndexed { index, (label, delayMs) ->
                                    SegmentedButton(
                                        selected = writingSpeedDelay == delayMs,
                                        onClick = { onWritingSpeedChange(delayMs) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = speedOptions.size)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (isChinese) {
                                    "用于等待多笔画字母（如 t / f / x）落下一笔的间隔时间。"
                                } else {
                                    "Wait window for multi-stroke letters like t, f, or x."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        ListItem(
                            modifier = Modifier.clickable { onSmartSingleStrokeBoostChange(!smartSingleStrokeBoost) },
                            headlineContent = {
                                Text(
                                    text = if (isChinese) "单笔字母智能加速" else "Smart single-stroke boost",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = if (isChinese) {
                                        "一笔成形的曲线字母（如 s / c / o / w）自动缩短至约 250ms 极速上屏"
                                    } else {
                                        "Auto-commits curved single-stroke letters in ~250ms while keeping full wait for multi-stroke stems"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = smartSingleStrokeBoost,
                                    onCheckedChange = onSmartSingleStrokeBoostChange
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            // Section 3: Appearance & Haptics
            item {
                SettingSectionHeader(if (isChinese) "外观与触感" else "Appearance & Haptics")
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                            Text(
                                text = if (isChinese) "主题模式" else "Theme Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(10.dp))
                            val themeOptions = listOf(
                                (if (isChinese) "跟随系统" else "System") to ThemeMode.SYSTEM,
                                (if (isChinese) "浅色" else "Light") to ThemeMode.LIGHT,
                                (if (isChinese) "深色" else "Dark") to ThemeMode.DARK
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                themeOptions.forEachIndexed { index, (label, mode) ->
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = { onThemeModeChange(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        SettingSwitchRow(
                            label = if (isChinese) "动态取色 (Material You)" else "Dynamic Color (Material You)",
                            supportingText = if (isChinese) "浅色模式跟随壁纸，深色模式固定高对比配色" else "Follows wallpaper in light mode; high-contrast palette in dark mode",
                            checked = dynamicColorEnabled,
                            onCheckedChange = onDynamicColorChange
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        SettingSwitchRow(
                            label = if (isChinese) "触觉反馈" else "Haptic Feedback",
                            supportingText = if (isChinese) "手写识别、退格与启动时提供微震提示" else "Subtle vibration on recognition, delete, and launch",
                            checked = hapticsEnabled,
                            onCheckedChange = onHapticsChange
                        ) {
                            HapticWaveIcon(
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(visible = hapticsEnabled) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    color = colorScheme.outlineVariant.copy(alpha = 0.28f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isChinese) "震动强度" else "Haptic Intensity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${(hapticStrength * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = hapticStrength,
                                    onValueChange = { raw ->
                                        val snapped = ((raw * 10f).roundToInt() / 10f).coerceIn(0.2f, 1.0f)
                                        if (snapped != hapticStrength) {
                                            onHapticStrengthChange(snapped)
                                            onPreviewHapticStrength(snapped)
                                        }
                                    },
                                    valueRange = 0.2f..1.0f,
                                    steps = 7,
                                    onValueChangeFinished = {
                                        onPreviewHapticStrength(hapticStrength)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Help & About
            item {
                SettingSectionHeader(if (isChinese) "帮助与关于" else "Help & About")
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onOpenGuide),
                        headlineContent = {
                            Text(
                                text = if (isChinese) "手势交互速览" else "Gesture Guide",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = if (isChinese) "查看全屏手写与上滑翻页操作" else "Tips for full-screen writing and scrolling",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // Footer Version & GitHub
            item {
                val packageInfo = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                }
                val vName = packageInfo?.versionName ?: "1.7"
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: BUILD_VERSION.toLong()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toLong() ?: BUILD_VERSION.toLong()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenGithub)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.github),
                        contentDescription = "GitHub Repository",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Gesture Explode v$vName (Build $vCode)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}