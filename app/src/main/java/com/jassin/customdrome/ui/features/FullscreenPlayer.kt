@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.jassin.customdrome.ui.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.mahozad.multiplatform.wavyslider.WaveDirection.*
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun FullscreenPlayer(
    onCollapse: () -> Unit,
    progress: Float,
    title: String,
    artist: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long?,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val p = progress.coerceIn(0f, 1f)
    val totalDurationMs = durationMs?.takeIf { it > 0L } ?: 0L
    val seekEnabled = totalDurationMs > 0L

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var displayedPositionMs by remember { mutableLongStateOf(currentPositionMs.coerceIn(0L, totalDurationMs)) }

    val playbackFraction =
        if (seekEnabled) {
            (displayedPositionMs.coerceIn(0L, totalDurationMs) / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    LaunchedEffect(currentPositionMs, totalDurationMs, isPlaying, isScrubbing) {
        displayedPositionMs = currentPositionMs.coerceIn(0L, totalDurationMs)
        if (!seekEnabled || !isPlaying || isScrubbing) return@LaunchedEffect

        while (true) {
            delay(1000)
            displayedPositionMs = (displayedPositionMs + 1000L).coerceAtMost(totalDurationMs)
            if (displayedPositionMs >= totalDurationMs) break
        }
    }

    LaunchedEffect(displayedPositionMs, totalDurationMs, isScrubbing) {
        if (seekEnabled && !isScrubbing) {
            scrubFraction = (displayedPositionMs.coerceIn(0L, totalDurationMs) / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 80f
                },
    ) {
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse player",
            )
        }

        // Title + artist block
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (80).dp)
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = p
                        translationY = (1f - p) * 40f
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (seekEnabled) {
                Spacer(modifier = Modifier.height(24.dp))

                WavySlider(
                    value = if (isScrubbing) scrubFraction else playbackFraction,
                    onValueChange = {
                        isScrubbing = true
                        scrubFraction = it
                        displayedPositionMs = (it * totalDurationMs).roundToLong().coerceIn(0L, totalDurationMs)
                    },
                    onValueChangeFinished = {
                        val targetPosition = (scrubFraction * totalDurationMs).roundToLong().coerceIn(0L, totalDurationMs)
                        onSeek(targetPosition)
                        displayedPositionMs = targetPosition
                        isScrubbing = false
                    },
                    enabled = true,
                    // Customize colors via SliderDefaults.colors(). Use MaterialTheme colors or explicit Color(...) values.
                    colors =
                        androidx.compose.material.SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                            thumbColor = MaterialTheme.colorScheme.primary,
                        ),
                    waveLength = 37.dp,
                    // collapse to a straight line when paused by setting waveHeight to 0.dp
                    waveHeight = if (isPlaying) 8.dp else 0.dp,
                    // stop horizontal movement when paused by setting velocity to 0.dp
                    waveVelocity = if (isPlaying) 20.dp to TAIL else 0.dp to TAIL,
                    waveThickness = 3.dp,
                    trackThickness = 4.dp,
                    incremental = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text =
                            formatDuration(
                                if (isScrubbing) (scrubFraction * totalDurationMs).roundToLong() else displayedPositionMs,
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Buttons block
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(60).dp)
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
                    .graphicsLayer {
                        alpha = p
                        translationY = (1f - p) * 120f
                    },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun formatDuration(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val seconds = (totalSeconds % 60L).toInt()
    val minutes = ((totalSeconds / 60L) % 60L).toInt()
    val hours = (totalSeconds / 3600L).toInt()

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
