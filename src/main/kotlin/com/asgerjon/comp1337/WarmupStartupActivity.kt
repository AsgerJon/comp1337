package com.asgerjon.comp1337

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class WarmupStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val settings = service<Comp1337Settings>().state
        if (settings.enabled) {
            service<LocalModelService>().warmup(settings)
        }
    }
}
