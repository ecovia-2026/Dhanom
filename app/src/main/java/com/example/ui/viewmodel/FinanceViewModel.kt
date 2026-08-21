package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.export.ChatAttachmentHelper
import com.example.data.export.ExportManager
import com.example.data.model.*
import com.example.data.prefs.AiSettings
import com.example.data.prefs.AppPrefs
import com.example.data.prefs.UserProfile
import com.example.data.repository.FinanceRepository
import com.example.domain.ai.DailySuggestion
import com.example.domain.ai.DailySuggestionEngine
import com.example.domain.ai.DhanomAiService
import com.example.domain.brain.BrainModelDownloader
import com.example.domain.brain.CloudBrainClient
import com.example.domain.brain.GemmaBrainEngine
import com.example.domain.brain.LocalBrainServer
import com.example.domain.analytics.*
import com.example.domain.ml.PersonalFinanceMlEngine
import com.example.domain.ml.PersonalizedFinancialInsight
import com.example.domain.nlp.ParsedFinanceCommand
import com.example.ui.theme.ThemePalettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class FinanceTab(val title: String) {
    DASHBOARD("Dashboard"),
    FLOW_ANALYTICS("Flow & Charts"),
    LEDGER("Ledger Table"),
    PORTFOLIO("Portfolio"),
    DHANOM_AI("Dhan-OM AI"),
    BUDGETS_GOALS("Budgets & Goals"),
    REPORTS("Reports & Export"),
    LOANS("Loans & Debts"),
    PROFILE("Profile")
}

enum class LedgerSort {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC,
    CATEGORY
}

