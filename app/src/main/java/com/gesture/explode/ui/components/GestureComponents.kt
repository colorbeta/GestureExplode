package com.gesture.explode.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * In-memory LRU cache for app launcher icons to eliminate flicker during fast list scrolling.
 */
object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(120)

    fun get(packageName: String): ImageBitmap? = cache.get(packageName)

    fun put(packageName: String, bitmap: ImageBitmap) {
        cache.put(packageName, bitmap)
    }
}

/**
 * In-memory LRU cache for contact thumbnail photos.
 */
object ContactAvatarCache {
    private val cache = LruCache<String, ImageBitmap>(120)

    fun get(uri: String): ImageBitmap? = cache.get(uri)

    fun put(uri: String, bitmap: ImageBitmap) {
        cache.put(uri, bitmap)
    }
}

@Composable
fun CachedAppIcon(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageBitmap by remember(packageName) { mutableStateOf(AppIconCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (imageBitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    drawable.toBitmap(120, 120).asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                AppIconCache.put(packageName, loaded)
                imageBitmap = loaded
            }
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            AppsGridIcon(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Displays the contact's actual thumbnail photo when available,
 * falling back to a crisp M3 monogram badge using the first character of the name.
 */
@Composable
fun ContactAvatar(
    displayName: String,
    photoUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var bitmap by remember(photoUri) {
        mutableStateOf(if (photoUri != null) ContactAvatarCache.get(photoUri) else null)
    }

    LaunchedEffect(photoUri) {
        if (photoUri != null && bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                ContactAvatarCache.put(photoUri, loaded)
                bitmap = loaded
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        val monogram = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monogram,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Clean vector SMS chat-bubble icon drawn with Canvas (no extra icon library required).
 */
@Composable
fun SmsBubbleIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.095f
        val bubble = Path().apply {
            moveTo(w * 0.22f, h * 0.16f)
            lineTo(w * 0.78f, h * 0.16f)
            quadraticTo(w * 0.90f, h * 0.16f, w * 0.90f, h * 0.28f)
            lineTo(w * 0.90f, h * 0.62f)
            quadraticTo(w * 0.90f, h * 0.74f, w * 0.78f, h * 0.74f)
            lineTo(w * 0.40f, h * 0.74f)
            lineTo(w * 0.16f, h * 0.88f)
            lineTo(w * 0.20f, h * 0.72f)
            quadraticTo(w * 0.10f, h * 0.70f, w * 0.10f, h * 0.60f)
            lineTo(w * 0.10f, h * 0.28f)
            quadraticTo(w * 0.10f, h * 0.16f, w * 0.22f, h * 0.16f)
            close()
        }
        drawPath(
            path = bubble,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Subtle message lines inside bubble
        drawLine(tint, Offset(w * 0.28f, h * 0.39f), Offset(w * 0.72f, h * 0.39f), strokeW * 0.85f, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.28f, h * 0.55f), Offset(w * 0.56f, h * 0.55f), strokeW * 0.85f, StrokeCap.Round)
    }
}

/**
 * Clean 2x2 rounded-square grid icon representing Applications (replaces Icons.Default.Face).
 */
@Composable
fun AppsGridIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gap = size.width * 0.14f
        val cell = (size.width - gap) / 2f
        val radius = CornerRadius(cell * 0.28f, cell * 0.28f)
        drawRoundRect(tint, Offset(0f, 0f), Size(cell, cell), radius)
        drawRoundRect(tint, Offset(cell + gap, 0f), Size(cell, cell), radius)
        drawRoundRect(tint, Offset(0f, cell + gap), Size(cell, cell), radius)
        drawRoundRect(tint, Offset(cell + gap, cell + gap), Size(cell, cell), radius)
    }
}

/**
 * Clean stacked-cards / icon-pack symbol drawn with Canvas for the Icon Packs setting row.
 */
@Composable
fun IconPackSymbol(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.10f
        // Back offset frame
        drawRoundRect(
            color = tint.copy(alpha = 0.45f),
            topLeft = Offset(w * 0.26f, h * 0.08f),
            size = Size(w * 0.66f, h * 0.66f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = strokeW)
        )
        // Front primary icon tile
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.26f),
            size = Size(w * 0.66f, h * 0.66f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = strokeW)
        )
        // Center dot representing stylized icon glyph
        drawCircle(
            color = tint,
            radius = w * 0.11f,
            center = Offset(w * 0.41f, h * 0.59f)
        )
    }
}

