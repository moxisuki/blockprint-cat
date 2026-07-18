package io.github.moxisuki.blockprint.cat.app.feature.about.data

import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import javax.inject.Inject

class AboutRepository @Inject constructor(
    private val remoteDataSource: HitokotoRemoteDataSource,
) {
    suspend fun loadHitokoto(): AppNetworkResult<HitokotoQuote> =
        remoteDataSource.fetchHitokoto()
}
