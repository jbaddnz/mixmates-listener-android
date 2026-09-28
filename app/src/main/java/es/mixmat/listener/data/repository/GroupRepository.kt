package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.asApiException
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.domain.model.Group
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val api: ListenerApi,
) {
    /**
     * Throws [es.mixmat.listener.data.api.ApiException] when the server sends an
     * error code. `auth_listen_disabled` reaches this endpoint from the shared
     * auth preamble and is a permanent admin flag, so it must be distinguishable
     * from a transient failure — offering "Try again" against it would be a lie.
     */
    suspend fun getGroups(): List<Group> =
        try {
            api.groups().data.items.map { it.toDomain() }
        } catch (e: HttpException) {
            throw e.asApiException() ?: e
        }
}
