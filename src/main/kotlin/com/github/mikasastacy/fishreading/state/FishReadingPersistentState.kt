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

        private fun bumpMap() {
            managedBooks = managedBooks.toMutableMap()
        }
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}
