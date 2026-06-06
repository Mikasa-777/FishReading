package com.example.fishreading

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import java.io.File

class LoadEpubAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1. 配置 IDEA 原生文件选择器（只允许选单个文件，锁定 epub 后缀）
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withTitle("选择你的摸鱼秘籍")
            .withDescription("请选择一本 .epub 格式的电子书")
            .withFileFilter { it.extension?.lowercase() == "epub" }

        // 2. 弹出选择器
        val virtualFile = FileChooser.chooseFile(descriptor, project, null)
        if (virtualFile != null) {
            val file = File(virtualFile.path)

            // 3. 调用服务解析电子书
            val readerService = project.getService(FishReaderService::class.java)
            val resultNotify = readerService.loadEpub(file)

            // 4. 极致轻量弹窗通知结果
            Messages.showInfoMessage(project, resultNotify, "图书装载成功")
        }
    }
}