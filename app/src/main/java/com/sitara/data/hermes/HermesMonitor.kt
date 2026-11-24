package com.sitara.data.hermes

import com.sitara.data.agent.ChatCompletionMonitor
import com.sitara.data.checkpoint.ProjectCheckpoints
import com.sitara.domain.model.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Records task outcomes without copying chat text; detailed learning stays behind the user's backend setting. */
object HermesMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false
    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch {
            ChatCompletionMonitor.events.collect { conversation -> record(conversation) }
        }
    }
    private suspend fun record(c: Conversation) {
        if (!HermesPreferences.enabled.value) return
        val title = c.title.trim().ifBlank { "Untitled task" }
        HermesBridgeClient.recordOutcome("Completed task: $title (agent: ${c.agent.name})", ProjectCheckpoints.pathOf(c.workspaceUri))
    }
}
