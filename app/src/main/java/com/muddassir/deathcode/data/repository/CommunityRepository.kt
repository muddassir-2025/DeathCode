package com.muddassir.deathcode.data.repository

import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.newId
import com.muddassir.deathcode.data.local.db.dao.CommunitySubmissionDao
import com.muddassir.deathcode.data.local.db.entity.CommunitySubmissionEntity
import com.muddassir.deathcode.data.remote.ApiException
import com.muddassir.deathcode.data.remote.DeathCodeApi
import com.muddassir.deathcode.data.remote.dto.CommunitySubmissionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** A local Community submission draft. */
data class CommunityDraft(
    val title: String,
    val markdown: String,
    val syntax: String? = null,
    val language: String? = null,
    val keywords: List<String> = emptyList(),
)

/** Result of pushing a submission to the backend. */
data class SubmitResult(
    val success: Boolean,
    val message: String,
    val needsAuth: Boolean = false,
)

/**
 * Community is a curated programming-content library, not a social network.
 *
 * Because only explicitly submitted content may leave the device, the flow is deliberately
 * two-step: content is written locally first (so nothing is lost offline), then uploaded
 * when the user is signed in and online.
 */
class CommunityRepository(
    private val submissionDao: CommunitySubmissionDao,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val api: DeathCodeApi,
) {

    fun observeSubmissions(): Flow<List<CommunitySubmissionEntity>> =
        submissionDao.observeAll()

    suspend fun getById(id: String): CommunitySubmissionEntity? = submissionDao.getById(id)

    suspend fun pendingCount(): Int =
        submissionDao.countByStatus(CommunitySubmissionEntity.STATUS_PENDING)

    suspend fun saveDraft(draft: CommunityDraft, id: String? = null): String {
        val existing = id?.let { submissionDao.getById(it) }
        val now = System.currentTimeMillis()
        val entity = CommunitySubmissionEntity(
            id = existing?.id ?: newId("sub"),
            title = draft.title.trim().ifEmpty { "Untitled submission" },
            markdown = draft.markdown,
            syntax = draft.syntax,
            language = draft.language,
            keywords = KeywordList.serialize(draft.keywords),
            status = existing?.status ?: CommunitySubmissionEntity.STATUS_DRAFT,
            remoteId = existing?.remoteId,
            reviewMessage = existing?.reviewMessage,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        submissionDao.upsert(entity)
        return entity.id
    }

    suspend fun delete(id: String) = submissionDao.deleteById(id)

    /**
     * Uploads a submission. Requires a signed-in community account; the server applies
     * rate limits and moderation, so the client never decides whether content is public.
     */
    suspend fun submit(id: String): SubmitResult {
        val submission = submissionDao.getById(id)
            ?: return SubmitResult(false, "Submission not found")

        val session = authRepository.session.first()
        if (!session.isSignedIn) {
            return SubmitResult(false, "Sign in to submit to Community", needsAuth = true)
        }

        val baseUrl = settingsRepository.settings.first().backendBaseUrl
        if (baseUrl.isBlank()) {
            return SubmitResult(false, "No backend configured. Add an API URL in Settings.")
        }

        return try {
            val response = api.submitCommunity(
                baseUrl = baseUrl,
                token = session.token!!,
                submission = CommunitySubmissionDto(
                    id = submission.remoteId,
                    title = submission.title,
                    markdown = submission.markdown,
                    syntax = submission.syntax,
                    language = submission.language,
                    keywords = KeywordList.parse(submission.keywords),
                ),
            )
            submissionDao.upsert(
                submission.copy(
                    remoteId = response.id,
                    status = response.status.uppercase()
                        .takeIf { it in RESOLVED_STATUSES }
                        ?: CommunitySubmissionEntity.STATUS_PENDING,
                    reviewMessage = response.message,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            SubmitResult(true, response.message ?: "Submitted for review")
        } catch (e: ApiException) {
            submissionDao.upsert(
                submission.copy(
                    status = CommunitySubmissionEntity.STATUS_FAILED,
                    reviewMessage = e.message,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            SubmitResult(false, e.message ?: "Upload failed — will retry later")
        } catch (e: Exception) {
            submissionDao.upsert(
                submission.copy(
                    status = CommunitySubmissionEntity.STATUS_FAILED,
                    reviewMessage = e.message,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            SubmitResult(false, e.message ?: "Upload failed — will retry later")
        }
    }

    private companion object {
        val RESOLVED_STATUSES = setOf(
            CommunitySubmissionEntity.STATUS_PENDING,
            CommunitySubmissionEntity.STATUS_APPROVED,
            CommunitySubmissionEntity.STATUS_REJECTED,
        )
    }
}
