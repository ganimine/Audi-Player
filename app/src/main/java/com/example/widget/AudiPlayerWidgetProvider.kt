package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.MediaItemData
import java.io.File

class AudiPlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, false, 0, 100)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Actions can be broadcasted to playback service / activity
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                // Sent to app to toggle
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mainIntent)
            }
            ACTION_NEXT -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_NEXT
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mainIntent)
            }
            ACTION_PREV -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_PREV
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mainIntent)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.audiplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.audiplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.example.audiplayer.ACTION_PREV"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            media: MediaItemData?,
            isPlaying: Boolean,
            positionMs: Long,
            durationMs: Long
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_audi_player)

            // Open app on click
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Track metadata
            if (media != null) {
                views.setTextViewText(R.id.widget_title, media.title)
                views.setTextViewText(R.id.widget_artist, media.artist)
                views.setTextViewText(R.id.widget_album, "${media.album} • ${media.codec}")

                // Progress
                if (durationMs > 0) {
                    val progressPercent = ((positionMs.toFloat() / durationMs.toFloat()) * 100).toInt()
                    views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)
                }

                // Embedded or local artwork
                if (!media.artworkUrl.isNullOrBlank()) {
                    try {
                        val file = File(media.artworkUrl)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                            }
                        } else if (media.artworkUrl.startsWith("content://")) {
                            views.setImageViewUri(R.id.widget_album_art, Uri.parse(media.artworkUrl))
                        }
                    } catch (_: Exception) {}
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
                }
            } else {
                views.setTextViewText(R.id.widget_title, "AUDI Player")
                views.setTextViewText(R.id.widget_artist, "Tap to open and play")
                views.setTextViewText(R.id.widget_album, "High-Res Lossless Audio")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
            }

            // Controls
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            val playPauseIntent = Intent(context, AudiPlayerWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                PendingIntent.getBroadcast(context, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val nextIntent = Intent(context, AudiPlayerWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getBroadcast(context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val prevIntent = Intent(context, AudiPlayerWidgetProvider::class.java).apply {
                action = ACTION_PREV
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                PendingIntent.getBroadcast(context, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(
            context: Context,
            media: MediaItemData?,
            isPlaying: Boolean,
            positionMs: Long,
            durationMs: Long
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, AudiPlayerWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, media, isPlaying, positionMs, durationMs)
            }
        }
    }
}
