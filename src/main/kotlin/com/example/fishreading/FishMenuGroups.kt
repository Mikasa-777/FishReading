package com.example.fishreading

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Separator
import java.io.File

class BookAndChapterMenuGroup : ActionGroup() {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return EMPTY_ARRAY
        val state = project.getService(FishReadingPersistentState::class.java).state
        val readerService = project.getService(FishReaderService::class.java)

        // 遍历所有缓存的书籍
        return state.managedBooks.map { (path, progress) ->
            val isCurrentBook = state.lastActiveBookPath == path
            val displayName = if (isCurrentBook) "✓ ${progress.bookName}" else progress.bookName

            // 每一本书都是一个二级弹窗组
            object : ActionGroup(displayName, true) {
                override fun getChildren(ae: AnActionEvent?): Array<AnAction> {
                    val actions = mutableListOf<AnAction>()

                    // 🎯 优化核心 1：如果这本书从未缓存过目录，提示去激活载入
                    if (progress.chapterTitles.isEmpty()) {
                        actions.add(object : AnAction("⚠️ 尚未缓存目录，点击激活载入") {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                readerService.loadEpub(File(path))
                                FishInlayManager.updateInlay(editor, readerService.getCurrentLine())
                            }
                        })
                        return actions.toTypedArray()
                    }

                    // 🎯 优化核心 2：在书籍菜单最顶部，强力插入一个“继续阅读”动作
                    val resumeText = if (isCurrentBook) "▶ 继续阅读 (当前书籍)" else "▶ 继续阅读 (从上次进度恢复)"
                    actions.add(object : AnAction(resumeText) {
                        override fun actionPerformed(event: AnActionEvent) {
                            val editor = event.getData(CommonDataKeys.EDITOR) ?: return

                            // 核心：直接调用 loadEpub，它内部会自动恢复这本书独有的 chapterIdx 和 lineIdx
                            readerService.loadEpub(File(path))
                            // 渲染引擎直接抓取恢复后的句子，完美复活！
                            FishInlayManager.updateInlay(editor, readerService.getCurrentLine())
                        }
                    })

                    // 塞入一根优雅的分割线，区分“继续阅读”和“章节列表”
                    actions.add(Separator.getInstance())

                    // 3. 展现具体的章节列表
                    val chapterActions = progress.chapterTitles.mapIndexed { chapIndex, title ->
                        val isCurrentChapter = isCurrentBook && readerService.getCurrentChapterIdx() == chapIndex
                        val chapterDisplayName = if (isCurrentChapter) "➔ $title" else title

                        object : AnAction(chapterDisplayName) {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                if (state.lastActiveBookPath != path) {
                                    readerService.loadEpub(File(path))
                                }
                                // 点击具体章节，依然保持去往该章开头的逻辑
                                readerService.jumpToChapter(chapIndex)
                                FishInlayManager.updateInlay(editor, readerService.getCurrentLine())
                            }
                        }
                    }
                    actions.addAll(chapterActions)

                    return actions.toTypedArray()
                }
            }
        }.toTypedArray()
    }
}