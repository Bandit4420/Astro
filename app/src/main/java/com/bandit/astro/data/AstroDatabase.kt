package com.bandit.astro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notified")
data class NotifiedEpisode(@PrimaryKey val airingId: Int, val sentAt: Long)

@Dao
interface AstroDao {
    @Query("SELECT * FROM collection ORDER BY updatedAt DESC")
    fun observeCollection(): Flow<List<LibraryEntry>>
    @Query("SELECT * FROM collection ORDER BY updatedAt DESC")
    suspend fun entries(): List<LibraryEntry>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entry: LibraryEntry)
    @Query("DELETE FROM collection WHERE mediaId = :id")
    suspend fun remove(id: Int)
    @Query("UPDATE collection SET progress = CASE WHEN total > 0 THEN MIN(progress + 1, total) ELSE MIN(progress + 1, 2147483647) END, status = CASE WHEN total > 0 AND progress + 1 >= total THEN 'COMPLETED' ELSE 'CURRENT' END, updatedAt = :now WHERE mediaId = :id")
    suspend fun increment(id: Int, now: Long = System.currentTimeMillis())
    @Query("UPDATE collection SET status = :status, updatedAt = :now WHERE mediaId = :id")
    suspend fun status(id: Int, status: String, now: Long = System.currentTimeMillis())
    @Query("SELECT COUNT(*) FROM notified WHERE airingId = :id")
    suspend fun wasNotified(id: Int): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markNotified(value: NotifiedEpisode)
    @Query("DELETE FROM notified WHERE sentAt < :before")
    suspend fun pruneNotifications(before: Long)
}

@Database(entities = [LibraryEntry::class, NotifiedEpisode::class], version = 1, exportSchema = true)
abstract class AstroDatabase : RoomDatabase() { abstract fun dao(): AstroDao }
