package com.muddassir.deathcode.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.muddassir.deathcode.data.local.db.dao.CommunitySubmissionDao
import com.muddassir.deathcode.data.local.db.dao.ContentNodeDao
import com.muddassir.deathcode.data.local.db.dao.KeyboardKeywordDao
import com.muddassir.deathcode.data.local.db.dao.PrivateOverrideDao
import com.muddassir.deathcode.data.local.db.dao.SearchDao
import com.muddassir.deathcode.data.local.db.dao.SnippetDao
import com.muddassir.deathcode.data.local.db.dao.SyncMetadataDao
import com.muddassir.deathcode.data.local.db.entity.CommunitySubmissionEntity
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.KeyboardKeywordEntity
import com.muddassir.deathcode.data.local.db.entity.NodeFtsEntity
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity
import com.muddassir.deathcode.data.local.db.entity.SnippetEntity
import com.muddassir.deathcode.data.local.db.entity.SyncMetadataEntity
import com.muddassir.deathcode.domain.model.ContentSource

/** Persists the [ContentSource] enum as its stable name. */
class ContentSourceConverter {
    @TypeConverter
    fun fromSource(source: ContentSource): String = source.name

    @TypeConverter
    fun toSource(name: String): ContentSource =
        ContentSource.entries.firstOrNull { it.name == name } ?: ContentSource.PRIVATE
}

/**
 * The runtime source of truth for the Android app.
 *
 * Normal usage (browse, search, notes, snippets, keyboard) never needs the backend; the
 * backend only feeds this database through the sync layer.
 */
@Database(
    entities = [
        ContentNodeEntity::class,
        NodeFtsEntity::class,
        PrivateOverrideEntity::class,
        SnippetEntity::class,
        KeyboardKeywordEntity::class,
        SyncMetadataEntity::class,
        CommunitySubmissionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ContentSourceConverter::class)
abstract class DeathCodeDatabase : RoomDatabase() {

    abstract fun contentNodeDao(): ContentNodeDao
    abstract fun searchDao(): SearchDao
    abstract fun privateOverrideDao(): PrivateOverrideDao
    abstract fun snippetDao(): SnippetDao
    abstract fun keyboardKeywordDao(): KeyboardKeywordDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun communitySubmissionDao(): CommunitySubmissionDao

    companion object {
        const val NAME = "deathcode.db"

        fun build(context: Context): DeathCodeDatabase =
            Room.databaseBuilder(context, DeathCodeDatabase::class.java, NAME)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            // Room does not guarantee FK enforcement for self-referencing
                            // cascades; enable it explicitly so deleting a node removes its
                            // whole subtree and any attached private overrides.
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    },
                )
                .build()
    }
}
