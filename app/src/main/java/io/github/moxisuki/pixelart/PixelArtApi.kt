package io.github.moxisuki.pixelart.api

import android.graphics.Bitmap
import io.github.moxisuki.pixelart.*
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis

object PixelArtApi {

    fun convert(request: PixelArtRequest): PixelArtResponse {
        lateinit var result: ConversionResult
        val elapsed = measureTimeMillis {
            val customBlocks = PaletteApi.createCustomPalette(request.customPalette)
            result = PixelArtConverter.convert(request.image, request.toOptions(), customBlocks)
        }
        return buildResponse(request, result, elapsed)
    }

    fun convertBitmap(bitmap: Bitmap, builderBlock: (PixelArtRequest.Builder.() -> Unit)? = null): PixelArtResponse {
        val builder = PixelArtRequest.builder().image(bitmap)
        builderBlock?.invoke(builder)
        return convert(builder.build())
    }

    fun toBlueprint(
        response: PixelArtResponse,
        originX: Int = 0, originY: Int = 64, originZ: Int = 0,
        direction: String = "north"
    ): BlueprintBuilder {
        return BlueprintBuilder()
            .name("pixel_art")
            .author("PixelArtApi")
            .consume(response, originX = originX, originY = originY, originZ = originZ, direction = direction)
    }

    fun saveImage(bitmap: Bitmap, path: String) {
        val file = java.io.File(path)
        file.parentFile?.mkdirs()
        val format = when {
            path.endsWith(".png", ignoreCase = true) -> Bitmap.CompressFormat.PNG
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> Bitmap.CompressFormat.JPEG
            else -> Bitmap.CompressFormat.PNG
        }
        FileOutputStream(file).use { bitmap.compress(format, 100, it) }
    }

    private fun buildResponse(request: PixelArtRequest, result: ConversionResult, elapsed: Long): PixelArtResponse {
        val width = result.width; val height = result.height; val totalBlocks = width * height
        val materialMap = PixelArtConverter.getMaterialList(result)
        val materialList = materialMap.map { (name, count) ->
            val block = PaletteApi.getBlock(name)
            BlockEntry(
                name = name,
                count = count,
                color = block?.let { "#%02x%02x%02x".format(it.rgb.first, it.rgb.second, it.rgb.third) } ?: "#000000",
                group = block?.group ?: "unknown"
            )
        }.sortedByDescending { it.count }

        val blockGrid: Array<Array<BlockState?>> = Array(height) { y ->
            Array(width) { x -> result.blocks[y][x]?.let { BlockState("minecraft:${it.name}") } }
        }

        val csv = if (request.generateCsv) ExportApi.generateCsv(materialList) else ""
        val commands = if (request.generateCommands) ExportApi.generateCommands(result.blocks, width, height, request.weDirection) else null
        val schematicData = if (request.generateSchematicData) ExportApi.buildSchematicData(result.blocks, width, height, request.minecraftVersion) else null

        val response = PixelArtResponse(width, height, totalBlocks, result.outputImage, materialList, csv, commands, schematicData, elapsed)
        response._internalBlockGrid = blockGrid
        return response
    }

    private fun PixelArtRequest.toOptions(): ConversionOptions = ConversionOptions(
        targetWidth = targetWidth, ditherMethod = ditherMethod, blockGroups = blockGroups,
        brightness = brightness, contrast = contrast, saturation = saturation,
        transparencyEnabled = transparencyEnabled, transparencyTolerance = transparencyTolerance,
        staircaseMode = staircaseMode
    )
}
