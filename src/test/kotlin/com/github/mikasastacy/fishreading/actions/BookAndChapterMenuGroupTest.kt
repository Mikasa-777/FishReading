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
        val state = service<FishReadingPersistentState>().state
        val path = "/books/book.txt"
        state.lastActiveBookPath = path
        state.getBookProgress(path, "book").apply {
            chapterIdx = 0
            chapterTitles = listOf("第一章", "第二章")
        }

        val submenu = BookAndChapterMenuGroup().getChildren(null).single() as ActionGroup
        val children = submenu.getChildren(null)

        assertEquals(
            listOf(
                "继续阅读 (当前书籍)",
                "忘记本书",
                Separator::class.java.name,
                "-> 第一章",
                "第二章",
            ),
            children.menuLabels()
        )
    }

    fun testUncachedBookSubmenuOnlyShowsLoadActionWithoutSeparator() {
        val state = service<FishReadingPersistentState>().state
        val path = "/books/book.txt"
        state.getBookProgress(path, "book")

        val submenu = BookAndChapterMenuGroup().getChildren(null).single() as ActionGroup
        val children = submenu.getChildren(null)

        assertEquals(listOf("尚未缓存目录，点击激活载入"), children.menuLabels())
    }

    private fun Array<AnAction>.menuLabels(): List<String> =
        map { action ->
            if (action is Separator) Separator::class.java.name else action.templatePresentation.text
        }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }
}
