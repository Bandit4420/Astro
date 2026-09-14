package com.bandit.astro

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandit.astro.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun migrationPreservesFirstPreviewProgressAndAddsMetadata()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val name="migration-test.db";context.deleteDatabase(name)
        val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use {db->
            db.execSQL("CREATE TABLE collection (mediaId INTEGER NOT NULL PRIMARY KEY,title TEXT NOT NULL,cover TEXT NOT NULL,type TEXT NOT NULL,format TEXT NOT NULL,total INTEGER NOT NULL,duration INTEGER NOT NULL,progress INTEGER NOT NULL,status TEXT NOT NULL,updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE notified (airingId INTEGER NOT NULL PRIMARY KEY,sentAt INTEGER NOT NULL)")
            db.execSQL("INSERT INTO collection VALUES (1,'Test','','ANIME','TV',12,24,7,'CURRENT',1000)")
            db.version=1
        }
        val migrated=Room.databaseBuilder(context,AstroDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
        try {val e=migrated.dao().entry(1)!!;assertEquals(7,e.progress);assertEquals("",e.notes);assertTrue(e.dirty);assertEquals(0,e.volumes)
            migrated.dao().event(HistoryEvent(mediaId=1,delta=1,unit="episode"));assertEquals(1,migrated.dao().history().size)
        } finally {migrated.close();context.deleteDatabase(name)}
    }
    @Test fun aConcurrentEditRemainsDirtyAfterSyncAcknowledgement()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,AstroDatabase::class.java).build()
        try {db.dao().add(LibraryEntry(1,"Test","","ANIME","TV",12,24));db.dao().increment(1);db.dao().acknowledge(1,1,123)
            val entry=db.dao().entry(1)!!;assertTrue(entry.dirty);assertEquals(1,entry.progress);assertEquals(123L,entry.remoteUpdated)
            db.dao().acknowledge(1,entry.revision,124);assertFalse(db.dao().entry(1)!!.dirty)
        }finally{db.close()}
    }
}
