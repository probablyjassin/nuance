package com.jassin.customdrome.ui.features

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.roundToInt

@Composable
fun PlayerSurface(
    progress: Float,
    playerHeightPx: Float,
    playerTopPx: Float,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit,
) {
    val density = LocalDensity.current

    val miniCoverSize = 48.dp
    val fullCoverSize = 350.dp

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(with(density) { playerHeightPx.toDp() })
                .offset { IntOffset(0, playerTopPx.roundToInt()) }
                .then(modifier)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
                ),
    ) {
        // miniplayer
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - progress * 2f).coerceIn(0f, 1f) },
        ) {
            MiniPlayerContent()
        }

        // fullscreen player
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            FullscreenPlayer(
                onCollapse = onCollapse,
                progress = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f),
            )
        }

        // album cover
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            // minimized: left, vertically centred
            val miniX = 12.dp
            val miniY = (72.dp - miniCoverSize) / 2 // = 12 dp

            // fullscreen: centred
            val fullX = (containerWidth - fullCoverSize) / 2
            val fullY = (containerHeight - fullCoverSize) / 7 - 24.dp

            val coverSize = lerp(miniCoverSize, fullCoverSize, progress)
            val coverX = lerp(miniX, fullX, progress)
            val coverY = lerp(miniY, fullY, progress)
            val coverCorner = lerp(8.dp, 20.dp, progress)
            val iconSize = lerp(24.dp, 80.dp, progress)

            Box(
                modifier =
                    Modifier
                        .size(coverSize)
                        .offset(x = coverX, y = coverY)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(coverCorner),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
