package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

internal fun BlueprintEntity.toLocalBlueprint(): LocalBlueprint = LocalBlueprint(
    id = id,
    documentId = documentId,
    fileName = fileName,
    displayName = displayName,
    author = author,
    format = format.toBlueprintFormat(),
    category = category,
    blockCount = blockCount,
    regionCount = regionCount,
    sizeBytes = sizeBytes,
    lastModifiedAt = lastModifiedAt,
)

internal fun BlueprintMaterialEntity.toBlueprintMaterial(): BlueprintMaterial = BlueprintMaterial(
    blockId = blockId,
    count = count,
)

private fun String.toBlueprintFormat(): BlueprintFormat = when (this) {
    BlueprintFormat.Litematica.name -> BlueprintFormat.Litematica
    BlueprintFormat.Schematic.name -> BlueprintFormat.Schematic
    BlueprintFormat.Nbt.name -> BlueprintFormat.Nbt
    BlueprintFormat.BuildingHelper.name -> BlueprintFormat.BuildingHelper
    else -> BlueprintFormat.Unknown
}
