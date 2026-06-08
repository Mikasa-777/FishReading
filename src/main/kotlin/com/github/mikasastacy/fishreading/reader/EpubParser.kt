package com.github.mikasastacy.fishreading.reader

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

data class Chapter(val title: String, val lines: List<String>)

object EpubParser {
    fun parse(file: File, maxLineLength: Int = 50): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { it.name.endsWith(".xhtml") || it.name.endsWith(".html") }
                .sortedBy { it.name }
                .toList()

            var anonymousChapterCount = 1
            for (entry in entries) {
                zip.getInputStream(entry).use { stream ->
                    val htmlContent = stream.readBytes().toString(StandardCharsets.UTF_8)
                    val chapterTitle = extractChapterTitle(htmlContent)
                        .takeUnless { it.isBlank() || it.lowercase().contains("untitled") }
                        ?: "第 ${anonymousChapterCount++} 部分"

                    val cleanText = cleanHtml(htmlContent)
                    if (cleanText.isNotBlank()) {
                        val chapterLines = TextSegmenter.segment(cleanText, maxLineLength)
                        if (chapterLines.isNotEmpty()) {
                            chapters.add(Chapter(chapterTitle, chapterLines))
                        }
                    }
                }
            }
        }

        return chapters
    }

    fun extractChapterTitle(htmlContent: String): String {
        val headingMatch = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.DOT_MATCHES_ALL).find(htmlContent)
        val titleMatch = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL).find(htmlContent)

        return when {
            headingMatch != null -> headingMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            titleMatch != null -> titleMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            else -> ""
        }
    }

    fun cleanHtml(htmlContent: String): String {
        return htmlContent
            .replace(Regex("<head>.*?</head>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .trim()
    }
}
