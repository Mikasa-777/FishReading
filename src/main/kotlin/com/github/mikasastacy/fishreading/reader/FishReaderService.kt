package com.github.mikasastacy.fishreading.reader

import com.github.mikasastacy.fishreading.i18n.MyMessageBundle
import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.File

@Service(Service.Level.APP)
class FishReaderService : Disposable {
    private val session = ReaderSession(emptyList())
    private var loadedBookPath: String? = null

    fun getCurrentLine(): String {
        ensureActiveBookLoaded()
        return session.currentLine()
    }

    fun getCurrentPage(): List<String> {
        ensureActiveBookLoaded()
        return session.currentPage(readingLineCount())
    }

    fun nextLine() {
        if (!ensureActiveBookLoaded()) return
        session.nextPage(readingLineCount())
        saveProgress()
    }

    fun nextPage() {
        if (!ensureActiveBookLoaded()) return
        session.nextPage(readingLineCount())
        saveProgress()
    }

    fun prevLine() {
        if (!ensureActiveBookLoaded()) return
        session.prevPage(readingLineCount())
        saveProgress()
    }

    fun prevPage() {
        if (!ensureActiveBookLoaded()) return
        session.prevPage(readingLineCount())
        saveProgress()
    }

    fun getChapterTitles(): List<String> {
        ensureActiveBookLoaded()
        return session.chapterTitles()
    }

    fun getCurrentChapterIdx(): Int = session.chapterIndex

    fun jumpToChapter(index: Int) {
        if (!ensureActiveBookLoaded()) return
        session.jumpToChapter(index)
        saveProgress()
    }

    fun loadBook(file: File): String {
        return when (val extension = file.extension.lowercase()) {
            "epub" -> loadEpub(file)
            "txt" -> loadTxt(file)
            else -> MyMessageBundle.message("reader.load.unsupportedExtension", extension)
        }
    }

    fun loadEpub(file: File): String {
        return try {
            val newChapters = EpubParser.parse(file)
            if (newChapters.isEmpty()) {
                MyMessageBundle.message("reader.load.noValidText")
            } else {
                applyLoadedBook(file, newChapters)
                MyMessageBundle.message("reader.load.success", file.nameWithoutExtension)
            }
        } catch (e: Exception) {
            MyMessageBundle.message("reader.load.failure", e.message)
        }
    }

    fun loadTxt(file: File): String {
        return try {
            val newChapters = TxtParser.parse(file)
            if (newChapters.isEmpty()) {
                MyMessageBundle.message("reader.load.noValidText")
            } else {
                applyLoadedBook(file, newChapters)
                MyMessageBundle.message("reader.load.success", file.nameWithoutExtension)
            }
        } catch (e: Exception) {
            MyMessageBundle.message("reader.load.failure", e.message)
        }
    }

    fun forgetBook(filePath: String): Boolean {
        val settings = service<FishReadingPersistentState>()
        val wasActiveBook = settings.lastActiveBookPath == filePath
        val wasLoadedBook = loadedBookPath == filePath

        settings.removeBook(filePath)

        if (!wasActiveBook && !wasLoadedBook) {
            return false
        }

        loadedBookPath = null
        session.resetToWelcome()
        return true
    }

    private fun applyLoadedBook(file: File, newChapters: List<Chapter>) {
        val settings = service<FishReadingPersistentState>()
        settings.setLastActiveBookPath(file.absolutePath)

        val progress = settings.rememberBook(file.absolutePath, file.nameWithoutExtension)
        settings.updateChapterTitles(file.absolutePath, newChapters.map { it.title })
        session.replaceChapters(newChapters, progress.chapterIdx, progress.lineIdx)
        loadedBookPath = file.absolutePath
    }

    private fun ensureActiveBookLoaded(): Boolean {
        val path = service<FishReadingPersistentState>().lastActiveBookPath ?: return false
        if (loadedBookPath == path) return true

        val file = File(path)
        if (!file.exists() || file.extension.lowercase() !in SUPPORTED_EXTENSIONS) {
            return false
        }

        loadBook(file)
        return loadedBookPath == path
    }

    private fun saveProgress() {
        val settings = service<FishReadingPersistentState>()
        val path = settings.lastActiveBookPath ?: return
        settings.saveProgress(path, session.chapterIndex, session.lineIndex)
    }

    private fun readingLineCount(): Int = service<FishReadingPersistentState>().normalizedReadingLineCount()

    override fun dispose() {}

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("epub", "txt")
    }
}
