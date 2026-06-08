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
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}
