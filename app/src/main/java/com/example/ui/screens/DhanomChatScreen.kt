package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.BrainMemoryEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MessageSender
import com.example.domain.ml.PersonalizedFinancialInsight
import com.example.ui.components.MarkdownLiteText
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhanomChatScreen(
    messages: List<ChatMessageEntity>,
    memories: List<BrainMemoryEntity>,
    insights: List<PersonalizedFinancialInsight> = emptyList(),
    isChatLoading: Boolean,
    aiMode: String = "offline",
    thinkingStage: String = "",
    uploadStatus: com.example.ui.viewmodel.FinanceViewModel.UploadStatus? = null,
    onSendMessage: (String) -> Unit,
    onAttachFile: (Uri) -> Unit,
    onQuickAdd: () -> Unit,
    onDeleteLast: () -> Unit,
    onClearChat: () -> Unit,
    onRefreshBrain: () -> Unit,
    onClearBrain: () -> Unit,
    committedPrompt: String = "",
    onSaveCommittedPrompt: (String) -> Unit = {},
    attachedImage: android.graphics.Bitmap? = null,
    onClearImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showCommands by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spoken = matches?.firstOrNull()?.trim()
        if (!spoken.isNullOrBlank()) {
            onSendMessage(spoken)
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAttachFile(it) }
    }

    // Camera: take a photo of a bill/receipt/item and hand it to the brain.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        cameraUri = null
        if (success && uri != null) onAttachFile(uri)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("dhanom_chat_screen")
    ) {
        // Top Bar Controls & Mode Selector
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animated "brain working" bot icon (pulsing glow + rotation)
                        val transition = rememberInfiniteTransition(label = "bot")
                        val pulse by transition.animateFloat(
                            initialValue = 0.94f, targetValue = 1.06f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
                        )
                        val glow by transition.animateFloat(
                            initialValue = 0.25f, targetValue = 0.55f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow"
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f + glow)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (aiMode == "gemma" || aiMode == "cloud") Color(0xFF2E7D32) else Color(0xFF9E9E9E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (aiMode) {
                                    "cloud" -> "Cloud Brain (most accurate)"
                                    "gemma" -> "Gemma 4 E2B (fast) (on-device)"
                                    else -> "Gemma 4 E2B (fast) (on-device)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showCommands = true }) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Quick commands",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showHistory = true }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Conversation history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showInstructions = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Standing instructions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onClearChat) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Chat History",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            }
        }

        // Chat Message List (single view — no sub-tabs)
        LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }

                if (isChatLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            androidx.compose.animation.AnimatedContent(
                                targetState = thinkingStage.ifBlank { "Thinking…" },
                                label = "thinking"
                            ) { stage ->
                                Text(
                                    text = stage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Prompt Suggestions (Enhanced with NLU & ML Queries)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SuggestionChip(
                        onClick = onQuickAdd,
                        label = { Text("+ Log expense") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = onDeleteLast,
                        label = { Text("− Delete last") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Show me my spending last month") },
                        label = { Text("Spending last month") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("What's my current investment return?") },
                        label = { Text("Investment return") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Predict my end of month balance") },
                        label = { Text("Predict month-end") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Categorize this transaction: Starbucks $6.50") },
                        label = { Text("Categorize Starbucks") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("When will I reach my goal?") },
                        label = { Text("Goal completion ETA") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Show cash flow flowchart") },
                        label = { Text("Flowchart") }
                    )
                }
            }

            // Attached image preview (with a remove button)
            attachedImage?.let { bmp ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached image",
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Image attached — the brain will see it and its OCR text.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearImage) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Upload status (Claude-style file state + type icon + percentage)
            uploadStatus?.let { up ->
                val typeIcon = when {
                    up.name.endsWith(".pdf", true) -> Icons.Default.PictureAsPdf
                    up.name.endsWith(".xlsx", true) || up.name.endsWith(".csv", true) -> Icons.Default.TableChart
                    up.name.endsWith(".docx", true) || up.name.endsWith(".txt", true) -> Icons.Default.Description
                    up.name.endsWith(".zip", true) -> Icons.Default.FolderZip
                    up.name.startsWith("IMG", true) || up.name.contains("bill_", true) -> Icons.Default.Image
                    else -> Icons.Default.InsertDriveFile
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (up.state.startsWith("Done")) Icons.Default.CheckCircle
                                    else if (up.state.startsWith("Failed")) Icons.Default.Error
                                    else typeIcon,
                                contentDescription = null,
                                tint = if (up.state.startsWith("Failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            val pct = (up.progress * 100).toInt()
                            Text(
                                if (up.progress > 0f && up.progress < 1f) "${up.name} · ${up.state} ($pct%)" else "${up.name} · ${up.state}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (up.progress > 0f && up.progress < 1f) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { up.progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Chat Input Bar (big field + send on the right; tool toggles above)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Tool buttons row (above the text field)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChatToolButton(Icons.Default.AttachFile, "Attach file") { fileLauncher.launch("*/*") }
                        ChatToolButton(Icons.Default.PhotoCamera, "Take photo of bill") {
                            try {
                                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                                val photo = File(dir, "bill_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                // No camera app — ignore, user can still attach.
                            }
                        }
                        ChatToolButton(Icons.Default.Mic, "Speak") {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Dhan-OM…")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                // No speech recognizer on device — fall back to typing.
                            }
                        }
                        ChatToolButton(Icons.Default.Bolt, "Quick commands") { showCommands = true }
                        Spacer(modifier = Modifier.weight(1f))
                        if (inputText.isNotBlank()) {
                            Text(
                                text = "${inputText.length}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big input + send button on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Dhan-OM or log transactions...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            minLines = 1,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
    }

    // ⚡ Quick commands sheet
    if (showCommands) {
        QuickCommandsSheet(
            onDismiss = { showCommands = false },
            onCommand = { cmd ->
                showCommands = false
                onSendMessage(cmd)
            }
        )
    }

    // Conversation history (stored on-device)
    if (showHistory) {
        ConversationHistorySheet(
            messages = messages,
            onDismiss = { showHistory = false }
        )
    }

    // Standing instructions (committed prompt) editor
    if (showInstructions) {
        var draft by remember(committedPrompt) { mutableStateOf(committedPrompt) }
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            title = { Text("Standing Instructions") },
            text = {
                Column {
                    Text(
                        "These are added to EVERY conversation automatically — set them once and I'll always follow them (e.g. \"always reply in Hinglish\", \"always suggest SIP options\", \"I am saving for a house\").",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("e.g. Always answer in short bullets and suggest investments") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSaveCommittedPrompt(draft); showInstructions = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showInstructions = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MlInsightCard(
    insight: PersonalizedFinancialInsight,
    onAction: (String) -> Unit
) {
    val (bgColor, icon, iconColor) = when (insight.type) {
        "PREDICTION" -> Triple(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), Icons.Default.Timeline, MaterialTheme.colorScheme.primary)
        "ANOMALY" -> Triple(Color(0xFFFFEBEE), Icons.Default.Warning, MaterialTheme.colorScheme.error)
        "SAVING_OPPORTUNITY" -> Triple(Color(0xFFE8F5E9), Icons.Default.Savings, Color(0xFF2E7D32))
        else -> Triple(MaterialTheme.colorScheme.primaryContainer, Icons.Default.Stars, MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${(insight.confidence * 100).toInt()}% Match",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = insight.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!insight.actionText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = { onAction(insight.title) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(insight.actionText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessageEntity) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Assistant avatar + name + model chip (Gemini/Claude style)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("ॐ", color = MaterialTheme.colorScheme.surface, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (!isUser) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)) {
                    Text(
                        text = "Dhan-OM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    message.actionType?.let { action ->
                        Spacer(Modifier.width(6.dp))
                        val label = when (action) {
                            "CLOUD_BRAIN" -> "☁️ Cloud"
                            "GEMMA_BRAIN" -> "⚙️ Gemma"
                            else -> null
                        }
                        if (label != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = if (isUser) 0.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (isUser) {
                        SelectionContainer {
                            Text(
                                text = message.messageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    } else {
                        SelectionContainer {
                            MarkdownLiteText(
                                text = message.messageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun BrainMemoryCard(memory: BrainMemoryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.topic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${(memory.confidenceScore * 100).toInt()}% Conf",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = memory.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (memory.actionSuggestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = memory.actionSuggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCommandsSheet(onDismiss: () -> Unit, onCommand: (String) -> Unit) {
    val commands = listOf(
        "💸 create a bill" to "Create invoice",
        "📄 export pdf report" to "Export PDF",
        "📊 export excel" to "Export Excel",
        "🗂️ export backup" to "Export backup",
        "⚠️ risk" to "Risk analysis",
        "💡 optimize my savings" to "Optimize savings",
        "📈 analysis" to "Full analysis",
        "🎯 change theme to midnight" to "Dark theme",
        "🔓 decrypt pdf" to "Unlock PDF",
        "📑 merge pdf" to "Merge PDFs",
        "✂️ split pdf" to "Split PDF",
        "➕ add income" to "Add income",
        "➖ delete last transaction" to "Delete last",
        "🎯 add a goal" to "Add goal",
        "💰 set a budget" to "Set budget"
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "⚡ Quick Commands",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            "Tap any command to run it instantly — no typing needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            items(commands) { (cmd, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommand(cmd) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistorySheet(messages: List<ChatMessageEntity>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Conversation History (on-device)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            "${messages.size} messages stored locally",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        val fmt = remember { SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()) }
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp).padding(bottom = 24.dp)) {
            items(messages.takeLast(60).reversed(), key = { it.id }) { m ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        (if (m.sender == MessageSender.USER) "You" else "Dhan-OM") + " · " + fmt.format(Date(m.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        m.messageText.take(300),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
