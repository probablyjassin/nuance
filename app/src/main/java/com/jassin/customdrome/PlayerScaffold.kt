package com.jassin.customdrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Height constants shared between scaffold and content padding
private val MiniPlayerHeight = 72.dp
private val BottomNavHeight = 80.dp

@Composable
fun PlayerScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val isSongPlaying = true // ← swap for real state later

    // Single source of truth: 0 = fully collapsed, 1 = fully expanded.
    // We snapTo() it on every drag frame so the sheet follows the finger exactly,
    // then animateTo() the nearest target on release for a springy snap.
    val expandProgress = remember { Animatable(0f, Float.VectorConverter) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = constraints.maxHeight.toFloat()

        val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }
        val bottomNavHeightPx = with(density) { BottomNavHeight.toPx() }

        // Total travel distance of the sheet top edge (collapsed→expanded).
        val travelPx = screenHeightPx - miniPlayerHeightPx - bottomNavHeightPx - 55f

        // ── Main content ──────────────────────────────────────────────────────
        content(PaddingValues(bottom = BottomNavHeight))

        // ── Bottom navigation bar ─────────────────────────────────────────────
        val navYOffsetPx = (expandProgress.value * bottomNavHeightPx).roundToInt()
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, navYOffsetPx) },
        ) {
            BottomBar(navController)
        }

        // ── Player surface ────────────────────────────────────────────────────
        if (isSongPlaying) {
            val progress = expandProgress.value
            val playerTopPx = travelPx * (1f - progress)
            val playerHeightPx = miniPlayerHeightPx + progress * (screenHeightPx - miniPlayerHeightPx)
            val cornerRadius = (16.dp * (1f - progress)).coerceAtLeast(0.dp)

            // Progress value at the moment the finger first touches down.
            // Used to measure how far the user actually dragged, so the snap
            // decision is relative to where they started rather than the midpoint.
            var startProgress by remember { mutableFloatStateOf(0f) }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(with(density) { playerHeightPx.toDp() })
                        .offset { IntOffset(0, playerTopPx.roundToInt()) }
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape =
                                RoundedCornerShape(
                                    topStart = cornerRadius,
                                    topEnd = cornerRadius,
                                ),
                        ).pointerInput(Unit) {
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
                                            (expandProgress.value + delta).coerceIn(0f, 1f),
                                        )
                                    }
                                },
                                onDragEnd = {
                                    // If the finger moved at all in a direction, commit to that state.
                                    val target =
                                        when {
                                            expandProgress.value > startProgress -> 1f
                                            expandProgress.value < startProgress -> 0f
                                            else -> startProgress.roundToInt().toFloat()
                                        }
                                    scope.launch {
                                        expandProgress.animateTo(
                                            targetValue = target,
                                            animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                        )
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        expandProgress.animateTo(
                                            targetValue = startProgress.roundToInt().toFloat(),
                                            animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                        )
                                    }
                                },
                            )
                        }
                        // Tap the mini bar to expand; disabled while already expanded.
                        .clickable(enabled = expandProgress.value < 0.5f) {
                            scope.launch {
                                expandProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec =
                                        spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                )
                            }
                        },
            ) {
                // Mini player fades out during the first half of the transition.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = (1f - progress * 2f).coerceIn(0f, 1f)
                            },
                ) {
                    MiniPlayerContent()
                }

                // Full player fades in during the second half.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
                            },
                ) {
                    FullPlayerContent(
                        onCollapse = {
                            scope.launch {
                                expandProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec =
                                        spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

// ── Mini player ───────────────────────────────────────────────────────────────

@Composable
private fun MiniPlayerContent() {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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

// ── Full screen player ────────────────────────────────────────────────────────

@Composable
private fun FullPlayerContent(onCollapse: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse player")
        }

        Box(
            modifier =
                Modifier
                    .size(280.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

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
