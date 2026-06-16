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
import java.awt.Component
import java.awt.Font
import java.awt.KeyboardFocusManager
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

@Service(Service.Level.APP)
class FishInlayService : Disposable {
    private var currentInlay: Inlay<*>? = null
    private var currentEditor: Editor? = null
    private var currentFocusListener: FocusListener? = null
    private val escapeKeyEventDispatcher = java.awt.KeyEventDispatcher { event ->
        handleEscapeKeyEvent(event, KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner)
    }

    init {
        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                clearInlay()
            }
        }, this)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(escapeKeyEventDispatcher)
    }

    fun updateInlay(editor: Editor, text: String) {
        clearInlay()
        currentEditor = editor
        registerFocusListener(editor)

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
        unregisterFocusListener()
        currentInlay?.let {
            if (it.isValid) {
                it.dispose()
            }
        }
        currentInlay = null
        currentEditor = null
    }

    override fun dispose() {
        clearInlay()
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(escapeKeyEventDispatcher)
    }

    internal fun hasActiveInlay(): Boolean = currentInlay?.isValid == true

    internal fun handleEscapeKeyEvent(event: KeyEvent, focusOwner: Component?): Boolean {
        if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE && isCurrentEditorFocusOwner(focusOwner)) {
            clearInlay()
        }
        return false
    }

    internal fun handleEditorFocusLost(editor: Editor) {
        if (editor == currentEditor) {
            clearInlay()
        }
    }

    private fun registerFocusListener(editor: Editor) {
        val listener = object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) {
                handleEditorFocusLost(editor)
            }
        }
        currentFocusListener = listener
        editor.contentComponent.addFocusListener(listener)
    }

    private fun unregisterFocusListener() {
        val editor = currentEditor
        val listener = currentFocusListener
        if (editor != null && listener != null) {
            editor.contentComponent.removeFocusListener(listener)
        }
        currentFocusListener = null
    }

    private fun isCurrentEditorFocusOwner(focusOwner: Component?): Boolean {
        val contentComponent = currentEditor?.contentComponent ?: return false
        return focusOwner == contentComponent || (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, contentComponent))
    }
}
