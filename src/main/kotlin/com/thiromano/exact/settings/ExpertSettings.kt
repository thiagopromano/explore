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
        var expertPath: String? = null,
        var minQueryLength: Int = DEFAULT_MIN_QUERY_LENGTH
    )

    private var state = State()

    var expertPath: String?
        get() = state.expertPath
        set(value) {
            state.expertPath = value
        }

    var minQueryLength: Int
        get() = state.minQueryLength
        set(value) {
            state.minQueryLength = value
        }

    companion object {
        const val DEFAULT_MIN_QUERY_LENGTH = 2

        fun getInstance(): ExpertSettings =
            ApplicationManager.getApplication().getService(ExpertSettings::class.java)
    }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

}
