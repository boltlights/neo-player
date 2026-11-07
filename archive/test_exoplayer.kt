import androidx.media3.exoplayer.ExoPlayer
fun test(exoPlayer: ExoPlayer) {
    // try to find setShuffleOrder
    exoPlayer.setShuffleOrder(androidx.media3.exoplayer.source.DefaultShuffleOrder(0))
}
