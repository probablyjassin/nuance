package com.jassin.customdrome.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jassin.customdrome.ui.common.SingleSongDisplay

@Composable
fun Songs(songs: List<SongUiModel>) {
    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        LazyColumn {
            items(songs) { song ->
                SingleSongDisplay(
                    title = song.title,
                    artist = song.artist,
                    cover = song.cover,
                )
            }
        }
    }
}
