@file:Suppress("unused")

package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

/**
 * 向后兼容 shim：原 ITB 私有 ExportPayloadCodec 已迁到 blueprintcommon。
 * 旧 import 与测试都通过 typealias 继续工作。
 */
typealias ExportPayloadCodec = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.ExportPayloadCodec
typealias DecodedExportPayload = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.DecodedExportPayload
