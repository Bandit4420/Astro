package com.bandit.astro

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bandit.astro.data.*
import com.bandit.astro.notifications.AiringNotifications
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AstroApplication : Application(), ImageLoaderFactory {
    val account by lazy { AccountStore(this) }
    private val databases = mutableMapOf<Int, AstroDatabase>()
    @Synchronized fun databaseFor(id: Int): AstroDatabase = databases.getOrPut(id) {
        Room.databaseBuilder(this, AstroDatabase::class.java, if (id == 0) "astro.db" else "astro-account-$id.db")
            .addMigrations(MIGRATION_1_2).build()
    }
    val database get() = databaseFor(account.session.value?.id ?: 0)
    val sync by lazy { SyncEngine(this) }
    val preferences by lazy { PreferenceStore(this) }
    val http by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS).build()
    }
    val api by lazy { AniListApi(http) }
    override fun onCreate() { super.onCreate(); AiringNotifications.createChannel(this) }
    override fun newImageLoader() = ImageLoader.Builder(this)
        .okHttpClient(http)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.12).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("artwork")).maxSizeBytes(64L * 1024 * 1024).build() }
        .crossfade(false).build()
}
