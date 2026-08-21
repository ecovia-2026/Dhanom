package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.prefs.UserProfile
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import com.example.util.WelcomeVoice
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val financeViewModel: FinanceViewModel by viewModels()
    private var welcomeVoice: WelcomeVoice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.dhanom_enter, R.anim.dhanom_exit)
        enableEdgeToEdge()

        if (savedInstanceState == null && financeViewModel.profile.value.welcomeVoice) {
            welcomeVoice = WelcomeVoice(this)
        }

        setContent {
            val themeId by financeViewModel.themeId.collectAsStateWithLifecycle()
            MyApplicationTheme(themeId = themeId) {
                DhanomFinanceApp(viewModel = financeViewModel)
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.dhanom_enter, R.anim.dhanom_exit)
    }

    override fun onDestroy() {
        welcomeVoice?.shutdown()
        welcomeVoice = null
        super.onDestroy()
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
    val dailySuggestions by viewModel.dailySuggestions.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()

    val cashFlowSummary by viewModel.cashFlowSummary.collectAsStateWithLifecycle()
    val categoryExpenses by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val budgetProgressList by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val flowchartData by viewModel.flowchartData.collectAsStateWithLifecycle()
    val dailyTrends by viewModel.dailyTrends.collectAsStateWithLifecycle()

    val holdings by viewModel.holdings.collectAsStateWithLifecycle()
    val portfolioSummary by viewModel.portfolioSummary.collectAsStateWithLifecycle()
    val assetAllocations by viewModel.assetAllocations.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val modelStatus by viewModel.modelStatus.collectAsStateWithLifecycle()
    val isModelInstalled by viewModel.isModelInstalled.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val thinkingStage by viewModel.thinkingStage.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val chatDraft by viewModel.chatDraft.collectAsStateWithLifecycle()

    val palette = LocalAppPalette.current
    var showGemmaPrompt by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.statusSnackbarMessage) {
        uiState.statusSnackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.background,
                modifier = Modifier.width(300.dp)
            ) {
                DrawerHeader(profile = profile, aiSettings = aiSettings)
                HorizontalDivider(color = palette.border)
                FinanceTab.entries.forEach { tab ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                tab.title,
                                fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.currentTab == tab) palette.accent else palette.onPrimaryContainer
                            )
                        },
                        icon = { Icon(iconForTab(tab), contentDescription = tab.title, tint = if (uiState.currentTab == tab) palette.accent else palette.secondaryText) },
                        selected = uiState.currentTab == tab,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectTab(tab)
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = palette.primaryContainer,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("dhanom_main_scaffold"),
            containerColor = palette.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.currentTab.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.accent
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.testTag("drawer_menu_button")) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = palette.accent)
                        }
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
                                    tint = palette.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = palette.background,
                        titleContentColor = palette.accent
                    )
                )
            },
            bottomBar = {
                DataUsageBar()
            }
        ) { innerPadding ->
            // Entrance animation + animated tab transitions.
            var entered by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { entered = true }
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(350)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(350))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = uiState.currentTab,
                        transitionSpec = {
                            (fadeIn(tween(220)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                                .togetherWith(fadeOut(tween(120)))
                        },
                        label = "tab"
                    ) { tab ->
                        when (tab) {
                    FinanceTab.DASHBOARD -> {
                        DashboardScreen(
                            summary = cashFlowSummary,
                            flowchartData = flowchartData,
                            categoryExpenses = categoryExpenses,
                            recentTransactions = transactions,
                            dailySuggestions = dailySuggestions,
                            loans = loans,
                            holdings = holdings,
                            goals = goals,
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
                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                        )
                    }

                    FinanceTab.PORTFOLIO -> {
                        PortfolioScreen(
                            holdings = holdings,
                            portfolioSummary = portfolioSummary,
                            assetAllocations = assetAllocations,
                            onAddHoldingClick = { viewModel.openAddHoldingDialog() },
                            onEditHolding = { viewModel.openAddHoldingDialog(it) },
                            onDeleteHolding = { viewModel.deleteHolding(it) },
                            onUpdatePrices = { viewModel.updateHoldingPrices() }
                        )
                    }

                    FinanceTab.DHANOM_AI -> {
                        DhanomChatScreen(
                            messages = chatMessages,
                            memories = brainMemories,
                            insights = personalizedInsights,
                            isChatLoading = uiState.isChatLoading,
                            aiMode = if (aiSettings.cloudEnabled && aiSettings.cloudApiKey.isNotBlank()) "cloud" else "local",
                            thinkingStage = thinkingStage,
                            uploadStatus = uploadStatus,
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            onAttachFile = { viewModel.importFile(it) },
                            onQuickAdd = { viewModel.openAddTransactionDialog() },
                            onDeleteLast = { viewModel.deleteLastTransaction() },
                            onClearChat = { viewModel.clearChat() },
                            onRefreshBrain = { viewModel.refreshBrainMemories() },
                            onClearBrain = { viewModel.clearMemories() },
                            chatDraft = chatDraft,
                            onDraftChange = { viewModel.updateChatDraft(it) }
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

                    FinanceTab.REPORTS -> {
                        ReportsExportScreen(
                            transactions = transactions,
                            holdings = holdings,
                            budgets = budgets,
                            goals = goals,
                            memories = brainMemories,
                            chatMessages = chatMessages,
                            cashFlowSummary = cashFlowSummary,
                            onExportBackup = { viewModel.exportAndShareBackup() },
                            onImportBackup = { viewModel.importBackupFromJson(it) }
                        )
                    }

                    FinanceTab.LOANS -> {
                        LoansScreen(
                            loans = loans,
                            onAddClick = { viewModel.openAddLoanDialog() },
                            onEdit = { viewModel.openAddLoanDialog(it) },
                            onDelete = { viewModel.deleteLoan(it) }
                        )
                    }

                    FinanceTab.PROFILE -> {
                        ProfileScreen(
                            profile = profile,
                            aiSettings = aiSettings,
                            themeId = viewModel.themeId.collectAsStateWithLifecycle().value,
                            modelStatus = modelStatus,
                            downloadProgress = downloadProgress,
                            serverStatus = serverStatus,
                            transactionsCount = transactions.size,
                            holdingsCount = holdings.size,
                            onSaveProfile = { viewModel.saveProfileName(it) },
                            onSaveAi = { viewModel.saveAiSettings(it) },
                            onSaveWelcomeVoice = { viewModel.saveWelcomeVoice(it) },
                            onSaveSmsTracking = { viewModel.saveSmsTracking(it) },
                            onSavePan = { viewModel.savePanNumber(it) },
                            onSelectTheme = { viewModel.saveTheme(it) },
                            onGenerateApiKey = { viewModel.generateApiKey() },
                            onDownloadModel = { viewModel.downloadGemmaModel() },
                            onCancelDownload = { viewModel.cancelModelDownload() },
                            onDeleteModel = { viewModel.deleteGemmaModel() },
                            onToggleServer = { viewModel.toggleBrainServer() },
                            onLoadDemoData = { viewModel.loadSampleData() },
                            onClearAllData = { viewModel.clearAllData() }
                        )
                    }
                    }
                }
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
            onSave = { category, limit -> viewModel.saveBudget(category, limit) }
        )
    }

    if (uiState.showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { viewModel.closeAddGoalDialog() },
            onSave = { title, target, days, tag -> viewModel.saveGoal(title, target, days, tag) }
        )
    }

    uiState.showDepositDialog?.let { goal ->
        DepositGoalDialog(
            goal = goal,
            onDismiss = { viewModel.closeDepositDialog() },
            onDeposit = { amount -> viewModel.depositToGoal(goal, amount) }
        )
    }

    if (uiState.showAddHoldingDialog) {
        AddEditHoldingDialog(
            existing = uiState.editingHolding,
            onDismiss = { viewModel.closeAddHoldingDialog() },
            onSave = { holding -> viewModel.saveHolding(holding) }
        )
    }

    if (uiState.showAddLoanDialog) {
        AddEditLoanDialog(
            existing = uiState.editingLoan,
            onDismiss = { viewModel.closeAddLoanDialog() },
            onSave = { loan -> viewModel.saveLoan(loan) }
        )
    }

    // First-run: offer to download the on-device Gemma 4 E4B brain.
    if (showGemmaPrompt && aiSettings.cloudApiKey.isBlank()) {
        AlertDialog(
            onDismissRequest = { showGemmaPrompt = false },
            title = { Text("Add a Cloud Brain key?") },
            text = {
                Text(
                    "For ChatGPT-class accuracy, open Profile → Cloud Brain, pick Groq or Gemini, and paste a free API key. " +
                    "Your ledger never leaves this phone. Local math still works offline with no key."
                )
            },
            confirmButton = {
                TextButton(onClick = { showGemmaPrompt = false; viewModel.selectTab(FinanceTab.PROFILE) }) {
                    Text("Open Profile", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGemmaPrompt = false }) { Text("Later") }
            }
        )
    }
}

