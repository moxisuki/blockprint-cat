package io.github.moxisuki.blockprint.cat.ui.home

import androidx.lifecycle.ViewModel
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val blueprintManager: BlueprintManager,
) : ViewModel() {

    val blueprints = blueprintManager.blueprints
    val blueprintCount = blueprintManager.blueprintCount
    val scanning = blueprintManager.scanning

    fun safFolderName(): String? {
        val state = blueprintManager.safState.value
        return (state as? io.github.moxisuki.blockprint.cat.data.saf.SafState.Ready)?.displayName
    }
}
