package com.github.yuchuri.snapcodejetbrains

import com.github.yuchuri.snapcodejetbrains.actions.SnapcodeAction
import com.github.yuchuri.snapcodejetbrains.render.SnapcodeRenderer
import com.github.yuchuri.snapcodejetbrains.settings.SnapcodeSettings
import com.github.yuchuri.snapcodejetbrains.settings.SnapcodeSettingsState
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SnapcodePluginTest : BasePlatformTestCase() {

    fun testActionIsRegistered() {
        val action = ActionManager.getInstance().getAction("Snapcode.TakeSnapshot")
        assertNotNull("Snapcode action should be registered", action)
        assertInstanceOf(action, SnapcodeAction::class.java)
    }

    fun testDefaultSettings() {
        val settings = SnapcodeSettings()
        assertEquals(40, settings.padding)
        assertTrue(settings.showWindowChrome)
        assertTrue(settings.shadowEnabled)
        assertEquals("DARK", settings.theme)
        assertEquals(14f, settings.fontSize)
        assertEquals("#1a1a2e", settings.backgroundColorStart)
        assertEquals("#16213e", settings.backgroundColorEnd)
    }

    fun testSettingsStatePersistence() {
        val state = SnapcodeSettingsState.instance
        assertNotNull(state)

        val original = state.state.copy()
        state.state.padding = 60
        assertEquals(60, state.state.padding)

        // Restore
        state.loadState(original)
        assertEquals(original.padding, state.state.padding)
    }

    fun testRendererProducesValidImage() {
        val settings = SnapcodeSettings()
        val image = SnapcodeRenderer.render(project, "val x = 42\nprintln(x)", PlainTextFileType.INSTANCE, settings)
        assertNotNull(image)
        assertTrue("Image width should be positive", image.width > 0)
        assertTrue("Image height should be positive", image.height > 0)
    }

    fun testRendererRespectsPadding() {
        val settingsSmall = SnapcodeSettings(padding = 10)
        val settingsLarge = SnapcodeSettings(padding = 80)
        val imageSmall = SnapcodeRenderer.render(project, "val x = 1", PlainTextFileType.INSTANCE, settingsSmall)
        val imageLarge = SnapcodeRenderer.render(project, "val x = 1", PlainTextFileType.INSTANCE, settingsLarge)
        // Larger padding should produce a larger image
        assertTrue(imageLarge.width > imageSmall.width)
        assertTrue(imageLarge.height > imageSmall.height)
    }
}
