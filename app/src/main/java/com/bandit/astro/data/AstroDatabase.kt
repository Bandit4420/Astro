package com.bandit.astro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notified")
data class NotifiedEpisode(@PrimaryKey val airingId: Int, val sentAt: Long)

@Dao
interface AstroDao {
    @Query("SELECT * FROM collection WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeCollection(): Flow<List<LibraryEntry>>
    @Query("SELECT * FROM collection WHERE deleted = 0 ORDER BY updatedAt DESC")
    suspend fun entries(): List<LibraryEntry>
    @Query("SELECT * FROM collection") suspend fun allEntries(): List<LibraryEntry>
    @Query("SELECT * FROM collection WHERE mediaId = :id") suspend fun entry(id: Int): LibraryEntry?
    @Upsert suspend fun put(entry: LibraryEntry)
    @Query("DELETE FROM collection WHERE mediaId = :id") suspend fun purge(id: Int)
    @Query("UPDATE collection SET dirty = CASE WHEN revision = :revision THEN 0 ELSE dirty END, remoteUpdated = :remote WHERE mediaId = :id")
    suspend fun acknowledge(id: Int, revision: Long, remote: Long)
    @Query("SELECT * FROM history ORDER BY at DESC") fun observeHistory(): Flow<List<HistoryEvent>>
    @Query("SELECT * FROM history ORDER BY at DESC") suspend fun history(): List<HistoryEvent>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun event(event: HistoryEvent)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entry: LibraryEntry)
    @Query("UPDATE collection SET deleted = 1, dirty = 1, revision = revision + 1 WHERE mediaId = :id")
    suspend fun remove(id: Int)
    @Query("UPDATE collection SET progress = CASE WHEN total > 0 THEN MIN(progress + 1, total) ELSE MIN(progress + 1, 2147483647) END, status = CASE WHEN total > 0 AND progress + 1 >= total THEN 'COMPLETED' ELSE 'CURRENT' END, updatedAt = :now, dirty = 1, revision = revision + 1 WHERE mediaId = :id")
    suspend fun increment(id: Int, now: Long = System.currentTimeMillis())
    @Query("UPDATE collection SET progress = MAX(progress - 1, 0), status = CASE WHEN status = 'COMPLETED' THEN 'CURRENT' ELSE status END, updatedAt = :now, dirty = 1, revision = revision + 1 WHERE mediaId = :id")
    suspend fun decrement(id: Int, now: Long = System.currentTimeMillis())
    @Query("UPDATE collection SET status = :status, progress = CASE WHEN :status = 'COMPLETED' AND total > 0 THEN total ELSE progress END, updatedAt = :now, dirty = 1, revision = revision + 1 WHERE mediaId = :id")
    suspend fun status(id: Int, status: String, now: Long = System.currentTimeMillis())
    @Query("SELECT COUNT(*) FROM notified WHERE airingId = :id")
    suspend fun wasNotified(id: Int): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markNotified(value: NotifiedEpisode)
    @Query("DELETE FROM notified WHERE sentAt < :before")
    suspend fun pruneNotifications(before: Long)
}

@Entity(tableName = "history")
data class HistoryEvent(@PrimaryKey val id: String = java.util.UUID.randomUUID().toString(), val mediaId: Int,
    val at: Long = System.currentTimeMillis(), val delta: Int, val unit: String, val minutes: Int = 0)

@Database(entities = [LibraryEntry::class, NotifiedEpisode::class, HistoryEvent::class], version = 2, exportSchema = true)
abstract class AstroDatabase : RoomDatabase() { abstract fun dao(): AstroDao }

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        listOf("volumes", "totalVolumes", "score", "nextEpisode", "nextAiring", "deleted", "remoteUpdated").forEach {
            db.execSQL("ALTER TABLE collection ADD COLUMN $it INTEGER NOT NULL DEFAULT 0")
        }
        listOf("dirty", "revision").forEach { db.execSQL("ALTER TABLE collection ADD COLUMN $it INTEGER NOT NULL DEFAULT 1") }
        listOf("notes", "genres", "studios").forEach { db.execSQL("ALTER TABLE collection ADD COLUMN $it TEXT NOT NULL DEFAULT ''") }
        db.execSQL("CREATE TABLE IF NOT EXISTS history (id TEXT NOT NULL, mediaId INTEGER NOT NULL, at INTEGER NOT NULL, delta INTEGER NOT NULL, unit TEXT NOT NULL, minutes INTEGER NOT NULL, PRIMARY KEY(id))")
    }
}
