package dev.tvlauncher.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.tvlauncher.core.catalog.InstalledApp
import dev.tvlauncher.core.config.Pattern
import dev.tvlauncher.core.config.ThemeSpec
import dev.tvlauncher.ui.home.Clock
import dev.tvlauncher.ui.home.NightBadge
import dev.tvlauncher.ui.theme.Emphasized
import dev.tvlauncher.ui.theme.Poppins
import dev.tvlauncher.ui.theme.ThemedBackground
import dev.tvlauncher.ui.theme.forNight
import dev.tvlauncher.ui.theme.rememberIsNight
import dev.tvlauncher.ui.theme.rememberReduceMotion
import dev.tvlauncher.ui.theme.rememberSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Gutter = 64.dp
private val CardRadius = 14.dp
private const val FOCUS_SCALE = 1.08f
private const val FOCUS_MS = 150
private val NightFill = Color(0xFFD6D3DD)
private val AllAppsTheme = ThemeSpec("all-apps", 0xFF454A53, 0xFF16191E, 0xFFFFFFFF, Pattern.TILES)

private val TitleStyle = TextStyle(
    fontFamily = Poppins,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 52.sp,
    letterSpacing = (-0.8).sp,
    color = Color.White,
    shadow = Shadow(Color.Black.copy(alpha = 0.22f), Offset(0f, 2f), 16f),
)
private val HintStyle = TextStyle(
    fontFamily = Poppins,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    color = Color.White.copy(alpha = 0.7f),
)

/**
 * Every TV app in a 5-column grid of 16:9 glass cards. [icon] is called lazily, per visible card,
 * off the main thread. [failure] is shown in a pill at the bottom when non-null.
 */
@Composable
fun AllAppsScreen(
    apps: List<InstalledApp>,
    icon: (String) -> ImageBitmap?,
    onOpen: (InstalledApp) -> Unit,
    onBack: () -> Unit,
    failure: String? = null,
    loading: Boolean = false,
) {
    BackHandler(onBack = onBack)
    val night = rememberIsNight()
    val showBadge = rememberSetting { it.showNightBadge }
    val reduceMotion = rememberReduceMotion() || rememberSetting { it.reduceMotion }
    val requester = remember { FocusRequester() }
    val firstKey = apps.firstOrNull()?.packageName
    LaunchedEffect(firstKey) {
        if (firstKey != null) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ThemedBackground(if (night) AllAppsTheme.forNight() else AllAppsTheme, night = night)

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = Gutter, top = 40.dp)) {
                Text("All apps", style = TitleStyle, maxLines = 1)
                Text(
                    if (loading) "Loading apps…" else "${apps.size} apps · Press OK to open · Back to return",
                    style = HintStyle,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp),
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 16.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppCard(
                        app = app,
                        icon = icon,
                        reduceMotion = reduceMotion,
                        night = night,
                        onClick = remember(app) { { onOpen(app) } },
                        modifier = if (app.packageName == firstKey) Modifier.focusRequester(requester) else Modifier,
                    )
                }
            }
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
                style = HintStyle.copy(color = Color.White, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AppCard(
    app: InstalledApp,
    icon: (String) -> ImageBitmap?,
    reduceMotion: Boolean,
    night: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val t by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(FOCUS_MS, easing = Emphasized),
        label = "appFocus",
    )
    val art by produceState<ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.Default) { icon(app.packageName) }
    }
    val shape = RoundedCornerShape(CardRadius)
    val focusFill = if (night) NightFill else Color.White

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer {
                    val s = if (reduceMotion) 1f else 1f + (FOCUS_SCALE - 1f) * t
                    scaleX = s
                    scaleY = s
                    shadowElevation = 16.dp.toPx() * t
                    this.shape = shape
                    clip = true
                }
                .drawBehind {
                    val r = CornerRadius(CardRadius.toPx())
                    if (t < 1f) {
                        val a = 1f - t
                        drawRoundRect(Color.Black.copy(alpha = 0.26f * a), cornerRadius = r)
                        drawRoundRect(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.10f * a),
                                0.6f to Color.White.copy(alpha = 0.03f * a),
                            ),
                            cornerRadius = r,
                        )
                        val sw = 1.dp.toPx()
                        drawRoundRect(
                            Color.White.copy(alpha = 0.12f * a),
                            topLeft = Offset(sw / 2, sw / 2),
                            size = Size(this.size.width - sw, this.size.height - sw),
                            cornerRadius = CornerRadius(r.x - sw / 2),
                            style = Stroke(sw),
                        )
                    }
                    if (t > 0f) drawRoundRect(focusFill.copy(alpha = t), cornerRadius = r)
                }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        ) {
            val a = art
            if (a != null) {
                if (a.width.toFloat() / a.height > 1.2f) {
                    Image(
                        a, null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(shape),
                    )
                    val dark = if (night) 0.5f else 0.35f
                    Box(
                        Modifier.fillMaxSize().drawBehind {
                            val alpha = dark * (1f - t)
                            if (alpha > 0f) drawRect(Color.Black.copy(alpha = alpha))
                        },
                    )
                } else {
                    // Square icon at 48% of the card height, centred.
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            a, null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxHeight(0.48f).aspectRatio(1f),
                        )
                    }
                }
            }
        }
        Text(
            app.label,
            color = Color.White.copy(alpha = if (focused) 1f else 0.62f),
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
    }
}
