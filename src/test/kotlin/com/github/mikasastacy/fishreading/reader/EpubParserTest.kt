package com.github.mikasastacy.fishreading.reader

import kotlin.test.Test
import kotlin.test.assertEquals

class EpubParserTest {
    @Test
    fun `extracts heading title before document title`() {
        val html = "<html><head><title>Fallback</title></head><body><h2><span>Chapter One</span></h2></body></html>"

        assertEquals("Chapter One", EpubParser.extractChapterTitle(html))
    }

    @Test
    fun `cleans head tags and common html entities`() {
        val html = "<html><head><title>Hidden</title></head><body><p>A&nbsp;&ldquo;quote&rdquo;</p></body></html>"

        assertEquals("A “quote”", EpubParser.cleanHtml(html))
    }
}