/**
 * Minimal vector backspace icon drawn with Canvas so no extra icon library dependency is required.
 */
@Composable
fun BackspaceIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val outline = Path().apply {
            moveTo(w * 0.34f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.80f)
            lineTo(w * 0.34f, h * 0.80f)
            lineTo(w * 0.10f, h * 0.50f)
            close()
        }
        drawPath(outline, color = tint, style = stroke)

        // Inner X mark
        drawLine(
            color = tint,
            start = Offset(w * 0.48f, h * 0.38f),
            end = Offset(w * 0.72f, h * 0.62f),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.72f, h * 0.38f),
            end = Offset(w * 0.48f, h * 0.62f),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Clean vibration/haptic icon drawn with Canvas for the Haptic Feedback setting row.
 */
@Composable
fun HapticWaveIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.09f
        // Center phone body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.16f),
            size = Size(w * 0.32f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
            style = Stroke(width = strokeW)
        )
        // Left wave lines
        drawLine(tint, Offset(w * 0.21f, h * 0.28f), Offset(w * 0.21f, h * 0.72f), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.10f, h * 0.38f), Offset(w * 0.10f, h * 0.62f), strokeW, StrokeCap.Round)
        // Right wave lines
        drawLine(tint, Offset(w * 0.79f, h * 0.28f), Offset(w * 0.79f, h * 0.72f), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.90f, h * 0.38f), Offset(w * 0.90f, h * 0.62f), strokeW, StrokeCap.Round)
    }
}

/**
 * M3 Expressive grouped card shape: rounded outer corners, tighter inner corners.
 */
fun groupedCardShape(index: Int, totalCount: Int): RoundedCornerShape {
    return when {
        totalCount <= 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )
        index == totalCount - 1 -> RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )
        else -> RoundedCornerShape(8.dp)
    }
}

/**
 * High-contrast, inviting empty state cue with subtle breathing gesture glyph.
 */
