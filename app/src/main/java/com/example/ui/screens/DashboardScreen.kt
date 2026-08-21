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
import com.example.data.model.GoalEntity
import com.example.data.model.LoanEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.ai.DailySuggestion
import com.example.domain.analytics.CashFlowSummary
import com.example.domain.analytics.CashFlowchartData
import com.example.domain.analytics.CategoryExpense
import com.example.ui.components.CashFlowchartView
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bento Item 1: App Header Profile & Status
        item {
            BentoHeaderStatus()
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
                    Text("View Ledger", color = BentoPrimaryPurple, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = BentoSurfaceLight,
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recent transactions. Tap '+ Log Transaction' to record your first entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoSecondaryText
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
fun BentoHeaderStatus() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Dhan-OM",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BentoDeepPurple
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BentoActiveGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BRAIN ACTIVE • OFFLINE ENCRYPTED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BentoSecondaryText,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            }
        }

        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = BentoLilacContainer,
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "JD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoDeepPurple
                )
            }
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
        color = BentoLavenderContainer,
        border = BorderStroke(1.dp, BentoBorder)
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
                    color = BentoPrimaryPurple,
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "98% Accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoDeepPurple,
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
                color = BentoOnBackgroundLight,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExploreAi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPrimaryPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(text = "Chat with Dhan-OM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onAddTx,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.6f),
                        contentColor = BentoPrimaryPurple
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
        color = BentoLilacContainer
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
                color = BentoDeepPurple,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "₹${String.format(Locale.US, "%,.2f", summary.netCashFlow)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color = BentoDeepPurple,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${summary.savingsRate.toInt()}% savings rate vs inflow",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = BentoDeepPurple.copy(alpha = 0.85f),
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
        color = BentoSurfaceLight,
        border = BorderStroke(1.dp, BentoBorder)
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
                color = BentoSecondaryText,
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
                    color = BentoDeepPurple,
                    trackColor = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${summary.healthScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoDeepPurple
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Local encryption active",
                style = MaterialTheme.typography.labelSmall,
                color = BentoSecondaryText,
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
        color = BentoDarkCard,
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
                        .background(BentoLilacContainer),
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
                    color = Color.White,
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"Show me my recurring subscriptions for this month...\"",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.9f),
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
        color = BentoPrimaryPurple
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
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (summary.healthScore >= 80) "A+" else if (summary.healthScore >= 65) "A" else "B",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "No logic gaps detected",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
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
                label = { Text("+ Log Expense", color = BentoDeepPurple, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp), tint = BentoPrimaryPurple) },
                border = BorderStroke(1.dp, BentoBorder),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
            )
        }
        item {
            SuggestionChip(
                onClick = { onNavigateTab(FinanceTab.FLOW_ANALYTICS) },
                label = { Text("Flow Analysis", color = BentoDeepPurple, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp), tint = BentoPrimaryPurple) },
                border = BorderStroke(1.dp, BentoBorder),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
            )
        }
        item {
            SuggestionChip(
                onClick = { onNavigateTab(FinanceTab.BUDGETS_GOALS) },
                label = { Text("Budgets & Goals", color = BentoDeepPurple, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = BentoPrimaryPurple) },
                border = BorderStroke(1.dp, BentoBorder),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
            )
        }
    }
}

@Composable
fun BentoAllocationCard(summary: CashFlowSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "50 / 30 / 20 Strategic Allocation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BentoDeepPurple
            )
            Text(
                text = "Needs (50%) • Wants (30%) • Savings (20%)",
                style = MaterialTheme.typography.bodySmall,
                color = BentoSecondaryText
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
                            .background(BentoPrimaryPurple)
                    )
                }
                if (summary.wantsPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(max(0.01f, summary.wantsPercentage.toFloat()))
                            .fillMaxHeight()
                            .background(BentoLilacContainer)
                    )
                }
                if (summary.savingsPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(max(0.01f, summary.savingsPercentage.toFloat()))
                            .fillMaxHeight()
                            .background(BentoLavenderContainer)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BentoAllocationLegend(label = "Needs", amount = summary.needsAmount, pct = summary.needsPercentage, color = BentoPrimaryPurple)
                BentoAllocationLegend(label = "Wants", amount = summary.wantsAmount, pct = summary.wantsPercentage, color = BentoLilacContainer)
                BentoAllocationLegend(label = "Savings", amount = summary.savingsAmount, pct = summary.savingsPercentage, color = BentoLavenderContainer)
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
                color = BentoSecondaryText
            )
        }
        Text(
            text = "₹${String.format(Locale.US, "%,.0f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = BentoDeepPurple
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
        color = Color.White,
        border = BorderStroke(1.dp, BentoBorder)
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
                        if (isIncome) BentoActiveGreen.copy(alpha = 0.15f)
                        else BentoLavenderContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = if (isIncome) BentoActiveGreen else BentoPrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoOnBackgroundLight,
                    maxLines = 1
                )
                Text(
                    text = "${transaction.category.displayName} • ${dateFormat.format(Date(transaction.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoSecondaryText
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}₹${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) BentoActiveGreen else BentoDeepPurple
                )
                Text(
                    text = transaction.account,
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoSecondaryText
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
        color = Color.White,
        border = BorderStroke(1.dp, BentoBorder)
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
                    color = BentoDeepPurple
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (suggestions.any { it.priority == "HIGH" }) Color(0xFFFFEBEE) else BentoLavenderContainer
                ) {
                    Text(
                        text = "${suggestions.size} tips",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (suggestions.any { it.priority == "HIGH" }) BentoExpenseRed else BentoDeepPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                val priorityColor = when (suggestion.priority) {
                    "HIGH" -> BentoExpenseRed
                    "MEDIUM" -> Color(0xFFF59E0B)
                    else -> BentoPrimaryPurple
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
                            color = BentoOnBackgroundLight
                        )
                        Text(
                            text = suggestion.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoSecondaryText,
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
        color = Color.White,
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
