import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ChatViewModel()
    @State private var inputText = ""
    @State private var messages: [MessageItem] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    // История сообщений для API (в формате ApiMessage)
    @State private var messageHistory: [ApiMessage] = []

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Список сообщений
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            if messages.isEmpty {
                                Text("Начните разговор с AI ассистентом")
                                    .foregroundColor(.secondary)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                                    .padding()
                            }

                            ForEach(messages) { message in
                                MessageBubbleView(message: message)
                                    .id(message.id)
                            }

                            if isLoading {
                                LoadingView()
                            }
                        }
                        .padding()
                    }
                    .onChange(of: messages.count) { _ in
                        withAnimation {
                            if let lastMessage = messages.last {
                                proxy.scrollTo(lastMessage.id, anchor: .bottom)
                            }
                        }
                    }
                }

                Divider()

                // Поле ввода
                InputView(
                    text: $inputText,
                    isEnabled: !isLoading,
                    onSend: sendMessage
                )
            }
            .navigationTitle("AI Chat Assistant")
            .navigationBarTitleDisplayMode(.inline)
            .alert("Ошибка", isPresented: .constant(errorMessage != nil)) {
                Button("OK") {
                    errorMessage = nil
                }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private func sendMessage() {
        guard !inputText.trimmingCharacters(in: .whitespaces).isEmpty else { return }

        // Инициализация API ключа (если еще не инициализирован)
        if !ApiConfig.shared.isConfigured() {
            let apiKey = ApiKeyProvider.shared.getApiKey()
            if !apiKey.isEmpty {
                ApiConfig.shared.initialize(key: apiKey)
            } else {
                errorMessage = "API ключ не настроен. Добавьте deepseek.api.key в Config.plist"
                return
            }
        }

        // Добавляем сообщение пользователя в UI
        let userMessage = MessageItem(text: inputText, isFromUser: true)
        messages.append(userMessage)

        let question = inputText
        inputText = ""
        isLoading = true

        // Добавляем сообщение пользователя в историю для API
        messageHistory.append(ApiMessage(role: "user", content: question))

        Task {
            do {
                let agent = AiAgent()

                // Отправляем запрос с историей
                let structuredResponse = try await agent.askWithHistory(messageHistory: messageHistory)

                await MainActor.run {
                    // Добавляем ответ AI в историю
                    let fullResponse = "{\"agentMessage\":\"\(structuredResponse.agentMessage)\"}"
                    messageHistory.append(ApiMessage(role: "assistant", content: fullResponse))

                    // Добавляем сообщение в UI
                    let aiMessage = MessageItem(
                        text: structuredResponse.agentMessage,
                        isFromUser: false,
                        structuredData: structuredResponse
                    )
                    messages.append(aiMessage)
                    isLoading = false
                }

                agent.close()
            } catch {
                await MainActor.run {
                    // Удаляем последнее сообщение пользователя из истории при ошибке
                    if !messageHistory.isEmpty {
                        messageHistory.removeLast()
                    }

                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }
}

// MARK: - Message Item
struct MessageItem: Identifiable {
    let id = UUID()
    let text: String
    let isFromUser: Bool
    let timestamp = Date()
    let structuredData: AiStructuredResponse?

    init(text: String, isFromUser: Bool, structuredData: AiStructuredResponse? = nil) {
        self.text = text
        self.isFromUser = isFromUser
        self.structuredData = structuredData
    }
}

// MARK: - Message Bubble View
struct MessageBubbleView: View {
    let message: MessageItem

    var body: some View {
        HStack {
            if message.isFromUser {
                Spacer()
            }

            VStack(alignment: message.isFromUser ? .trailing : .leading, spacing: 4) {
                Text(message.isFromUser ? "Вы" : "AI")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(message.isFromUser ? .blue : .green)

                Text(message.text)
                    .padding(12)
                    .background(message.isFromUser ? Color.blue.opacity(0.2) : Color.green.opacity(0.2))
                    .cornerRadius(12)
            }
            .frame(maxWidth: 300, alignment: message.isFromUser ? .trailing : .leading)

            if !message.isFromUser {
                Spacer()
            }
        }
    }
}

// MARK: - Structured Field View
struct StructuredFieldView: View {
    let label: String
    let value: String
    var emphasized: Bool = false
    var isCode: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(label):")
                .font(.caption)
                .fontWeight(emphasized ? .bold : .semibold)

            Text(value)
                .font(isCode ? .system(.caption, design: .monospaced) : .body)
                .fontWeight(emphasized ? .medium : .regular)
        }
    }
}

// MARK: - Loading View
struct LoadingView: View {
    var body: some View {
        HStack {
            HStack(spacing: 8) {
                ProgressView()
                    .scaleEffect(0.8)
                Text("AI думает...")
                    .font(.caption)
            }
            .padding(12)
            .background(Color.green.opacity(0.2))
            .cornerRadius(12)

            Spacer()
        }
    }
}

// MARK: - Input View
struct InputView: View {
    @Binding var text: String
    let isEnabled: Bool
    let onSend: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            TextField("Введите ваш вопрос...", text: $text, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .lineLimit(1...4)
                .disabled(!isEnabled)

            Button(action: onSend) {
                Image(systemName: "paperplane.fill")
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(isEnabled && !text.trimmingCharacters(in: .whitespaces).isEmpty ? Color.blue : Color.gray)
                    .cornerRadius(22)
            }
            .disabled(!isEnabled || text.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .padding()
        .background(Color(UIColor.systemBackground))
    }
}

// MARK: - ObservableObject ViewModel
class ChatViewModel: ObservableObject {
    // Заглушка для возможного использования в будущем
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
