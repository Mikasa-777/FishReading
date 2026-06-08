package com.github.mikasastacy.fishreading.inlay

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Font

@Service(Service.Level.APP)
class FishInlayService : Disposable {
    private var currentInlay: Inlay<*>? = null

    init {
        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                clearInlay()
            }
        }, this)
    }

    fun updateInlay(editor: Editor, text: String) {
        clearInlay()

        val currentOffset = editor.caretModel.offset
        val document = editor.document
        val chars = document.immutableCharSequence
        val lineNumber = document.getLineNumber(currentOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)

        var i = lineStartOffset
        while (i < chars.length && (chars[i] == ' ' || chars[i] == '\t')) {
            i++
        }
        val indentPrefix = chars.subSequence(lineStartOffset, i).toString()

        val attributes = TextAttributes().apply {
            foregroundColor = JBColor.GRAY
            fontType = Font.ITALIC
        }

        currentInlay = editor.inlayModel.addBlockElement(
            currentOffset,
            true,
            true,
            0,
            FishInlayRenderer(indentPrefix + text, attributes)
        )
    }

    fun clearInlay() {
        currentInlay?.let {
            if (it.isValid) {
                it.dispose()
            }
        }
        currentInlay = null
    }

    override fun dispose() {
        clearInlay()
    }
}
