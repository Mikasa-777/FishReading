package com.example.fishreading

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore

class LoadBookAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1. ✨ 扩展过滤器：支持 epub，且预留未来多格式的支持
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "选择你的摸鱼秘籍"
            description = "支持格式：.epub"
            withFileFilter { it.extension?.lowercase() in listOf("epub", "txt") } // 扩展点
        }

        val virtualFile = FileChooser.chooseFile(descriptor, project, null)
        if (virtualFile != null) {
            // 2. ✨ 核心修复：改用 IDEA 官方跨平台 VfsUtilCore 工具类转换路径，彻底杜绝 Windows 盘符引发的 FileNotFoundException
            val file = VfsUtilCore.virtualToIoFile(virtualFile)

            // 3. 触发全新的多态加载服务
            val readerService = service<FishReaderService>()
            val resultNotify = readerService.loadBook(file) // 切换为新方法

            Messages.showInfoMessage(project, resultNotify, "图书装载成功")
        }
    }
}