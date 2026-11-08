package dev.tvlauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Text
import dev.tvlauncher.core.config.ConfigParser
import dev.tvlauncher.core.home.HomeController
import dev.tvlauncher.core.home.HomeTile
import dev.tvlauncher.core.home.LaunchResult
import dev.tvlauncher.ui.theme.EaseOut
import dev.tvlauncher.ui.theme.Ink
import dev.tvlauncher.ui.theme.Poppins
import dev.tvlauncher.ui.theme.ThemedBackground
import dev.tvlauncher.ui.theme.forNight
import dev.tvlauncher.ui.theme.rememberIsNight
import dev.tvlauncher.ui.theme.rememberReduceMotion
import dev.tvlauncher.ui.theme.rememberSetting
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Layout, in dp on the TV's 960 x 540 dp screen. Proportions measured from the reference reel.
private val Gutter = 64.dp
private val TileSize = 156.dp
private val TileGap = 16.dp
private val RowPadV = 24.dp // room for the focused tile's scale and shadow
private const val ROW_BOTTOM_FRACTION = 0.80f // focused-row tile bottoms sit at 80% of the height

private val TitleShadow = Shadow(Color.Black.copy(alpha = 0.22f), Offset(0f, 2f), 16f)
private val TitleStyle = TextStyle(
    fontFamily = Poppins,
    fontWeight = FontWeight.Bold,
    fontSize = 60.sp,
    lineHeight = 68.sp,
    letterSpacing = (-1.2).sp,
    color = Color.White,
    shadow = TitleShadow,
)
private val BodyStyle = TextStyle(
    fontFamily = Poppins,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    color = Color.White.copy(alpha = 0.74f),
)

/**
 * Reel-style home: themed background, the focused tile's title and hint, one row of square tiles,
 * and a clock. [homeSignal] increments when Home is pressed while already home; focus then returns
 * to the first tile.
 */
