package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.asApiException
import es.mixmat.listener.data.api.dto.CreateGroupRequest
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.domain.model.GroupList
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
     *
     * Never cached: `canCreate` is the server's call and is re-read every time.
     */
    suspend fun getGroups(): GroupList =
        try {
            api.groups().data.toDomain()
        } catch (e: HttpException) {
            throw e.asApiException() ?: e
        }

    /**
     * Refuses the whole request with `already_has_group`, `name_required` or
     * `auth_listen_disabled` (403), or `name_taken` (409), all as ApiException.
     */
    suspend fun createGroup(name: String): Group =
        try {
            api.createGroup(CreateGroupRequest(name = name)).data.toDomain()
        } catch (e: HttpException) {
            throw e.asApiException() ?: e
        }
}
