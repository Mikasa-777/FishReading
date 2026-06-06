package com.example.fishreading

import com.intellij.openapi.components.*

// 定义单本书的进度数据
// 找到 BookProgress 类，修改为如下：
class BookProgress {
    var chapterIdx: Int = 0
    var lineIdx: Int = 0
    var bookName: String = ""
    var chapterTitles: List<String> = arrayListOf() // ✨ 新增这一行：用于缓存该书的所有章节名
}

@State(name = "FishReadingState", storages = [Storage("fish_reading_config.xml")])
@Service(Service.Level.PROJECT)
class FishReadingPersistentState : PersistentStateComponent<FishReadingPersistentState.State> {

    class State {
        var lastActiveBookPath: String? = null
        // 存储所有导入过的书：文件绝对路径 -> 进度信息
        var managedBooks: MutableMap<String, BookProgress> = mutableMapOf()
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }
}