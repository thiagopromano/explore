package com.thiromano.exact.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "com.thiromano.exact.settings.ExpertSettings",
    storages = [Storage("ExactElixirSupport.xml")]
)
class ExpertSettings : PersistentStateComponent<ExpertSettings.State> {

    data class State(
        var expertPath: String? = null
    )

    private var state = State()

    var expertPath: String?
        get() = state.expertPath
        set(value) {
            state.expertPath = value
        }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): ExpertSettings =
            ApplicationManager.getApplication().getService(ExpertSettings::class.java)
    }
}
