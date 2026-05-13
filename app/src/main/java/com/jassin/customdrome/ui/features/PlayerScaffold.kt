package com.jassin.customdrome.ui.features

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jassin.customdrome.ui.common.TabsBar
import com.jassin.customdrome.ui.common.TopBar
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

// Height constants shared between scaffold and content padding
private val MiniPlayerHeight = 72.dp
private val BottomNavHeight = 80.dp

private fun Float.powCurve(exponent: Float): Float =
    this
        .coerceIn(0f, 1f)
        .toDouble()
        .pow(exponent.toDouble())
        .toFloat()

@Composable
fun PlayerScaffold(
    navController: NavHostController,
    showNavBars: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    val isSongPlaying = true // ← swap for real state later

    // 0 = fully collapsed, 1 = fully expanded.
    // snapTo() on every drag frame so the sheet follows the finger exactly,
    // then animateTo() the nearest target on release for a springy snap.
    val expandProgress = remember { Animatable(0f, Float.VectorConverter) }
    val scope = rememberCoroutineScope()

    // Top Navbar (only show on the main pages)
    if (showNavBars) {
        TopBar(onGoToSettings = { navController.navigate(route = "settings") })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = constraints.maxHeight.toFloat()

        val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }
        val bottomNavHeightPx = with(density) { BottomNavHeight.toPx() }

        // Total travel distance of the sheet top edge (collapsed→expanded).
        val travelPx =
            screenHeightPx - miniPlayerHeightPx - bottomNavHeightPx - with(density) { 15.dp.toPx() }

        // ── Main content ──────────────────────────────────────────────────────
        content(PaddingValues(bottom = BottomNavHeight))

        // ── Bottom navigation bar ─────────────────────────────────────────────
        if (showNavBars) {
            val navEasing = expandProgress.value * 16 * expandProgress.value
            val navYOffsetPx = (navEasing * bottomNavHeightPx).roundToInt()
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter),
                // .offset { IntOffset(0, navYOffsetPx) },
            ) {
                TabsBar(navController)
            }

            // ── Player surface ────────────────────────────────────────────────────
            if (isSongPlaying) {
                val progress = expandProgress.value
                val p = progress // still Float, unchanged

                val topCurve = 1f // 1.0 = linear
                val heightCurve = 0.72f // < 1 = faster early growth, > 1 = slower early growth

                val topP = p.powCurve(topCurve)
                val heightP = p.powCurve(heightCurve)

                val playerTopPx = travelPx * (1f - topP)
                val playerHeightPx =
                    miniPlayerHeightPx + heightP * (screenHeightPx - miniPlayerHeightPx)

                val cornerRadius = (16.dp * (1f - progress)).coerceAtLeast(0.dp)

                var startProgress by remember { mutableFloatStateOf(0f) }

                // Re-register the BackHandler on every navigation change so it always
                // sits on top of the OnBackPressedDispatcher stack (LIFO wins).
                val currentEntry by navController.currentBackStackEntryAsState()
                key(currentEntry) {
                    BackHandler(enabled = expandProgress.value > 0.5f) {
                        scope.launch { expandProgress.animateTo(0f, tween(300)) }
                    }
                }

                PlayerSurface(
                    progress = progress,
                    playerHeightPx = playerHeightPx,
                    playerTopPx = playerTopPx,
                    cornerRadius = cornerRadius,
                    onCollapse = {
                        scope.launch { expandProgress.animateTo(0f, tween(300)) }
                    },
                    modifier =
                        Modifier
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        scope.launch { expandProgress.stop() }
                                        startProgress = expandProgress.value
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val delta = -dragAmount / travelPx
                                        scope.launch {
                                            expandProgress.snapTo(
                                                (expandProgress.value + delta).coerceIn(
                                                    0f,
                                                    1f,
                                                ),
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        val target =
                                            if (expandProgress.value > startProgress) 1f else 0f
                                        scope.launch {
                                            expandProgress.animateTo(
                                                target,
                                                tween(60, easing = LinearEasing),
                                            )
                                        }
                                    },
                                )
                            }.clickable(
                                enabled = progress < 0.5f,
                                // This tracks the interaction state (press, drag, etc.)
                                interactionSource = remember { MutableInteractionSource() },
                                // This defines the visual effect. Setting it to null removes the circle/ripple.
                                indication = null,
                            ) {
                                scope.launch { expandProgress.animateTo(1f, tween(150)) }
                            },
                )
            }
        }
    }
}

@Composable
fun MiniPlayerContent() {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Reserve room for the floating album cover (48 dp) + 8 dp breathing room
        Spacer(modifier = Modifier.width(56.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Song Title",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = "Artist Name",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play / Pause")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun FullPlayerContent(
    onCollapse: () -> Unit,
    progress: Float,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize(),
        // .padding(horizontal = 24.dp)
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        /*Row(
            modifier = Modifier.padding(top = 32.dp),
        ) {
            Text("hi")
        }*/

        Spacer(modifier = Modifier.size(280.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Song Title", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Artist Name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            FilledIconButton(onClick = {}, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play / Pause",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = {}, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
