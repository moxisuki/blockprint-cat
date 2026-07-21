package io.github.moxisuki.blockprint.cat.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.IconIndexResolver
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import io.github.moxisuki.blockprint.cat.app.feature.home.toHomeBlueprintItem
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BlueprintDetailViewModel @Inject constructor(
    private val blueprintRepository: BlueprintRepository,
    private val iconIndexResolver: IconIndexResolver,
) : ViewModel() {

    private val blueprintId = MutableStateFlow<String?>(null)

    internal val state: StateFlow<BlueprintDetailState> = blueprintId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(BlueprintDetailState())
            } else {
                combine(
                    blueprintRepository.observeBlueprint(id),
                    blueprintRepository.observeMaterials(id),
                    iconIndexResolver.ready,
                ) { blueprint, materials, _ ->
                    BlueprintDetailState(
                        blueprint = blueprint?.toHomeBlueprintItem(),
                        materials = materials
                            .take(10)
                            .map { material ->
                                BlueprintDetailMaterialItem(
                                    name = material.blockId,
                                    count = material.count,
                                    iconUrls = iconIndexResolver.getIconUrls(material.blockId),
                                )
                            },
                        isLoading = false,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BlueprintDetailState(),
        )

    init {
        viewModelScope.launch {
            iconIndexResolver.ensureLoaded()
        }
    }

    fun setBlueprintId(id: String) {
        blueprintId.value = id
    }
}
