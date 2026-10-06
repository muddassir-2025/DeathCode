package com.muddassir.deathcode.data.seed

import com.muddassir.deathcode.data.repository.ContentRepository
import com.muddassir.deathcode.data.repository.SnippetDraft
import com.muddassir.deathcode.data.repository.SnippetRepository
import com.muddassir.deathcode.data.repository.SyncRepository
import com.muddassir.deathcode.domain.model.ContentSource

/**
 * First-run initialization.
 *
 * Runs once, entirely offline, and leaves the app immediately usable: official content,
 * the user's private root and a starter set of keyboard templates all exist before the
 * first screen renders. Nothing here depends on a successful network request.
 */
class ContentSeeder(
    private val syncRepository: SyncRepository,
    private val snippetRepository: SnippetRepository,
    private val contentRepository: ContentRepository,
) {

    suspend fun seedIfNeeded() {
        seedOfficialContent()
        seedSnippets()
        contentRepository.ensurePrivateRoot()
    }

    private suspend fun seedOfficialContent() {
        if (syncRepository.isBundledSeeded() &&
            syncRepository.localVersion(ContentSource.OFFICIAL) > 0
        ) {
            return
        }
        val packageDto = BundledOfficialContent.packageDto()
        val result = syncRepository.applyPackage(packageDto, ContentSource.OFFICIAL, force = true)
        if (result.applied) {
            syncRepository.markBundledSeeded(packageDto.packageVersion)
        }
    }

    private suspend fun seedSnippets() {
        if (snippetRepository.count() > 0) return
        BundledSnippets.all().forEach { seed ->
            snippetRepository.save(
                draft = SnippetDraft(
                    title = seed.title,
                    language = seed.language,
                    body = seed.body,
                    keywords = seed.keywords,
                ),
                source = seed.source,
            )
        }
    }
}