@Composable
fun HomeScreen(
    controller: HomeController,
    icon: (String) -> ImageBitmap?,
    onShowAllApps: () -> Unit,
    homeSignal: Int = 0,
) {
    var tiles by remember { mutableStateOf(controller.tiles()) }
    var focusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var switchingTo by remember { mutableStateOf<HomeTile?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var focusTick by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val requester = remember { FocusRequester() }
    val systemReduceMotion = rememberReduceMotion()
    val reduceMotion = systemReduceMotion || rememberSetting { it.reduceMotion }
    val night = rememberIsNight()
    val showBadge = rememberSetting { it.showNightBadge }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tiles = controller.tiles()
                switchingTo = null
                focusTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            focusedKey = tiles.firstOrNull()?.key
            listState.scrollToItem(0)
            focusTick++
        }
    }
    LaunchedEffect(focusTick) {
        // Wait a frame so the item holding the requester has been composed, then focus it.
        withFrameNanos { }
        runCatching { requester.requestFocus() }
    }
    LaunchedEffect(failure) {
        if (failure != null) {
            delay(3000)
            failure = null
        }
    }

    val selectedKey = focusedKey?.takeIf { k -> tiles.any { it.key == k } } ?: tiles.firstOrNull()?.key
    val focused = tiles.firstOrNull { it.key == selectedKey }

    val homeAlpha by animateFloatAsState(
        targetValue = if (switchingTo == null) 1f else 0f,
        animationSpec = tween(150, easing = EaseOut),
        label = "homeAlpha",
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val h = maxHeight
        val allThemes = remember(tiles) { tiles.map { it.theme }.distinctBy { it.id } }
        val current = switchingTo?.theme ?: focused?.theme ?: ConfigParser.DEFAULT_THEME
        ThemedBackground(
            if (night) current.forNight() else current,
            prefetch = allThemes,
            night = night,
        )

        // Title, hint and row share one column anchored so the tiles end at 80% of the height.
        Column(
            Modifier
                .fillMaxWidth()
                .height(h * ROW_BOTTOM_FRACTION + RowPadV)
                .graphicsLayer { alpha = homeAlpha },
            verticalArrangement = Arrangement.Bottom,
        ) {
            Crossfade(
                targetState = focused,
                animationSpec = tween(150),
                label = "title",
                modifier = Modifier.padding(start = Gutter, end = Gutter),
            ) { tile ->
                Column {
                    Text(tile?.label.orEmpty(), style = TitleStyle, maxLines = 1)
                    HintLine(
                        if (tile?.available == false) "Not available on this TV" else tile?.hint.orEmpty(),
                        Modifier.padding(top = 2.dp, start = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp - RowPadV / 2))
            TileRow(
                tiles = tiles,
                selectedKey = selectedKey,
                requester = requester,
                icon = icon,
                reduceMotion = reduceMotion,
                night = night,
                state = listState,
                onFocused = { focusedKey = it.key },
                onOpen = { tile ->
                    when (val r = controller.open(tile)) {
                        LaunchResult.Opened -> switchingTo = tile
                        LaunchResult.ShowAllApps -> onShowAllApps()
                        is LaunchResult.Failed -> failure = r.message
                    }
                },
            )
        }

        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = Gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(night && showBadge, enter = fadeIn(tween(300)), exit = fadeOut(tween(150))) {
                NightBadge(Modifier.padding(end = 12.dp))
            }
            Clock()
        }

        AnimatedVisibility(
            visible = failure != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        ) {
            Text(
                failure.orEmpty(),
                style = BodyStyle.copy(color = Color.White, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }

        switchingTo?.let { SwitchingOverlay(it, h, reduceMotion) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileRow(
    tiles: List<HomeTile>,
    selectedKey: String?,
    requester: FocusRequester,
    icon: (String) -> ImageBitmap?,
    reduceMotion: Boolean,
    night: Boolean,
    state: androidx.compose.foundation.lazy.LazyListState,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
) {
    // Keep the focused tile at least one gutter away from either screen edge when the row scrolls.
    val gutterPx = with(LocalDensity.current) { (Gutter - 8.dp).toPx() }
    val spec = remember(gutterPx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val end = offset + size
                return when {
                    offset < gutterPx -> offset - gutterPx
                    end > containerSize - gutterPx -> end - (containerSize - gutterPx)
                    else -> 0f
                }
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Gutter, vertical = RowPadV),
            horizontalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            items(tiles, key = { it.key }) { tile ->
                TileCard(
                    tile = tile,
                    size = TileSize,
                    icon = icon,
                    reduceMotion = reduceMotion,
                    night = night,
                    onFocused = remember(tile) { { onFocused(tile) } },
                    onClick = remember(tile) { { onOpen(tile) } },
                    modifier = if (tile.key == selectedKey) Modifier.focusRequester(requester) else Modifier,
                )
            }
        }
    }
}

/** Hint text; the word OK is drawn as a small outlined key cap. */
@Composable
private fun HintLine(hint: String, modifier: Modifier = Modifier) {
    val parts = hint.split("OK", limit = 2)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (parts.size == 2) {
            Text(parts[0].trimEnd(), style = BodyStyle)
            KeyCap("OK", Modifier.padding(horizontal = 6.dp))
            Text(parts[1].trimStart(), style = BodyStyle)
        } else {
            Text(hint, style = BodyStyle)
        }
    }
}

@Composable
private fun KeyCap(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        style = BodyStyle.copy(fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold),
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/**
 * "Switching to <name>" shown while the app or input opens. Chunks enter staggered 100 ms apart
 * (opacity + 12 dp rise, 300 ms ease-out); the underline then fills like a progress bar.
 * Reduced motion: opacity only, no rise, no fill.
 */
@Composable
private fun SwitchingOverlay(tile: HomeTile, h: Dp, reduceMotion: Boolean) {
    val chunks = remember(tile.key) { List(4) { Animatable(0f) } }
    val fill = remember(tile.key) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(tile.key) {
        chunks.forEachIndexed { i, a ->
            launch {
                delay(100L + i * 100L)
                a.animateTo(1f, tween(300, easing = EaseOut))
            }
        }
        if (!reduceMotion) {
            delay(300)
            fill.animateTo(1f, tween(900, easing = EaseOut))
        }
    }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    fun Modifier.chunk(i: Int) = graphicsLayer {
        val p = chunks[i].value
        alpha = p
        translationY = if (reduceMotion) 0f else (1f - p) * rise
    }

    val mark = remember(tile.key) { markFor(tile) }
    Column(
        Modifier.fillMaxSize().padding(start = Gutter, top = h * 0.24f),
    ) {
        Box(Modifier.chunk(0).height(64.dp)) {
            if (mark != null) {
                MarkImage(
                    mark,
                    Color.White,
                    if (mark.wide) Modifier.size(140.dp, 44.dp) else Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.chunk(1)) {
            Text("Switching to", style = BodyStyle.copy(fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f)))
            Text(tile.label, style = TitleStyle, maxLines = 1)
        }
        Box(
            Modifier.chunk(2).padding(top = 8.dp, start = 3.dp).width(280.dp).height(3.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50)),
        ) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = fill.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }.background(
                    Brush.horizontalGradient(listOf(Color.White, Color.White.copy(alpha = 0.7f))),
                    RoundedCornerShape(50),
                ),
            )
        }
        Row(
            Modifier.chunk(3).padding(top = 40.dp, start = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹  Back",
                style = BodyStyle.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = Ink),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Text(
                "to return to the launcher",
                style = BodyStyle.copy(fontSize = 12.sp),
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
