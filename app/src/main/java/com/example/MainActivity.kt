package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private val financeViewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force a fresh layout cycle for the emulator preview
        android.util.Log.d("DhanomApp", "MainActivity onCreate initializing")
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DhanomFinanceApp(viewModel = financeViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhanomFinanceApp(viewModel: FinanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredLedgerTransactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val brainMemories by viewModel.brainMemories.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val personalizedInsights by viewModel.personalizedInsights.collectAsStateWithLifecycle()

    val cashFlowSummary by viewModel.cashFlowSummary.collectAsStateWithLifecycle()
    val categoryExpenses by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val budgetProgressList by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val flowchartData by viewModel.flowchartData.collectAsStateWithLifecycle()
    val dailyTrends by viewModel.dailyTrends.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusSnackbarMessage) {
        uiState.statusSnackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dhanom_main_scaffold"),
        containerColor = BentoBackgroundLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.currentTab) {
                            FinanceTab.DASHBOARD -> "Dhanom AI"
                            FinanceTab.FLOW_ANALYTICS -> "Flow & Trends"
                            FinanceTab.LEDGER -> "Transaction Ledger"
                            FinanceTab.DHANOM_AI -> "Dhanom Chat"
                            FinanceTab.BUDGETS_GOALS -> "Budgets & Goals"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = BentoDeepPurple
                    )
                },
                actions = {
                    if (uiState.currentTab == FinanceTab.DASHBOARD || uiState.currentTab == FinanceTab.LEDGER) {
                        IconButton(
                            onClick = { viewModel.openAddTransactionDialog() },
                            modifier = Modifier.testTag("topbar_add_tx_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add Transaction",
                                tint = BentoPrimaryPurple
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoBackgroundLight,
                    titleContentColor = BentoDeepPurple
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BentoSurfaceLight,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(width = 1.dp, color = BentoBorderSubtle)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == FinanceTab.DASHBOARD,
                    onClick = { viewModel.selectTab(FinanceTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == FinanceTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Overview", fontWeight = if (uiState.currentTab == FinanceTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoDeepPurple,
                        selectedTextColor = BentoDeepPurple,
                        indicatorColor = BentoLavenderContainer,
                        unselectedIconColor = BentoSecondaryText,
                        unselectedTextColor = BentoSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == FinanceTab.FLOW_ANALYTICS,
                    onClick = { viewModel.selectTab(FinanceTab.FLOW_ANALYTICS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == FinanceTab.FLOW_ANALYTICS) Icons.Filled.AccountTree else Icons.Outlined.AccountTree,
                            contentDescription = "Flow & Trends"
                        )
                    },
                    label = { Text("Trends", fontWeight = if (uiState.currentTab == FinanceTab.FLOW_ANALYTICS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoDeepPurple,
                        selectedTextColor = BentoDeepPurple,
                        indicatorColor = BentoLavenderContainer,
                        unselectedIconColor = BentoSecondaryText,
                        unselectedTextColor = BentoSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_flowchart")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == FinanceTab.LEDGER,
                    onClick = { viewModel.selectTab(FinanceTab.LEDGER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == FinanceTab.LEDGER) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Ledger"
                        )
                    },
                    label = { Text("Ledger", fontWeight = if (uiState.currentTab == FinanceTab.LEDGER) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoDeepPurple,
                        selectedTextColor = BentoDeepPurple,
                        indicatorColor = BentoLavenderContainer,
                        unselectedIconColor = BentoSecondaryText,
                        unselectedTextColor = BentoSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_ledger")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == FinanceTab.DHANOM_AI,
                    onClick = { viewModel.selectTab(FinanceTab.DHANOM_AI) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == FinanceTab.DHANOM_AI) Icons.Filled.SmartToy else Icons.Outlined.SmartToy,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("Chat", fontWeight = if (uiState.currentTab == FinanceTab.DHANOM_AI) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoDeepPurple,
                        selectedTextColor = BentoDeepPurple,
                        indicatorColor = BentoLavenderContainer,
                        unselectedIconColor = BentoSecondaryText,
                        unselectedTextColor = BentoSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_ai")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == FinanceTab.BUDGETS_GOALS,
                    onClick = { viewModel.selectTab(FinanceTab.BUDGETS_GOALS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == FinanceTab.BUDGETS_GOALS) Icons.Filled.TrackChanges else Icons.Outlined.TrackChanges,
                            contentDescription = "Goals"
                        )
                    },
                    label = { Text("Goals", fontWeight = if (uiState.currentTab == FinanceTab.BUDGETS_GOALS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoDeepPurple,
                        selectedTextColor = BentoDeepPurple,
                        indicatorColor = BentoLavenderContainer,
                        unselectedIconColor = BentoSecondaryText,
                        unselectedTextColor = BentoSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_goals")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                FinanceTab.DASHBOARD -> {
                    DashboardScreen(
                        summary = cashFlowSummary,
                        flowchartData = flowchartData,
                        categoryExpenses = categoryExpenses,
                        recentTransactions = transactions,
                        onNavigateTab = { viewModel.selectTab(it) },
                        onAddTransactionClick = { viewModel.openAddTransactionDialog() },
                        onTransactionClick = { viewModel.openAddTransactionDialog(it) }
                    )
                }

                FinanceTab.FLOW_ANALYTICS -> {
                    FlowAnalyticsScreen(
                        flowchartData = flowchartData,
                        categoryExpenses = categoryExpenses,
                        dailyTrends = dailyTrends
                    )
                }

                FinanceTab.LEDGER -> {
                    LedgerTableScreen(
                        transactions = filteredTransactions,
                        searchQuery = uiState.searchQuery,
                        filterCategory = uiState.filterCategory,
                        filterType = uiState.filterType,
                        sortOrder = uiState.sortOrder,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onFilterCategoryChange = { viewModel.setFilterCategory(it) },
                        onFilterTypeChange = { viewModel.setFilterType(it) },
                        onSortChange = { viewModel.setSortOrder(it) },
                        onAddTransactionClick = { viewModel.openAddTransactionDialog() },
                        onEditTransaction = { viewModel.openAddTransactionDialog(it) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onResetSampleData = { viewModel.resetToSampleData() }
                    )
                }

                FinanceTab.DHANOM_AI -> {
                    DhanomChatScreen(
                        messages = chatMessages,
                        memories = brainMemories,
                        insights = personalizedInsights,
                        isChatLoading = uiState.isChatLoading,
                        enableInternetKnowledge = uiState.enableInternetKnowledge,
                        onToggleInternetKnowledge = { viewModel.toggleInternetKnowledge() },
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onClearChat = { viewModel.clearChat() },
                        onRefreshBrain = { viewModel.refreshBrainMemories() },
                        onClearBrain = { viewModel.clearMemories() }
                    )
                }

                FinanceTab.BUDGETS_GOALS -> {
                    BudgetsGoalsScreen(
                        budgetProgressList = budgetProgressList,
                        rawBudgets = budgets,
                        goals = goals,
                        onAddBudgetClick = { viewModel.openAddBudgetDialog() },
                        onAddGoalClick = { viewModel.openAddGoalDialog() },
                        onDepositGoalClick = { viewModel.openDepositDialog(it) },
                        onDeleteBudget = { viewModel.deleteBudget(it) },
                        onDeleteGoal = { viewModel.deleteGoal(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showAddTransactionDialog) {
        AddEditTransactionDialog(
            existing = uiState.editingTransaction,
            onDismiss = { viewModel.closeAddTransactionDialog() },
            onSave = { title, amount, type, category, necessity, account, merchant, notes ->
                viewModel.saveTransaction(title, amount, type, category, necessity, account, merchant, notes)
            }
        )
    }

    if (uiState.showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { viewModel.closeAddBudgetDialog() },
            onSave = { category, limit ->
                viewModel.saveBudget(category, limit)
            }
        )
    }

    if (uiState.showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { viewModel.closeAddGoalDialog() },
            onSave = { title, target, days, tag ->
                viewModel.saveGoal(title, target, days, tag)
            }
        )
    }

    uiState.showDepositDialog?.let { goal ->
        DepositGoalDialog(
            goal = goal,
            onDismiss = { viewModel.closeDepositDialog() },
            onDeposit = { amount ->
                viewModel.depositToGoal(goal, amount)
            }
        )
    }
}
