package com.bandit.astro.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bandit.astro.*
import com.bandit.astro.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object AiringNotifications {
    const val CHANNEL = "airing"
    private const val WORK = "astro-airing-reminders"
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Episode releases", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "New episodes of anime in your currently watching collection"
            })
    }
    fun allowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun schedule(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) { manager.cancelUniqueWork(WORK); return }
        val request = PeriodicWorkRequestBuilder<AiringWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES).build()
        manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
    fun send(context: Context, airing: Airing): Boolean {
        if (!allowed(context)) return false
        val intent = Intent(context, MainActivity::class.java).putExtra("mediaId", airing.media.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, airing.media.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(airing.media.title)
            .setContentText("Episode ${airing.episode} has aired. Your next story is ready.")
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        return try {
            NotificationManagerCompat.from(context).notify(airing.id, notification)
            true
        } catch (_: SecurityException) { false }
    }
}

class AiringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AstroApplication
        if (!app.preferences.flow.first().reminders || !AiringNotifications.allowed(app)) return Result.success()
        val dao = app.database.dao()
        val collection = dao.entries().filter { it.type == "ANIME" && it.status == "CURRENT" }.associateBy { it.mediaId }
        if (collection.isEmpty()) return Result.success()
        val now = System.currentTimeMillis() / 1000
        return try {
            for (ids in collection.keys.toList().chunked(50)) {
                var page = 1
                do {
                    val (episodes, more) = app.api.schedule(now - 6 * 3600 - 1, now + 1, page, ids)
                    for (airing in episodes) {
                        // Re-read progress and preferences: the user can change them during this request.
                        if (!app.preferences.flow.first().reminders) return Result.success()
                        val entry = dao.entries().firstOrNull { it.mediaId == airing.media.id } ?: continue
                        if (entry.status != "CURRENT" || !episodeIsDue(airing.airingAt, now, entry.progress, airing.episode)) continue
                        if (dao.wasNotified(airing.id) == 0 && AiringNotifications.send(app, airing))
                            dao.markNotified(NotifiedEpisode(airing.id, now))
                    }
                    page++
                } while (more)
            }
            dao.pruneNotifications(now - 30L * 86400)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: ApiException) { if (e.retryable) Result.retry() else Result.failure() }
        catch (_: java.io.IOException) { Result.retry() }
        catch (_: org.json.JSONException) { Result.failure() }
    }
}
