package com.github.mikasastacy.fishreading.state

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class BookProgress {
    var chapterIdx: Int = 0
    var lineIdx: Int = 0
    var bookName: String = ""
    var chapterTitles: List<String> = arrayListOf()
}

@State(name = "FishReadingState", storages = [Storage("fish_reading_config.xml")])
@Service(Service.Level.APP)
class FishReadingPersistentState : PersistentStateComponent<FishReadingPersistentState.State> {

    class State {
        var lastActiveBookPath: String? = null
        var readingLineCount: Int = DEFAULT_READING_LINE_COUNT
        var managedBooks: MutableMap<String, BookProgress> = mutableMapOf()
            private set

        fun getBookProgress(filePath: String, bookName: String): BookProgress {
            val existing = managedBooks[filePath]
            if (existing != null) {
                existing.bookName = bookName
                return existing
            }
            val progress = BookProgress().apply { this.bookName = bookName }
            managedBooks = managedBooks.toMutableMap().also { it[filePath] = progress }
            return progress
        }

        fun updateChapterTitles(filePath: String, titles: List<String>) {
            managedBooks[filePath]?.chapterTitles = titles
            bumpMap()
        }

        fun saveProgress(filePath: String, chapterIdx: Int, lineIdx: Int) {
            managedBooks[filePath]?.apply {
                this.chapterIdx = chapterIdx
                this.lineIdx = lineIdx
            }
            bumpMap()
        }

        fun removeBook(filePath: String) {
            if (filePath !in managedBooks) return
            managedBooks = managedBooks.toMutableMap().also { it.remove(filePath) }
            if (lastActiveBookPath == filePath) {
                lastActiveBookPath = null
            }
        }

        fun normalizedReadingLineCount(): Int = readingLineCount.coerceIn(MIN_READING_LINE_COUNT, MAX_READING_LINE_COUNT)

        fun updateReadingLineCount(lineCount: Int) {
            readingLineCount = lineCount.coerceIn(MIN_READING_LINE_COUNT, MAX_READING_LINE_COUNT)
        }

        private fun bumpMap() {
            managedBooks = managedBooks.toMutableMap()
        }

        companion object {
            const val MIN_READING_LINE_COUNT = 1
            const val MAX_READING_LINE_COUNT = 20
            const val DEFAULT_READING_LINE_COUNT = 1
        }
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}
