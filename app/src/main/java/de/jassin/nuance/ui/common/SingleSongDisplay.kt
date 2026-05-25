package de.jassin.nuance.ui.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.jassin.nuance.data.repository.SongsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SingleSongDisplay(
    title: String,
    artist: String,
    songId: String,
    songsRepository: SongsRepository,
    onClick: () -> Unit = {},
    // onLongPress: () -> Unit = {},
    onCoverLoaded: (songId: String, coverBytes: ByteArray) -> Unit = { _, _ -> },
    cachedCover: ByteArray? = null,
) {
    val coverBytes by produceState<ByteArray?>(initialValue = cachedCover, key1 = songId) {
        Log.d("SingleSongDisplay", "produceState triggered for songId=$songId, cachedCover=${cachedCover != null}")
        if (cachedCover != null) {
            Log.d("SingleSongDisplay", "using parent cache for songId=$songId")
            value = cachedCover
        } else {
            value =
                withContext(Dispatchers.IO) {
                    songsRepository.getCoverArtQueued(songId)
                }
            if (value != null) {
                Log.d("SingleSongDisplay", "loaded cover for songId=$songId, notifying parent")
                onCoverLoaded(songId, value!!)
            }
        }
    }

    /*.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                    )
                }*/

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val coverContainerShape = RoundedCornerShape(10.dp)
        val coverImageShape = RoundedCornerShape(7.dp)

        if (coverBytes != null) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(coverContainerShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(0.dp),
            ) {
                AsyncImage(
                    model = coverBytes,
                    contentDescription = "Album cover",
                    modifier = Modifier.fillMaxSize().clip(coverImageShape),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(coverContainerShape)
                        .background(Color.LightGray),
                contentAlignment = Alignment.Center,
            ) {
                Text("…")
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// decodeSampledBitmap moved to ui.util.ImageUtils
