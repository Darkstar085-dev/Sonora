package com.sipun.sonora.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sipun.sonora.MainActivity
import com.sipun.sonora.R
import com.sipun.sonora.data.preferences.SonoraPreferences
import com.sipun.sonora.data.preferences.ThemePreferences
import com.sipun.sonora.domain.Song
import com.sipun.sonora.player.ArtworkLoader
import com.sipun.sonora.player.SonoraPlaybackService
import java.io.InputStream
import java.util.Locale

class SonoraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }

            ACTION_WIDGET_SKIP_NEXT -> {
                sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            }

            ACTION_WIDGET_SKIP_PREVIOUS -> {
                sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }

            ACTION_WIDGET_TOGGLE_SHUFFLE -> {
                toggleShuffle(context)
            }

            ACTION_WIDGET_CYCLE_REPEAT -> {
                cycleRepeat(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.sipun.sonora.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_SKIP_NEXT = "com.sipun.sonora.ACTION_WIDGET_SKIP_NEXT"
        const val ACTION_WIDGET_SKIP_PREVIOUS = "com.sipun.sonora.ACTION_WIDGET_SKIP_PREVIOUS"
        const val ACTION_WIDGET_TOGGLE_SHUFFLE = "com.sipun.sonora.ACTION_WIDGET_TOGGLE_SHUFFLE"
        const val ACTION_WIDGET_CYCLE_REPEAT = "com.sipun.sonora.ACTION_WIDGET_CYCLE_REPEAT"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SonoraWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_sonora_4x2)
            val preferences = SonoraPreferences(context)
            val accentColor = getPrimaryAccentColor(context)
            val song = preferences.lastPlayed()
            val isPlaying = preferences.isPlaying()
            val shuffleEnabled = preferences.shuffleEnabled()
            val repeatMode = preferences.repeatMode()
            val positionMs = preferences.lastPlayedPositionMs()
            val durationMs = preferences.lastPlayedDurationMs()

            val defaultInactiveColor =
                ContextCompat.getColor(context, R.color.widget_m3_on_surface_variant)

            // Apply App Primary Accent Color to Play/Pause circle background
            views.setInt(R.id.widget_btn_play_pause_bg, "setColorFilter", accentColor)

            // Apply App Primary Accent Color to Progress Bar
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.widget_progress,
                    "setProgressTintList",
                    ColorStateList.valueOf(accentColor)
                )
            }

            // Apply Shuffle state & tint
            val shuffleColor = if (shuffleEnabled) accentColor else defaultInactiveColor
            views.setInt(R.id.widget_btn_shuffle, "setColorFilter", shuffleColor)

            // Apply Repeat state & tint
            val repeatColor = if (repeatMode > 0) accentColor else defaultInactiveColor
            views.setInt(R.id.widget_btn_repeat, "setColorFilter", repeatColor)
            views.setImageViewResource(
                R.id.widget_btn_repeat,
                if (repeatMode == 2) R.drawable.ic_widget_repeat_one else R.drawable.ic_widget_repeat,
            )

            if (song != null) {
                views.setTextViewText(R.id.widget_song_title, song.title)
                val artistAlbumText = if (song.artist.isNotBlank() && song.album.isNotBlank()) {
                    "${song.artist} • ${song.album}"
                } else {
                    song.artist.ifBlank { song.album }
                }
                views.setTextViewText(R.id.widget_song_artist, artistAlbumText)

                views.setTextViewText(R.id.widget_time_current, formatTime(positionMs))
                views.setTextViewText(R.id.widget_time_total, formatTime(durationMs))

                val progress = if (durationMs > 0L) {
                    ((positionMs.toFloat() / durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                views.setProgressBar(R.id.widget_progress, 100, progress, false)

                views.setImageViewResource(
                    R.id.widget_btn_play_pause,
                    if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                )

                val artworkBitmap = loadArtworkBitmap(context, song)
                if (artworkBitmap != null) {
                    views.setImageViewBitmap(R.id.widget_artwork, artworkBitmap)
                    views.setViewVisibility(R.id.widget_artwork_icon, View.GONE)
                } else {
                    views.setImageViewResource(R.id.widget_artwork, R.drawable.widget_artwork_bg)
                    views.setViewVisibility(R.id.widget_artwork_icon, View.VISIBLE)
                }
            } else {
                views.setTextViewText(R.id.widget_song_title, "Sonora Music")
                views.setTextViewText(R.id.widget_song_artist, "No song playing")
                views.setTextViewText(R.id.widget_time_current, "0:00")
                views.setTextViewText(R.id.widget_time_total, "0:00")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setImageViewResource(R.id.widget_btn_play_pause, R.drawable.ic_widget_play)
                views.setImageViewResource(R.id.widget_artwork, R.drawable.widget_artwork_bg)
                views.setViewVisibility(R.id.widget_artwork_icon, View.VISIBLE)
            }

            // Pending Intents
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_artwork, mainPendingIntent)

            val playPauseIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause_container,
                playPausePendingIntent
            )

            val prevIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SKIP_PREVIOUS
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            val nextIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SKIP_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            val repeatIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CYCLE_REPEAT
            }
            val repeatPendingIntent = PendingIntent.getBroadcast(
                context,
                4,
                repeatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_repeat, repeatPendingIntent)

            val shuffleIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE_SHUFFLE
            }
            val shufflePendingIntent = PendingIntent.getBroadcast(
                context,
                5,
                shuffleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_shuffle, shufflePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPrimaryAccentColor(context: Context): Int {
            return try {
                val themePrefs = ThemePreferences.from(context)
                val color = themePrefs.customColor.value
                if (color != 0) color else 0xFFE92B2B.toInt()
            } catch (_: Exception) {
                0xFFE92B2B.toInt()
            }
        }

        private fun toggleShuffle(context: Context) {
            val future = MediaController.Builder(
                context.applicationContext,
                SessionToken(
                    context.applicationContext,
                    ComponentName(context.applicationContext, SonoraPlaybackService::class.java)
                ),
            ).buildAsync()
            future.addListener({
                runCatching {
                    val controller = future.get()
                    controller.shuffleModeEnabled = !controller.shuffleModeEnabled
                    MediaController.releaseFuture(future)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        private fun cycleRepeat(context: Context) {
            val future = MediaController.Builder(
                context.applicationContext,
                SessionToken(
                    context.applicationContext,
                    ComponentName(context.applicationContext, SonoraPlaybackService::class.java)
                ),
            ).buildAsync()
            future.addListener({
                runCatching {
                    val controller = future.get()
                    controller.repeatMode = when (controller.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    MediaController.releaseFuture(future)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = ComponentName(context, SonoraPlaybackService::class.java)
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            ContextCompat.startForegroundService(context, downIntent)
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = ComponentName(context, SonoraPlaybackService::class.java)
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            ContextCompat.startForegroundService(context, upIntent)
        }

        private fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        private fun loadArtworkBitmap(context: Context, song: Song): Bitmap? {
            // 1. Try loading from albumArtUri if present
            val artUriStr = song.albumArtUri
            if (!artUriStr.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(artUriStr)
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val original = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (original != null) {
                        return getRoundedCornerBitmap(original, 24f)
                    }
                } catch (_: Exception) {
                    // Fallthrough to embedded artwork extraction
                }
            }

            // 2. Fallback to embedded artwork extraction from song.uri
            if (song.uri.isNotBlank()) {
                try {
                    val artworkLoader = ArtworkLoader(context.applicationContext)
                    val bytes = artworkLoader.extractEmbeddedArtwork(Uri.parse(song.uri))
                    if (bytes != null) {
                        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (original != null) {
                            return getRoundedCornerBitmap(original, 24f)
                        }
                    }
                } catch (_: Exception) {
                    // Ignored
                }
            }
            return null
        }

        private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusDp: Float): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply { isAntiAlias = true }
            val rect = Rect(0, 0, width, height)
            val rectF = RectF(rect)
            val cornerRadiusPx = cornerRadiusDp * (width.toFloat() / 200f).coerceAtLeast(1f)

            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