@Composable
fun EmptyGesturePrompt(
    isChinese: Boolean,
    isModelReady: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "gesture_prompt_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer.copy(alpha = 0.35f))
                .border(
                    width = 1.dp,
                    color = colorScheme.primary.copy(alpha = 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.72f, h * 0.22f)
                    cubicTo(w * 0.22f, h * 0.14f, w * 0.18f, h * 0.46f, w * 0.52f, h * 0.50f)
                    cubicTo(w * 0.86f, h * 0.54f, w * 0.82f, h * 0.86f, w * 0.28f, h * 0.80f)
                }
                drawPath(
                    path = path,
                    color = colorScheme.primary.copy(alpha = pulseAlpha),
                    style = Stroke(
                        width = w * 0.11f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawCircle(
                    color = colorScheme.primary.copy(alpha = pulseAlpha),
                    radius = w * 0.08f,
                    center = Offset(w * 0.28f, h * 0.80f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (isChinese) "画一个字符开始搜索" else "Draw a character to search",
            style = MaterialTheme.typography.headlineSmall,
            color = colorScheme.onSurface.copy(alpha = 0.82f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isChinese) {
                "全屏手写 · 上滑翻页 · 左滑退格"
            } else {
                "Write anywhere · Swipe up to scroll · Swipe left to delete"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )

        if (!isModelReady) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = CircleShape,
                color = colorScheme.secondaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = if (isChinese) "正在初始化离线手写引擎..." else "Preparing offline ink engine...",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Friendly empty search result feedback instead of a blank screen.
 */
@Composable
fun NoSearchResultsHint(
    query: String,
    isChinese: Boolean,
    hasCandidates: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isChinese) "未找到与「$query」匹配的结果" else "No matches for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isChinese) {
                if (hasCandidates) "可点击下方候选字符快速替换，或向左滑动退格"
                else "向左平划退格，或点击右下角清空重写"
            } else {
                if (hasCandidates) "Tap a candidate chip below to replace, or swipe left to delete"
                else "Swipe left to delete or tap clear below"
            },
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Floating one-tap candidate correction bar above the bottom search pill.
 */
@Composable
fun CandidateCorrectionBar(
    visible: Boolean,
    candidates: List<String>,
    isChinese: Boolean,
    onSelectCandidate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible && candidates.isNotEmpty(),
        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(220)),
        exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(150)),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isChinese) "候选" else "Alt",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 2.dp)
                ) {
                    items(candidates) { cand ->
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectCandidate(cand) }
                        ) {
                            Text(
                                text = cand.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subtle floating indicator shown during the 1.2s free bidirectional scroll session.
 * Tapping it returns immediately to handwriting mode.
 */
@Composable
fun ScrollSessionBanner(
    visible: Boolean,
    isChinese: Boolean,
    onExitScrollSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(180)),
        exit = fadeOut(tween(220)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.primaryContainer.copy(alpha = 0.92f),
            border = BorderStroke(0.5.dp, colorScheme.primary.copy(alpha = 0.35f)),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onExitScrollSession)
        ) {
            Text(
                text = if (isChinese) "列表浏览中 · 上下自由滑动 · 轻触恢复手写" else "Browsing list · Scroll freely · Tap to write",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Right-edge draggable Material 3 pill Fast Scroller.
 * Visible whenever search results overflow the viewport.
 */
@Composable
fun DraggableFastScroller(
    listState: LazyListState,
    totalItems: Int,
    trackHeightPx: Float,
    visible: Boolean,
    hapticsEnabled: Boolean,
    hapticStrength: Float,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentTotalItems by rememberUpdatedState(totalItems)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)
    val currentHapticStrength by rememberUpdatedState(hapticStrength)

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 6.dp,
        animationSpec = tween(140),
        label = "fast_scroller_width"
    )

    AnimatedVisibility(
        visible = visible && totalItems > 0,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(220)),
        modifier = modifier
    ) {
        val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
        if (visibleItemsInfo.isNotEmpty()) {
            val visibleCount = visibleItemsInfo.size.toFloat()
            val maxScrollableIndex = (totalItems - visibleCount).coerceAtLeast(1f)
            val firstItemInfo = visibleItemsInfo.first()
            val exactScrollIndex = firstItemInfo.index.toFloat() +
                (abs(firstItemInfo.offset).toFloat() / firstItemInfo.size.coerceAtLeast(1))

            val topMarginPx = with(density) { 12.dp.toPx() }
            val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
            val bottomReservePx = with(density) { 156.dp.toPx() } + navBarBottomPx
            val effectiveTrackPx = (trackHeightPx - topMarginPx - bottomReservePx).coerceAtLeast(120f)
            val thumbHeightPx = (effectiveTrackPx * (visibleCount / totalItems.coerceAtLeast(1)))
                .coerceIn(with(density) { 44.dp.toPx() }, effectiveTrackPx * 0.55f)
            val usableSpanPx = (effectiveTrackPx - thumbHeightPx).coerceAtLeast(1f)

            val scrollFraction = if (isDragging) {
                dragFraction
            } else {
                (exactScrollIndex / maxScrollableIndex).coerceIn(0f, 1f)
            }
            val thumbOffsetPx = topMarginPx + usableSpanPx * scrollFraction

            val currentTopMarginPx by rememberUpdatedState(topMarginPx)
            val currentThumbHeightPx by rememberUpdatedState(thumbHeightPx)
            val currentUsableSpanPx by rememberUpdatedState(usableSpanPx)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { startOffset ->
                                isDragging = true
                                onDragStateChange(true)
                                HapticEngine.trigger(
                                    context = context,
                                    view = view,
                                    enabled = currentHaptics,
                                    strength = currentHapticStrength,
                                    cue = HapticCue.SCROLL_START
                                )
                                val f = ((startOffset.y - currentTopMarginPx - currentThumbHeightPx / 2f) / currentUsableSpanPx)
                                    .coerceIn(0f, 1f)
                                dragFraction = f
                                val target = (f * (currentTotalItems - 1).coerceAtLeast(0)).roundToInt()
                                coroutineScope.launch { listState.scrollToItem(target) }
                            },
                            onDragEnd = {
                                isDragging = false
                                onDragStateChange(false)
                            },
                            onDragCancel = {
                                isDragging = false
                                onDragStateChange(false)
                            },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val f = ((change.position.y - currentTopMarginPx - currentThumbHeightPx / 2f) / currentUsableSpanPx)
                                    .coerceIn(0f, 1f)
                                dragFraction = f
                                val target = (f * (currentTotalItems - 1).coerceAtLeast(0)).roundToInt()
                                coroutineScope.launch { listState.scrollToItem(target) }
                            }
                        )
                    }
            ) {
                // Visual pill thumb aligned cleanly to right edge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp)
                        .offset(y = with(density) { thumbOffsetPx.toDp() })
                        .width(thumbWidth)
                        .height(with(density) { thumbHeightPx.toDp() })
                        .clip(CircleShape)
                        .background(
                            if (isDragging) colorScheme.primary
                            else colorScheme.primary.copy(alpha = if (listState.isScrollInProgress) 0.72f else 0.36f)
                        )
                )
            }
        }
    }
}

