package io.github.moxisuki.blockprint.cat.data.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the user's launcher-icon preference and toggles the corresponding
 * activity-alias enabled state via PackageManager. Backed by SharedPreferences
 * so the choice survives process death.
 */
@Singleton
class AppIconManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageName: String = context.packageName

    val variants: List<IconVariant> = listOf(
        IconVariant("v1", R.mipmap.ic_launcher_default, R.mipmap.ic_launcher_default_round, R.drawable.ic_launcher_background, R.string.icon_variant_v1, "$packageName.MainActivityV1"),
        IconVariant("v2", R.mipmap.ic_launcher_blue,    R.mipmap.ic_launcher_blue_round,    R.drawable.ic_launcher_background, R.string.icon_variant_v2, "$packageName.MainActivityV2"),
        IconVariant("v3", R.mipmap.ic_launcher_purple,  R.mipmap.ic_launcher_purple_round,  R.drawable.ic_launcher_background, R.string.icon_variant_v3, "$packageName.MainActivityV3"),
        IconVariant("v4", R.mipmap.ic_launcher_v4,      R.mipmap.ic_launcher_v4_round,      R.drawable.ic_launcher_background, R.string.icon_variant_v4, "$packageName.MainActivityV4"),
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _current = MutableStateFlow(migrateStoredVariant(prefs.getString(KEY_VARIANT, "v1")!!))
    val current: StateFlow<String> = _current.asStateFlow()

    /**
     * Older builds stored IDs like "default" / "blue" / "purple" — map those
     * legacy names to the v1/v2/v3 equivalent so `reconcile()` doesn't throw
     * `IllegalArgumentException` on first launch after the rename. The mapped
     * ID is persisted immediately.
     */
    private fun migrateStoredVariant(stored: String): String {
        val mapped = when (stored) {
            "default", "v1" -> "v1"
            "blue",    "v2" -> "v2"
            "purple",  "v3" -> "v3"
            else -> variants.first().id
        }
        if (mapped != stored) {
            prefs.edit().putString(KEY_VARIANT, mapped).apply()
        }
        return mapped
    }

    fun apply(variantId: String) {
        require(variants.any { it.id == variantId }) { "unknown variant: $variantId" }
        val pm = context.packageManager
        variants.forEach { v ->
            val state = if (v.id == variantId) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(
                ComponentName(context, v.aliasClassName),
                state, PackageManager.DONT_KILL_APP,
            )
        }
        prefs.edit().putString(KEY_VARIANT, variantId).apply()
        _current.value = variantId
    }

    /** Called once on app start: enforce that the enabled alias matches the stored choice. */
    fun reconcile() {
        apply(_current.value)
    }

    private companion object {
        const val PREFS = "app_icon"
        const val KEY_VARIANT = "variant"
    }
}

data class IconVariant(
    val id: String,
    @DrawableRes val iconRes: Int,
    @DrawableRes val roundIconRes: Int,
    @DrawableRes val backgroundRes: Int,
    @StringRes val nameRes: Int,
    val aliasClassName: String,
)