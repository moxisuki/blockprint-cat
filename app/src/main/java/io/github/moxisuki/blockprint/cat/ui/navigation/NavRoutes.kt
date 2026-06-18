package io.github.moxisuki.blockprint.cat.ui.navigation

import io.github.moxisuki.blockprint.cat.data.community.CommunitySource

object NavRoutes {
    const val HOME = "home"
    const val COMMUNITY = "community"
    const val COMMUNITY_LOGIN = "community/login"
    const val COMMUNITY_DETAIL = "community/detail"
    const val SETTINGS = "settings"
    const val CONNECTION = "connection"
    const val QR_SCANNER = "qr-scanner"
    const val RENDER = "render"
    const val DETAIL = "detail"
    const val PREVIEW = "preview"
    const val ABOUT = "about"
    const val TERMS = "terms"
    const val COMMUNITY_SETTINGS = "community-settings"

    fun detailRoute(uuid: String): String = "$DETAIL/$uuid"
    fun previewRoute(uuid: String): String = "$PREVIEW/$uuid"

    /** Build a community-detail route — source + id 两段。 */
    fun communityDetailRoute(source: CommunitySource, id: String): String =
        "$COMMUNITY_DETAIL/${source.name}/$id"

    fun renderWithMod(slug: String): String = "$RENDER?mod=$slug"
}
