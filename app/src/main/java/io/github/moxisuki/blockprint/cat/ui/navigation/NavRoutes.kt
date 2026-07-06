package io.github.moxisuki.blockprint.cat.ui.navigation

import io.github.moxisuki.blockprint.cat.data.community.CommunitySource

object NavRoutes {
    const val HOME = "home"
    const val COMMUNITY = "community"
    const val COMMUNITY_LOGIN = "community/login"
    const val COMMUNITY_DETAIL = "community/detail"
    const val SETTINGS = "settings"
    const val TOOLS = "tools"
    const val IMAGE_TO_BLUEPRINT = "tools/image_to_blueprint"
    const val TEXT_TO_BLUEPRINT = "tools/text_to_blueprint"
    const val CONNECTION = "connection"
    const val QR_SCANNER = "qr-scanner"
    const val RENDER = "render"
    const val DETAIL = "detail"
    const val PREVIEW = "preview"
    const val ABOUT = "about"
    const val CHANGELOG = "changelog"
    const val TERMS = "terms"
    const val COMMUNITY_SETTINGS = "community-settings"
    const val BLUEPRINT_PREVIEW = "blueprintPreview"
    const val BLUEPRINT_PREVIEW_ROUTE = "$BLUEPRINT_PREVIEW/{result}"

    fun detailRoute(uuid: String): String = "$DETAIL/$uuid"
    fun previewRoute(uuid: String): String = "$PREVIEW/$uuid"

    /** Build a community-detail route — source + id 两段。 */
    fun communityDetailRoute(source: CommunitySource, id: String): String =
        "$COMMUNITY_DETAIL/${source.name}/$id"

    fun renderWithMod(slug: String): String = "$RENDER?mod=$slug"

    fun blueprintPreviewRoute(encodedResult: String): String =
        "$BLUEPRINT_PREVIEW/${java.net.URLEncoder.encode(encodedResult, "UTF-8")}"

    fun imageToBlueprintRoute(imageUri: String? = null): String =
        if (imageUri.isNullOrBlank()) IMAGE_TO_BLUEPRINT
        else "$IMAGE_TO_BLUEPRINT?imageUri=${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
}
