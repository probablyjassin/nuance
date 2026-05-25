package de.jassin.nuance.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun PlayerSurface(
    modifier: Modifier = Modifier,
    progress: Float,
    playerHeightPx: Float,
    playerTopPx: Float,
    cornerRadius: Dp,
    nowPlayingTitle: String,
    nowPlayingArtist: String,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    currentPositionMs: Long = 0L,
    durationMs: Long? = null,
    onSeek: (Long) -> Unit = {},
    nowPlayingCoverBytes: ByteArray? = null,
    onCollapse: () -> Unit,
    // Pixel offsets applied during a dismissal swipe (follow the finger)
    dismissOffsetYPx: Float = 0f,
) {
    val density = LocalDensity.current

    val fullCoverSize = 350.dp

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(with(density) { playerHeightPx.toDp() })
                // apply dismissal offsets in px so the surface can follow the finger
                .offset {
                    IntOffset(
                        0,
                        (playerTopPx + dismissOffsetYPx).roundToInt(),
                    )
                }.then(modifier)
                .background(
                    color = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
                ).clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)),
    ) {
        // miniplayer
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - progress * 10f).coerceIn(0f, 1f)
                        translationY = -progress * 5000f
                    },
        ) {
            MiniPlayerContent(
                title = nowPlayingTitle,
                artist = nowPlayingArtist,
                isPlaying = isPlaying,
                onPrevious = onPrevious,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
            )
        }

        // fullscreen player
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            FullscreenPlayer(
                onCollapse = onCollapse,
                progress = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f),
                title = nowPlayingTitle,
                artist = nowPlayingArtist,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onPrevious = onPrevious,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onSeek = onSeek,
            )
        }

        // album cover
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            // minimized: left, with top gap
            val miniX = 12.dp
            val miniY = 8.dp
            val miniSize = 48.dp

            // middle state: intermediate position and size
            val middleX = 12.dp
            val middleY = 10.dp
            val middleSize = 350.dp

            // fullscreen: centred
            val fullX = (containerWidth - fullCoverSize) / 2
            val fullY = (containerHeight - fullCoverSize) / 7 - 24.dp

            // Determine which leg of animation we're in and interpolate accordingly
            val size: Dp
            val x: Dp
            val y: Dp
            val corner: Dp
            val icon: Dp

            if (progress < 0.5f) {
                val legProgress = progress / 0.5f
                size = lerp(miniSize, middleSize, legProgress)
                x = lerp(miniX, middleX, legProgress)
                y = lerp(miniY, middleY, legProgress)
                corner = lerp(8.dp, 16.dp, legProgress)
                icon = lerp(24.dp, 50.dp, legProgress)
            } else {
                // Second half: middle to full
                val legProgress = (progress - 0.5f) / 0.5f
                size = lerp(middleSize, fullCoverSize, legProgress)
                x = lerp(middleX, fullX, legProgress)
                y = lerp(middleY, fullY, legProgress)
                corner = lerp(16.dp, 20.dp, legProgress)
                icon = lerp(50.dp, 80.dp, legProgress)
            }

            Box(
                modifier =
                    Modifier
                        .size(size)
                        .offset(x = x, y = y)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(corner),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (nowPlayingCoverBytes != null) {
                    // Use Coil to load and decode the ByteArray off the UI thread
                    AsyncImage(
                        model = nowPlayingCoverBytes,
                        contentDescription = "Album cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(corner)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(icon),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
