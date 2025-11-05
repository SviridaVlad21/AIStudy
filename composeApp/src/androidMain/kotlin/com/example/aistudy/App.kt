package com.example.aistudy

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aistudy.ui.ChatScreen
import com.example.aistudy.viewmodel.ChatViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel: ChatViewModel = viewModel { ChatViewModel() }
        val uiState by viewModel.uiState.collectAsState()

        ChatScreen(
            uiState = uiState,
            onInputChange = viewModel::updateInputText,
            onSendClick = viewModel::sendMessage,
            onErrorDismiss = viewModel::clearError,
            modifier = Modifier
        )
    }
}