/**
 * Material 3 Settings switch row supporting either an ImageVector or a custom leading icon composable.
 */
@Composable
fun SettingSwitchRow(
    label: String,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp)
    )
}

/**
 * Material 3 ModalBottomSheet showing concise gesture usage tips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureGuideBottomSheet(
    isChinese: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isChinese) "手势交互速览" else "Gesture Guide",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            val tips = if (isChinese) {
                listOf(
                    "全屏自由手写" to "屏幕任意位置均可直接手写字母、数字或中文拼音首字母，多笔画会自动合并识别。",
                    "垂直上滑翻页（1.2秒自由浏览）" to "结果超出一屏时，在屏幕任意位置垂直向上滑即刻切换流畅滚动；停稳后 1.2 秒内上下往返自由翻页，也可拖动右侧快速滑块直达任意位置。",
                    "单笔向左平划退格" to "水平向左滑过屏幕可删除最后一个字符；也可直接点击底部胶囊左侧的退格键。",
                    "候选字符一键纠错" to "遇到 f/t、d/o 等易混淆字母识别偏差时，点击搜索框上方的候选气泡即可直接替换末位。"
                )
            } else {
                listOf(
                    "Full-screen writing" to "Write letters, digits, or initials anywhere on screen. Multi-stroke characters merge automatically.",
                    "Swipe up to scroll + Fast Scroller" to "Swipe straight up anywhere to scroll smoothly with a 1.2s bidirectional browse window, or drag the right-edge pill handle.",
                    "Swipe left to backspace" to "Draw a flat single stroke to the left to remove the last character, or tap the backspace key below.",
                    "One-tap candidate fix" to "If similar letters like f/t or d/o are misread, tap a candidate chip above the search pill to swap instantly."
                )
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    tips.forEachIndexed { idx, (title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        if (idx < tips.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category icon badge used inside search result cards.
 */
@Composable
fun CategoryIconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
