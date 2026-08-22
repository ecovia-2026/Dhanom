package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.GoalEntity
import com.example.data.model.LoanEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.ai.DailySuggestion
import com.example.domain.analytics.CashFlowSummary
import com.example.domain.analytics.CashFlowchartData
import com.example.domain.analytics.CategoryExpense
import com.example.ui.components.CashFlowchartView
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceTab
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun DashboardScreen(
    summary: CashFlowSummary,
    flowchartData: CashFlowchartData,
    categoryExpenses: List<CategoryExpense>,
    recentTransactions: List<TransactionEntity>,
    dailySuggestions: List<DailySuggestion> = emptyList(),
    loans: List<LoanEntity> = emptyList(),
    holdings: List<PortfolioHoldingEntity> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    userName: String = "User",
    photoPath: String = "",
    accounts: List<AccountEntity> = emptyList(),
    accountBalances: Map<Long, Double> = emptyMap(),
    memoryFactCount: Int = 0,
    activeTaskCount: Int = 0,
    dueTasks: List<TaskEntity> = emptyList(),
    onNavigateTab: (FinanceTab) -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bento Item 1: App Header Profile & Status
        item {
            BentoHeaderStatus(userName = userName, photoPath = photoPath, onProfileClick = { onNavigateTab(FinanceTab.PROFILE) })
        }

        // Bento Item 1a: Accounts / net worth snapshot (Monefy/Wallet-style)
        if (accounts.isNotEmpty()) {
            item {
                AccountsSnapshotCard(
                    accounts = accounts,
                    balances = accountBalances,
                    onNavigateTab = onNavigateTab
                )
            }
        }

        // Money snapshot: circle chart + quick menu (savings / investments / loans / share market)
        item {
            MoneySnapshotCard(
                goals = goals,
                holdings = holdings,
                loans = loans,
                onNavigateTab = onNavigateTab
            )
        }

        // Bento Item 1b: Tasks due today / overdue (nudge)
        if (dueTasks.isNotEmpty()) {
            item {
                TasksDueCard(tasks = dueTasks, onClick = { onNavigateTab(FinanceTab.MEMORY) })
            }
        }

        // Bento Item 1c: Brain memory snapshot ("I know N facts about you")
        item {
            BrainMemorySnapshotCard(
                factCount = memoryFactCount,
                taskCount = activeTaskCount,
                onClick = { onNavigateTab(FinanceTab.MEMORY) }
            )
        }

        // Bento Item 2: Intelligence Insight Bento Card
        item {
            BentoIntelligenceInsightCard(
                summary = summary,
                onExploreAi = { onNavigateTab(FinanceTab.DHANOM_AI) },
                onAddTx = onAddTransactionClick
            )
        }

        // Bento Item 2b: Daily Financial Suggestions
        if (dailySuggestions.isNotEmpty()) {
            item {
                DailySuggestionsCard(suggestions = dailySuggestions.take(3))
            }
        }

        // Bento Item 3: Asymmetric 2-Column Row (Net Balance & Security Score)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoNetBalanceCard(
                    summary = summary,
                    modifier = Modifier.weight(1f)
                )
                BentoHealthScoreCard(
                    summary = summary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bento Item 4: Asymmetric Bottom Row (Ask Dhanom Dark Card & Data Health Grade Card)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoAskDhanomCard(
                    modifier = Modifier.weight(1.8f),
                    onClick = { onNavigateTab(FinanceTab.DHANOM_AI) }
                )
                BentoDataHealthGradeCard(
                    summary = summary,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }

        // Bento Item 5: Quick Command Chips Row
        item {
            BentoQuickActionsRow(
                onNavigateTab = onNavigateTab,
                onAddTransactionClick = onAddTransactionClick
            )
        }

        // Bento Item 6: Cash Flow Flowchart (Sankey Bento Card)
        item {
            CashFlowchartView(
                data = flowchartData,
                onCategoryClick = { onNavigateTab(FinanceTab.FLOW_ANALYTICS) }
            )
        }

        // Bento Item 7: 50/30/20 Allocation Card
        item {
            BentoAllocationCard(summary = summary)
        }

        // Bento Item 8: Recent Ledger Transactions Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { onNavigateTab(FinanceTab.LEDGER) }) {
                    Text("View Ledger", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recent transactions. Tap '+ Log Transaction' to record your first entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(5), key = { it.id }) { tx ->
                BentoTransactionRowItem(
                    transaction = tx,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }
    }
}

