package io.github.moxisuki.blockprint.cat.app.feature.community.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class McsCommunityModelsTest {
    @Test
    fun parseDetail_readsNewApiBlueprintShape() {
        val detail = McsCommunityParser.parseDetail(
            """
            {
              "id": "blueprint-1",
              "slug": "small-factory",
              "title": "Small Factory",
              "description": "A compact factory.",
              "attribution": "original",
              "author": {
                "id": "author-1",
                "displayName": "Builder",
                "level": 2
              },
              "category": {
                "id": "category-1",
                "slug": "create-factory",
                "name": "Factories",
                "iconKey": "modded"
              },
              "namespaces": ["create", "minecraft"],
              "sourceGameVersion": {
                "edition": "java",
                "release": "1.21.1",
                "family": "java-1.21"
              },
              "currentVersion": {
                "number": 3,
                "sourceFormat": "create",
                "createdAt": "2026-09-12T10:00:00Z"
              },
              "engagement": {
                "viewCount": 112,
                "downloadCount": 19,
                "likeCount": 4,
                "favouriteCount": 7
              },
              "trustedAnalysis": {
                "sourceGameVersion": {
                  "edition": "java",
                  "release": "1.21.1",
                  "family": "java-1.21"
                },
                "dimensions": {"x": 21, "y": 5, "z": 8},
                "sourceBlockCount": 294,
                "visibleBlockCount": 290,
                "paletteSize": 44,
                "tileEntityCount": 2,
                "entityCount": 1,
                "materialKindCount": 3,
                "namespaces": ["create", "minecraft"],
                "materials": [
                  {"blockId": "create:shaft", "count": 12},
                  {"blockId": "minecraft:stone", "count": 4}
                ],
                "transportAvailable": true
              },
              "viewerSourceFormat": "litematica",
              "sourceUrl": "/api/v1/blueprints/blueprint-1/versions/3/source",
              "downloadUrl": "/api/v1/blueprints/blueprint-1/versions/3/download",
              "previewUrl": "/api/v1/blueprints/blueprint-1/versions/3/preview?r=3",
              "previewImages": [
                {
                  "url": "/api/v1/blueprints/blueprint-1/versions/3/preview?r=3",
                  "role": "cover"
                }
              ],
              "originalSourceByteSize": 7430,
              "viewerSourceByteSize": 5169
            }
            """.trimIndent(),
        )

        assertThat(detail.summary.id).isEqualTo("blueprint-1")
        assertThat(detail.summary.currentVersion.number).isEqualTo(3)
        assertThat(detail.summary.currentVersion.sourceFormat).isEqualTo("create")
        assertThat(detail.summary.gameVersion.release).isEqualTo("1.21.1")
        assertThat(detail.summary.category?.name).isEqualTo("Factories")
        assertThat(detail.summary.engagement.downloadCount).isEqualTo(19)
        assertThat(detail.analysis.dimensions.toString()).isEqualTo("21 x 5 x 8")
        assertThat(detail.analysis.materials.first().blockId).isEqualTo("create:shaft")
        assertThat(detail.analysis.materials.first().count).isEqualTo(12)
        assertThat(detail.viewerSourceFormat).isEqualTo("litematica")
        assertThat(detail.previewImages.single()).isEqualTo(
            "https://www.mcschematic.top/api/v1/blueprints/blueprint-1/versions/3/preview?r=3",
        )
        assertThat(detail.downloadUrl).isEqualTo(
            "https://www.mcschematic.top/api/v1/blueprints/blueprint-1/versions/3/download",
        )
    }
}
