package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

internal object AssetMirrors {
    const val BMC_API = "https://bmclapi2.bangbang93.com"
    const val MODRINTH_API = "https://api.modrinth.com/v2"
    const val BROWSER_UA = "blockprintcat/1.0"

    /** Mojang official URL → BMCLAPI mirror. Mirrors the legacy swap table. */
    fun mojang(originalUrl: String): String = originalUrl
        .replace("https://launchermeta.mojang.com/", "$BMC_API/")
        .replace("https://launcher.mojang.com/", "$BMC_API/")
        .replace("https://piston-meta.mojang.com/", "$BMC_API/")
        .replace("https://piston-data.mojang.com/", "$BMC_API/")
        .replace("https://libraries.minecraft.net/", "$BMC_API/maven/")
        .replace("https://resources.download.minecraft.net/", "$BMC_API/assets/")
        .replace("http://resources.download.minecraft.net/", "$BMC_API/assets/")
}
