package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.domain.model.Group
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val api: ListenerApi,
) {
    suspend fun getGroups(): List<Group> =
        api.groups().data.map { it.toDomain() }
}
