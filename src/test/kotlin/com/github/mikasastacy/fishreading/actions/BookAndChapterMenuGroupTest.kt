package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BookAndChapterMenuGroupTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        resetPersistentState()
    }

    override fun tearDown() {
        try {
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

    private fun Array<AnAction>.menuLabels(): List<String> =
        map { action ->
            if (action is Separator) Separator::class.java.name else action.templatePresentation.text
        }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }
}