@Composable
fun BentoHeaderStatus(userName: String = "User", photoPath: String = "", onProfileClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile photo + name together (tap to open Profile)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                ProfileAvatar(photoPath = photoPath, name = userName, size = 50.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Dhan-OM",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "BRAIN ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AccountsSnapshotCard(
    accounts: List<AccountEntity>,
    balances: Map<Long, Double>,
    onNavigateTab: (FinanceTab) -> Unit
) {
    val total = balances.values.sum()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onNavigateTab(FinanceTab.ACCOUNTS) },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💰 Accounts · Net worth ₹${String.format(Locale.US, "%,.0f", total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            accounts.take(4).forEach { acc ->
                val bal = balances[acc.id] ?: acc.initialBalance
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(Color(acc.colorArgb)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(com.example.ui.screens.accountTypeIcon(acc.type), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(acc.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(
                        "₹${String.format(Locale.US, "%,.0f", bal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (bal >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun TasksDueCard(
    tasks: List<TaskEntity>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF3E0),
        border = BorderStroke(1.dp, Color(0xFFFFB74D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE0B2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🗓️ ${tasks.size} ${if (tasks.size == 1) "task" else "tasks"} due today",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = tasks.take(3).joinToString(" · ") { it.title.take(28) } +
                        if (tasks.size > 3) " +${tasks.size - 3} more" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFE65100)
            )
        }
    }
}

@Composable
fun BrainMemorySnapshotCard(
    factCount: Int,
    taskCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🧠 I know $factCount ${if (factCount == 1) "fact" else "facts"} about you",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (taskCount > 0) "Following $taskCount scheduled ${if (taskCount == 1) "task" else "tasks"} · tap to view your memory"
                    else "Tap to view everything I remember",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BentoIntelligenceInsightCard(
    summary: CashFlowSummary,
    onExploreAi: () -> Unit,
    onAddTx: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bento_insight_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTELLIGENCE INSIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "98% Accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"${summary.healthSummary}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExploreAi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(text = "Chat with Dhan-OM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onAddTx,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(text = "+ Log Entry", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun BentoNetBalanceCard(
    summary: CashFlowSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "NET BALANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "₹${String.format(Locale.US, "%,.2f", summary.netCashFlow)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${summary.savingsRate.toInt()}% savings rate vs inflow",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun BentoHealthScoreCard(
    summary: CashFlowSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SECURITY SCORE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { summary.healthScore / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${summary.healthScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Local encryption active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun BentoAskDhanomCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "ASK DHANOM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface,
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"Show me my recurring subscriptions for this month...\"",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun BentoDataHealthGradeCard(
    summary: CashFlowSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DATA HEALTH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (summary.healthScore >= 80) "A+" else if (summary.healthScore >= 65) "A" else "B",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "No logic gaps detected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun BentoQuickActionsRow(
    onNavigateTab: (FinanceTab) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            SuggestionChip(
                onClick = onAddTransactionClick,
                label = { Text("+ Log Expense", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
        item {
            SuggestionChip(
                onClick = { onNavigateTab(FinanceTab.FLOW_ANALYTICS) },
                label = { Text("Flow Analysis", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
        item {
            SuggestionChip(
                onClick = { onNavigateTab(FinanceTab.BUDGETS_GOALS) },
                label = { Text("Budgets & Goals", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
fun BentoAllocationCard(summary: CashFlowSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "50 / 30 / 20 Strategic Allocation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Needs (50%) • Wants (30%) • Savings (20%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                if (summary.needsPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(max(0.01f, summary.needsPercentage.toFloat()))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (summary.wantsPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(max(0.01f, summary.wantsPercentage.toFloat()))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
                if (summary.savingsPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(max(0.01f, summary.savingsPercentage.toFloat()))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BentoAllocationLegend(label = "Needs", amount = summary.needsAmount, pct = summary.needsPercentage, color = MaterialTheme.colorScheme.primary)
                BentoAllocationLegend(label = "Wants", amount = summary.wantsAmount, pct = summary.wantsPercentage, color = MaterialTheme.colorScheme.secondaryContainer)
                BentoAllocationLegend(label = "Savings", amount = summary.savingsAmount, pct = summary.savingsPercentage, color = MaterialTheme.colorScheme.primaryContainer)
            }
        }
    }
}

@Composable
private fun BentoAllocationLegend(
    label: String,
    amount: Double,
    pct: Double,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label (${pct.toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "₹${String.format(Locale.US, "%,.0f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BentoTransactionRowItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIncome) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${transaction.category.displayName} • ${dateFormat.format(Date(transaction.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}₹${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = transaction.account,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailySuggestionsCard(suggestions: List<DailySuggestion>) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("daily_suggestions_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Financial Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (suggestions.any { it.priority == "HIGH" }) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${suggestions.size} tips",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (suggestions.any { it.priority == "HIGH" }) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                val priorityColor = when (suggestion.priority) {
                    "HIGH" -> MaterialTheme.colorScheme.error
                    "MEDIUM" -> Color(0xFFF59E0B)
                    else -> MaterialTheme.colorScheme.primary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = suggestion.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneySnapshotCard(
    goals: List<GoalEntity>,
    holdings: List<PortfolioHoldingEntity>,
    loans: List<LoanEntity>,
    onNavigateTab: (FinanceTab) -> Unit
) {
    val palette = LocalAppPalette.current
    val savings = goals.filter { !it.isCompleted }.sumOf { it.currentAmount }
    val investments = holdings.sumOf { it.currentValue }
    val borrowed = loans.filter { it.type == com.example.data.model.LoanType.LOAN }.sumOf { it.outstandingAmount }
    val debts = loans.filter { it.type == com.example.data.model.LoanType.DEBT }.sumOf { it.outstandingAmount }

    val segments = listOf(
        Triple("Savings", savings, Color(0xFF2E7D32)),
        Triple("Investments", investments, Color(0xFF0B6BCB)),
        Triple("Loans", borrowed, Color(0xFFB45309)),
        Triple("Debts", debts, Color(0xFFD32F2F))
    )
    val total = segments.sumOf { it.second }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Money Snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(140.dp)) {
                        val stroke = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Butt)
                        var start = -90f
                        segments.forEach { seg ->
                            if (seg.second > 0 && total > 0) {
                                val sweep = (seg.second / total * 360.0).toFloat()
                                drawArc(color = seg.third, startAngle = start, sweepAngle = sweep, useCenter = false, style = stroke)
                                start += sweep
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("₹${String.format(java.util.Locale.US, "%,.0f", total)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.accent)
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    segments.forEach { seg ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(seg.third))
                            Spacer(Modifier.width(8.dp))
                            Text(seg.first, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("₹${String.format(java.util.Locale.US, "%,.0f", seg.second)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Quick menu under the chart
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickMoneyChip("Savings", Icons.Default.Savings, Modifier.weight(1f)) { onNavigateTab(FinanceTab.BUDGETS_GOALS) }
                QuickMoneyChip("Investments", Icons.Default.ShowChart, Modifier.weight(1f)) { onNavigateTab(FinanceTab.PORTFOLIO) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickMoneyChip("Loans & Debts", Icons.Default.AccountBalanceWallet, Modifier.weight(1f)) { onNavigateTab(FinanceTab.LOANS) }
                QuickMoneyChip("Share Market", Icons.Default.CandlestickChart, Modifier.weight(1f)) { onNavigateTab(FinanceTab.PORTFOLIO) }
            }
        }
    }
}

@Composable
private fun QuickMoneyChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = palette.primaryContainer
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = palette.onPrimaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.onPrimaryContainer,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
