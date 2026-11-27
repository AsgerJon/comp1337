package com.asgerjon.comp1337

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(name = "Comp1337Settings", storages = [Storage("comp1337.xml")])
class Comp1337Settings : PersistentStateComponent<Comp1337Settings.State> {
    class State {
        var enabled: Boolean = true
        var backend: String = "openai"
        var endpoint: String = "http://127.0.0.1:1234/v1"
        var model: String = "qwen2.5-coder-1.5b"
        var maxTokens: Int = 64
        var prefixChars: Int = 4000
        var suffixChars: Int = 1000
        var timeoutMs: Int = 1500
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }
}
