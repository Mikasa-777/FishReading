package com.example.fishreading

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

@Service(Service.Level.APP) // ✨ 修正：确保这里是 APP 级别
class FishReaderService : Disposable {

    data class Chapter(val title: String, val lines: List<String>)

    private var chapters: List<Chapter> = listOf(
        Chapter("欢迎使用", listOf("// [FishReading] 请去 Tools 菜单加载或选择书籍"))
    )
    private var currentChapterIdx = 0
    private var currentLineIdx = 0

    init {
        // 注册全局光标监听器。只要光标移动（点击或上下换行），立刻擦除虚拟文字
        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                FishInlayManager.clearInlay()
            }
        }, this)

        // 自动加载上次看过的书
        val state = com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(FishReadingPersistentState::class.java).state
        state.lastActiveBookPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                loadEpub(file)
            }
        }
    }

    fun getCurrentLine(): String {
        val chapter = chapters.getOrNull(currentChapterIdx) ?: return "// 暂无内容"
        return chapter.lines.getOrNull(currentLineIdx) ?: "// 本章结束"
    }

    fun nextLine() {
        val currentChapter = chapters.getOrNull(currentChapterIdx) ?: return
        if (currentLineIdx < currentChapter.lines.size - 1) {
            currentLineIdx++
        } else if (currentChapterIdx < chapters.size - 1) {
            currentChapterIdx++
            currentLineIdx = 0
        }
        saveProgress()
    }

    fun prevLine() {
        if (currentLineIdx > 0) {
            currentLineIdx--
        } else if (currentChapterIdx > 0) {
            currentChapterIdx--
            currentLineIdx = (chapters[currentChapterIdx].lines.size - 1).coerceAtLeast(0)
        }
        saveProgress()
    }

    fun getChapterTitles(): List<String> = chapters.mapIndexed { i, c -> "[${i + 1}] ${c.title}" }
//    fun getChapterTitlesRaw(): List<String> = chapters.map { it.title }
    fun getCurrentChapterIdx(): Int = currentChapterIdx

    fun jumpToChapter(index: Int) {
        if (index in chapters.indices) {
            currentChapterIdx = index
            currentLineIdx = 0
            saveProgress()
        }
    }

    private fun saveProgress() {
        val state = com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(FishReadingPersistentState::class.java).state
        val path = state.lastActiveBookPath ?: return
        state.managedBooks[path]?.apply {
            chapterIdx = currentChapterIdx
            lineIdx = currentLineIdx
        }
    }

    /**
     * 智能文本切分与聚合算法（核心优化点 ✨）
     * 黄金舒适区间：20 ~ 50 个字
     */
    private fun segmentTextIntelligently(rawText: String, maxLen: Int = 50): List<String> {

        // 1. 先按传统的句子结束符初筛切断
        val rawPieces = rawText.split(Regex("(?<=[。！？\n])"))
        val result = mutableListOf<String>()
        var buffer = StringBuilder()

        for (rawPiece in rawPieces) {
            val piece = rawPiece.trim()
            if (piece.isEmpty()) continue

            // 2. 情况 3：针对单句就长得离谱的超长句，调用下游函数先按逗号分级切碎
            val chunks = if (piece.length > maxLen) {
                splitLongSentence(piece, maxLen)
            } else {
                listOf(piece)
            }

            for (chunk in chunks) {
                if (buffer.isEmpty()) {
                    buffer.append(chunk)
                } else if (buffer.length + chunk.length <= maxLen) {
                    // 情况 1 & 2：如果当前缓冲区加上新句没有超标，直接合并（完美解决过短问题）
                    buffer.append(chunk)
                } else {
                    // 缓冲区满了，冲刷进结果集，并开启新一轮的收集
                    result.add("// ${buffer.toString().trim()}")
                    buffer = StringBuilder(chunk)
                }
            }
        }

        // 别忘了冲刷最后留在缓冲区里的残余文本
        if (buffer.isNotEmpty()) {
            result.add("// ${buffer.toString().trim()}")
        }
        return result
    }

    /**
     * 辅助函数：处理长句，优先按次级标点拆分
     */
    private fun splitLongSentence(sentence: String, maxLen: Int): List<String> {
        val subPieces = sentence.split(Regex("(?<=[，；、,;])"))
        val chunks = mutableListOf<String>()
        var buffer = StringBuilder()

        for (sub in subPieces) {
            val trimmed = sub.trim()
            if (trimmed.isEmpty()) continue

            if (buffer.isEmpty()) {
                buffer.append(trimmed)
            } else if (buffer.length + trimmed.length <= maxLen) {
                buffer.append(trimmed)
            } else {
                chunks.add(buffer.toString())
                buffer = StringBuilder(trimmed)
            }
        }

        if (buffer.isNotEmpty()) {
            val finalStr = buffer.toString()
            // 如果遇到丧心病狂、连逗号都没有的超级长句，直接强行物理截断
            if (finalStr.length > maxLen) {
                var start = 0
                while (start < finalStr.length) {
                    val end = minOf(start + maxLen, finalStr.length)
                    chunks.add(finalStr.substring(start, end))
                    start = end
                }
            } else {
                chunks.add(finalStr)
            }
        }
        return chunks
    }

    /**
     * EPUB 解析器
     */
    fun loadEpub(file: File): String {
        val newChapters = mutableListOf<Chapter>()
        try {
            ZipFile(file).use { zip ->
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
                            // ✨ 核心调用切换：改用我们全新的智能贪婪分段算法
                            val chapterLines = segmentTextIntelligently(cleanText, maxLen = 50)
                            if (chapterLines.isNotEmpty()) {
                                newChapters.add(Chapter(chapterTitle, chapterLines))
                            }
                        }
                    }
                }
            }

            if (newChapters.isNotEmpty()) {
                this.chapters = newChapters

                val state = com.intellij.openapi.application.ApplicationManager.getApplication()
                    .getService(FishReadingPersistentState::class.java).state
                state.lastActiveBookPath = file.absolutePath

                val progress = state.managedBooks.getOrPut(file.absolutePath) {
                    BookProgress().apply { bookName = file.nameWithoutExtension }
                }

                progress.chapterTitles = newChapters.map { it.title }
                this.currentChapterIdx = progress.chapterIdx
                this.currentLineIdx = progress.lineIdx

                return "成功装载《${file.nameWithoutExtension}》"
            }
        } catch (e: Exception) {
            return "加载失败: ${e.message}"
        }
        return "未发现有效文本"
    }

    override fun dispose() {}
}