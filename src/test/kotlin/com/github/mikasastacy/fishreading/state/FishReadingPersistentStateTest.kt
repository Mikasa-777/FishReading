package com.github.mikasastacy.fishreading.state

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FishReadingPersistentStateTest : BasePlatformTestCase() {

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

    fun testRememberBookCreatesProgressAndRefreshesDisplayName() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"

        val created = settings.rememberBook(path, "Old Name")
        val refreshed = settings.rememberBook(path, "New Name")

        assertEquals(BookProgress(bookName = "Old Name"), created)
        assertEquals(BookProgress(bookName = "New Name"), refreshed)
        assertEquals(refreshed, settings.bookProgress(path))
        assertEquals(mapOf(path to refreshed), settings.managedBooks)
    }

    fun testUpdateChapterTitlesAndSaveProgressCopyBookState() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"
        settings.rememberBook(path, "book")

        settings.updateChapterTitles(path, listOf("第一章", "第二章"))
        settings.saveProgress(path, chapterIdx = 1, lineIdx = 5)

        assertEquals(
            BookProgress(
                chapterIdx = 1,
                lineIdx = 5,
                bookName = "book",
                chapterTitles = listOf("第一章", "第二章")
            ),
            settings.bookProgress(path)
        )
    }

    fun testRemoveCurrentBookClearsActivePath() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"
        settings.rememberBook(path, "book")
        settings.setLastActiveBookPath(path)

        settings.removeBook(path)

        assertNull(settings.bookProgress(path))
        assertNull(settings.lastActiveBookPath)
    }

    fun testReadingLineCountIsNormalizedAndPersistedWithinRange() {
        val settings = service<FishReadingPersistentState>()

        settings.loadState(FishReadingPersistentState.State(readingLineCount = 0))
        assertEquals(1, settings.normalizedReadingLineCount())

        settings.updateReadingLineCount(21)
        assertEquals(20, settings.readingLineCount)
        assertEquals(20, settings.normalizedReadingLineCount())
    }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }
}
