package com.example.fishreading

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes

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

        // 2. ✨ 新增：动态计算当前光标所在行的缩进字符串
        val document = editor.document
        val chars = document.immutableCharSequence
        val lineNumber = document.getLineNumber(currentOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)

        // 从行首开始遍历，直到遇到非空白字符，把前面的空格/Tab全抓出来
        var i = lineStartOffset
        while (i < chars.length && (chars[i] == ' ' || chars[i] == '\t')) {
            i++
        }
        val indentPrefix = chars.subSequence(lineStartOffset, i).toString()

        // 将缩进前缀和小说内容拼接（例如："    " + "// 突然..."）
        val indentedText = indentPrefix + text

        // 3. 设置伪装字体的颜色（灰色斜体，高仿注释）
        val attributes = TextAttributes().apply {
            foregroundColor = com.intellij.ui.JBColor.GRAY
            fontType = java.awt.Font.ITALIC
        }

        // 4. 渲染拼接了缩进的新文本
        currentInlay = inlayModel.addBlockElement(
            currentOffset,
            true,
            true,
            0,
            FishInlayRenderer(indentedText, attributes) // ✨ 这里改传 indentedText
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