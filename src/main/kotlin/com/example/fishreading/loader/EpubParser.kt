package com.example.fishreading.loader

import com.example.fishreading.FishReaderService
import com.example.fishreading.FishTextSplitter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile


class EpubParser : BookParser {

    override fun parse(file: File): List<FishReaderService.Chapter> {
        val newChapters = mutableListOf<FishReaderService.Chapter>()

        // ✨ 核心修复：强行指定用 UTF_8 解码 Zip 内部文件名，彻底解决 Windows 系统下因默认 GBK 导致的找不到文件或乱码问题
        ZipFile(file, StandardCharsets.UTF_8).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { it.name.endsWith(".xhtml") || it.name.endsWith(".html") }
                .sortedBy { it.name }
                .toList()

            var anonymousChapterCount = 1
            for (entry in entries) {
                zip.getInputStream(entry).use { stream ->
                    val htmlContent = stream.readBytes().toString(StandardCharsets.UTF_8)
                    var chapterTitle = ""
                    val headingMatch = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.DOT_MATCHES_ALL).find(htmlContent)
                    val titleMatch = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL).find(htmlContent)

                    if (headingMatch != null) {
                        chapterTitle = headingMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    } else if (titleMatch != null) {
                        chapterTitle = titleMatch.groupValues[1].trim()
                    }
                    if (chapterTitle.isBlank() || chapterTitle.lowercase().contains("untitled")) {
                        chapterTitle = "第 $anonymousChapterCount 部分"
                        anonymousChapterCount++
                    }

                    val cleanText = htmlContent
                        .replace(Regex("<head>.*?</head>", RegexOption.DOT_MATCHES_ALL), "")
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&nbsp;", " ").replace("&ldquo;", "“").replace("&rdquo;", "”").trim()

                    if (cleanText.isNotBlank()) {
                        // ✨ 核心调用切换：改用共享工具类的智能贪婪分段算法，保持 100% 舒适体验
                        val chapterLines = FishTextSplitter.segmentTextIntelligently(cleanText, maxLen = 50)
                        if (chapterLines.isNotEmpty()) {
                            // 注意：由于 Chapter 声明在 Service 内部，通过外部限定名引用
                            newChapters.add(FishReaderService.Chapter(chapterTitle, chapterLines))
                        }
                    }
                }
            }
        }
        return newChapters
    }
}