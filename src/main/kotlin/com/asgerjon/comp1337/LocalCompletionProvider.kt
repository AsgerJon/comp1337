package com.asgerjon.comp1337

import com.intellij.codeInsight.inline.completion.InlineCompletionProvider
import com.intellij.codeInsight.inline.completion.InlineCompletionProviderID
import com.intellij.codeInsight.inline.completion.InlineCompletionRequest
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionVariant
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service

class LocalCompletionProvider : InlineCompletionProvider() {
    override val id: InlineCompletionProviderID = InlineCompletionProviderID("comp1337")

    override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
        val settings = service<Comp1337Settings>().state
        if (!settings.enabled) return InlineCompletionSuggestion.Empty

        val offset = request.startOffset
        val text = ReadAction.compute<String, RuntimeException> { request.document.text }
        if (offset < 0 || offset > text.length) return InlineCompletionSuggestion.Empty

        val prefix = text.substring(0, offset).takeLast(settings.prefixChars)
        val suffix = text.substring(offset).take(settings.suffixChars)

        val completion = service<LocalModelService>().complete(prefix, suffix, settings)
            ?: return InlineCompletionSuggestion.Empty
        if (completion.isBlank()) return InlineCompletionSuggestion.Empty

        return object : InlineCompletionSingleSuggestion {
            override suspend fun getVariant(): InlineCompletionVariant =
                InlineCompletionVariant.build {
                    emit(InlineCompletionGrayTextElement(completion))
                }
        }
    }
}
