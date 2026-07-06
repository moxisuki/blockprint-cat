package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintSink
import io.github.moxisuki.blockprint.core.SchematicFormat

/**
 * Test-only [BlueprintSink] that records every ingest call. Avoids pulling in
 * Hilt's full BlueprintManager graph for unit tests of BlueprintPreviewViewModel
 * state setters / init logic.
 */
internal object NoopBlueprintSink : BlueprintSink {
    val ingested: MutableList<Pair<String, Int>> = mutableListOf()

    override suspend fun ingest(name: String, bytes: ByteArray, onProgress: ((Long, Long) -> Unit)?): BlueprintMeta {
        ingested.add(name to bytes.size)
        return BlueprintMeta(
            uuid = "test-${ingested.size}",
            fileDocId = "doc-$name",
            fileName = name,
            displayName = name,
            author = "test",
            regionCount = 1,
            blockCount = bytes.size,
            format = SchematicFormat.Litematica,
        )
    }
}