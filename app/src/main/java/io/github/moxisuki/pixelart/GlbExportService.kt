package io.github.moxisuki.pixelart.api

import android.graphics.Bitmap
import io.github.moxisuki.pixelart.ConversionOptions
import io.github.moxisuki.pixelart.ImageProcessor
import java.io.File
import java.io.OutputStream

object GlbExportService {

    enum class Layout { FLAT, VERTICAL_WALL }

    fun convert(
        image: Bitmap,
        targetWidth: Int = 128,
        assetsDirs: List<File>,
        outputFile: File,
        layout: Layout = Layout.FLAT,
        blockOptions: ConversionOptions.() -> ConversionOptions = { this }
    ) {
        val baseOptions = ConversionOptions(targetWidth = targetWidth).blockOptions()
        val response = PixelArtApi.convert(
            PixelArtRequest.builder()
                .image(image)
                .width(targetWidth)
                .dither(baseOptions.ditherMethod)
                .groups(baseOptions.blockGroups)
                .preprocessing(baseOptions.brightness, baseOptions.contrast, baseOptions.saturation)
                .build()
        )

        // GLB export requires blockprint-core asset directories and BlockPrintToGlb
        // TODO: wire up with io.github.moxisuki.blockprint.core.api.BlockPrintToGlb when assets are configured
    }

    fun convertToStream(
        image: Bitmap,
        targetWidth: Int = 128,
        assetsDirs: List<File>,
        outputStream: OutputStream,
        layout: Layout = Layout.FLAT,
        blockOptions: ConversionOptions.() -> ConversionOptions = { this }
    ) {
        val baseOptions = ConversionOptions(targetWidth = targetWidth).blockOptions()
        val response = PixelArtApi.convert(
            PixelArtRequest.builder()
                .image(image)
                .width(targetWidth)
                .dither(baseOptions.ditherMethod)
                .groups(baseOptions.blockGroups)
                .preprocessing(baseOptions.brightness, baseOptions.contrast, baseOptions.saturation)
                .build()
        )

        // TODO: wire up BlockPrintToGlb for stream output
    }

    fun convertFile(
        inputPath: String,
        outputGlbPath: String,
        targetWidth: Int = 128,
        assetsDirs: List<File>,
        layout: Layout = Layout.FLAT,
        blockOptions: ConversionOptions.() -> ConversionOptions = { this }
    ) {
        val image = ImageProcessor.loadImage(File(inputPath))
        convert(image, targetWidth, assetsDirs, File(outputGlbPath), layout, blockOptions)
    }
}
