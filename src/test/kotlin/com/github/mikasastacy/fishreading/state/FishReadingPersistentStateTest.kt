package com.github.mikasastacy.fishreading.state

import com.intellij.openapi.components.service
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer

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

    fun testSaveTxtChapterRecognitionStoresRegexTitlesAndResetsProgress() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"
        settings.rememberBook(path, "book")
        settings.saveProgress(path, chapterIdx = 2, lineIdx = 8)

        settings.saveTxtChapterRecognition(
            filePath = path,
            titles = listOf("### 起始", "### 继续"),
            chapterTitleRegex = """^###\s+.+$"""
        )

        assertEquals(
            BookProgress(
                chapterIdx = 0,
                lineIdx = 0,
                bookName = "book",
                chapterTitles = listOf("### 起始", "### 继续"),
                chapterTitleRegex = """^###\s+.+$"""
            ),
            settings.bookProgress(path)
        )
    }

    fun testSaveTxtChapterRecognitionClearsCustomRegexWhenDefaultIsRequested() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"
        settings.rememberBook(path, "book")
        settings.saveTxtChapterRecognition(
            filePath = path,
            titles = listOf("### 起始"),
            chapterTitleRegex = """^###\s+.+$"""
        )

        settings.saveTxtChapterRecognition(
            filePath = path,
            titles = listOf("第一章 起始"),
            chapterTitleRegex = null
        )

        assertEquals(
            BookProgress(
                bookName = "book",
                chapterTitles = listOf("第一章 起始"),
                chapterTitleRegex = null
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

    fun testXmlSerializationIncludesActiveBookAndReadingLineCount() {
        val settings = service<FishReadingPersistentState>()
        val path = "/books/book.txt"

        settings.setLastActiveBookPath(path)
        settings.updateReadingLineCount(5)
        settings.rememberBook(path, "book")
        settings.saveTxtChapterRecognition(
            filePath = path,
            titles = listOf("### 起始"),
            chapterTitleRegex = """^###\s+.+$"""
        )

        val xml = JDOMUtil.writeElement(
            XmlSerializer.serialize(settings.state)
        )

        assertTrue(xml, xml.contains("lastActiveBookPath"))
        assertTrue(xml, xml.contains(path))
        assertTrue(xml, xml.contains("readingLineCount"))
        assertTrue(xml, xml.contains("5"))
        assertTrue(xml, xml.contains("managedBooks"))
        assertTrue(xml, xml.contains("bookName"))
        assertTrue(xml, xml.contains("book"))
        assertTrue(xml, xml.contains("chapterTitleRegex"))
        assertTrue(xml, xml.contains("^###\\s+.+$"))

        val restored = XmlSerializer.deserialize(
            XmlSerializer.serialize(settings.state),
            FishReadingPersistentState.State::class.java
        )

        assertEquals(path, restored.lastActiveBookPath)
        assertEquals(5, restored.readingLineCount)
        assertEquals(
            BookProgress(
                bookName = "book",
                chapterTitles = listOf("### 起始"),
                chapterTitleRegex = """^###\s+.+$"""
            ),
            restored.managedBooks[path]
        )
    }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }
}
