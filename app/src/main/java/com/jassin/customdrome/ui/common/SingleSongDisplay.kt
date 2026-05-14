package com.jassin.customdrome.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jassin.customdrome.data.repository.SongsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SingleSongDisplay(
    title: String,
    artist: String,
    songId: String,
    songsRepository: SongsRepository,
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val coverContainerShape = RoundedCornerShape(10.dp)
        val coverImageShape = RoundedCornerShape(7.dp)

        if (coverBytes != null) {
            val bmp =
                remember(coverBytes) {
                    decodeSampledBitmap(
                        bytes = coverBytes!!,
                    )
                }
            bmp?.let {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(coverContainerShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(0.dp),
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Album cover",
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(coverImageShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            } ?: Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(coverContainerShape)
                        .background(Color.LightGray),
                contentAlignment = Alignment.Center,
            ) {
                Text("?")
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

private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
    if (bytes.isEmpty()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)

    val options =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
): Int {
    var inSampleSize = 1
    val reqWidth = 64
    val reqHeight = 64
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}
