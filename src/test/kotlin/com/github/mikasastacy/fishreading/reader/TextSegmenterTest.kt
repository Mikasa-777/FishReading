package com.github.mikasastacy.fishreading.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextSegmenterTest {
    @Test
    fun `merges short sentences until max length`() {
        val lines = TextSegmenter.segment("鱼来了。快藏好！继续写代码。", maxLen = 10)

        assertEquals(listOf("// 鱼来了。快藏好！", "// 继续写代码。"), lines)
    }

    @Test
    fun `splits long sentence by secondary punctuation`() {
        val lines = TextSegmenter.segment("这是第一段很长很长，足够触发切分；这是第二段也很长很长。", maxLen = 12)

        assertEquals(
            listOf("// 这是第一段很长很长，", "// 足够触发切分；", "// 这是第二段也很长很长。"),
            lines
        )
    }

    @Test
    fun `hard splits long text without punctuation`() {
        val lines = TextSegmenter.segment("abcdefghijklmnopqrstuvwxyz", maxLen = 10)

        assertEquals(listOf("// abcdefghij", "// klmnopqrst", "// uvwxyz"), lines)
        assertTrue(lines.all { it.removePrefix("// ").length <= 10 })
    }
}
