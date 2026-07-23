package io.github.moxisuki.blockprint.cat.app.core.resourcepack.data

import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId

internal fun ResourcePackEntity.toDomain(): ResourcePackEntry = ResourcePackEntry(
    id = ResourcePackId(id),
    kind = ResourcePackEntry.Kind.valueOf(kind.uppercase()),
    displayName = displayName,
    version = versionName,
    mcVersion = mcVersion,
    fileCount = fileCount,
    totalSize = totalSize,
    installedAt = installedAt,
    namespaces = namespaces.split(',').filter { it.isNotBlank() }.toSet(),
    hasAssets = fileCount > 0,
)

/**
 * Source defaults to "modrinth" so call sites that don't yet know the canonical
 * source (early installers) can compose an entity without ceremony.
 * `sourceVersionId` is only meaningful for source = "modrinth".
 */
internal fun ResourcePackEntry.toEntity(
    source: String = "modrinth",
    sourceVersionId: String? = null,
): ResourcePackEntity = ResourcePackEntity(
    id = id.value,
    kind = kind.name.lowercase(),
    projectSlug = if (id.isVanilla) "" else id.modSlug,
    displayName = displayName,
    versionName = version,
    mcVersion = mcVersion,
    fileCount = fileCount,
    totalSize = totalSize,
    installedAt = installedAt,
    namespaces = namespaces.sorted().joinToString(","),
    source = source,
    sourceVersionId = sourceVersionId,
)

object ResourcePackPersistence {
    fun sample(): ResourcePackEntry = ResourcePackEntry(
        id = ResourcePackId.mod("create"),
        kind = ResourcePackEntry.Kind.MOD,
        displayName = "Create",
        version = "6.0.4",
        mcVersion = "1.20.1",
        fileCount = 234,
        totalSize = 5_242_880L,
        installedAt = 1_700_000_000_000L,
        namespaces = setOf("create", "create_connected"),
        hasAssets = true,
    )
}
