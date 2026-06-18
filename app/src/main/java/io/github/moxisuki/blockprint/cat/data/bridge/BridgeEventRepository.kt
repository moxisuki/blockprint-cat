package io.github.moxisuki.blockprint.cat.data.bridge

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BridgeEventRepository @Inject constructor(
    private val dao: BridgeEventDao,
) {
    fun observe(limit: Int = 200): Flow<List<BridgeEventEntity>> =
        dao.observeRecent(limit)

    suspend fun log(type: String, message: String, host: String? = null, port: Int? = null) {
        val now = System.currentTimeMillis()
        dao.insert(
            BridgeEventEntity(
                ts = now,
                type = type,
                message = message,
                host = host,
                wsPort = port,
            )
        )
        dao.trim(200)
    }

    suspend fun clear() = dao.clear()
}