data class FinanceUiState(
    val currentTab: FinanceTab = FinanceTab.DASHBOARD,
    val searchQuery: String = "",
    val filterCategory: TransactionCategory? = null,
    val filterType: TransactionType? = null,
    val sortOrder: LedgerSort = LedgerSort.DATE_DESC,
    val isChatLoading: Boolean = false,
    val enableInternetKnowledge: Boolean = false,
    val showAddTransactionDialog: Boolean = false,
    val editingTransaction: TransactionEntity? = null,
    val showAddBudgetDialog: Boolean = false,
    val showAddGoalDialog: Boolean = false,
    val showDepositDialog: GoalEntity? = null,
    val showAddHoldingDialog: Boolean = false,
    val editingHolding: PortfolioHoldingEntity? = null,
    val showAddLoanDialog: Boolean = false,
    val editingLoan: LoanEntity? = null,
    val showImportBackupDialog: Boolean = false,
    val statusSnackbarMessage: String? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val aiService = DhanomAiService()
    private val prefs = AppPrefs(application)
    private val gemmaBrain = GemmaBrainEngine(application)
    private val cloudBrain = CloudBrainClient()
    private val brainDownloader = BrainModelDownloader()
    private var brainServer: LocalBrainServer? = null

    private val _modelStatus = MutableStateFlow("")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()

    private val _isModelInstalled = MutableStateFlow(false)
    val isModelInstalled: StateFlow<Boolean> = _isModelInstalled.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _serverStatus = MutableStateFlow("Stopped")
    val serverStatus: StateFlow<String> = _serverStatus.asStateFlow()

    /** Current file-upload status shown in the chat composer (Claude-style). */
    data class UploadStatus(val name: String, val state: String, val progress: Float = -1f)
    private val _uploadStatus = MutableStateFlow<UploadStatus?>(null)
    val uploadStatus: StateFlow<UploadStatus?> = _uploadStatus.asStateFlow()

    /** Cycling "thinking" stage shown while the brain works. */
    private val _thinkingStage = MutableStateFlow("")
    val thinkingStage: StateFlow<String> = _thinkingStage.asStateFlow()

    val profile: StateFlow<UserProfile> = prefs.profile
    val aiSettings: StateFlow<AiSettings> = prefs.aiSettings
    val themeId: StateFlow<String> = prefs.themeId
    val chatDraft: StateFlow<String> = prefs.chatDraft

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            goalDao = db.goalDao(),
            brainMemoryDao = db.brainMemoryDao(),
            chatMessageDao = db.chatMessageDao(),
            portfolioDao = db.portfolioDao(),
            loanDao = db.loanDao()
        )

        // Start EMPTY with a welcome message only — never auto-load demo data.
        viewModelScope.launch {
            repository.onboardIfNeeded()
        }

        _modelStatus.value = if (gemmaBrain.isModelAvailable()) {
            "Ready · ${gemmaBrain.modelSizeLabel()}"
        } else {
            "Not installed"
        }
        _isModelInstalled.value = gemmaBrain.isModelAvailable()

        // Warm up the on-device brain in the background so the first reply is fast.
        if (gemmaBrain.isModelAvailable()) {
            viewModelScope.launch { gemmaBrain.preload() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        brainServer?.shutdown()
        brainServer = null
        gemmaBrain.close()
    }

    private val _uiState = MutableStateFlow(
        FinanceUiState(
            currentTab = runCatching { FinanceTab.valueOf(prefs.loadLastTab()) }.getOrDefault(FinanceTab.DASHBOARD)
        )
    )
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val brainMemories: StateFlow<List<BrainMemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val holdings: StateFlow<List<PortfolioHoldingEntity>> = repository.allHoldings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashFlowSummary: StateFlow<CashFlowSummary> = transactions
        .map { FinancialAnalyticsEngine.calculateCashFlowSummary(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FinancialAnalyticsEngine.calculateCashFlowSummary(emptyList())
        )

    val categoryBreakdown: StateFlow<List<CategoryExpense>> = transactions
        .map { FinancialAnalyticsEngine.calculateCategoryBreakdown(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetProgressList: StateFlow<List<CategoryBudgetProgress>> = combine(budgets, transactions) { bList, txList ->
        FinancialAnalyticsEngine.calculateBudgetProgress(bList, txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flowchartData: StateFlow<CashFlowchartData> = transactions
        .map { FinancialAnalyticsEngine.generateCashFlowchartData(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FinancialAnalyticsEngine.generateCashFlowchartData(emptyList())
        )

    val dailyTrends: StateFlow<List<DailyTrendPoint>> = transactions
        .map { FinancialAnalyticsEngine.calculateDailyTrends(it, 10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalizedInsights: StateFlow<List<PersonalizedFinancialInsight>> = combine(
        transactions,
        budgets,
        goals,
        cashFlowSummary
    ) { txList, bList, gList, summary ->
        PersonalFinanceMlEngine.generatePersonalizedInsights(txList, bList, gList, summary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolioSummary: StateFlow<PortfolioSummary> = holdings
        .map { PortfolioAnalyticsEngine.calculatePortfolioSummary(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PortfolioAnalyticsEngine.calculatePortfolioSummary(emptyList())
        )

    val assetAllocations: StateFlow<List<AssetClassAllocation>> = holdings
        .map { PortfolioAnalyticsEngine.calculateAssetClassAllocations(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySuggestions: StateFlow<List<DailySuggestion>> = combine(
        transactions,
        budgets,
        goals,
        holdings,
        cashFlowSummary
    ) { txList, bList, gList, hList, summary ->
        DailySuggestionEngine.generateDailySuggestions(txList, bList, gList, hList, summary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLedgerTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        _uiState
    ) { txList, state ->
        var list = txList

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase().trim()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                        it.merchant.lowercase().contains(q) ||
                        it.category.displayName.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q) ||
                        it.account.lowercase().contains(q)
            }
        }

        if (state.filterCategory != null) {
            list = list.filter { it.category == state.filterCategory }
        }

        if (state.filterType != null) {
            list = list.filter { it.type == state.filterType }
        }

        when (state.sortOrder) {
            LedgerSort.DATE_DESC -> list.sortedByDescending { it.timestamp }
            LedgerSort.DATE_ASC -> list.sortedBy { it.timestamp }
            LedgerSort.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            LedgerSort.AMOUNT_ASC -> list.sortedBy { it.amount }
            LedgerSort.CATEGORY -> list.sortedBy { it.category.displayName }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: FinanceTab) {
        _uiState.update { it.copy(currentTab = tab) }
        prefs.saveLastTab(tab.name)
    }

    fun updateChatDraft(text: String) {
        prefs.saveChatDraft(text)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterCategory(category: TransactionCategory?) {
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun setFilterType(type: TransactionType?) {
        _uiState.update { it.copy(filterType = type) }
    }

    fun setSortOrder(sort: LedgerSort) {
        _uiState.update { it.copy(sortOrder = sort) }
    }

    fun toggleInternetKnowledge() {
        _uiState.update { it.copy(enableInternetKnowledge = !it.enableInternetKnowledge) }
    }

    fun openAddTransactionDialog(existing: TransactionEntity? = null) {
        _uiState.update { it.copy(showAddTransactionDialog = true, editingTransaction = existing) }
    }

    fun closeAddTransactionDialog() {
        _uiState.update { it.copy(showAddTransactionDialog = false, editingTransaction = null) }
    }

    fun saveTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        necessity: ExpenseNecessity,
        account: String,
        merchant: String,
        notes: String
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.editingTransaction
            if (existing != null) {
                repository.updateTransaction(
                    existing.copy(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        necessity = necessity,
                        account = account,
                        merchant = merchant,
                        notes = notes
                    )
                )
                showSnackbar("Transaction updated")
            } else {
                repository.insertTransaction(
                    TransactionEntity(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        necessity = necessity,
                        account = account,
                        merchant = merchant,
                        notes = notes,
                        timestamp = System.currentTimeMillis()
                    )
                )
                showSnackbar("Transaction added successfully")
            }
            closeAddTransactionDialog()
            refreshBrainMemories()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            showSnackbar("Transaction deleted")
            refreshBrainMemories()
        }
    }

    fun openAddBudgetDialog() {
        _uiState.update { it.copy(showAddBudgetDialog = true) }
    }

    fun closeAddBudgetDialog() {
        _uiState.update { it.copy(showAddBudgetDialog = false) }
    }

    fun saveBudget(category: TransactionCategory, monthlyLimit: Double) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)

            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = monthlyLimit,
                    periodMonth = month,
                    periodYear = year
                )
            )
            closeAddBudgetDialog()
            showSnackbar("Budget set for ${category.displayName}")
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            showSnackbar("Budget removed")
        }
    }

    fun openAddGoalDialog() {
        _uiState.update { it.copy(showAddGoalDialog = true) }
    }

    fun closeAddGoalDialog() {
        _uiState.update { it.copy(showAddGoalDialog = false) }
    }

    fun saveGoal(title: String, targetAmount: Double, targetDays: Int, tag: String) {
        viewModelScope.launch {
            val deadline = System.currentTimeMillis() + (targetDays.toLong() * 24 * 3600 * 1000)
            repository.insertGoal(
                GoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    targetDateMillis = deadline,
                    categoryTag = tag
                )
            )
            closeAddGoalDialog()
            showSnackbar("Goal '$title' created")
        }
    }

    fun openDepositDialog(goal: GoalEntity) {
        _uiState.update { it.copy(showDepositDialog = goal) }
    }

    fun closeDepositDialog() {
        _uiState.update { it.copy(showDepositDialog = null) }
    }

    fun depositToGoal(goal: GoalEntity, depositAmount: Double) {
        viewModelScope.launch {
            val newAmount = goal.currentAmount + depositAmount
            val isComplete = newAmount >= goal.targetAmount
            repository.updateGoal(
                goal.copy(
                    currentAmount = newAmount,
                    isCompleted = isComplete
                )
            )
            repository.insertTransaction(
                TransactionEntity(
                    title = "Deposit towards ${goal.title}",
                    amount = depositAmount,
                    type = TransactionType.TRANSFER,
                    category = TransactionCategory.SAVINGS_TRANSFER,
                    necessity = ExpenseNecessity.SAVINGS,
                    account = "Emergency Savings",
                    merchant = goal.title
                )
            )
            closeDepositDialog()
            showSnackbar("Deposited ₹${String.format(java.util.Locale.US, "%.2f", depositAmount)} towards ${goal.title}!")
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            showSnackbar("Goal removed")
        }
    }

    fun openAddHoldingDialog(existing: PortfolioHoldingEntity? = null) {
        _uiState.update { it.copy(showAddHoldingDialog = true, editingHolding = existing) }
    }

    fun closeAddHoldingDialog() {
        _uiState.update { it.copy(showAddHoldingDialog = false, editingHolding = null) }
    }

    fun saveHolding(holding: PortfolioHoldingEntity) {
        viewModelScope.launch {
            val existing = _uiState.value.editingHolding
            if (existing != null) {
                repository.updateHolding(holding)
                showSnackbar("Holding updated")
            } else {
                repository.insertHolding(holding)
                showSnackbar("Holding added to portfolio")
            }
            closeAddHoldingDialog()
        }
    }

    fun deleteHolding(holding: PortfolioHoldingEntity) {
        viewModelScope.launch {
            repository.deleteHolding(holding)
            showSnackbar("Holding removed")
        }
    }

    // ---------------- Loans & Debts ----------------

    fun openAddLoanDialog(existing: LoanEntity? = null) {
        _uiState.update { it.copy(showAddLoanDialog = true, editingLoan = existing) }
    }

    fun closeAddLoanDialog() {
        _uiState.update { it.copy(showAddLoanDialog = false, editingLoan = null) }
    }

    fun saveLoan(loan: LoanEntity) {
        viewModelScope.launch {
            val existing = _uiState.value.editingLoan
            if (existing != null) repository.updateLoan(loan)
            else repository.insertLoan(loan)
            closeAddLoanDialog()
            showSnackbar("${loan.title} saved")
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            showSnackbar("${loan.title} removed")
        }
    }

    fun cancelModelDownload() {
        brainDownloader.cancel()
    }

    /** Deletes the most recent transaction (used by the chat quick-action). */
    fun deleteLastTransaction() {
        viewModelScope.launch {
            val last = transactions.value.maxByOrNull { it.timestamp }
            if (last != null) {
                repository.deleteTransaction(last)
                refreshBrainMemories()
                showSnackbar("Deleted: ${last.title} (₹${String.format(java.util.Locale.US, "%.2f", last.amount)})")
            } else {
                showSnackbar("No transactions to delete")
            }
        }
    }

    fun updateHoldingPrices() {
        viewModelScope.launch {
            // In a real app, this would fetch live prices. For offline-first, we keep manual entry.
            showSnackbar("Edit any holding to update its current price")
        }
    }

    fun exportAndShareBackup() {
        viewModelScope.launch {
            val bundle = com.example.data.export.BackupBundle(
                transactions = transactions.value,
                budgets = budgets.value,
                goals = goals.value,
                memories = brainMemories.value,
                chatMessages = chatMessages.value,
                holdings = holdings.value
            )
            val context = getApplication<Application>()
            val file = ExportManager.exportBackupJson(context, bundle)
            val intent = ExportManager.createShareIntent(context, file, "application/json")
            val chooser = ExportManager.createShareChooser(intent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            showSnackbar("Backup ready to share via QuickShare/Nearby Share")
        }
    }

    fun importBackupFromJson(json: String) {
        viewModelScope.launch {
            try {
                val bundle = ExportManager.parseBackupJson(json)
                bundle.transactions.forEach { repository.insertTransaction(it) }
                bundle.budgets.forEach { repository.insertBudget(it) }
                bundle.goals.forEach { repository.insertGoal(it) }
                bundle.memories.forEach { repository.insertMemory(it) }
                bundle.chatMessages.forEach { repository.insertChatMessage(it) }
                bundle.holdings.forEach { repository.insertHolding(it) }
                showSnackbar("Backup imported successfully! ${bundle.transactions.size} transactions restored.")
            } catch (e: Exception) {
                showSnackbar("Import failed: ${e.message}")
            }
        }
    }

    fun sendChatMessage(text: String, recordUser: Boolean = true) {
        if (text.isBlank()) return
        val userText = text.trim()
        if (recordUser) updateChatDraft("")

        viewModelScope.launch {
            if (recordUser) {
                repository.insertChatMessage(
                    ChatMessageEntity(
                        sender = MessageSender.USER,
                        messageText = userText,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            _uiState.update { it.copy(isChatLoading = true) }

            // Cycle through human-like "thinking" stages while the brain works.
            val stages = listOf(
                "Thinking…",
                "Analyzing your data…",
                "Reading your records…",
                "Writing the best answer…",
                "Output verification ongoing…"
            )
            val stageJob = launch {
                var i = 0
                while (true) {
                    _thinkingStage.value = stages[i % stages.size]
                    i++
                    kotlinx.coroutines.delay(1100)
                }
            }

            try {

            val gemmaStatus = {
                when {
                    !gemmaBrain.isModelAvailable() && _downloadProgress.value in 0f..0.99f && _modelStatus.value.startsWith("Downloading") ->
                        "Gemma 4 brain is downloading (${(_downloadProgress.value * 100).toInt()}%). Please wait."
                    !gemmaBrain.isModelAvailable() ->
                        "Gemma 4 brain is not installed yet. Open Profile → AI Brain → Download model (~3.7 GB, Wi-Fi recommended)."
                    else -> "Gemma brain is loading. Try again in a moment."
                }
            }
            val cloudGenerate: (suspend (String) -> String?)? = if (aiSettings.value.cloudEnabled && aiSettings.value.cloudApiKey.isNotBlank()) {
                { prompt -> cloudBrain.generate(aiSettings.value.cloudEndpoint, aiSettings.value.cloudApiKey, aiSettings.value.cloudModel, prompt, userText) }
            } else null
            val aiResponse = aiService.processUserMessage(
                userMessage = userText,
                currentTransactions = transactions.value,
                currentBudgets = budgets.value,
                currentGoals = goals.value,
                learnedMemories = brainMemories.value,
                cloudGenerate = cloudGenerate,
                gemmaGenerate = { prompt -> gemmaBrain.generate(prompt) },
                gemmaStatus = gemmaStatus
            )

            when (val cmd = aiResponse.parsedCommand) {
                is ParsedFinanceCommand.AddTransactionCommand -> {
                    repository.insertTransaction(cmd.transaction)
                    refreshBrainMemories()
                }
                is ParsedFinanceCommand.DeleteTransactionCommand -> {
                    cmd.transactionsToDelete.forEach { repository.deleteTransaction(it) }
                    refreshBrainMemories()
                }
                is ParsedFinanceCommand.SetBudgetCommand -> {
                    val cal = Calendar.getInstance()
                    repository.insertBudget(
                        BudgetEntity(
                            category = cmd.category,
                            monthlyLimit = cmd.limit,
                            periodMonth = cal.get(Calendar.MONTH) + 1,
                            periodYear = cal.get(Calendar.YEAR)
                        )
                    )
                }
                is ParsedFinanceCommand.AddGoalCommand -> {
                    repository.insertGoal(cmd.goal)
                }
                is ParsedFinanceCommand.ShowAnalyticsCommand -> {
                    when (cmd.targetTab) {
                        "FLOWCHART", "GRAPHS" -> selectTab(FinanceTab.FLOW_ANALYTICS)
                        "TABLE" -> selectTab(FinanceTab.LEDGER)
                        "HABITS" -> selectTab(FinanceTab.DHANOM_AI)
                        "BUDGETS" -> selectTab(FinanceTab.BUDGETS_GOALS)
                    }
                }
                is ParsedFinanceCommand.CustomizeCommand -> handleCustomize(cmd)
                else -> {}
            }

            // Execute any structured tool-calls the brain emitted (cloud or on-device).
            val finalReply = if (aiResponse.brain != "local") applyGemmaActions(aiResponse.replyText) else aiResponse.replyText

            repository.insertChatMessage(
                ChatMessageEntity(
                    sender = MessageSender.DHANOM_AI,
                    messageText = finalReply,
                    timestamp = System.currentTimeMillis(),
                    actionType = when (aiResponse.brain) {
                        "cloud" -> "CLOUD_BRAIN"
                        "gemma" -> "GEMMA_BRAIN"
                        else -> null
                    }
                )
            )

            // Learn from every conversation (the self-improving brain).
            learnFromChat(userText, finalReply)
            } catch (t: Throwable) {
                repository.insertChatMessage(
                    ChatMessageEntity(
                        sender = MessageSender.DHANOM_AI,
                        messageText = "I couldn't finish that request (${t.message ?: "unknown error"}). Please try again.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } finally {
                stageJob.cancel()
                _thinkingStage.value = ""
                _uiState.update { it.copy(isChatLoading = false) }
            }
        }
    }

    /**
     * Parses JSON tool-calls emitted by the on-device Gemma brain, executes each
     * one against the local database, and returns a clean reply with confirmations.
     * The model never touches the DB directly — it only proposes structured actions.
     */
    private suspend fun applyGemmaActions(rawReply: String): String {
        val pattern = Regex("""\{[^{}]*"action"[^{}]*\}""")
        val matches = pattern.findAll(rawReply).map { it.value }.toList()
        if (matches.isEmpty()) return rawReply

        var cleaned = rawReply
        val confirmations = mutableListOf<String>()

        for (json in matches) {
            try {
                val obj = org.json.JSONObject(json)
                when (obj.optString("action")) {
                    "add_expense" -> {
                        val amount = obj.optDouble("amount", 0.0)
                        if (amount > 0) {
                            val cat = categoryFrom(obj.optString("category"))
                            val merchant = obj.optString("merchant").ifBlank { cat.displayName }
                            repository.insertTransaction(
                                TransactionEntity(
                                    title = merchant,
                                    amount = amount,
                                    type = TransactionType.EXPENSE,
                                    category = cat,
                                    necessity = cat.defaultNecessity,
                                    account = "Cash",
                                    merchant = merchant
                                )
                            )
                            confirmations.add("✅ Logged expense: ₹${fmt(amount)} · $merchant (${cat.displayName})")
                        }
                    }
                    "add_income" -> {
                        val amount = obj.optDouble("amount", 0.0)
                        if (amount > 0) {
                            val cat = when (obj.optString("category").lowercase()) {
                                "freelance" -> TransactionCategory.FREELANCE
                                "dividend", "return" -> TransactionCategory.INVESTMENT_RETURN
                                else -> TransactionCategory.SALARY
                            }
                            val merchant = obj.optString("merchant").ifBlank { cat.displayName }
                            repository.insertTransaction(
                                TransactionEntity(
                                    title = merchant,
                                    amount = amount,
                                    type = TransactionType.INCOME,
                                    category = cat,
                                    necessity = ExpenseNecessity.NEED,
                                    account = "Bank",
                                    merchant = merchant
                                )
                            )
                            confirmations.add("✅ Added income: ₹${fmt(amount)} · $merchant")
                        }
                    }
                    "add_goal" -> {
                        val amount = obj.optDouble("amount", 0.0)
                        val title = obj.optString("title").ifBlank { "Savings Goal" }
                        if (amount > 0) {
                            val days = obj.optInt("days", 90)
                            repository.insertGoal(
                                GoalEntity(
                                    title = title,
                                    targetAmount = amount,
                                    currentAmount = 0.0,
                                    targetDateMillis = System.currentTimeMillis() + days.toLong() * 24 * 3600 * 1000,
                                    categoryTag = "Goal"
                                )
                            )
                            confirmations.add("🎯 Created goal: $title (₹${fmt(amount)}, $days days)")
                        }
                    }
                    "set_budget" -> {
                        val limit = obj.optDouble("limit", 0.0)
                        if (limit > 0) {
                            val cat = categoryFrom(obj.optString("category"))
                            val cal = Calendar.getInstance()
                            repository.insertBudget(
                                BudgetEntity(
                                    category = cat,
                                    monthlyLimit = limit,
                                    periodMonth = cal.get(Calendar.MONTH) + 1,
                                    periodYear = cal.get(Calendar.YEAR)
                                )
                            )
                            confirmations.add("💰 Budget set: ${cat.displayName} ₹${fmt(limit)}/month")
                        }
                    }
                    "delete_last" -> {
                        val last = transactions.value.maxByOrNull { it.timestamp }
                        if (last != null) {
                            repository.deleteTransaction(last)
                            confirmations.add("🗑️ Deleted: ${last.title}")
                        }
                    }
                    "delete_all" -> {
                        repository.clearTransactions()
                        confirmations.add("🧹 Cleared all transactions")
                    }
                }
                cleaned = cleaned.replace(json, "")
            } catch (_: Exception) {
                // ignore malformed JSON blocks
            }
        }

        cleaned = cleaned.trim()
        if (confirmations.isNotEmpty()) {
            cleaned = if (cleaned.isBlank()) confirmations.joinToString("\n")
            else cleaned + "\n\n" + confirmations.joinToString("\n")
        }
        return cleaned
    }

    private fun categoryFrom(s: String): TransactionCategory = when (s.lowercase().trim()) {
        "housing", "rent" -> TransactionCategory.HOUSING
        "groceries", "grocery", "food" -> TransactionCategory.GROCERIES
        "utilities", "utility", "bills", "bill" -> TransactionCategory.UTILITIES
        "transportation", "transport", "travel_fuel" -> TransactionCategory.TRANSPORTATION
        "healthcare", "health", "medical" -> TransactionCategory.HEALTHCARE
        "dining", "food_delivery", "restaurant" -> TransactionCategory.DINING
        "entertainment" -> TransactionCategory.ENTERTAINMENT
        "shopping" -> TransactionCategory.SHOPPING
        "travel", "vacation" -> TransactionCategory.TRAVEL
        "education", "courses" -> TransactionCategory.EDUCATION
        "investment", "invest" -> TransactionCategory.INVESTMENT
        "insurance" -> TransactionCategory.INSURANCE
        "tax", "taxes" -> TransactionCategory.TAX
        "mutual_fund", "sip" -> TransactionCategory.MUTUAL_FUND
        "gold" -> TransactionCategory.GOLD
        "crypto" -> TransactionCategory.CRYPTO
        "subscriptions", "subscription" -> TransactionCategory.SUBSCRIPTIONS
        "gifts", "donations" -> TransactionCategory.GIFTS_DONATIONS
        else -> TransactionCategory.OTHER
    }

    private fun fmt(v: Double): String = String.format(java.util.Locale.US, "%,.0f", v)

    /** Stores a compact memory for every meaningful conversation, de-duplicated by topic. */
    private suspend fun learnFromChat(userText: String, replyText: String) {
        if (userText.length < 8) return
        val topic = "Chat: ${userText.take(40)}"
        if (brainMemories.value.any { it.topic == topic }) return
        repository.insertMemory(
            BrainMemoryEntity(
                memoryType = MemoryType.RECOMMENDATION_ACTIVE,
                topic = topic,
                description = "User asked: ${userText.take(160)}",
                confidenceScore = 0.7f,
                detectedCount = 1,
                lastObservedAt = System.currentTimeMillis(),
                actionSuggestion = "Learned from conversation — consider acting on this topic."
            )
        )
    }

    private fun handleCustomize(cmd: ParsedFinanceCommand.CustomizeCommand) {
        when (cmd.action) {
            "THEME" -> {
                val id = ThemePalettes.all.firstOrNull {
                    it.name.equals(cmd.payload, ignoreCase = true) || it.id.equals(cmd.payload, ignoreCase = true)
                }?.id
                if (id != null) {
                    prefs.saveThemeId(id)
                    showSnackbar("Theme changed to ${cmd.payload}")
                } else {
                    showSnackbar("Theme not found. Try: purple, emerald, ocean, sunset, rose, midnight, royal, gold, teal, graphite")
                }
            }
            "SORT" -> {
                _uiState.update {
                    it.copy(sortOrder = when (cmd.payload) {
                        "AMOUNT" -> LedgerSort.AMOUNT_DESC
                        "CATEGORY" -> LedgerSort.CATEGORY
                        "DATE_ASC" -> LedgerSort.DATE_ASC
                        else -> LedgerSort.DATE_DESC
                    })
                }
                selectTab(FinanceTab.LEDGER)
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            showSnackbar("Chat history cleared")
        }
    }

    fun refreshBrainMemories() {
        viewModelScope.launch {
            val txList = transactions.value
            val summary = cashFlowSummary.value

            val detected = FinancialAnalyticsEngine.detectHabitsAndAnomalies(txList)
            detected.forEach {
                repository.insertMemory(it)
            }

            val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(txList)
            repository.insertMemory(
                BrainMemoryEntity(
                    memoryType = MemoryType.SAVINGS_VELOCITY,
                    topic = "End-of-Month Run Rate Forecast",
                    description = forecast.forecastSummary,
                    confidenceScore = 0.94f,
                    actionSuggestion = "Burn rate: ₹${String.format(java.util.Locale.US, "%.0f", forecast.dailyBurnRate)}/day; Projected Net: ₹${String.format(java.util.Locale.US, "%,.0f", forecast.projectedMonthEndNetSavings)}."
                )
            )

            val recurring = PersonalFinanceMlEngine.detectRecurringPatterns(txList)
            recurring.filter { it.isSubscription }.forEach { sub ->
                repository.insertMemory(
                    BrainMemoryEntity(
                        memoryType = MemoryType.MERCHANT_PATTERN,
                        topic = "Subscription: ${sub.merchantOrTitle}",
                        description = "Recurring payment of ~₹${String.format(java.util.Locale.US, "%.2f", sub.averageAmount)} every ${sub.intervalDays.toInt()} days (Annual: ~₹${String.format(java.util.Locale.US, "%,.0f", sub.projectedAnnualCost)}).",
                        confidenceScore = 0.95f,
                        actionSuggestion = "Monitor active utility and cancel if unutilized."
                    )
                )
            }
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            repository.clearMemories()
            showSnackbar("Dhan-OM Brain memory reset")
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            showSnackbar("Demo data loaded — clear it anytime from Profile")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.onboardIfNeeded()
            showSnackbar("All data cleared — starting fresh")
        }
    }

    fun saveProfileName(name: String) {
        prefs.saveProfileName(name.trim().ifBlank { "User" })
        showSnackbar("Profile saved")
    }

    fun saveWelcomeVoice(enabled: Boolean) {
        prefs.saveWelcomeVoice(enabled)
    }

    fun saveSmsTracking(enabled: Boolean) {
        prefs.saveSmsTracking(enabled)
        if (enabled) {
            scanSmsInbox()
            showSnackbar("SMS tracking ON — scanning inbox and auto-logging bank/UPI/card messages")
        } else {
            showSnackbar("SMS tracking OFF")
        }
    }

    /** Back-fill ledger from existing bank SMS (last 45 days). */
    fun scanSmsInbox() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val found = com.example.data.sms.SmsInboxScanner.scan(ctx)
            var added = 0
            val existing = transactions.value
            found.forEach { (tx, _) ->
                val dup = existing.any { e ->
                    e.notes.startsWith("Auto-logged") &&
                        kotlin.math.abs(e.amount - tx.amount) < 0.01 &&
                        e.merchant.equals(tx.merchant, ignoreCase = true) &&
                        kotlin.math.abs(e.timestamp - tx.timestamp) < 36 * 3600_000L
                }
                if (!dup) {
                    repository.insertTransaction(tx)
                    added++
                }
            }
            if (added > 0) {
                showSnackbar("Auto-logged $added bank SMS transaction(s)")
            }
        }
    }

    fun savePanNumber(pan: String) {
        prefs.savePanNumber(pan.trim().uppercase())
        showSnackbar("PAN saved")
    }

    fun saveAiSettings(settings: AiSettings) {
        prefs.saveAi(settings)
        showSnackbar("AI Brain settings saved")
    }

    fun saveTheme(id: String) {
        prefs.saveThemeId(id)
        showSnackbar("Theme applied")
    }

    /** Generates a fresh API key for sharing the brain (OpenAI-style auth). */
    fun generateApiKey() {
        val key = "dhanom-" + java.util.UUID.randomUUID().toString().replace("-", "") +
                java.util.UUID.randomUUID().toString().replace("-", "").take(8)
        val cur = aiSettings.value
        prefs.saveAi(cur.copy(serverApiKey = key))
        showSnackbar("New API key generated")
    }

    fun downloadGemmaModel() {
        val url = aiSettings.value.gemmaModelUrl.trim()
        if (url.isBlank()) {
            showSnackbar("Paste a model download URL first")
            return
        }
        if (gemmaBrain.isModelAvailable()) {
            showSnackbar("Gemma model already installed")
            return
        }
        _modelStatus.value = "Downloading…"
        _downloadProgress.value = 0f
        brainDownloader.download(
            url = url,
            dest = gemmaBrain.modelFile(),
            onProgress = { p ->
                _downloadProgress.value = p
                _modelStatus.value = "Downloading ${(p * 100).toInt()}%"
            },
            onDone = { ok, msg ->
                if (ok) {
                    _modelStatus.value = "Ready · ${gemmaBrain.modelSizeLabel()}"
                    _isModelInstalled.value = true
                    _downloadProgress.value = 1f
                    showSnackbar("Gemma model installed")
                } else {
                    _modelStatus.value = "Failed: $msg"
                    _isModelInstalled.value = false
                    _downloadProgress.value = 0f
                    showSnackbar("Download failed: $msg")
                }
            }
        )
    }

    fun deleteGemmaModel() {
        viewModelScope.launch {
            gemmaBrain.close()
            gemmaBrain.modelFile().delete()
            _modelStatus.value = "Not installed"
            _isModelInstalled.value = false
            _downloadProgress.value = 0f
            showSnackbar("Gemma model removed")
        }
    }

    fun toggleBrainServer() {
        val current = brainServer
        if (current != null) {
            current.shutdown()
            brainServer = null
            _serverStatus.value = "Stopped"
            showSnackbar("Brain server stopped")
        } else {
            val port = aiSettings.value.serverPort
            val apiKey = aiSettings.value.serverApiKey.trim()
            val server = LocalBrainServer(
                port = port,
                generate = { prompt ->
                    gemmaBrain.generate(prompt) ?: "Gemma brain is not ready yet — download the model in Profile → AI Brain."
                },
                token = apiKey.ifBlank { null }
            )
            try {
                server.start()
                brainServer = server
                val ip = localIpAddress()
                _serverStatus.value = "Running at http://${ip ?: "localhost"}:$port"
                showSnackbar("Brain server started on port $port")
            } catch (e: Exception) {
                _serverStatus.value = "Failed: ${e.message}"
                showSnackbar("Server start failed: ${e.message}")
            }
        }
    }

    private fun localIpAddress(): String? {
        return try {
            val nis = java.net.NetworkInterface.getNetworkInterfaces() ?: return null
            java.util.Collections.list(nis)
                .flatMap { java.util.Collections.list(it.inetAddresses) }
                .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- File import (attach in chat) ----------------

    /**
     * Accepts any file type up to 500 MB. Copies with a stream (never
     * `readBytes()` of the whole file), detects images by MIME/extension/magic
     * bytes so a single photo cannot OOM the process, and posts a chat bubble
     * with a downsampled preview.
     */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            var name = "file"
            try {
                name = ChatAttachmentHelper.queryDisplayName(ctx, uri).ifBlank { "file" }
                _uploadStatus.value = UploadStatus(name, "Uploading 0%", 0f)
                val copied = withContext(Dispatchers.IO) {
                    ChatAttachmentHelper.copyFromUri(ctx, uri) { p ->
                        val pct = (p * 100).toInt().coerceIn(0, 100)
                        _uploadStatus.value = UploadStatus(name, "Uploading $pct%", p)
                    }
                }
                name = copied.displayName
                _uploadStatus.value = UploadStatus(name, "Processing…", 1f)

                val sizeLabel = ChatAttachmentHelper.formatSize(copied.size)
                val previewPath = (copied.previewFile ?: copied.file).absolutePath
                val actionType = if (copied.isImage) "ATTACHMENT_IMAGE" else "ATTACHMENT_FILE"
                val caption = if (copied.isImage) {
                    "📷 ${copied.displayName} ($sizeLabel)"
                } else {
                    "📎 ${copied.displayName} ($sizeLabel)"
                }

                repository.insertChatMessage(
                    ChatMessageEntity(
                        sender = MessageSender.USER,
                        messageText = caption,
                        timestamp = System.currentTimeMillis(),
                        actionType = actionType,
                        actionPayload = previewPath
                    )
                )

                val n = copied.displayName.lowercase()
                when {
                    copied.isImage -> {
                        // Do NOT send the image into Gemma (that was crashing). Show it
                        // in the bubble; the user can ask "log this receipt" next.
                    }
                    n.endsWith(".json") && copied.size <= ChatAttachmentHelper.MAX_PARSE_BYTES -> {
                        val text = withContext(Dispatchers.IO) {
                            String(ChatAttachmentHelper.readPrefix(copied.file), Charsets.UTF_8)
                        }
                        importBackupFromJson(text.take(2_000_000))
                    }
                    n.endsWith(".csv") && copied.size <= ChatAttachmentHelper.MAX_PARSE_BYTES -> {
                        val text = withContext(Dispatchers.IO) {
                            String(ChatAttachmentHelper.readPrefix(copied.file), Charsets.UTF_8)
                        }
                        importCsv(text.take(2_000_000))
                    }
                    (n.endsWith(".xlsx") || n.endsWith(".xls") || n.endsWith(".docx") ||
                        n.endsWith(".pptx") || n.endsWith(".zip") || n.endsWith(".md") ||
                        n.endsWith(".skill") || n.endsWith(".yaml") || n.endsWith(".yml")) &&
                        copied.size <= ChatAttachmentHelper.MAX_PARSE_BYTES -> {
                        val extracted = withContext(Dispatchers.IO) {
                            com.example.data.export.FileImportHelper.toText(
                                copied.displayName,
                                ChatAttachmentHelper.readPrefix(copied.file)
                            )?.take(50_000).orEmpty()
                        }
                        if (n.endsWith(".xlsx") &&
                            (extracted.lowercase().contains("amount") || extracted.lowercase().contains("title"))
                        ) {
                            importCsv(extracted)
                        } else if (extracted.isNotBlank()) {
                            sendChatMessage(
                                "Here is the content of ${copied.displayName}:\n\n" + extracted.take(4000),
                                recordUser = false
                            )
                        } else {
                            sendChatMessage(
                                "The user attached ${copied.displayName} ($sizeLabel). Acknowledge the file.",
                                recordUser = false
                            )
                        }
                    }
                    else -> {
                        val header = withContext(Dispatchers.IO) {
                            ChatAttachmentHelper.readPrefix(copied.file, 512)
                        }
                        if (ChatAttachmentHelper.isProbablyText(header) && copied.size <= ChatAttachmentHelper.MAX_PARSE_BYTES) {
                            val text = withContext(Dispatchers.IO) {
                                String(ChatAttachmentHelper.readPrefix(copied.file, 50_000), Charsets.UTF_8)
                            }
                            sendChatMessage(
                                "Here is the content of ${copied.displayName}:\n\n" + text.take(4000),
                                recordUser = false
                            )
                        } else {
                            sendChatMessage(
                                "The user attached ${copied.displayName} ($sizeLabel, ${copied.mime}). Acknowledge the file. Do not invent its contents.",
                                recordUser = false
                            )
                        }
                    }
                }

                _uploadStatus.value = UploadStatus(name, "Done ✓")
                showSnackbar("Attached $name ($sizeLabel)")
                kotlinx.coroutines.delay(2500)
                if (_uploadStatus.value?.name == name) _uploadStatus.value = null
            } catch (t: Throwable) {
                _uploadStatus.value = UploadStatus(name, "Failed: ${t.message?.take(60) ?: "error"}")
                showSnackbar("Could not attach file: ${t.message ?: "unknown error"}")
            }
        }
    }

    /** Imports transactions from a Dhan-OM CSV export (or any Title,Amount,Category CSV). */
    fun importCsv(csv: String) {
        viewModelScope.launch {
            val parsed = com.example.data.export.ExportManager.parseTransactionsCsv(csv)
            parsed.forEach { repository.insertTransaction(it) }
            refreshBrainMemories()
            showSnackbar("Imported ${parsed.size} transaction(s) from CSV")
        }
    }

    // ---------------- Self-heal (crash log) ----------------

    fun readCrashLog(): String =
        com.example.DhanomApplication.readCrashLog(getApplication())

    fun clearCrashLog() {
        com.example.DhanomApplication.clearCrashLog(getApplication())
        showSnackbar("Crash log cleared")
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(statusSnackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(statusSnackbarMessage = null) }
    }
}
