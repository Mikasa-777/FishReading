package com.example.fishreading

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import java.io.File

class LoadEpubAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1. 配置 IDEA 原生文件选择器（只允许选单个文件，锁定 epub 后缀）
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "选择你的摸鱼秘籍"
            description = "Chose one epub book"
            withFileFilter { it.extension?.lowercase() == "epub" }
        }

        // 2. 弹出选择器
        val virtualFile = FileChooser.chooseFile(descriptor, project, null)
        if (virtualFile != null) {
            val file = File(virtualFile.path)

            // 3. 调用服务解析电子书
            val readerService = ApplicationManager.getApplication().getService(FishReaderService::class.java)
            val resultNotify = readerService.loadEpub(file)

            // 4. 极致轻量弹窗通知结果
            Messages.showInfoMessage(project, resultNotify, "图书装载成功")
        }
    }
}