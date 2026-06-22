package io.github.moxisuki.blockprint.cat.ui.preview

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PreviewEntryPoint {
    fun blueprintManager(): BlueprintManager

    companion object {
        fun resolve(context: Context): BlueprintManager =
            dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, PreviewEntryPoint::class.java)
                .blueprintManager()
    }
}
