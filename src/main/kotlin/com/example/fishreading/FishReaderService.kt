package com.example.fishreading

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

@Service(Service.Level.PROJECT)
class FishReaderService : Disposable {

    data class Chapter(val title: String, val lines: List<String>)

    private var chapters: List<Chapter> = listOf(
        Chapter("欢迎使用", listOf("// [FishReading] 请去 Tools 菜单加载或选择书籍"))
    )
    private var currentChapterIdx = 0
    private var currentLineIdx = 0

    init {
        // 🎯 需求 3：注册全局光标监听器。只要光标移动（点击或上下换行），立刻擦除虚拟文字
        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                FishInlayManager.clearInlay()
            }
        }, this)

        // 🎯 需求 1：自动加载上次看过的书
        // ✨ 修改 3：将原先的 project.getService 改为全局 ApplicationManager 获取
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
    fun getCurrentChapterIdx(): Int = currentChapterIdx

    fun jumpToChapter(index: Int) {
        if (index in chapters.indices) {
            currentChapterIdx = index
            currentLineIdx = 0
            saveProgress()
        }
    }

    // 保存当前进度到持久化数据中
    private fun saveProgress() {
        // ✨ 修改 3：将原先的 project.getService 改为全局 ApplicationManager 获取
        val state = com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(FishReadingPersistentState::class.java).state
        val path = state.lastActiveBookPath ?: return
        state.managedBooks[path]?.apply {
            chapterIdx = currentChapterIdx
            lineIdx = currentLineIdx
        }
    }

    /**
     * 加载/切换电子书
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
                            val lines = cleanText.split(Regex("(?<=[。！？\n])"))
                            val chapterLines = lines.map { it.trim() }.filter { it.isNotEmpty() }.map { "// $it" }
                            if (chapterLines.isNotEmpty()) {
                                newChapters.add(Chapter(chapterTitle, chapterLines))
                            }
                        }
                    }
                }
            }

            if (newChapters.isNotEmpty()) {
                this.chapters = newChapters

                // 写入并更新多书管理状态
                // ✨ 修改 3：将原先的 project.getService 改为全局 ApplicationManager 获取
                val state = com.intellij.openapi.application.ApplicationManager.getApplication()
                    .getService(FishReadingPersistentState::class.java).state
                state.lastActiveBookPath = file.absolutePath

                val progress = state.managedBooks.getOrPut(file.absolutePath) {
                    BookProgress().apply { bookName = file.nameWithoutExtension }
                }

                progress.chapterTitles = newChapters.map { it.title } // ✨ 新增这一行：将解析出的章节名同步保存到配置中

                // 恢复这本书专属的历史进度
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