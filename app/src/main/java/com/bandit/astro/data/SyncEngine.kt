package com.bandit.astro.data

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import com.bandit.astro.AstroApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

data class SyncState(val running: Boolean = false, val message: String = "", val at: Long = 0, val conflict: Int = 0)
fun syncConflict(local: LibraryEntry, remote: LibraryEntry?): Boolean = local.dirty &&
    ((remote == null && local.remoteUpdated > 0 && !local.deleted) ||
    (remote != null && remote.remoteUpdated != local.remoteUpdated &&
        (local.deleted || local.progress != remote.progress || local.volumes != remote.volumes || local.score != remote.score || local.status != remote.status)))
class SyncEngine(private val app: AstroApplication) {
    val mutex = Mutex()
    val state = MutableStateFlow(SyncState())
    suspend fun run(forceId: Int = 0, useRemote: Boolean = false) = mutex.withLock {
        val session = app.account.session.value ?: return@withLock
        val db = app.databaseFor(session.id); val dao = db.dao()
        fun current() { check(app.account.session.value?.id == session.id && app.account.session.value?.token == session.token) { "Account changed. Sync stopped." }; check(session.expiresAt > System.currentTimeMillis()) { "Your AniList sign-in expired. Sign in again." } }
        state.value = state.value.copy(running = true, message = "Syncing with AniList…", conflict = 0)
        try {
            current()
            check(app.api.identity(session.token).id == session.id) { "AniList account verification failed." }
            val remote = app.api.remoteList(session.id, session.token).associateBy { it.entry.mediaId }.toMutableMap()
            for (local in dao.allEntries().filter { it.dirty }) {
                current()
                val server = remote[local.mediaId]
                if (syncConflict(local, server?.entry) && forceId != local.mediaId) {
                    state.value = state.value.copy(conflict = local.mediaId)
                    throw IllegalStateException("${local.title} changed on AniList too. Choose which progress to keep.")
                }
                if (forceId == local.mediaId && useRemote) {
                    db.withTransaction { val fresh=dao.entry(local.mediaId); if(fresh?.revision==local.revision) {
                        if(server==null) dao.purge(local.mediaId) else dao.put(server.entry.copy(notes=fresh.notes,revision=fresh.revision))
                    } }
                    continue
                }
                if (local.deleted) {
                    if (server != null) app.api.delete(server.entryId, session.token)
                    current()
                    db.withTransaction { if (dao.entry(local.mediaId)?.revision == local.revision) dao.purge(local.mediaId) }
                    remote.remove(local.mediaId)
                } else {
                    val updated = app.api.save(local, session.token)
                    current(); dao.acknowledge(local.mediaId, local.revision, updated)
                    remote[local.mediaId] = RemoteEntry(server?.entryId ?: 0, local.copy(dirty=false, remoteUpdated=updated))
                }
                delay(2200) // Respect AniList's constrained request budget; no busy polling.
            }
            current()
            db.withTransaction {
                for ((id, value) in remote) {
                    val old=dao.entry(id)
                    if (old == null || !old.dirty) dao.put(value.entry.copy(notes=old?.notes.orEmpty(), revision=old?.revision ?: 1))
                }
                for (old in dao.allEntries()) if (!old.dirty && old.mediaId !in remote) dao.purge(old.mediaId)
            }
            state.value = SyncState(message="All caught up with AniList", at=System.currentTimeMillis())
        } catch(e: CancellationException) { state.value=state.value.copy(running=false); throw e }
        catch(e: Exception) { state.value=state.value.copy(running=false,message=e.message ?: "Sync paused. Changes are saved on this device."); throw e }
    }
}
object AutoSync {
    fun schedule(app: AstroApplication, enabled: Boolean, soon: Boolean = false) {
        val manager=WorkManager.getInstance(app); val id=app.account.session.value?.id ?: 0
        if(!enabled || id==0) { manager.cancelUniqueWork("astro-auto-sync"); manager.cancelUniqueWork("astro-sync-soon"); return }
        val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val data=workDataOf("account" to id)
        if(soon) manager.enqueueUniqueWork("astro-sync-soon",ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>().setInputData(data).setInitialDelay(5,TimeUnit.SECONDS).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,1,TimeUnit.MINUTES).build())
        else manager.enqueueUniquePeriodicWork("astro-auto-sync",ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SyncWorker>(1,TimeUnit.HOURS).setInputData(data).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,1,TimeUnit.MINUTES).build())
    }
}
class SyncWorker(context: Context, parameters: WorkerParameters): CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result {
        val app=applicationContext as AstroApplication
        if(app.account.session.value?.id != inputData.getInt("account",0) || !app.preferences.flow.first().autoSync) return Result.success()
        return try { app.sync.run(); Result.success() }
        catch(e: CancellationException) { throw e }
        catch(e: ApiException) { if(e.retryable && runAttemptCount<5) Result.retry() else Result.failure() }
        catch(_: java.io.IOException) { if(runAttemptCount<5) Result.retry() else Result.failure() }
        catch(_: Exception) { Result.failure() }
    }
}
