package io.github.moxisuki.blockprint.cat.data.blueprint

/**
 * 写入单文件蓝图所需的最小 API。BlueprintManager 实现此接口；
 * 测试桩可以无 Hilt 依赖地实现。
 */
interface BlueprintSink {
    suspend fun ingest(name: String, bytes: ByteArray, onProgress: ((Long, Long) -> Unit)? = null): BlueprintMeta
}