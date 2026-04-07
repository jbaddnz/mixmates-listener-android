package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.dto.ShareRequest
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.domain.model.HistoryDetail
import es.mixmat.listener.domain.model.HistoryItem
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

    suspend fun share(id: String, groupIds: List<String>): Map<String, String> =
        api.historyShare(id, ShareRequest(groupIds)).data.results
}
