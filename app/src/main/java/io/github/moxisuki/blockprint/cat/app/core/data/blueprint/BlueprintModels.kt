package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

data class LocalBlueprint(
    val id: String,
    val documentId: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val format: BlueprintFormat,
    val category: String,
    val blockCount: Int,
    val regionCount: Int,
    val sizeBytes: Long,
    val lastModifiedAt: Long,
)

data class BlueprintMaterial(
    val blockId: String,
    val count: Int,
)

enum class BlueprintFormat {
    Litematica,
    Schematic,
    Nbt,
    BuildingHelper,
    Unknown,
}

internal data class BlueprintFileEntry(
    val documentId: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedAt: Long,
)

internal data class ParsedBlueprint(
    val displayName: String,
    val author: String,
    val format: BlueprintFormat,
    val blockCount: Int,
    val regionCount: Int,
    val materials: List<BlueprintMaterial>,
)

data class BlueprintImportPreview(
    val sourceUri: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val format: BlueprintFormat,
    val blockCount: Int,
    val regionCount: Int,
    val sizeBytes: Long,
)
