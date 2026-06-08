package com.example.fishreading

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopupFactory

class SelectChapterAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val readerService = ApplicationManager.getApplication().getService(FishReaderService::class.java)

        // 1. 获取所有清洗好的章节标题列表
        val titles = readerService.getChapterTitles()
        if (titles.isEmpty()) return

        // 2. 使用 IDEA 原生极致弹窗构建器
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(titles)
            .setTitle("选择摸鱼章节 (支持动态搜索)")
            .setItemChosenCallback { selectedValue ->
                // 当用户选中某一行时，提取出章节索引并跳转
                val indexString = selectedValue.substringAfter("[").substringBefore("]")
                val chapterIndex = indexString.toIntOrNull()?.minus(1) ?: return@setItemChosenCallback

                // 跳转章节并刷新编辑器里的虚拟注释
                readerService.jumpToChapter(chapterIndex)
                FishInlayManager.updateInlay(editor, readerService.getCurrentLine())
            }
            .setMovable(true)
            .setResizable(true)
            .setNamerForFiltering { it } // ✨ 修正：这里改用官方标准的过滤属性方法
            .createPopup()
            .showInBestPositionFor(e.dataContext) // 在光标最佳位置优雅弹出
    }
}