package com.github.mikasastacy.fishreading.inlay

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import java.awt.event.KeyEvent

class FishInlayServiceTest : BasePlatformTestCase() {
    private lateinit var inlayService: FishInlayService
    private val extraEditors = mutableListOf<Editor>()

    override fun setUp() {
        super.setUp()
        inlayService = service()
        inlayService.clearInlay()
    }

    override fun tearDown() {
        try {
            inlayService.clearInlay()
            extraEditors.forEach { EditorFactory.getInstance().releaseEditor(it) }
            extraEditors.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testEscapeClearsInlayWhenCurrentEditorIsFocused() {
        val editor = configureEditor()
        inlayService.updateInlay(editor, "// reading")

        val handled = inlayService.handleEscapeKeyEvent(escapePressed(editor.contentComponent), editor.contentComponent)

        assertFalse(handled)
        assertFalse(inlayService.hasActiveInlay())
    }

    fun testEscapeKeepsInlayWhenFocusIsOutsideCurrentEditor() {
        val editor = configureEditor()
        val otherEditor = createEditor(project)
        inlayService.updateInlay(editor, "// reading")

        val handled = inlayService.handleEscapeKeyEvent(escapePressed(otherEditor.contentComponent), otherEditor.contentComponent)

        assertFalse(handled)
        assertTrue(inlayService.hasActiveInlay())
    }

    fun testNonEscapeAndReleasedEscapeDoNotClearInlay() {
        val editor = configureEditor()
        inlayService.updateInlay(editor, "// reading")

        assertFalse(inlayService.handleEscapeKeyEvent(keyPressed(editor.contentComponent, KeyEvent.VK_ENTER), editor.contentComponent))
        assertTrue(inlayService.hasActiveInlay())

        assertFalse(inlayService.handleEscapeKeyEvent(escapeReleased(editor.contentComponent), editor.contentComponent))
        assertTrue(inlayService.hasActiveInlay())
    }

    fun testCurrentEditorFocusLostClearsInlay() {
        val editor = configureEditor()
        inlayService.updateInlay(editor, "// reading")

        inlayService.handleEditorFocusLost(editor)

        assertFalse(inlayService.hasActiveInlay())
    }

    fun testOtherEditorFocusLostDoesNotClearCurrentInlay() {
        val editor = configureEditor()
        val otherEditor = createEditor(project)
        inlayService.updateInlay(editor, "// reading")

        inlayService.handleEditorFocusLost(otherEditor)

        assertTrue(inlayService.hasActiveInlay())
    }

    fun testOldEditorFocusLostDoesNotClearNewEditorInlayAfterConsecutiveUpdates() {
        val oldEditor = configureEditor()
        val newEditor = createEditor(project)
        inlayService.updateInlay(oldEditor, "// old")
        inlayService.updateInlay(newEditor, "// new")

        inlayService.handleEditorFocusLost(oldEditor)

        assertTrue(inlayService.hasActiveInlay())
    }

    fun testRendererUsesLineCountHeightAndLongestLineWidth() {
        val editor = configureEditor()
        inlayService.updateInlay(editor, listOf("// short", "// much longer"))
        val inlay = editor.inlayModel.getBlockElementsInRange(0, editor.document.textLength).single()
        val metrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))

        assertEquals(editor.lineHeight * 2, inlay.heightInPixels)
        assertEquals(metrics.stringWidth("    // much longer"), inlay.widthInPixels)
    }

    fun testRendererDirectlyComputesMultilineDimensions() {
        val editor = configureEditor()
        val inlay = editor.inlayModel.addBlockElement(
            0,
            true,
            true,
            0,
            FishInlayRenderer(
                listOf("// a", "// abc"),
                TextAttributes().apply { foregroundColor = JBColor.GRAY }
            )
        ) ?: error("inlay was not created")
        val metrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))

        assertEquals(editor.lineHeight * 2, inlay.heightInPixels)
        assertEquals(metrics.stringWidth("// abc"), inlay.widthInPixels)
    }

    private fun configureEditor(): Editor {
        myFixture.configureByText("sample.kt", "    val value = 1\n")
        return myFixture.editor
    }

    private fun createEditor(project: Project): Editor {
        val document = EditorFactory.getInstance().createDocument("fun other() {}\n")
        return EditorFactory.getInstance().createEditor(document, project).also(extraEditors::add)
    }

    private fun escapePressed(source: java.awt.Component): KeyEvent = keyPressed(source, KeyEvent.VK_ESCAPE)

    private fun escapeReleased(source: java.awt.Component): KeyEvent =
        KeyEvent(source, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)

    private fun keyPressed(source: java.awt.Component, keyCode: Int): KeyEvent =
        KeyEvent(source, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED)
}
