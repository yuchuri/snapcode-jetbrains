package com.github.yuchuri.snapcodejetbrains.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "SnapcodeSettings",
    storages = [Storage("SnapcodeSettings.xml")]
)
class SnapcodeSettingsState : PersistentStateComponent<SnapcodeSettings> {

    private var myState = SnapcodeSettings()

    override fun getState(): SnapcodeSettings = myState

    override fun loadState(state: SnapcodeSettings) {
        myState = state
    }

    companion object {
        val instance: SnapcodeSettingsState
            get() = ApplicationManager.getApplication().getService(SnapcodeSettingsState::class.java)
    }
}
