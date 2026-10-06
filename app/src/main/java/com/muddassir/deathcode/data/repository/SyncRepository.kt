package com.muddassir.deathcode.data.repository

import androidx.room.withTransaction
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.slugify
import com.muddassir.deathcode.data.local.db.DeathCodeDatabase
import com.muddassir.deathcode.data.local.db.dao.ContentNodeDao
import com.muddassir.deathcode.data.local.db.dao.KeyboardKeywordDao
import com.muddassir.deathcode.data.local.db.dao.SearchDao
import com.muddassir.deathcode.data.local.db.dao.SyncMetadataDao
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.KeyboardKeywordEntity
import com.muddassir.deathcode.data.local.db.entity.SyncMetadataEntity
import com.muddassir.deathcode.data.remote.ApiException
import com.muddassir.deathcode.data.remote.ContentChecksum
import com.muddassir.deathcode.data.remote.ContentPackageValidator
import com.muddassir.deathcode.data.remote.DeathCodeApi
import com.muddassir.deathcode.data.remote.dto.ContentNodeDto
import com.muddassir.deathcode.data.remote.dto.ContentPackageDto
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Observable synchronization state for the settings/sync UI. */
sealed interface SyncState {
    data class UpToDate(val version: Long) : SyncState
    data class UpdateAvailable(val version: Long) : SyncState
    data object Syncing : SyncState
    data class Failed(val message: String, val retryable: Boolean = true) : SyncState
    data class NotConfigured(val message: String = "No backend configured") : SyncState
}

/** Outcome of applying a downloaded package. */
data class ApplyResult(
    val applied: Boolean,
    val version: Long,
    val nodeCount: Int,
    val message: String,
)

/**
 * Official + approved-Community content distribution.
 *
 * Safety rules implemented here (per spec §36):
 * - the current local version is never destroyed because an update fails;
 * - a download is validated (checksum) before anything is written;
 * - the write happens in a single transaction, so a crash mid-update rolls back;
 * - private notes/templates attached to surviving content are preserved, because nodes are
 *   keyed by a deterministic id derived from their server id and are upserted in place.
 */
