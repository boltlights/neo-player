package com.example

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

fun testSetMediaItems(player: ExoPlayer) {
    val items = listOf(MediaItem.Builder().setMediaId("1").build())
    player.setMediaItems(items, false)
}
