package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReadingLineCountMenuGroupTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        resetPersistentState()
    }

    override fun tearDown() {
        try {
            resetPersistentState()
        } finally {
            super.tearDown()
        }
    }

    fun testPresetLineCountIsMarkedWithCheck() {
        service<FishReadingPersistentState>().updateReadingLineCount(5)

        assertEquals(
            listOf("1", "2", "✓ 5", "10", "自定义"),
            ReadingLineCountMenuGroup().getChildren(null).menuLabels()
        )
    }

    fun testCustomLineCountShowsNumberOnlyWhenNotPreset() {
        val settings = service<FishReadingPersistentState>()
        settings.updateReadingLineCount(7)

        assertEquals(
            listOf("1", "2", "5", "10", "✓ 自定义(7)"),
            ReadingLineCountMenuGroup().getChildren(null).menuLabels()
        )

        settings.updateReadingLineCount(10)

        assertEquals(
            listOf("1", "2", "5", "✓ 10", "自定义"),
            ReadingLineCountMenuGroup().getChildren(null).menuLabels()
        )
    }

    private fun Array<AnAction>.menuLabels(): List<String> = map { it.templatePresentation.text }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }
}