class SyncRepository(
    private val db: DeathCodeDatabase,
    private val nodeDao: ContentNodeDao,
    private val searchDao: SearchDao,
    private val keywordDao: KeyboardKeywordDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val settingsRepository: SettingsRepository,
    private val contentRepository: ContentRepository,
    private val api: DeathCodeApi,
) {

    fun observeVersion(source: ContentSource): Flow<Long> =
        syncMetadataDao.observeValue(versionKey(source)).map { it?.toLongOrNull() ?: 0L }

    suspend fun localVersion(source: ContentSource): Long =
        syncMetadataDao.getValue(versionKey(source))?.toLongOrNull() ?: 0L

    /**
     * Checks the backend for a newer published version and applies it when found.
     *
     * Any failure (offline, malformed payload, checksum mismatch, write error) leaves the
     * existing local content exactly as it was.
     */
    suspend fun checkAndSync(source: ContentSource = ContentSource.OFFICIAL): SyncState {
        val settings = settingsRepository.settings.first()
        return sync(source, settings.backendBaseUrl)
    }

    /** Synchronizes [source] using an explicit base URL. */
    suspend fun sync(source: ContentSource, baseUrl: String): SyncState {
        if (baseUrl.isBlank()) return SyncState.NotConfigured()

        val kind = kindOf(source)
        val local = localVersion(source)

        return try {
            val manifest = api.fetchManifest(baseUrl, kind)
            if (manifest.version <= local) {
                SyncState.UpToDate(local)
            } else {
                val packageDto = api.fetchPackage(baseUrl, kind)
                val result = applyPackage(packageDto, source)
                if (result.applied) {
                    settingsRepository.setLastSyncMessage("Updated to version ${result.version}")
                    SyncState.UpToDate(result.version)
                } else {
                    SyncState.Failed(result.message, retryable = true)
                }
            }
        } catch (e: ApiException) {
            val message = if (e.isNetworkError) "Offline or server unreachable" else e.message.orEmpty()
            settingsRepository.setLastSyncMessage("Sync failed — will retry later")
            SyncState.Failed(message, retryable = true)
        } catch (e: Exception) {
            settingsRepository.setLastSyncMessage("Sync failed — will retry later")
            SyncState.Failed(e.message ?: "Synchronization failed", retryable = true)
        }
    }

    /**
     * Validates and applies a package. Never partially applies: everything happens in one
     * transaction, and validation runs before the transaction opens.
     */
    suspend fun applyPackage(
        packageDto: ContentPackageDto,
        source: ContentSource,
        force: Boolean = false,
    ): ApplyResult {
        if (packageDto.nodes.isEmpty()) {
            return ApplyResult(false, localVersion(source), 0, "Package contained no content")
        }

        if (!ContentChecksum.isIntact(packageDto)) {
            return ApplyResult(
                applied = false,
                version = localVersion(source),
                nodeCount = 0,
                message = "Checksum mismatch — local content kept unchanged",
            )
        }

        val local = localVersion(source)
        if (!force && packageDto.packageVersion <= local) {
            return ApplyResult(false, local, 0, "Already up to date")
        }

        // A full snapshot must be internally consistent; a partial package may attach to
        // content the device already has, so unresolved parents are tolerated.
        val ordered = ContentPackageValidator.orderParentsFirst(
            packageDto.nodes,
            requireParents = packageDto.fullSnapshot,
        ) ?: return ApplyResult(
                applied = false,
                version = local,
                nodeCount = 0,
                message = "Malformed package: inconsistent parent references",
            )

        return try {
            db.withTransaction {
                val incomingRemoteIds = ordered.map { it.id }.toSet()
                val prefix = idPrefix(source)

                ordered.forEach { dto ->
                    nodeDao.upsert(dto.toEntity(source, prefix))
                }

                // Only a declared full snapshot may remove content. A partial publication
                // can add and update, but never deletes local content — so an incomplete or
                // delta package can never destroy a working local library.
                if (packageDto.fullSnapshot) {
                    val stale = nodeDao.getAllBySource(source)
                        .filter { it.remoteId != null && it.remoteId !in incomingRemoteIds }
                        .map { it.id }
                    if (stale.isNotEmpty()) {
                        nodeDao.deleteByIds(stale)
                        searchDao.deleteByNodeIds(stale)
                        keywordDao.deleteForTargets(KeyboardKeywordEntity.TARGET_NODE, stale)
                    }
                }

                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        key = versionKey(source),
                        value = packageDto.packageVersion.toString(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        key = checksumKey(source),
                        value = packageDto.checksum.ifBlank { ContentChecksum.compute(packageDto) },
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        key = SyncMetadataEntity.KEY_LAST_SYNC_AT,
                        value = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )

                // Paths and keyword mappings may have changed for the whole source.
                contentRepository.reindexAll()
            }

            ApplyResult(
                applied = true,
                version = packageDto.packageVersion,
                nodeCount = ordered.size,
                message = "Applied version ${packageDto.packageVersion}",
            )
        } catch (e: Exception) {
            // The transaction rolled back; the previous local version is still intact.
            ApplyResult(
                applied = false,
                version = localVersion(source),
                nodeCount = 0,
                message = "Update failed and was rolled back: ${e.message}",
            )
        }
    }

    /** Marks the bundled package as seeded without contacting any server. */
    suspend fun markBundledSeeded(version: Long) {
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = SyncMetadataEntity.KEY_BUNDLED_SEEDED,
                value = "true",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = versionKey(ContentSource.OFFICIAL),
                value = version.toString(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun isBundledSeeded(): Boolean =
        syncMetadataDao.getValue(SyncMetadataEntity.KEY_BUNDLED_SEEDED) == "true"

    /**
     * Publishes the locally edited official content as a new content version.
     *
     * This is the device-side half of the Draft → Preview → Publish flow: the admin edits
     * and previews locally, and publishing bumps the version so devices can detect that
     * their local content is behind.
     */
    suspend fun publishOfficialDraft(): Long {
        val next = localVersion(ContentSource.OFFICIAL) + 1
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = SyncMetadataEntity.KEY_OFFICIAL_VERSION,
                value = next.toString(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        settingsRepository.setLastSyncMessage("Published official version $next")
        return next
    }

    // --------------------------------------------------------------- helpers

    private fun ContentNodeDto.toEntity(source: ContentSource, prefix: String): ContentNodeEntity {
        val now = System.currentTimeMillis()
        return ContentNodeEntity(
            id = "$prefix:$id",
            parentId = parentId?.let { "$prefix:$it" },
            title = title,
            slug = slug.ifBlank { slugify(title) },
            markdown = markdown,
            syntax = syntax,
            notes = notes,
            language = language,
            keywords = KeywordList.serialize(keywords),
            source = source,
            remoteId = id,
            contentVersion = 0,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun kindOf(source: ContentSource): String = when (source) {
        ContentSource.OFFICIAL -> "official"
        ContentSource.COMMUNITY -> "community"
        ContentSource.PRIVATE -> "official"
    }

    private fun idPrefix(source: ContentSource): String = when (source) {
        ContentSource.OFFICIAL -> "official"
        ContentSource.COMMUNITY -> "community"
        ContentSource.PRIVATE -> "official"
    }

    private fun versionKey(source: ContentSource): String = when (source) {
        ContentSource.COMMUNITY -> SyncMetadataEntity.KEY_COMMUNITY_VERSION
        else -> SyncMetadataEntity.KEY_OFFICIAL_VERSION
    }

    private fun checksumKey(source: ContentSource): String = when (source) {
        ContentSource.COMMUNITY -> SyncMetadataEntity.KEY_COMMUNITY_CHECKSUM
        else -> SyncMetadataEntity.KEY_OFFICIAL_CHECKSUM
    }
}
