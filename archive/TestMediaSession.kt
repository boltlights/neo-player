import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.common.Player

fun test(context: Context, player: Player, callback: MediaLibrarySession.Callback) {
    val session = MediaLibrarySession.Builder(context, player, callback).build()
    val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(Intent.ACTION_MEDIA_BUTTON), PendingIntent.FLAG_IMMUTABLE)
    session.setMediaButtonReceiver(pendingIntent)
}