@Composable
private fun DrawerHeader(profile: UserProfile, aiSettings: com.example.data.prefs.AiSettings) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        Text("ॐ", style = MaterialTheme.typography.displaySmall, color = palette.primary)
        Spacer(Modifier.height(6.dp))
        Text("Dhan-OM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.accent)
        Text("Personal Finance AI Companion", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
        Spacer(Modifier.height(10.dp))
        Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.onPrimaryContainer)
        Text(if (aiSettings.cloudEnabled && aiSettings.cloudApiKey.isNotBlank()) "Brain: Cloud (accurate)" else "Brain: on-device math", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
    }
}

@Composable
private fun iconForTab(tab: FinanceTab): ImageVector = when (tab) {
    FinanceTab.DASHBOARD -> Icons.Filled.Dashboard
    FinanceTab.FLOW_ANALYTICS -> Icons.Filled.AccountTree
    FinanceTab.LEDGER -> Icons.AutoMirrored.Filled.ReceiptLong
    FinanceTab.PORTFOLIO -> Icons.Filled.ShowChart
    FinanceTab.DHANOM_AI -> Icons.Filled.SmartToy
    FinanceTab.BUDGETS_GOALS -> Icons.Filled.TrackChanges
    FinanceTab.REPORTS -> Icons.Filled.Assessment
    FinanceTab.LOANS -> Icons.Filled.AccountBalanceWallet
    FinanceTab.PROFILE -> Icons.Filled.Person
}
