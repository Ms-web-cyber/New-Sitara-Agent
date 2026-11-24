package com.sitara.data.agent

import com.sitara.domain.model.ChatAttachment
import com.sitara.domain.model.ChatMessage
import com.sitara.domain.model.Conversation
import com.sitara.domain.model.ToolCall
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * State of the chat on screen, shared between ChatViewModel and the agent backend that owns the active conversation.
 */
class ChatSessionStore {
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val currentConversation = MutableStateFlow<Conversation?>(null)
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationError = MutableStateFlow<String?>(null)
    val userEmail = MutableStateFlow<String?>(null)
    val attachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val terminatedToolDialog = MutableStateFlow<Pair<ToolCall, String>?>(null)
}
