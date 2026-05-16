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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun FullscreenPlayer(
    onCollapse: () -> Unit,
    progress: Float,
) {
    val p = progress.coerceIn(0f, 1f)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 80f
                },
    ) {
        // Title + artist block
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (40).dp)
                    .graphicsLayer {
                        alpha = p
                        translationY = (1f - p) * 40f
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Song Title", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Artist Name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
