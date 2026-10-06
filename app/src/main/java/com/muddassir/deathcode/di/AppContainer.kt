package com.muddassir.deathcode.di

import android.content.Context
import com.muddassir.deathcode.data.local.db.DeathCodeDatabase
import com.muddassir.deathcode.data.local.search.SearchEngine
import com.muddassir.deathcode.data.remote.DeathCodeApi
import com.muddassir.deathcode.data.repository.AuthRepository
import com.muddassir.deathcode.data.repository.CommunityRepository
import com.muddassir.deathcode.data.repository.ContentRepository
import com.muddassir.deathcode.data.repository.KeyboardRepository
import com.muddassir.deathcode.data.repository.PersonalRepository
import com.muddassir.deathcode.data.repository.SettingsRepository
import com.muddassir.deathcode.data.repository.SnippetRepository
import com.muddassir.deathcode.data.repository.SyncRepository
import com.muddassir.deathcode.data.seed.ContentSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Hand-rolled dependency container.
 *
 * The app is a single module with a small graph, so a service locator keeps the build simple
 * (no annotation processing beyond Room) while still giving one obvious place to see how the
 * pieces fit together. Boundary discipline is enforced by package structure instead.
 */
class AppContainer(private val appContext: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: DeathCodeDatabase by lazy { DeathCodeDatabase.build(appContext) }

    val api: DeathCodeApi by lazy { DeathCodeApi() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val authRepository: AuthRepository by lazy { AuthRepository(appContext, api) }

    val contentRepository: ContentRepository by lazy {
        ContentRepository(
            db = database,
            nodeDao = database.contentNodeDao(),
            searchDao = database.searchDao(),
            overrideDao = database.privateOverrideDao(),
            keywordDao = database.keyboardKeywordDao(),
        )
    }

    val personalRepository: PersonalRepository by lazy {
        PersonalRepository(
            db = database,
            overrideDao = database.privateOverrideDao(),
            contentRepository = contentRepository,
        )
    }

    val snippetRepository: SnippetRepository by lazy {
        SnippetRepository(
            db = database,
            snippetDao = database.snippetDao(),
            keywordDao = database.keyboardKeywordDao(),
        )
    }

    val searchEngine: SearchEngine by lazy {
        SearchEngine(
            nodeDao = database.contentNodeDao(),
            searchDao = database.searchDao(),
            overrideDao = database.privateOverrideDao(),
        )
    }

    val keyboardRepository: KeyboardRepository by lazy {
        KeyboardRepository(
            keywordDao = database.keyboardKeywordDao(),
            snippetDao = database.snippetDao(),
            nodeDao = database.contentNodeDao(),
            overrideDao = database.privateOverrideDao(),
        )
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(
            db = database,
            nodeDao = database.contentNodeDao(),
            searchDao = database.searchDao(),
            keywordDao = database.keyboardKeywordDao(),
            syncMetadataDao = database.syncMetadataDao(),
            settingsRepository = settingsRepository,
            contentRepository = contentRepository,
            api = api,
        )
    }

    val communityRepository: CommunityRepository by lazy {
        CommunityRepository(
            submissionDao = database.communitySubmissionDao(),
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            api = api,
        )
    }

    val seeder: ContentSeeder by lazy {
        ContentSeeder(
            syncRepository = syncRepository,
            snippetRepository = snippetRepository,
            contentRepository = contentRepository,
        )
    }

    private val initMutex = Mutex()
    private var initialized = false

    /**
     * Idempotent first-run initialization. Safe to call from the activity and from the IME
     * (the keyboard process may start before the UI).
     */
    suspend fun ensureInitialized() {
        initMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) { seeder.seedIfNeeded() }
            initialized = true
        }
    }
}
