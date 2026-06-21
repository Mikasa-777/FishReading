package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.inlay.FishInlayService
import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages

class ReadingLineCountMenuGroup : ActionGroup("阅读行数", true) {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val lineCount = service<FishReadingPersistentState>().normalizedReadingLineCount()
        val actions = PRESET_LINE_COUNTS.map { count ->
            SetReadingLineCountAction(count, labelForPreset(count, lineCount))
        }
        return (actions + CustomReadingLineCountAction(labelForCustom(lineCount))).toTypedArray()
    }

    private class SetReadingLineCountAction(
        private val lineCount: Int,
        text: String
    ) : AnAction(text) {
        override fun actionPerformed(e: AnActionEvent) {
            applyReadingLineCount(lineCount)
        }
    }

    private class CustomReadingLineCountAction(text: String) : AnAction(text) {
        override fun actionPerformed(e: AnActionEvent) {
            val settings = service<FishReadingPersistentState>()
            val input = Messages.showInputDialog(
                e.project,
                "请输入阅读行数 (1-20)",
                "自定义阅读行数",
                null,
                settings.normalizedReadingLineCount().toString(),
                ReadingLineCountValidator
            ) ?: return

            applyReadingLineCount(input.toInt())
        }
    }

    companion object {
        private val PRESET_LINE_COUNTS = listOf(1, 2, 5, 10)

        private fun labelForPreset(count: Int, current: Int): String =
            if (count == current) "✓ $count" else count.toString()

        private fun labelForCustom(current: Int): String {
            val text = if (current in PRESET_LINE_COUNTS) "自定义" else "自定义($current)"
            return if (current !in PRESET_LINE_COUNTS) "✓ $text" else text
        }

        private fun applyReadingLineCount(lineCount: Int) {
            service<FishReadingPersistentState>().updateReadingLineCount(lineCount)

            val inlayService = service<FishInlayService>()
            val editor = inlayService.activeEditor() ?: return
            inlayService.updateInlay(editor, service<FishReaderService>().getCurrentPage())
        }
    }
}

private object ReadingLineCountValidator : InputValidator {
    override fun checkInput(inputString: String?): Boolean = inputString?.toIntOrNull() in 1..20

    override fun canClose(inputString: String?): Boolean = checkInput(inputString)
}
