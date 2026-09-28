package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.asApiException
import es.mixmat.listener.data.api.dto.ReportRequest
import es.mixmat.listener.data.api.dto.ShareRequest
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.domain.model.HistoryDetail
import es.mixmat.listener.domain.model.HistoryItem
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

data class HistoryPage(
    val items: List<HistoryItem>,
    val cursor: String?,
    val hasMore: Boolean,
)

@Singleton
class HistoryRepository @Inject constructor(
    private val api: ListenerApi,
) {
    suspend fun getHistory(cursor: String? = null, limit: Int? = null): HistoryPage {
        val response = api.history(cursor, limit).data
        return HistoryPage(
            items = response.items.map { it.toDomain() },
            cursor = response.cursor,
            hasMore = response.hasMore,
        )
    }

    suspend fun getDetail(id: String): HistoryDetail =
        api.historyDetail(id).data.toDomain()

    suspend fun delete(id: String) {
        api.historyDelete(id)
    }

    /**
     * Throws [es.mixmat.listener.data.api.ApiException] when the server sends an
     * error code, so callers can tell a locked group from a group the user has
     * left — 403 here carries `group_locked`, `not_found` and
     * `auth_listen_disabled`, and each needs different copy.
     */
    suspend fun share(id: String, groupIds: List<String>): Map<String, String> =
        try {
            api.historyShare(id, ShareRequest(groupIds)).data.results
                .associate { it.groupId to it.status }
        } catch (e: HttpException) {
            throw e.asApiException() ?: e
        }

    suspend fun report(id: String, reason: String? = null) {
        api.historyReport(id, ReportRequest(reason))
    }
}
