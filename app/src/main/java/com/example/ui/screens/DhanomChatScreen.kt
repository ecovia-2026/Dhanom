package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrainMemoryEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MessageSender
import com.example.domain.ml.PersonalizedFinancialInsight
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhanomChatScreen(
    messages: List<ChatMessageEntity>,
    memories: List<BrainMemoryEntity>,
    insights: List<PersonalizedFinancialInsight> = emptyList(),
    isChatLoading: Boolean,
    enableInternetKnowledge: Boolean,
    onToggleInternetKnowledge: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onRefreshBrain: () -> Unit,
    onClearBrain: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Chat, 1 = ML Brain Inspector
    val listState = rememberLazyListState()

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
            color = BentoOffWhite,
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoLavenderContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = BentoDeepPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dhanom AI Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoCardText
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (enableInternetKnowledge) BentoPositiveGreen else Color(0xFF9E9E9E))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (enableInternetKnowledge) "Internet Knowledge Active" else "On-Device ML (Encrypted Offline)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BentoSecondaryText
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleInternetKnowledge) {
                            Icon(
                                imageVector = if (enableInternetKnowledge) Icons.Default.Public else Icons.Default.PublicOff,
                                contentDescription = "Toggle Internet Knowledge",
                                tint = if (enableInternetKnowledge) BentoPrimaryPurple else BentoSecondaryText
                            )
                        }
                        IconButton(onClick = onClearChat) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Chat History",
                                tint = BentoSecondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-tab Navigation
                PrimaryTabRow(
                    selectedTabIndex = selectedSubTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        text = { Text("Conversational NLU", fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        text = { Text("ML Habit Brain (${insights.size + memories.size})", fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        if (selectedSubTab == 0) {
            // Chat Message List
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
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = BentoDeepPurple
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Dhanom is analyzing cash flows & learning patterns...",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoSecondaryText
                            )
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

            // Chat Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Dhanom or log transactions...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) BentoDeepPurple else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (inputText.isNotBlank()) Color.White else BentoSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // ML Habit Brain Inspector
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Header card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoLavenderContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🧠 On-Device Neural Brain",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoDeepPurple
                                )
                                OutlinedButton(
                                    onClick = onRefreshBrain,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Re-analyze")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Dhanom continuously runs Bayesian categorization, time-series burn rate forecasting, recurring periodicity analysis, and Z-score outlier detection completely offline on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoCardText
                            )
                        }
                    }
                }

                // Section: Live ML Predictive Insights
                if (insights.isNotEmpty()) {
                    item {
                        Text(
                            text = "Live ML Insights & Predictions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoDeepPurple,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(insights, key = { it.id }) { insight ->
                        MlInsightCard(insight = insight, onAction = { onSendMessage(it) })
                    }
                }

                // Section: Stored Brain Memories
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Learned Memory Store (${memories.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoDeepPurple
                        )
                        if (memories.isNotEmpty()) {
                            TextButton(onClick = onClearBrain) {
                                Text("Clear Memories", color = BentoExpenseRed, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (memories.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No memory entries yet. Tap 'Re-analyze' or log transactions to trigger pattern discovery.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BentoSecondaryText
                                )
                            }
                        }
                    }
                } else {
                    items(memories, key = { it.id }) { mem ->
                        BrainMemoryCard(memory = mem)
                    }
                }
            }
        }
    }
}

@Composable
fun MlInsightCard(
    insight: PersonalizedFinancialInsight,
    onAction: (String) -> Unit
) {
    val (bgColor, icon, iconColor) = when (insight.type) {
        "PREDICTION" -> Triple(BentoLilacContainer.copy(alpha = 0.5f), Icons.Default.Timeline, BentoDeepPurple)
        "ANOMALY" -> Triple(Color(0xFFFFEBEE), Icons.Default.Warning, BentoExpenseRed)
        "SAVING_OPPORTUNITY" -> Triple(Color(0xFFE8F5E9), Icons.Default.Savings, BentoPositiveGreen)
        else -> Triple(BentoLavenderContainer, Icons.Default.Stars, BentoPrimaryPurple)
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
                        color = BentoCardText
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${(insight.confidence * 100).toInt()}% Match",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoDeepPurple,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = insight.message,
                style = MaterialTheme.typography.bodyMedium,
                color = BentoCardText
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
fun ChatBubbleItem(message: ChatMessageEntity) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BentoDeepPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) BentoDeepPurple else Color.White,
            tonalElevation = 1.dp,
            shadowElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else BentoCardText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else BentoSecondaryText,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun BrainMemoryCard(memory: BrainMemoryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = BentoCardText
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoLavenderContainer
                ) {
                    Text(
                        text = "${(memory.confidenceScore * 100).toInt()}% Conf",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoDeepPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = memory.description,
                style = MaterialTheme.typography.bodyMedium,
                color = BentoCardText
            )

            if (memory.actionSuggestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoLavenderContainer.copy(alpha = 0.5f)
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
                            color = BentoCardText
                        )
                    }
                }
            }
        }
    }
}
