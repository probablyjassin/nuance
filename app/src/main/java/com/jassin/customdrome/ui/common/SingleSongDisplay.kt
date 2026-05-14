package com.jassin.customdrome.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun SingleSongDisplay(
    title: String,
    artist: String,
    cover: ByteArray? = null,
) {
    Row {
        cover?.let { bytes ->
            val bmp = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Album cover",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column {
            Text(text = title)
            Text(text = artist)
        }
    }
}
