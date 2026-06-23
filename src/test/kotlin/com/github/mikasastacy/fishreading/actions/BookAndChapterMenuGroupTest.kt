package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.inlay.FishInlayService
import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.createTempFile

class BookAndChapterMenuGroupTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        resetPersistentState()
        service<FishInlayService>().clearInlay()
    }

    override fun tearDown() {
        try {
            service<FishInlayService>().clearInlay()
            resetPersistentState()
        } finally {
            super.tearDown()
        }
    }

    fun testBookSubmenuSeparatesBuiltInActionsFromCachedChapters() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"
        settings.setLastActiveBookPath(path)
        settings.rememberBook(path, "book")
        settings.updateChapterTitles(path, listOf("第一章", "第二章"))

        val submenu = BookAndChapterMenuGroup().getChildren(null).single() as ActionGroup
        val children = submenu.getChildren(null)

        assertEquals(
            listOf(
                "Resume reading (current book)",
                "Forget this book",
                Separator::class.java.name,
                "-> 第一章",
                "第二章",
            ),
            children.menuLabels()
        )
    }

    fun testUncachedBookSubmenuOnlyShowsLoadActionWithoutSeparator() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"
        settings.rememberBook(path, "book")

        val submenu = BookAndChapterMenuGroup().getChildren(null).single() as ActionGroup
        val children = submenu.getChildren(null)

        assertEquals(listOf("No cached table of contents. Click to load it."), children.menuLabels())
    }

    fun testForgetCurrentBookClearsCurrentInlay() {
        val file = createTxtFile(
            """
            第一章 当前
            当前图书第一行。
            """.trimIndent()
        )
        val readerService = service<FishReaderService>()
        val inlayService = service<FishInlayService>()
        val editor = configureEditor()
        readerService.loadBook(file)
        inlayService.updateInlay(editor, readerService.getCurrentPage())
        assertTrue(inlayService.hasActiveInlay())

        forgetActionFor(file.absolutePath).actionPerformed(actionEvent(editor))

        assertFalse(inlayService.hasActiveInlay())
    }

    fun testForgetNonCurrentBookKeepsCurrentInlay() {
        val currentFile = createTxtFile(
            """
            第一章 当前
            当前图书第一行。
            """.trimIndent()
        )
        val savedPath = "/books/saved.txt"
        val settings = service<FishReadingPersistentState>()
        val readerService = service<FishReaderService>()
        val inlayService = service<FishInlayService>()
        val editor = configureEditor()
        readerService.loadBook(currentFile)
        settings.rememberBook(savedPath, "saved")
        settings.updateChapterTitles(savedPath, listOf("第一章 已保存"))
        inlayService.updateInlay(editor, readerService.getCurrentPage())
        assertTrue(inlayService.hasActiveInlay())

        forgetActionFor(savedPath).actionPerformed(actionEvent(editor))

        assertTrue(inlayService.hasActiveInlay())
    }

    private fun Array<AnAction>.menuLabels(): List<String> =
        map { action ->
            if (action is Separator) Separator::class.java.name else action.templatePresentation.text
        }

    private fun forgetActionFor(path: String): AnAction {
        val settings = service<FishReadingPersistentState>()
        val submenuIndex = settings.managedBooks.keys.indexOf(path)
        assertTrue("book was not registered in menu state: $path", submenuIndex >= 0)
        val submenu = BookAndChapterMenuGroup().getChildren(null)[submenuIndex] as ActionGroup
        return submenu.getChildren(null).first {
            it.templatePresentation.text == "Forget this book"
        }
    }

    private fun configureEditor(): Editor {
        myFixture.configureByText("sample.kt", "    val value = 1\n")
        return myFixture.editor
    }

    private fun actionEvent(editor: Editor): AnActionEvent =
        AnActionEvent.createFromDataContext("test", null, DataContext { dataId ->
            if (CommonDataKeys.EDITOR.`is`(dataId)) editor else null
        })

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }

    private fun createTxtFile(text: String) = createTempFile(prefix = "book", suffix = ".txt").toFile().apply {
        Files.write(toPath(), text.toByteArray(StandardCharsets.UTF_8))
    }
}
