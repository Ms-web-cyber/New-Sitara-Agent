package com.sitara

import androidx.compose.runtime.Composable
import com.sitara.ui.chat.ChatScreen
import com.sitara.ui.chat.ChatViewModel

@Composable
fun MainNavigation(viewModel: ChatViewModel? = null) {
    if (viewModel != null) {
        ChatScreen(viewModel = viewModel)
    } else {
        ChatScreen()
    }
}
