package com.jassin.customdrome.ui.features

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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

// Height constants shared between scaffold and content padding
private val TopBarHeight = 64.dp
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
    val scope = rememberCoroutineScope()

    val isSongPlaying = true // TODO

    // animation logic for the miniplayer -> fullscreen player transition
    // 0 = collapsed, 1 = expanded
    // snapTo() on every drag frame so the sheet follows the finger
    val expandProgress = remember { Animatable(0f, Float.VectorConverter) }

    // top nav
    if (showNavBars) {
        TopBar(onGoToSettings = { navController.navigate(route = "settings") })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = constraints.maxHeight.toFloat()

        val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }
        val bottomNavHeightPx = with(density) { BottomNavHeight.toPx() }

        // total travel distance of the sheet's top edge
        val travelPx =
            screenHeightPx - miniPlayerHeightPx - bottomNavHeightPx - with(density) { 15.dp.toPx() }

        // main content
        content(
            PaddingValues(
                top = if (showNavBars) TopBarHeight else 0.dp,
                bottom = BottomNavHeight,
            ),
        )

        // bottom navbar
        if (showNavBars) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter),
            ) {
                TabsBar(navController)
            }

            // player surface
            if (isSongPlaying) {
                val progress = expandProgress.value

                val topCurve = 1f
                val heightCurve = 0.72f

                val topP = progress.powCurve(topCurve)
                val heightP = progress.powCurve(heightCurve)

                val playerTopPx = travelPx * (1f - topP)
                val playerHeightPx =
                    miniPlayerHeightPx + heightP * (screenHeightPx - miniPlayerHeightPx)

                val cornerRadius = (16.dp * (1f - progress)).coerceAtLeast(0.dp)

                var startProgress by remember { mutableFloatStateOf(0f) }

                // BackHandler re-registered on every navigation change
                val currentEntry by navController.currentBackStackEntryAsState()
                key(currentEntry) {
                    BackHandler(enabled = expandProgress.value > 0.5f) {
                        scope.launch {
                            expandProgress.animateTo(
                                0f,
                                tween(300),
                            )
                        }
                    }
                }

                PlayerSurface(
                    progress = progress,
                    playerHeightPx = playerHeightPx,
                    playerTopPx = playerTopPx,
                    cornerRadius = cornerRadius,
                    onCollapse = {
                        scope.launch {
                            expandProgress.animateTo(
                                0f,
                                tween(300),
                            )
                        }
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
