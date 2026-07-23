package io.github.moxisuki.blockprint.cat.app.testfakes

import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackDao
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeResourcePackDao(initial: List<ResourcePackEntity> = emptyList()) : ResourcePackDao {
    private val state = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<ResourcePackEntity>> = state

    override suspend fun get(id: String): ResourcePackEntity? = state.value.firstOrNull { it.id == id }

    override suspend fun upsert(entity: ResourcePackEntity) {
        state.value = (state.value.filterNot { it.id == entity.id } + entity)
    }

    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() { state.value = emptyList() }
}
