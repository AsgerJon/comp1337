package com.asgerjon.comp1337

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import javax.swing.JComponent

class Comp1337Configurable : Configurable {
    override fun getDisplayName(): String = "comp1337"

    override fun createComponent(): JComponent {
        val state = service<Comp1337Settings>().state
        return panel {
            row {
                checkBox("Enable local inline completions").bindSelected(state::enabled)
            }
            row("Backend (openai = LM Studio/Ollama /v1, llamacpp = llama-server /infill):") {
                comboBox(listOf("openai", "llamacpp")).bindItem(state::backend.toNullableProperty())
            }
            row("Endpoint (loopback only):") {
                textField().bindText(state::endpoint)
            }
            row("Model (exact id from /v1/models):") {
                textField().bindText(state::model)
            }
            row("Max tokens:") {
                textField().bindIntText(state::maxTokens)
            }
            row("Prefix chars before caret:") {
                textField().bindIntText(state::prefixChars)
            }
            row("Suffix chars after caret:") {
                textField().bindIntText(state::suffixChars)
            }
            row("Request timeout (ms):") {
                textField().bindIntText(state::timeoutMs)
            }
        }
    }
}
