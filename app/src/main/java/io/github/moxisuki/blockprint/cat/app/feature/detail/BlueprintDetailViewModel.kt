package io.github.moxisuki.blockprint.cat.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintMaterial
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.IconIndexResolver
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import io.github.moxisuki.blockprint.cat.app.feature.home.toHomeBlueprintItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BlueprintDetailViewModel @Inject constructor(
    private val blueprintRepository: BlueprintRepository,
    private val iconIndexResolver: IconIndexResolver,
    private val assetLocator: ResourcePackAssetLocator,
    private val resourcePackRepository: ResourcePackRepository,
    private val languageManager: AppLanguageManager,
    private val previewRepository: PreviewRepository,
) : ViewModel() {

    private val blueprintId = MutableStateFlow<String?>(null)
    private val _conversionState = MutableStateFlow<BlueprintConversionState>(BlueprintConversionState.Idle)
    internal val conversionState: StateFlow<BlueprintConversionState> = _conversionState.asStateFlow()

    internal val state: StateFlow<BlueprintDetailState> = blueprintId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(BlueprintDetailState())
            } else {
                combine(
                    combine(
                        blueprintRepository.observeBlueprint(id),
                        blueprintRepository.observeMaterials(id),
                        iconIndexResolver.ready,
                        languageManager.language,
                        resourcePackRepository.installedPacks,
                    ) { blueprint, materials, _, _, installedPacks ->
                        Triple(
                            blueprint,
                            materials,
                            installedPacks.flatMap { it.namespaces }.toSet(),
                        )
                    },
                    previewRepository.observeCacheStatus(id),
                ) { (blueprint, materials, installedNamespaces), previewCache ->
                    DetailSnapshot(
                        blueprint = blueprint,
                        materials = materials,
                        installedNamespaces = installedNamespaces,
                        previewCache = previewCache,
                    )
                }.transformLatest { snapshot ->
                    val blueprint = snapshot.blueprint
                    val materials = snapshot.materials
                    val installedNamespaces = snapshot.installedNamespaces
                    val namespaceItems = materialNamespaces(materials).map { namespace ->
                        BlueprintNamespaceItem(
                            namespace = namespace,
                            isInstalled = namespace in installedNamespaces,
                        )
                    }
                    val baseMaterials = materials.map { material ->
                            BlueprintDetailMaterialItem(
                                name = material.blockId,
                                count = material.count,
                                iconUrls = iconIndexResolver.getIconUrls(material.blockId),
                            )
                        }
                    val localizedMaterials = withContext(Dispatchers.IO) {
                        val localeCandidates = languageManager.resourcePackLocaleCandidates()
                        baseMaterials.map { material ->
                            material.copy(
                                displayName = assetLocator.loadDisplayName(
                                    blockId = material.name,
                                    locales = localeCandidates,
                                ),
                            )
                        }
                    }
                    emit(
                        BlueprintDetailState(
                            blueprint = blueprint?.toHomeBlueprintItem(),
                            materials = localizedMaterials,
                            namespaces = namespaceItems,
                            previewCache = snapshot.previewCache,
                            isLoading = false,
                        ),
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
        if (blueprintId.value != id) {
            _conversionState.value = BlueprintConversionState.Idle
        }
        blueprintId.value = id
    }

    fun convert(target: BlueprintFormat) {
        val id = blueprintId.value ?: return
        if (target == BlueprintFormat.BuildingHelper ||
            target == BlueprintFormat.Unknown ||
            _conversionState.value is BlueprintConversionState.Running
        ) {
            return
        }
        _conversionState.value = BlueprintConversionState.Running(target)
        viewModelScope.launch {
            try {
                val fileName = blueprintRepository.convertBlueprint(id, target)
                _conversionState.value = BlueprintConversionState.Success(target, fileName)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _conversionState.value = BlueprintConversionState.Failure(
                    error.message ?: error::class.java.simpleName,
                )
            }
        }
    }

    fun dismissConversion() {
        if (_conversionState.value !is BlueprintConversionState.Running) {
            _conversionState.value = BlueprintConversionState.Idle
        }
    }

    private fun materialNamespaces(
        materials: List<BlueprintMaterial>,
    ): List<String> {
        val namespaces = linkedSetOf("minecraft")
        materials.forEach { material ->
            val namespace = material.blockId
                .substringBefore(':', missingDelimiterValue = "minecraft")
                .trim()
                .lowercase(Locale.ROOT)
            if (namespace.isNotBlank()) namespaces += namespace
        }
        return namespaces.toList()
    }
}

private data class DetailSnapshot(
    val blueprint: io.github.moxisuki.blockprint.cat.app.core.data.blueprint.LocalBlueprint?,
    val materials: List<BlueprintMaterial>,
    val installedNamespaces: Set<String>,
    val previewCache: io.github.moxisuki.blockprint.cat.app.core.preview.PreviewCacheInfo,
)
