package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

import io.github.moxisuki.blockprint.core.MaterialList
import io.github.moxisuki.blockprint.core.SchematicFormat
import io.github.moxisuki.blockprint.core.api.BlockPrintReader
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlueprintCoreReader @Inject constructor() {

    internal fun read(bytes: ByteArray, fileName: String): ParsedBlueprint {
        val document = BlockPrintReader.readLenient(bytes)
        val fileStem = fileName.substringBeforeLast('.', fileName)
        val displayName = document.name
            .takeIf { it.isNotBlank() }
            ?: fileStem.takeIf { it.isNotBlank() }
            ?: fileName
        val materials = MaterialList.from(document, includeAir = false)
            .toSortedByCount()
            .map { (blockId, count) ->
                BlueprintMaterial(
                    blockId = blockId,
                    count = count,
                )
            }

        return ParsedBlueprint(
            displayName = displayName,
            author = document.author,
            format = document.format.toBlueprintFormat(),
            blockCount = document.blockCount(includeAir = false),
            regionCount = document.regions.size,
            materials = materials,
        )
    }

    private fun SchematicFormat.toBlueprintFormat(): BlueprintFormat = when (this) {
        SchematicFormat.Litematica -> BlueprintFormat.Litematica
        SchematicFormat.Sponge -> BlueprintFormat.Schematic
        SchematicFormat.Structure,
        SchematicFormat.PartialNbt,
        -> BlueprintFormat.Nbt
        SchematicFormat.BuildingHelper -> BlueprintFormat.BuildingHelper
        SchematicFormat.Unknown -> BlueprintFormat.Unknown
    }
}

internal fun isSupportedBlueprintFile(fileName: String): Boolean {
    val lower = fileName.lowercase(Locale.ROOT)
    return lower.endsWith(".litematic") ||
        lower.endsWith(".schematic") ||
        lower.endsWith(".schem") ||
        lower.endsWith(".nbt") ||
        lower.endsWith(".json")
}
