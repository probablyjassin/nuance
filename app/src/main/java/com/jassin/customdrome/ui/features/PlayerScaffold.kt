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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jassin.customdrome.data.models.SongUiModel
import com.jassin.customdrome.playback.PlaybackManager
import com.jassin.customdrome.playback.toPlaybackItem
import com.jassin.customdrome.ui.common.FullScreenSearchOverlay
import com.jassin.customdrome.ui.common.TabsBar
import com.jassin.customdrome.ui.common.TopBar
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    playbackManager: PlaybackManager,
    songsRepository: com.jassin.customdrome.data.repository.SongsRepository,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val playbackState by playbackManager.state.collectAsState()
    val currentSong = playbackState.currentItem
    val searchSongs by produceState(initialValue = emptyList<SongUiModel>(), key1 = songsRepository) {
        value = runCatching { songsRepository.loadSongs() }.getOrDefault(emptyList())
    }

    var searchOverlayVisible by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchOverlayProgress = remember { Animatable(0f) }

    val dismissOffsetY = remember { Animatable(0f) }

    // animation logic for the miniplayer -> fullscreen player transition
    // 0 = collapsed, 1 = expanded
    // snapTo() on every drag frame so the sheet follows the finger
    val expandProgress = remember { Animatable(0f, Float.VectorConverter) }

    // top nav
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = constraints.maxHeight.toFloat()

        val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }
        val bottomNavHeightPx = with(density) { BottomNavHeight.toPx() }
        val bottomNavHideDistancePx = bottomNavHeightPx + with(density) { 24.dp.toPx() }

        // total travel distance of the sheet's top edge
        val travelPx =
            screenHeightPx - miniPlayerHeightPx - bottomNavHeightPx - with(density) { 15.dp.toPx() }

        val filteredSearchSongs =
            remember(searchQuery, searchSongs) {
                val normalizedQuery = searchQuery.trim()
                if (normalizedQuery.isBlank()) {
                    searchSongs
                } else {
                    searchSongs.filter { song ->
                        listOfNotNull(song.title, song.artist, song.album)
                            .joinToString(" ")
                            .contains(normalizedQuery, ignoreCase = true)
                    }
                }
            }

        Box(modifier = Modifier.fillMaxSize()) {
            // main content
            content(
                PaddingValues(
                    top = if (showNavBars) TopBarHeight else 0.dp,
                    bottom = BottomNavHeight,
                ),
            )

            if (showNavBars) {
                TopBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(0f),
                    onSearchClick = {
                        scope.launch {
                            searchOverlayVisible = true
                            searchOverlayProgress.animateTo(1f, tween(260))
                        }
                    },
                    onGoToSettings = { navController.navigate(route = "settings") },
                )

                BackHandler(enabled = searchOverlayVisible || searchOverlayProgress.value > 0.01f) {
                    scope.launch {
                        searchOverlayProgress.animateTo(0f, tween(220))
                        searchOverlayVisible = false
                    }
                }

                // player surface
                if (currentSong != null) {
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
                var startedFromCollapsed by remember { mutableStateOf(false) }
                var didDownwardDismiss by remember { mutableStateOf(false) }
                var collapsedDragDirection by remember { mutableFloatStateOf(0f) } // 0 = undecided, -1 = up/expand, 1 = down/dismiss
                var collapsedDragAccumulatedY by remember { mutableFloatStateOf(0f) }

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
                    nowPlayingTitle = currentSong.title,
                    nowPlayingArtist = currentSong.artist,
                    isPlaying = playbackState.isPlaying,
                    currentPositionMs = playbackState.positionMs,
                    durationMs = playbackState.currentDurationMs,
                    onPrevious = { playbackManager.previous() },
                    onTogglePlayPause = { playbackManager.togglePlayPause() },
                    onNext = { playbackManager.next() },
                    onSeek = playbackManager::seekTo,
                    nowPlayingCoverBytes = playbackState.currentCoverArt,
                    dismissOffsetYPx = dismissOffsetY.value,
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
                            .zIndex(3f)
                            .pointerInput(Unit) {
                                // Use the higher level detector but track velocity manually with VelocityTracker.
                                var velocityTracker: VelocityTracker? = null
                                detectVerticalDragGestures(
                                    onDragStart = { _ ->
                                        scope.launch { expandProgress.stop() }
                                        startProgress = expandProgress.value
                                        startedFromCollapsed = expandProgress.value < 0.15f
                                        didDownwardDismiss = false
                                        collapsedDragDirection = 0f
                                        collapsedDragAccumulatedY = 0f
                                        velocityTracker = VelocityTracker()
                                        // intentionally not consuming to keep compatibility with current Compose API
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        // add positions for velocity calculation
                                        velocityTracker?.addPosition(change.uptimeMillis, change.position)
                                        // intentionally not consuming to keep compatibility with current Compose API

                                        // Lock the intent (dismiss vs expand) only after a tiny accumulated threshold,
                                        // so initial jitter doesn't classify an upward drag as downward dismiss.
                                        if (startedFromCollapsed && collapsedDragDirection == 0f) {
                                            collapsedDragAccumulatedY += dragAmount
                                            val directionLockThresholdPx = with(density) { 2.dp.toPx() }
                                            if (abs(collapsedDragAccumulatedY) >= directionLockThresholdPx) {
                                                collapsedDragDirection = if (collapsedDragAccumulatedY > 0f) 1f else -1f
                                                didDownwardDismiss = collapsedDragDirection > 0f
                                            }
                                        }

                                        when {
                                            // Gesture intent was locked as downward dismiss.
                                            startedFromCollapsed && collapsedDragDirection > 0f -> {
                                                scope.launch {
                                                    dismissOffsetY.snapTo((dismissOffsetY.value + dragAmount).coerceAtLeast(0f))
                                                }
                                            }

                                            // Gesture intent was locked as upward expand.
                                            startedFromCollapsed && collapsedDragDirection < 0f -> {
                                                scope.launch {
                                                    val delta = -dragAmount / travelPx
                                                    expandProgress.snapTo((expandProgress.value + delta).coerceIn(0f, 1f))
                                                }
                                            }

                                            // Downward drag while not collapsed keeps the sheet behavior stable.
                                            !startedFromCollapsed -> {
                                                val delta = -dragAmount / travelPx
                                                scope.launch {
                                                    expandProgress.snapTo((expandProgress.value + delta).coerceIn(0f, 1f))
                                                }
                                            }

                                            // startedFromCollapsed + undecided direction: wait for enough movement.
                                            else -> {
                                                // no-op until drag direction is locked
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val velocityY = velocityTracker?.calculateVelocity()?.y ?: 0f

                                        // thresholds
                                        val distanceThreshold = with(density) { 1.dp.toPx() }
                                        val velocityThreshold = 10f // px/sec

                                        if (startedFromCollapsed) {
                                            if (didDownwardDismiss) {
                                                scope.launch {
                                                }
                                                // Dismiss path: either sufficient distance or a downward fling
                                                if (dismissOffsetY.value > distanceThreshold || velocityY > velocityThreshold) {
                                                    val targetY = screenHeightPx + 400f
                                                    scope.launch {
                                                        // use the fling velocity for a natural animation
                                                        dismissOffsetY.animateTo(targetY, tween(300))
                                                        playbackManager.clearQueue()
                                                        dismissOffsetY.snapTo(0f)
                                                    }
                                                } else {
                                                    scope.launch { dismissOffsetY.animateTo(0f, tween(200)) }
                                                }
                                            } else {
                                                // Expansion path: upward fling expands; downward fling collapses.

                                                when {
                                                    // strong upward fling -> expand
                                                    velocityY < -velocityThreshold -> {
                                                        scope.launch {
                                                            expandProgress.animateTo(1f, tween(60))
                                                        }
                                                    }

                                                    // strong downward fling -> collapse/don't expand
                                                    velocityY > velocityThreshold -> {
                                                        scope.launch {
                                                            expandProgress.animateTo(0f, tween(60))
                                                        }
                                                    }

                                                    // otherwise decide by how far the finger moved relative to start
                                                    else -> {
                                                        /*val target =
                                                            if (
                                                                expandProgress.value !in 0.45f..0.8f
                                                            ) {
                                                                1f
                                                            } else {
                                                                0f
                                                            }*/
                                                        scope.launch { expandProgress.animateTo(1f, tween(60, easing = LinearEasing)) }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Not started from collapsed: behave like a sheet, but use velocity to pick destination.
                                            when {
                                                velocityY < -velocityThreshold -> {
                                                    scope.launch { expandProgress.animateTo(1f, tween(60)) }
                                                }

                                                velocityY > velocityThreshold -> {
                                                    scope.launch { expandProgress.animateTo(0f, tween(60)) }
                                                }

                                                else -> {
                                                    val target = if (expandProgress.value > startProgress) 1f else 0f
                                                    scope.launch { expandProgress.animateTo(target, tween(60, easing = LinearEasing)) }
                                                }
                                            }
                                        }

                                        // reset transient flags and tracker
                                        startedFromCollapsed = false
                                        didDownwardDismiss = false
                                        collapsedDragDirection = 0f
                                        collapsedDragAccumulatedY = 0f
                                        velocityTracker = null
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            dismissOffsetY.animateTo(0f, tween(200))
                                            startedFromCollapsed = false
                                            didDownwardDismiss = false
                                        }
                                        collapsedDragDirection = 0f
                                        collapsedDragAccumulatedY = 0f
                                        velocityTracker = null
                                    },
                                )
                            }.clickable(
                                enabled = progress < 0.5f,
                                // This tracks the interaction state (press, drag, etc.)
                                interactionSource = remember { MutableInteractionSource() },
                                // This defines the visual effect. Setting it to null removes the circle/ripple.
                                indication = null,
                            ) {
                                scope.launch { expandProgress.animateTo(1f, tween(120)) }
                            },
                )
                }

                if (searchOverlayVisible || searchOverlayProgress.value > 0.01f) {
                    FullScreenSearchOverlay(
                        progress = searchOverlayProgress.value,
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClose = {
                            scope.launch {
                                searchOverlayProgress.animateTo(0f, tween(220))
                                searchOverlayVisible = false
                            }
                        },
                        items = filteredSearchSongs,
                        itemKey = { it.id },
                        itemTitle = { it.title },
                        itemSubtitle = { song ->
                            listOfNotNull(song.artist, song.album)
                                .joinToString(" • ")
                                .takeIf { it.isNotBlank() }
                        },
                        itemSearchText = { song -> listOfNotNull(song.title, song.artist, song.album).joinToString(" ") },
                        onItemSelected = { selectedSong ->
                            val startIndex = filteredSearchSongs.indexOfFirst { it.id == selectedSong.id }
                            if (startIndex >= 0) {
                                scope.launch {
                                    searchOverlayProgress.animateTo(0f, tween(180))
                                    searchOverlayVisible = false
                                }
                                playbackManager.playQueue(
                                    queue = filteredSearchSongs.map { it.toPlaybackItem() },
                                    startIndex = startIndex,
                                )
                            }
                        },
                        modifier = Modifier.zIndex(10f),
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                translationY = bottomNavHideDistancePx * expandProgress.value
                            }
                            .zIndex(1f),
                ) {
                    TabsBar(navController)
                }
            }
        }
    }
}
