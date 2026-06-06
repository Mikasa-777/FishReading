package com.example.fishreading

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

object FishInlayManager {
    private var currentInlay: Inlay<*>? = null

    // 统一负责清除旧文本，并在光标处绘制新文本
    fun updateInlay(editor: Editor, text: String) {
        // 1. 清理上一句
        currentInlay?.let {
            it.dispose()
            currentInlay = null
        }

        val inlayModel = editor.inlayModel
        val caretModel = editor.caretModel
        val currentOffset = caretModel.offset

        // 2. 设置伪装字体的颜色（灰色斜体，高仿注释）
        val attributes = TextAttributes().apply {
            foregroundColor = Color.GRAY
            fontType = Font.ITALIC
        }

        // 3. 渲染新文本
        currentInlay = inlayModel.addBlockElement(
            currentOffset,
            true,
            true,
            0,
            FishInlayRenderer(text, attributes)
        )
    }

    fun clearInlay() {
        currentInlay?.let {
            if (it.isValid) { // ✨ 修改：用 isValid 替代 !isDisposed
                it.dispose()
            }
            currentInlay = null
        }
    }
}