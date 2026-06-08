package com.github.mikasastacy.fishreading.reader

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderSessionTest {
    @Test
    fun `next and previous move across chapter boundaries`() {
        val session = ReaderSession(
            listOf(
                Chapter("一", listOf("// a1", "// a2")),
                Chapter("二", listOf("// b1"))
            )
        )

        session.nextLine()
        assertEquals("// a2", session.currentLine())

        session.nextLine()
        assertEquals(1, session.chapterIndex)
        assertEquals(0, session.lineIndex)
        assertEquals("// b1", session.currentLine())

        session.prevLine()
        assertEquals(0, session.chapterIndex)
        assertEquals(1, session.lineIndex)
        assertEquals("// a2", session.currentLine())
    }

    @Test
    fun `jumpToChapter moves to chapter start`() {
        val session = ReaderSession(
            listOf(
                Chapter("一", listOf("// a1")),
                Chapter("二", listOf("// b1", "// b2"))
            )
        )

        session.jumpToChapter(1)

        assertEquals(1, session.chapterIndex)
        assertEquals(0, session.lineIndex)
        assertEquals("// b1", session.currentLine())
    }

    @Test
    fun `restores existing progress within bounds`() {
        val session = ReaderSession(
            listOf(
                Chapter("一", listOf("// a1")),
                Chapter("二", listOf("// b1", "// b2"))
            ),
            chapterIndex = 1,
            lineIndex = 1
        )

        assertEquals("// b2", session.currentLine())
    }

    @Test
    fun `clamps restored progress when book structure changed`() {
        val session = ReaderSession(
            listOf(Chapter("一", listOf("// a1"))),
            chapterIndex = 99,
            lineIndex = 99
        )

        assertEquals(0, session.chapterIndex)
        assertEquals(0, session.lineIndex)
        assertEquals("// a1", session.currentLine())
    }
}
