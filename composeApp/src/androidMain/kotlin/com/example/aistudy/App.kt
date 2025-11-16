package com.example.aistudy

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aistudy.database.DatabaseDriverFactory
import com.example.aistudy.repository.ChatMessageRepository
import com.example.aistudy.ui.ChatScreen
import com.example.aistudy.viewmodel.ChatViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val context = LocalContext.current
        val repository = remember {
            ChatMessageRepository(DatabaseDriverFactory(context))
        }

        val viewModel: ChatViewModel = viewModel {
            ChatViewModel(repository)
        }
        val uiState by viewModel.uiState.collectAsState()

        ChatScreen(
            uiState = uiState,
            onInputChange = viewModel::updateInputText,
            onSendClick = viewModel::sendMessage,
            onErrorDismiss = viewModel::clearError,
            onClearChat = viewModel::clearHistory,
            modifier = Modifier
        )
    }
}