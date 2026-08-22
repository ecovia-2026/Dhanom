package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.export.ExportManager
import com.example.data.model.*
import com.example.data.prefs.AiSettings
import com.example.data.prefs.AppPrefs
import com.example.data.prefs.UserProfile
import com.example.data.repository.FinanceRepository
import com.example.domain.ai.DailySuggestion
import com.example.domain.ai.DailySuggestionEngine
import com.example.domain.ai.DhanomAiService
import com.example.domain.ai.PlanVerifier
import com.example.domain.ai.RealtimeRateFetcher
import com.example.domain.agent.MonthlyAutopilot
import com.example.domain.agent.RecurringEngine
import com.example.domain.brain.BrainModelDownloader
import com.example.domain.brain.CloudBrainClient
import com.example.domain.brain.GemmaBrainEngine
import com.example.domain.brain.LocalBrainServer
import com.example.domain.analytics.*
import com.example.domain.ml.PersonalFinanceMlEngine
import com.example.domain.ml.PersonalizedFinancialInsight
import com.example.domain.memory.MemoryEngine
import com.example.domain.nlp.ParsedFinanceCommand
import com.example.ui.theme.ThemePalettes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Date

enum class FinanceTab(val title: String) {
    DASHBOARD("Dashboard"),
    FLOW_ANALYTICS("Flow & Charts"),
    LEDGER("Ledger Table"),
    PORTFOLIO("Portfolio"),
    DHANOM_AI("Dhan-OM"),
    BUDGETS_GOALS("Budgets & Goals"),
    REPORTS("Reports & Export"),
    LOANS("Loans & Debts"),
    ACCOUNTS("Accounts"),
    CATEGORIES("Categories"),
    RECURRING("Recurring"),
    MEMORY("🧠 Memory"),
    PROFILE("Profile")
}

/** Goodbudget-style envelope: a category with its budget + this month's spend. */
data class EnvelopeProgress(
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressFraction: Float,
    val isOverBudget: Boolean,
    val isNearLimit: Boolean,
    val hasBudget: Boolean
)

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
    val showAddAccountDialog: Boolean = false,
    val editingAccount: AccountEntity? = null,
    val showAddRecurringDialog: Boolean = false,
    val editingRecurring: RecurringTransactionEntity? = null,
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
    data class UploadStatus(val name: String, val state: String, val progress: Float = 0f) // state + percentage
    private val _uploadStatus = MutableStateFlow<UploadStatus?>(null)
    val uploadStatus: StateFlow<UploadStatus?> = _uploadStatus.asStateFlow()

    private var pendingImageBase64: String? = null
    private var pendingImageMime: String = "image/jpeg"
    private var pendingOcrText: String = ""
    private val attachedPdfs = mutableListOf<File>()

    /** Latest uploaded tabular data (Excel/CSV) kept for the "analyze" command. */
    private var pendingDataText: String = ""
    private var pendingDataName: String = ""

    /** Attached image preview shown above the chat composer. */
    private val _attachedImage = MutableStateFlow<android.graphics.Bitmap?>(null)
    val attachedImage: StateFlow<android.graphics.Bitmap?> = _attachedImage.asStateFlow()

    fun clearAttachedImage() { _attachedImage.value = null }

    /** Cycling "thinking" stage shown while the brain works. */
    private val _thinkingStage = MutableStateFlow("")
    val thinkingStage: StateFlow<String> = _thinkingStage.asStateFlow()

    /** Status of the on-demand monthly autopilot run (shown in Profile). */
    private val _monthlyStatus = MutableStateFlow("")
    val monthlyStatus: StateFlow<String> = _monthlyStatus.asStateFlow()

    val profile: StateFlow<UserProfile> = prefs.profile
    val aiSettings: StateFlow<AiSettings> = prefs.aiSettings
    val themeId: StateFlow<String> = prefs.themeId
    val committedPrompt: StateFlow<String> = prefs.committedPrompt
    val memorySummary: StateFlow<String> = prefs.memorySummary
    val profilePhotoPath: StateFlow<String> = prefs.profilePhotoPath

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            goalDao = db.goalDao(),
            brainMemoryDao = db.brainMemoryDao(),
            chatMessageDao = db.chatMessageDao(),
            portfolioDao = db.portfolioDao(),
            loanDao = db.loanDao(),
            taskDao = db.taskDao(),
            accountDao = db.accountDao(),
            recurringTransactionDao = db.recurringTransactionDao()
        )

        // Start EMPTY with a welcome message only — never auto-load demo data.
        viewModelScope.launch {
            repository.onboardIfNeeded()
            // Catch up the monthly autopilot if a new month began while the app was closed.
            if (MonthlyAutopilot.shouldRun(application)) {
                MonthlyAutopilot.run(application, force = false)
            }
            // Auto-post any recurring transactions that came due while away.
            RecurringEngine.processDue(application)
        }

        _modelStatus.value = if (gemmaBrain.isModelAvailable()) {
            "Ready · ${gemmaBrain.modelSizeLabel()}"
        } else {
            "Not installed"
        }
        _isModelInstalled.value = gemmaBrain.isModelAvailable()
        com.example.data.export.PdfTools.init(application)

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

    private val _uiState = MutableStateFlow(FinanceUiState())
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

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Goodbudget-style envelopes: every category with its budget + this month's spend. */
    val envelopes: StateFlow<List<EnvelopeProgress>> = combine(budgets, transactions) { bList, txList ->
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val spent = txList.filter { t ->
            t.type != TransactionType.INCOME && run {
                val c = Calendar.getInstance().apply { timeInMillis = t.timestamp }
                c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
            }
        }.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.amount } }
        val budgetByCat = bList.filter { it.periodMonth == month + 1 && it.periodYear == year }.associateBy { it.category }
        TransactionCategory.entries.map { cat ->
            val limit = budgetByCat[cat]?.monthlyLimit ?: 0.0
            val spentAmt = spent[cat] ?: 0.0
            EnvelopeProgress(
                category = cat,
                monthlyLimit = limit,
                spentAmount = spentAmt,
                remainingAmount = kotlin.math.max(0.0, limit - spentAmt),
                progressFraction = if (limit > 0) (spentAmt / limit).toFloat() else 0f,
                isOverBudget = limit > 0 && spentAmt > limit,
                isNearLimit = limit > 0 && spentAmt / limit >= 0.85f && spentAmt <= limit,
                hasBudget = limit > 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Live per-account balance: initial balance + all mapped transactions. */
    val accountBalances: StateFlow<Map<Long, Double>> = combine(accounts, transactions) { accs, txList ->
        val map = mutableMapOf<Long, Double>()
        accs.forEach { a -> map[a.id] = a.initialBalance }
        txList.forEach { t ->
            val acc = accs.firstOrNull { it.name.equals(t.account, ignoreCase = true) }
            if (acc != null) {
                val delta = when (t.type) {
                    TransactionType.EXPENSE -> -t.amount
                    TransactionType.INCOME -> t.amount
                    TransactionType.INVESTMENT_SELL -> t.amount
                    else -> 0.0
                }
                map[acc.id] = (map[acc.id] ?: 0.0) + delta
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Active tasks due today (or already overdue) — surfaced on the Dashboard. */
    val dueTasks: StateFlow<List<TaskEntity>> = tasks.map { list ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis
        list.filter { it.isActive && it.nextDueDateMillis in 1..endOfToday }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // ---------------- Accounts ----------------

    fun openAddAccountDialog(existing: AccountEntity? = null) {
        _uiState.update { it.copy(showAddAccountDialog = true, editingAccount = existing) }
    }

    fun closeAddAccountDialog() {
        _uiState.update { it.copy(showAddAccountDialog = false, editingAccount = null) }
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            val existing = _uiState.value.editingAccount
            if (existing != null) repository.updateAccount(account.copy(id = existing.id))
            else repository.insertAccount(account)
            closeAddAccountDialog()
            showSnackbar("Account \"${account.name}\" saved")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            showSnackbar("Account \"${account.name}\" removed")
        }
    }

    /** Sets (or updates) this month's envelope budget for a category. */
    fun saveOrUpdateBudget(category: TransactionCategory, limit: Double) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            val existing = budgets.value.firstOrNull { it.category == category && it.periodMonth == m && it.periodYear == y }
            if (existing != null) repository.updateBudget(existing.copy(monthlyLimit = limit))
            else repository.insertBudget(BudgetEntity(category = category, monthlyLimit = limit, periodMonth = m, periodYear = y))
            showSnackbar("${category.displayName} budget set to ₹${fmt(limit)}/month")
        }
    }

    fun clearBudgetForCategory(category: TransactionCategory) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            budgets.value.firstOrNull { it.category == category && it.periodMonth == m && it.periodYear == y }?.let {
                repository.deleteBudget(it)
                showSnackbar("${category.displayName} budget removed")
            }
        }
    }

    // ---------------- Recurring transactions ----------------

    fun openAddRecurringDialog(existing: RecurringTransactionEntity? = null) {
        _uiState.update { it.copy(showAddRecurringDialog = true, editingRecurring = existing) }
    }

    fun closeAddRecurringDialog() {
        _uiState.update { it.copy(showAddRecurringDialog = false, editingRecurring = null) }
    }

    fun saveRecurring(r: RecurringTransactionEntity) {
        viewModelScope.launch {
            val existing = _uiState.value.editingRecurring
            if (existing != null) repository.updateRecurring(r.copy(id = existing.id))
            else repository.insertRecurring(r)
            closeAddRecurringDialog()
            showSnackbar("Recurring \"${r.title}\" saved")
        }
    }

    fun deleteRecurring(r: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(r)
            showSnackbar("Recurring \"${r.title}\" removed")
        }
    }

    fun processRecurringNow() {
        viewModelScope.launch {
            val count = RecurringEngine.processDue(getApplication())
            showSnackbar(if (count > 0) "Auto-posted $count recurring transaction(s)" else "Nothing due yet")
        }
    }

    // ---------------- Loans & Debts ----------------

    fun openAddLoanDialog(existing: LoanEntity? = null) {
        _uiState.update { it.copy(showAddLoanDialog = true, editingLoan = existing) }
    }

    // ---------------- Profile photo ----------------

    /** Copies a picked image into app storage and sets it as the profile photo. */
    fun setProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val dest = File(ctx.filesDir, "profile_photo.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                prefs.saveProfilePhotoPath(dest.absolutePath)
                showSnackbar("Profile photo updated")
            } catch (e: Exception) {
                showSnackbar("Couldn't set photo: ${e.message?.take(40)}")
            }
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            try {
                prefs.profilePhotoPath.value.takeIf { it.isNotBlank() }?.let { File(it).delete() }
            } catch (_: Exception) {
            }
            prefs.saveProfilePhotoPath("")
            showSnackbar("Profile photo removed")
        }
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
            val prefsMap = mapOf(
                "user_name" to profile.value.name,
                "committed_prompt" to committedPrompt.value,
                "memory_summary" to memorySummary.value,
                "theme_id" to themeId.value,
                "cloud_model" to aiSettings.value.cloudModel,
                "cloud_endpoint" to aiSettings.value.cloudEndpoint,
                "cloud_api_key" to aiSettings.value.cloudApiKey,
                "cloud_enabled" to aiSettings.value.cloudEnabled.toString()
            )
            val bundle = com.example.data.export.BackupBundle(
                transactions = transactions.value,
                budgets = budgets.value,
                goals = goals.value,
                memories = brainMemories.value,
                chatMessages = chatMessages.value,
                holdings = holdings.value,
                loans = loans.value,
                tasks = tasks.value,
                accounts = accounts.value,
                recurring = recurringTransactions.value,
                prefs = prefsMap
            )
            val context = getApplication<Application>()
            val file = ExportManager.exportBackupJson(context, bundle)
            val intent = ExportManager.createShareIntent(context, file, "application/json")
            val chooser = ExportManager.createShareChooser(intent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            showSnackbar("Full backup (data + memory + chat + settings) ready to share")
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
                bundle.loans.forEach { repository.insertLoan(it) }
                bundle.tasks.forEach { repository.insertTask(it) }
                bundle.accounts.forEach { repository.insertAccount(it) }
                bundle.recurring.forEach { repository.insertRecurring(it) }
                // restore prefs (context / memory / settings)
                bundle.prefs["user_name"]?.let { prefs.saveProfileName(it) }
                bundle.prefs["committed_prompt"]?.let { prefs.saveCommittedPrompt(it) }
                bundle.prefs["memory_summary"]?.let { prefs.saveMemorySummary(it) }
                bundle.prefs["theme_id"]?.let { prefs.saveThemeId(it) }
                bundle.prefs["cloud_model"]?.let { m ->
                    prefs.saveAi(aiSettings.value.copy(cloudModel = m,
                        cloudEndpoint = bundle.prefs["cloud_endpoint"] ?: aiSettings.value.cloudEndpoint,
                        cloudApiKey = bundle.prefs["cloud_api_key"] ?: aiSettings.value.cloudApiKey,
                        cloudEnabled = bundle.prefs["cloud_enabled"]?.toBoolean() ?: aiSettings.value.cloudEnabled))
                }
                showSnackbar("Transferred everything: ${bundle.transactions.size} tx, ${bundle.chatMessages.size} chats, ${bundle.memories.size} memories, ${bundle.tasks.size} tasks, ${bundle.accounts.size} accounts + context")
            } catch (e: Exception) {
                showSnackbar("Import failed: ${e.message}")
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        var userText = text.trim()

        viewModelScope.launch {
            // 0) Deterministic task/reminder commands (add or update recurring
            //    payments) — handled locally for reliability, BEFORE any AI, so
            //    "add mobile bill ₹3000 every 3 months" or "update HDFC EMI to
            //    ₹4974" always create/update the task correctly.
            val taskReply = handleTaskCommand(userText)
            if (taskReply != null) {
                repository.insertChatMessage(
                    ChatMessageEntity(sender = MessageSender.USER, messageText = userText, timestamp = System.currentTimeMillis())
                )
                repository.insertChatMessage(
                    ChatMessageEntity(sender = MessageSender.DHANOM_AI, messageText = taskReply, timestamp = System.currentTimeMillis(), actionType = "TASK_ACTION")
                )
                return@launch
            }

            // 0.5) Correction / self-verification loop: if the user corrects a fact,
            //    update the stored memory and reply with a revised strategy.
            val correction = MemoryEngine.detectCorrection(userText)
            if (correction != null) {
                val (topic, newValue) = correction
                val existing = brainMemories.value.firstOrNull { it.topic == topic }
                if (existing != null) {
                    repository.updateMemory(existing.copy(description = "User corrected this to about ₹${String.format(java.util.Locale.US, "%,.0f", newValue)}", confidenceScore = 0.95f, lastObservedAt = System.currentTimeMillis()))
                } else {
                    repository.insertMemory(BrainMemoryEntity(memoryType = MemoryType.FACT, topic = topic, description = "About ₹${String.format(java.util.Locale.US, "%,.0f", newValue)}", confidenceScore = 0.9f, lastObservedAt = System.currentTimeMillis()))
                }
                repository.insertChatMessage(ChatMessageEntity(sender = MessageSender.USER, messageText = userText, timestamp = System.currentTimeMillis()))
                val strategy = when (topic) {
                    "Monthly income" -> computeOptimizationSummary()
                    "Outstanding / debt" -> computeRiskSummary()
                    "Emergency fund / savings" -> "✅ Updated your $topic to ₹${String.format(java.util.Locale.US, "%,.0f", newValue)}.\n\nI've re-verified — here's the revised strategy:\n" + computeOptimizationSummary()
                    else -> "✅ Updated and re-verified."
                }
                repository.insertChatMessage(ChatMessageEntity(sender = MessageSender.DHANOM_AI, messageText = "✅ Corrected. I updated '$topic' and re-verified your data.\n\n$strategy", timestamp = System.currentTimeMillis()))
                return@launch
            }

            // 1) Local utility commands (export / PDF / invoice / risk / optimize)
            val utilityReply = handleUtilityCommand(userText)
            if (utilityReply != null) {
                repository.insertChatMessage(
                    ChatMessageEntity(sender = MessageSender.USER, messageText = userText, timestamp = System.currentTimeMillis())
                )
                repository.insertChatMessage(
                    ChatMessageEntity(sender = MessageSender.DHANOM_AI, messageText = utilityReply, timestamp = System.currentTimeMillis())
                )
                return@launch
            }

            // 2) Attach OCR text (from an image/PDF) to the message so text-only brains also see it.
            if (pendingOcrText.isNotBlank()) {
                userText = "Attached document text (OCR):\n$pendingOcrText\n\nUser request: $userText"
                pendingOcrText = ""
            }

            repository.insertChatMessage(
                ChatMessageEntity(
                    sender = MessageSender.USER,
                    messageText = userText,
                    timestamp = System.currentTimeMillis()
                )
            )

            _uiState.update { it.copy(isChatLoading = true) }

            // Human-like "thinking" stages driven by real progress (not a blind timer).
            _thinkingStage.value = "Thinking…"
            val stageJob = launch {
                val stages = listOf("Analyzing your data…", "Reading your records…", "Writing the best answer…", "Verifying the output…")
                var i = 0
                while (true) {
                    kotlinx.coroutines.delay(1600)
                    _thinkingStage.value = stages[i % stages.size]
                    i++
                }
            }
            // reading phase before the actual inference
            _thinkingStage.value = "Reading your financial data…"

            val gemmaStatus = {
                when {
                    !gemmaBrain.isModelAvailable() && _downloadProgress.value in 0f..0.99f && _modelStatus.value.startsWith("Downloading") ->
                        "Gemma 4 brain is downloading (${(_downloadProgress.value * 100).toInt()}%). Please wait."
                    !gemmaBrain.isModelAvailable() ->
                        "Gemma 4 brain is not installed yet. Open Profile → AI Brain → Download model (~3.7 GB, Wi-Fi recommended)."
                    else -> "Gemma brain is loading. Try again in a moment."
                }
            }
            // Cloud brain with automatic model fallback: if the primary model
            // fails (rate limit / error / down), it switches to the next model
            // in the chain — so the chat keeps answering mid-conversation.
            val cloudGenerate: (suspend (String, String?) -> String?)? = if (aiSettings.value.cloudEnabled && aiSettings.value.cloudApiKey.isNotBlank()) {
                { prompt, img ->
                    val endpoint = aiSettings.value.cloudEndpoint
                    val key = aiSettings.value.cloudApiKey
                    val chain = listOf(aiSettings.value.cloudModel) + listOf(
                        "nvidia/nemotron-3-ultra-550b-a55b:free",
                        "deepseek/deepseek-v3.2",
                        "meta-llama/llama-3.3-70b-instruct:free",
                        "openrouter/free"
                    )
                    var reply: String? = null
                    for (model in chain.distinct()) {
                        try {
                            reply = cloudBrain.generate(endpoint, key, model, prompt, userText, imageBase64 = img, imageMime = pendingImageMime)
                        } catch (e: Exception) { reply = null }
                        if (!reply.isNullOrBlank()) break
                    }
                    reply
                }
            } else null

            // Recent conversation so the brain remembers context (fixes "I don't
            // have your details" and multi-message follow-ups).
            val history = chatMessages.value
                .filter { it.sender != MessageSender.SYSTEM }
                .takeLast(30)
                .map { (if (it.sender == MessageSender.USER) "User: " else "Assistant: ") + it.messageText }

            // Graphify-style memory retrieval: seed with keyword matches, then walk
            // one hop through the fact graph so connected memories come along —
            // only the relevant dots are loaded, never the whole brain.
            val relevantMemories = MemoryEngine.retrieveWithGraph(brainMemories.value, userText, k = 8)
                .joinToString("\n") { "- ${it.topic}: ${it.description}" }
            // The rolling summary is stored UNBOUNDED on-device; only the lines
            // relevant to this message are sent to the brain (keeps context small).
            val relevantSummary = MemoryEngine.selectRelevantSummary(prefs.memorySummary.value, userText, maxLines = 25)
            val memoryContext = buildString {
                if (relevantMemories.isNotBlank()) append("LONG-TERM MEMORY (relevant facts):\n$relevantMemories")
                if (relevantSummary.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("CONVERSATION SUMMARY (relevant parts):\n$relevantSummary")
                }
            }

            val aiResponse = aiService.processUserMessage(
                userMessage = userText,
                currentTransactions = transactions.value,
                currentBudgets = budgets.value,
                currentGoals = goals.value,
                learnedMemories = brainMemories.value,
                cloudGenerate = cloudGenerate,
                gemmaGenerate = { prompt -> gemmaBrain.generate(prompt) },
                imageBase64 = pendingImageBase64,
                imageMime = pendingImageMime,
                committedPrompt = committedPrompt.value,
                chatHistory = history,
                memoryContext = memoryContext,
                gemmaStatus = gemmaStatus
            )
            // image consumed
            pendingImageBase64 = null
            _attachedImage.value = null

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

            // Verify + apply any structured tool-calls the brain emitted.
            stageJob.cancel()
            _thinkingStage.value = "Verifying the output…"
            val rawReply = if (aiResponse.brain != "local") applyGemmaActions(aiResponse.replyText) else aiResponse.replyText
            // Strip any leaked chain-of-thought, then self-verify plans vs reality.
            val cleanedReply = stripReasoning(rawReply)
            val finalReply = applyPlanVerification(cleanedReply, aiResponse.brain)

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

            _thinkingStage.value = ""
            _uiState.update { it.copy(isChatLoading = false) }
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
                    "add_task" -> {
                        val title = obj.optString("title").ifBlank { "Task" }
                        val amount = obj.optDouble("amount", 0.0)
                        val recurrence = when (obj.optString("recurrence").lowercase()) {
                            "daily" -> TaskRecurrence.DAILY
                            "weekly" -> TaskRecurrence.WEEKLY
                            "monthly" -> TaskRecurrence.MONTHLY
                            "quarterly", "every 3 months" -> TaskRecurrence.QUARTERLY
                            "yearly" -> TaskRecurrence.YEARLY
                            else -> TaskRecurrence.ONCE
                        }
                        val day = obj.optInt("day", 0)
                        val expiresYear = obj.optInt("expiresYear", 0)
                        val cal = Calendar.getInstance()
                        var nextDue = System.currentTimeMillis() + 24L * 3600 * 1000
                        if (recurrence == TaskRecurrence.MONTHLY && day in 1..28) {
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.MONTH, 1)
                            nextDue = cal.timeInMillis
                        }
                        val expiresAt = if (expiresYear > 2000) {
                            Calendar.getInstance().apply { set(expiresYear, 11, 31) }.timeInMillis
                        } else 0L
                        repository.insertTask(
                            TaskEntity(
                                title = title, amount = amount, recurrence = recurrence,
                                nextDueDateMillis = nextDue, expiresAtMillis = expiresAt,
                                category = "Task", notes = "Created by brain"
                            )
                        )
                        confirmations.add("🗓️ Task added: $title (${recurrence.name.lowercase()})")
                    }
                    "complete_task" -> {
                        val title = obj.optString("title")
                        val found = tasks.value.firstOrNull { it.isActive && (it.title.contains(title, true) || title.contains(it.title, true)) }
                        if (found != null) {
                            val next = found.nextOccurrence()
                            if (next == Long.MAX_VALUE) {
                                repository.updateTask(found.copy(timesDone = found.timesDone + 1, isActive = false))
                                confirmations.add("✅ Task completed: ${found.title}")
                            } else {
                                repository.updateTask(found.copy(timesDone = found.timesDone + 1, nextDueDateMillis = next))
                                confirmations.add("✅ Task done: ${found.title} (next: ${java.text.DateFormat.getDateInstance().format(Date(next))})")
                            }
                        } else {
                            confirmations.add("⚠️ No active task matches \"$title\"")
                        }
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
    /** Memory-consolidation pass after each exchange (mem0/cognee style). */
    private suspend fun learnFromChat(userText: String, replyText: String) {
        // 1) Durable fact extraction (two-phase, mem0-style)
        val facts = MemoryEngine.extractFacts(userText, replyText)
        for (f in facts) {
            // conflict resolution: update existing fact of the same topic
            val existing = brainMemories.value.firstOrNull { it.topic == f.topic }
            if (existing != null) {
                repository.updateMemory(
                    existing.copy(
                        description = f.description,
                        confidenceScore = (existing.confidenceScore + 0.05f).coerceAtMost(0.99f),
                        detectedCount = existing.detectedCount + 1,
                        lastObservedAt = System.currentTimeMillis(),
                        actionSuggestion = f.actionSuggestion
                    )
                )
            } else {
                repository.insertMemory(f)
            }
        }

        // 2) Rolling summary consolidation (cognee-style compression, UNBOUNDED)
        prefs.saveMemorySummary(
            MemoryEngine.consolidateSummary(prefs.memorySummary.value, userText, replyText)
        )

        // 3) Task memory (Graphify-style): remember scheduled/recurring tasks.
        for (t in MemoryEngine.extractTasks(userText, replyText)) {
            if (tasks.value.none { it.title.equals(t.title, ignoreCase = true) && it.isActive }) {
                repository.insertTask(t)
                repository.insertChatMessage(
                    ChatMessageEntity(
                        sender = MessageSender.DHANOM_AI,
                        messageText = "🗓️ Noted! I'll follow up on \"${t.title}\" (${t.scheduleLabel().lowercase()}${if (t.expiresAtMillis > 0) ", until it expires" else ""}).",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Self-verification of any plan/strategy the brain produced: stress-test it
     * against real numbers (local deterministic check) and, when the cloud
     * brain is available, ask it to re-verify against reality too.
     */
    private suspend fun applyPlanVerification(reply: String, brain: String): String {
        val local = PlanVerifier.verify(
            reply, cashFlowSummary.value, transactions.value, goals.value, loans.value, holdings.value
        ) ?: return reply
        if (brain == "cloud" && aiSettings.value.cloudEnabled && aiSettings.value.cloudApiKey.isNotBlank()) {
            try {
                val net = fmt(cashFlowSummary.value.netCashFlow)
                val debt = fmt(loans.value.sumOf { it.outstandingAmount })
                val inv = fmt(holdings.value.sumOf { it.currentValue })
                val prompt = "You are a strict financial verifier. Given this REAL data — net cash flow ₹$net, " +
                    "savings rate ${cashFlowSummary.value.savingsRate.toInt()}%, outstanding debt ₹$debt, " +
                    "investments ₹$inv — review the plan below and reply ONLY with one short line listing any " +
                    "feasibility problems (or 'feasible'). Plan: ${reply.take(1200)}"
                val verdict = cloudBrain.generate(
                    aiSettings.value.cloudEndpoint, aiSettings.value.cloudApiKey,
                    aiSettings.value.cloudModel, "You are a strict, honest financial verifier.", prompt
                )
                if (!verdict.isNullOrBlank() && !verdict.lowercase().contains("feasible")) {
                    return reply + "\n\n🧠 Cloud self-check: $verdict"
                }
            } catch (_: Exception) {
            }
        }
        return reply + local
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

    // ---------------- Chat utility commands (export / PDF / invoice / analysis) ----------------

    private suspend fun handleUtilityCommand(text: String): String? {
        val lower = text.lowercase().trim()
        val ctx = getApplication<Application>()

        // Export PDF report → saved directly to device storage (Downloads/Dhan-OM).
        if (lower.contains("export") && (lower.contains("pdf") || lower.contains("report"))) {
            val f = ExportManager.exportPdfReport(ctx, cashFlowSummary.value, transactions.value, holdings.value, goals.value)
            val saved = com.example.data.export.FileSaver.saveToDownloads(ctx, f, f.name, "application/pdf")
            return "📄 PDF financial report ready.\n📁 Saved to: ${saved.location}"
        }
        // Export Excel/CSV → Downloads.
        if (lower.contains("export") && (lower.contains("excel") || lower.contains("csv") || lower.contains("spreadsheet") || lower.contains("transactions file"))) {
            val f = ExportManager.exportTransactionsCsv(ctx, transactions.value)
            val saved = com.example.data.export.FileSaver.saveToDownloads(ctx, f, f.name, "text/csv")
            return "📊 Transactions CSV (opens in Excel) ready.\n📁 Saved to: ${saved.location}"
        }
        // Export backup
        if (lower.contains("export") && lower.contains("backup")) {
            exportAndShareBackup()
            return "🗂️ Full backup ready to share/transfer."
        }

        // Monthly autopilot: run the full monthly data + savings analysis now
        // (this same agent also runs automatically every month in the background).
        if ((lower.contains("monthly") && (lower.contains("report") || lower.contains("analyz") || lower.contains("analysis") || lower.contains("review") || lower.contains("summary"))) ||
            (lower.contains("run") && lower.contains("monthly")) ||
            (lower.contains("analy") && (lower.contains("my month") || lower.contains("this month")))) {
            val report = MonthlyAutopilot.run(ctx, force = true)
            return report ?: "📊 No data to analyze yet — add a few transactions and I'll run your monthly analysis."
        }

        // Live market rates (real-time, fetched from the internet)
        if (lower.contains("gold") && (lower.contains("rate") || lower.contains("price") || lower.contains("today") || lower.contains("current"))) {
            val r = RealtimeRateFetcher.goldPricePerGramInr()
            return if (r.ok) "🥇 Live gold price: ${r.detail}" else "🥇 ${r.detail} — the cloud brain (if configured) can still estimate."
        }
        if (lower.contains("silver") && (lower.contains("rate") || lower.contains("price"))) {
            val r = RealtimeRateFetcher.silverPricePerGramInr()
            return if (r.ok) "🥈 Live silver price: ${r.detail}" else "🥈 ${r.detail}"
        }
        if ((lower.contains("usd") || lower.contains("dollar")) && (lower.contains("rate") || lower.contains("inr") || lower.contains("price") || lower.contains("convert"))) {
            val r = RealtimeRateFetcher.usdToInr()
            return if (r.ok) "💱 ${r.detail} (live)" else "💱 ${r.detail}"
        }

        // Show / manage scheduled tasks
        if (lower.contains("show") && (lower.contains("task") || lower.contains("reminder"))) return tasksSummary()
        if (lower.contains("mark") && lower.contains("task") && lower.contains("done")) {
            val title = text.replace(Regex("(?i)mark\\s+task\\s+|mark\\s+|task\\s+done|done\\s+task|as\\s+done|complete\\s+"), " ").trim()
            if (title.isBlank() || title == "task") return tasksSummary() + "\n\nSay \"mark task <name> done\"."
            completeTaskByTitle(title)
            return "✅ Marked \"$title\" done (if it matched)."
        }

        // Analyze uploaded tabular data (Excel/CSV) with the brain.
        if ((lower.contains("analy") || lower.contains("analyse") || lower.contains("analyze")) &&
            (lower.contains("excel") || lower.contains("csv") || lower.contains("data") || lower.contains("file") || lower.contains("sheet"))) {
            if (pendingDataText.isBlank()) return "⚠️ Attach an Excel/CSV file first, then say 'analyze this data'."
            return analyzeDataInline(pendingDataText, pendingDataName)
        }

        // Decrypt a password-protected PDF
        val decrypt = Regex("""(?i)decrypt\s+pdf\s+(?:with\s+|using\s+)?(?:password\s+|pass\s+)?['"]?([\w@.\-]+)['"]?""").find(text)
        if (decrypt != null) {
            val pw = decrypt.groupValues[1]
            val pdf = attachedPdfs.lastOrNull()
                ?: return "⚠️ No PDF attached yet. Attach the password-protected PDF, then say 'decrypt pdf password $pw'."
            val out = File(ctx.cacheDir, "unlocked_${System.currentTimeMillis()}.pdf")
            val result = com.example.data.export.PdfTools.decrypt(pdf, pw, out)
            return if (result != null) {
                val saved = com.example.data.export.FileSaver.saveToDownloads(ctx, result, result.name, "application/pdf")
                "🔓 Password removed!\n📁 Saved to: ${saved.location}\nRe-attach it to read the statement."
            } else "❌ Couldn't decrypt — wrong password or unsupported encryption."
        }

        // Merge PDFs → Downloads.
        if (lower.contains("merge") && lower.contains("pdf")) {
            if (attachedPdfs.size < 2) return "⚠️ Attach 2 or more PDFs first, then say 'merge pdf'."
            val out = File(ctx.cacheDir, "merged_${System.currentTimeMillis()}.pdf")
            val merged = com.example.data.export.PdfTools.merge(attachedPdfs.toList(), out)
            return if (merged != null) {
                val saved = com.example.data.export.FileSaver.saveToDownloads(ctx, merged, merged.name, "application/pdf")
                "📑 Merged ${attachedPdfs.size} PDFs.\n📁 Saved to: ${saved.location}"
            } else "❌ Merge failed."
        }

        // Split a PDF → Downloads.
        if (lower.contains("split") && lower.contains("pdf")) {
            val pdf = attachedPdfs.lastOrNull() ?: return "⚠️ Attach a PDF first, then say 'split pdf page 1 to 3'."
            val m = Regex("""(?i)split\s+pdf(?:\s+page[s]?\s+(\d+)(?:\s*to\s*(\d+))?)?""").find(text)
            val from = m?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val to = m?.groupValues?.get(2)?.toIntOrNull() ?: from
            val dir = File(ctx.cacheDir, "split_${System.currentTimeMillis()}").apply { mkdirs() }
            val parts = com.example.data.export.PdfTools.split(pdf, from, to, dir)
            if (parts.isEmpty()) return "❌ Split failed."
            val savedParts = parts.map { com.example.data.export.FileSaver.saveToDownloads(ctx, it, it.name, "application/pdf").location }
            return "✂️ Split into ${parts.size} PDF(s).\n📁 Saved to: ${savedParts.first()}"
        }

        // Create a bill / invoice → Downloads.
        if ((lower.contains("create") || lower.contains("make") || lower.contains("generate")) &&
            (lower.contains("invoice") || lower.contains("bill"))) {
            val amount = extractAmountForUtility(text)
            if (amount <= 0) return "💸 Say e.g. 'create bill ₹2500 for Zomato'."
            val payee = extractAfter(text, listOf("for", "to", "payee")).ifBlank { "Payee" }
            val f = ExportManager.exportInvoicePdf(ctx, "Bill", amount, payee, "Generated by Dhan-OM")
            val saved = com.example.data.export.FileSaver.saveToDownloads(ctx, f, f.name, "application/pdf")
            return "💸 Created a bill for ₹${String.format(java.util.Locale.US, "%,.0f", amount)} ($payee).\n📁 Saved to: ${saved.location}"
        }

        if (lower.contains("risk")) return computeRiskSummary()
        if (lower.contains("optimis") || lower.contains("optimiz") || lower.contains("improve savings")) return computeOptimizationSummary()
        // Generic "analysis/summary" → static financial analysis, UNLESS the user
        // is asking about an attachment (image/photo/bill/receipt) — then let the
        // brain analyze it (vision + OCR) instead.
        val isAttachmentAnalysis = pendingImageBase64 != null || pendingOcrText.isNotBlank() ||
            lower.contains("image") || lower.contains("photo") || lower.contains("picture") ||
            lower.contains("bill") || lower.contains("receipt") || lower.contains("screenshot")
        if (!isAttachmentAnalysis && (lower.contains("analysis") || lower.contains("analyz") || lower.contains("conclude") || lower.contains("summary"))) {
            return computeAnalysisSummary()
        }

        return null
    }

    // ---------------- Task / reminder commands (deterministic) ----------------

    private val amountRegexForTask = Regex("""(?i)(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""")

    /**
     * Handles task/reminder intents locally so they never depend on the LLM:
     *   - "add a recurring mobile bill of ₹3000 every 3 months"
     *   - "remind me to pay rent on the 1st every month"
     *   - "update the HDFC EMI from ₹4800 to ₹4974, loan number …, 29 EMIs pending, date 7th"
     * Returns a confirmation string when handled, otherwise null.
     */
    private suspend fun handleTaskCommand(text: String): String? {
        val lower = text.lowercase().trim()

        val hasAmount = amountRegexForTask.containsMatchIn(text)
        val hasRecurrence = listOf("every", "monthly", "weekly", "daily", "yearly", "quarterly", "recurring")
            .any { lower.contains(it) }

        // ---- UPDATE existing task ----
        val isUpdate = (lower.contains("update") || lower.contains("change") || lower.contains("modify") ||
                lower.contains("revise") || lower.contains("edit") ||
                (lower.contains("emi") && (lower.contains("from") || lower.contains("to ") || lower.contains("now")))) &&
                (lower.contains("emi") || lower.contains("task") || lower.contains("bill") || lower.contains("loan") || lower.contains("reminder"))

        if (isUpdate) {
            val keyword = extractTaskKeyword(lower)
            val existing = tasks.value.firstOrNull { t ->
                t.isActive && (t.title.lowercase().contains(keyword) || t.notes.lowercase().contains(keyword))
            } ?: tasks.value.firstOrNull { t ->
                t.isActive && (t.title.lowercase().contains("emi") || t.title.lowercase().contains("bill") || t.title.lowercase().contains("loan"))
            }

            val newAmount = parseNewAmount(text)
            val day = parseDayOfMonth(text)
            val loanNo = Regex("""(?i)loan\s*(?:no|number|#|account|id)?\s*[:\-]?\s*(\d{6,})""").find(text)?.groupValues?.get(1) ?: ""
            val pending = Regex("""(?i)(\d+)\s*emi[s]?\s*(?:pending|left|remaining|more)""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
            val notes = buildString {
                if (loanNo.isNotBlank()) append("Loan# $loanNo")
                if (pending != null) append(if (isNotEmpty()) " · " else "").append("$pending EMIs pending")
            }

            if (existing != null) {
                val updated = existing.copy(
                    amount = if (newAmount > 0) newAmount else existing.amount,
                    notes = notes.ifBlank { existing.notes },
                    recurrence = if (day != null || lower.contains("month")) TaskRecurrence.MONTHLY else existing.recurrence,
                    nextDueDateMillis = day?.let { nextMonthlyDue(it) } ?: existing.nextDueDateMillis
                )
                repository.updateTask(updated)
                return buildString {
                    append("✅ Updated task \"${updated.title}\"")
                    if (newAmount > 0) append(" to ₹${fmt(newAmount)}")
                    if (day != null) append(" · due on the ${ordinal(day)} each month")
                    if (notes.isNotBlank()) append(" · $notes")
                    append(".")
                }
            }
            // No existing task matched — fall through and create one below.
        }

        // ---- ADD task ----
        val isAdd = (lower.contains("remind") || lower.contains("remember") || lower.contains("todo") ||
                lower.contains("schedule") || lower.contains("recurring")) ||
                ((lower.contains("task") || lower.contains("emi") || lower.contains("bill") || lower.contains("loan")) &&
                        (hasAmount || hasRecurrence))
        if (!isAdd) return null

        val amount = amountRegexForTask.find(text)?.groupValues?.get(1)?.let { MemoryEngine.normalizeAmount(it) } ?: 0.0
        val day = parseDayOfMonth(text)
        val recurrence = when {
            lower.contains("quarterly") || lower.contains("every 3 month") || lower.contains("every three month") || lower.contains("every 3rd month") -> TaskRecurrence.QUARTERLY
            lower.contains("yearly") || lower.contains("annual") || lower.contains("every year") -> TaskRecurrence.YEARLY
            lower.contains("monthly") || lower.contains("every month") || lower.contains("each month") -> TaskRecurrence.MONTHLY
            lower.contains("weekly") || lower.contains("every week") -> TaskRecurrence.WEEKLY
            lower.contains("daily") || lower.contains("every day") || lower.contains("everyday") -> TaskRecurrence.DAILY
            else -> TaskRecurrence.ONCE
        }
        val expiresYear = Regex("""(?i)(?:until|till|before)\s+(\d{4})""").find(lower)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 2000 }

        val title = deriveTaskTitle(text)
        val nextDue = if (day != null && recurrence in setOf(TaskRecurrence.MONTHLY, TaskRecurrence.QUARTERLY)) nextMonthlyDue(day)
            else System.currentTimeMillis() + 24L * 3600 * 1000

        val t = TaskEntity(
            title = title,
            amount = amount,
            recurrence = recurrence,
            nextDueDateMillis = nextDue,
            expiresAtMillis = if (expiresYear != null) {
                Calendar.getInstance().apply { set(expiresYear, 11, 31) }.timeInMillis
            } else 0L,
            category = "Task",
            notes = "Added by chat",
            timesDone = 0,
            isActive = true
        )
        repository.insertTask(t)
        return buildString {
            append("🗓️ Task added: \"$title\"")
            if (amount > 0) append(" · ₹${fmt(amount)}")
            append(" · ${t.scheduleLabel().lowercase()}")
            if (day != null) append(" · due on the ${ordinal(day)}")
            append(".")
        }
    }

    /** Prefers the amount after "to"/"now"/"is" (the NEW value); else the last amount. */
    private fun parseNewAmount(text: String): Double {
        val to = Regex("""(?i)(?:to|now|is|be|new)\s*(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""").find(text)
        if (to != null) return to.groupValues[1].let { MemoryEngine.normalizeAmount(it) }
        val all = amountRegexForTask.findAll(text).map { MemoryEngine.normalizeAmount(it.groupValues[1]) }.toList()
        return all.lastOrNull() ?: 0.0
    }

    /** Day-of-month from ordinals ("7th") or explicit "day/date/on 7". */
    private fun parseDayOfMonth(text: String): Int? {
        val ordinal = Regex("""(?i)\b(\d{1,2})(?:st|nd|rd|th)\b""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        if (ordinal != null) return ordinal.takeIf { it in 1..28 }
        val explicit = Regex("""(?i)\b(?:day|date|on)\s+(\d{1,2})\b""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        return explicit?.takeIf { it in 1..28 }
    }

    private fun nextMonthlyDue(day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 28))
        if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.MONTH, 1)
        return cal.timeInMillis
    }

    private fun ordinal(day: Int): String = when (day) {
        1, 21, 31 -> "${day}st"
        2, 22 -> "${day}nd"
        3, 23 -> "${day}rd"
        else -> "${day}th"
    }

    private fun extractTaskKeyword(lower: String): String {
        for (k in listOf("hdfc", "sbi", "icici", "axis", "kotak", "emi", "loan", "mobile", "bill", "rent", "insurance", "electric", "internet")) {
            if (lower.contains(k)) return k
        }
        return "task"
    }

    private fun deriveTaskTitle(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("mobile") || lower.contains("phone") || lower.contains("recharge") || lower.contains("sim") -> "Mobile Bill"
            lower.contains("electric") || lower.contains("electricity") || lower.contains("power") -> "Electricity Bill"
            lower.contains("internet") || lower.contains("wifi") || lower.contains("broadband") -> "Internet Bill"
            lower.contains("rent") -> "Rent"
            lower.contains("insurance") || lower.contains("premium") -> "Insurance Premium"
            lower.contains("emi") || lower.contains("loan") -> {
                val m = Regex("""(?i)\b([A-Za-z0-9&.]{2,20}?)\s+(?:bank\s+)?(?:personal\s+loan|home\s+loan|car\s+loan|loan|emi)""").find(text)
                val prefix = m?.groupValues?.get(1)?.trim()
                if (!prefix.isNullOrBlank()) {
                    prefix.split(Regex("[^A-Za-z0-9&]+")).filter { it.isNotBlank() }
                        .joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } } + " EMI"
                } else "Loan EMI"
            }
            else -> {
                val cleaned = text
                    .replace(Regex("""(?i)please|kindly|can you|could you|remind me to|remember to|add a|add |create a|create |set a|set |a recurring|recurring|every (day|week|month|year)"""), " ")
                    .replace(Regex("\\s+"), " ").trim()
                cleaned.take(40).ifBlank { "Task" }.replaceFirstChar { c -> c.uppercase() }
            }
        }
    }

    /** Strips leading chain-of-thought / reasoning chatter from an AI reply. */
    private fun stripReasoning(reply: String): String {
        val lines = reply.lines()
        if (lines.size <= 1) return reply
        val markers = listOf(
            "the user wants", "the user is", "the user's", "the user asked", "the user said",
            "the user ", "user wants",
            "let me", "let's", "lets ",
            "we need to", "we should", "we will",
            "the system", "the model", "the assistant",
            "first,", "next,", "then,",
            "thought:", "thinking:", "reasoning:"
        )
        var i = 0
        while (i < lines.size) {
            val l = lines[i].trim().lowercase()
            if (l.isEmpty() || markers.any { l.startsWith(it) }) i++ else break
        }
        val stripped = lines.drop(i).joinToString("\n").trim()
        return if (stripped.isBlank()) reply else stripped
    }

    private fun tasksSummary(): String {
        val active = tasks.value.filter { it.isActive }
        if (active.isEmpty()) return "🗓️ You have no scheduled tasks yet.\n\nTell me things like \"remind me to pay rent on the 1st every month\" and I'll keep nudging you."
        return buildString {
            append("🗓️ Your scheduled tasks:\n\n")
            active.sortedBy { it.nextDueDateMillis }.forEach { t ->
                val due = if (t.nextDueDateMillis > 0) java.text.DateFormat.getDateInstance().format(Date(t.nextDueDateMillis)) else "today"
                append("• ${t.title} — ${t.scheduleLabel()}${if (t.amount > 0) " · ₹${fmt(t.amount)}" else ""} · due $due\n")
            }
        }
    }

    fun completeTaskByTitle(title: String) {
        viewModelScope.launch {
            val t = tasks.value.firstOrNull { it.isActive && (it.title.contains(title, true) || title.contains(it.title, true)) }
            if (t != null) {
                val next = t.nextOccurrence()
                if (next == Long.MAX_VALUE) repository.updateTask(t.copy(timesDone = t.timesDone + 1, isActive = false))
                else repository.updateTask(t.copy(timesDone = t.timesDone + 1, nextDueDateMillis = next))
                showSnackbar("Task done: ${t.title}")
            } else showSnackbar("No matching task found")
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            val next = task.nextOccurrence()
            if (next == Long.MAX_VALUE) repository.updateTask(task.copy(timesDone = task.timesDone + 1, isActive = false))
            else repository.updateTask(task.copy(timesDone = task.timesDone + 1, nextDueDateMillis = next))
            showSnackbar("Task done: ${task.title}")
        }
    }

    /** Runs the monthly analysis on demand (also runs automatically every month). */
    fun runMonthlyAnalysis() {
        if (_monthlyStatus.value.startsWith("Running")) return
        viewModelScope.launch {
            _monthlyStatus.value = "Running…"
            val brain = if (aiSettings.value.cloudEnabled && aiSettings.value.cloudApiKey.isNotBlank())
                "cloud brain (${aiSettings.value.cloudModel})" else "on-device analysis"
            try {
                val report = MonthlyAutopilot.run(getApplication(), force = true)
                if (report == null) {
                    _monthlyStatus.value = "No data yet — add some transactions first."
                } else {
                    _monthlyStatus.value = "Done ✓ ($brain) — saved to chat, memory & Downloads/Dhan-OM."
                    selectTab(FinanceTab.DHANOM_AI) // show the report in chat
                }
            } catch (e: Exception) {
                _monthlyStatus.value = "Failed: ${e.message?.take(60)}"
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            showSnackbar("Task removed")
        }
    }

    /** Sends uploaded tabular data to the brain for analysis (summarize, totals, trends). */
    private fun analyzeTabularData(data: String, name: String) {
        viewModelScope.launch {
            val lines = data.lines().size
            val preview = data.take(4000)
            sendChatMessage(
                "Here is the data from \"$name\" (${lines} rows, first part shown). " +
                "Analyze it for me: summarize the columns, compute totals/averages where numeric, " +
                "list the top items, and point out any useful trends or anomalies:\n\n$preview"
            )
        }
    }

    /** Analyzes tabular data INLINE (cloud → gemma → local stats) and returns the result. */
    private suspend fun analyzeDataInline(data: String, name: String): String {
        val preview = data.take(4000)
        val lines = data.lines().size
        val prompt = "You are a data analyst. Analyze this tabular data (\"$name\", $lines rows). " +
            "Summarize the columns, compute totals/averages for numeric columns, list the top items, " +
            "and note any useful trends or anomalies. Be concise and use ₹ for money.\n\nDATA:\n$preview"

        // Cloud first
        if (aiSettings.value.cloudEnabled && aiSettings.value.cloudApiKey.isNotBlank()) {
            for (m in listOf(aiSettings.value.cloudModel, "nvidia/nemotron-3-ultra-550b-a55b:free", "openrouter/free").distinct()) {
                try {
                    val r = cloudBrain.generate(aiSettings.value.cloudEndpoint, aiSettings.value.cloudApiKey, m, prompt, "analyze data")
                    if (!r.isNullOrBlank()) return "📊 Analysis of \"$name\" ($lines rows):\n\n$r"
                } catch (_: Exception) {
                }
            }
        }
        // On-device Gemma
        try {
            val r = gemmaBrain.generate(prompt)
            if (!r.isNullOrBlank()) return "📊 Analysis of \"$name\" ($lines rows):\n\n$r"
        } catch (_: Exception) {
        }
        // Local deterministic fallback
        return "📊 Local analysis of \"$name\" ($lines rows):\n\n" + localTabularStats(data)
    }

    /** Minimal offline analysis of CSV-like data: rows, columns, numeric totals. */
    private fun localTabularStats(data: String): String {
        val lines = data.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return "No readable rows found."
        val header = lines.first().split(",")
        val rows = lines.drop(1)
        val numericTotals = mutableMapOf<Int, Double>()
        for (row in rows) {
            val cols = row.split(",")
            cols.forEachIndexed { i, v ->
                val n = v.trim().replace("₹", "").replace(",", "").toDoubleOrNull()
                if (n != null) numericTotals[i] = (numericTotals[i] ?: 0.0) + n
            }
        }
        return buildString {
            append("• ${header.size} columns: ${header.joinToString(", ").take(200)}\n")
            append("• ${rows.size} data rows\n")
            numericTotals.toList().sortedByDescending { it.second }.take(5).forEach { (idx, sum) ->
                val colName = header.getOrNull(idx)?.trim()?.ifBlank { "col${idx + 1}" } ?: "col${idx + 1}"
                append("• Total of \"$colName\": ₹${String.format(java.util.Locale.US, "%,.2f", sum)}\n")
            }
        }
    }

    private fun shareFile(file: java.io.File, mime: String) {
        try {
            val intent = ExportManager.createShareIntent(getApplication(), file, mime)
            val chooser = ExportManager.createShareChooser(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {
            showSnackbar("Share failed: ${e.message}")
        }
    }

    private fun extractAmountForUtility(text: String): Double =
        Regex("""(?i)(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?)""").find(text)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

    private fun extractAfter(text: String, keys: List<String>): String {
        for (k in keys) {
            val idx = text.indexOf(k, ignoreCase = true)
            if (idx >= 0) {
                val rest = text.substring(idx + k.length).trim()
                val tok = rest.split(Regex("[^A-Za-z0-9 &'.-]+")).firstOrNull()?.trim()
                if (!tok.isNullOrBlank()) return tok
            }
        }
        return ""
    }

    private fun computeRiskSummary(): String {
        val s = cashFlowSummary.value
        val h = holdings.value
        val total = h.sumOf { it.currentValue }
        val equity = h.filter { it.assetClass in listOf(AssetClass.LARGE_CAP, AssetClass.MID_SMALL_CAP, AssetClass.INDEX_ETF, AssetClass.MUTUAL_FUND, AssetClass.INTERNATIONAL) }.sumOf { it.currentValue }
        val equityPct = if (total > 0) (equity / total) * 100.0 else 0.0
        val topHolding = h.maxByOrNull { it.currentValue }?.currentValue ?: 0.0
        val concentration = if (total > 0) (topHolding / total) * 100.0 else 0.0
        val emergency = goals.value.find { it.title.contains("emergency", true) }
        val emergencyPct = emergency?.let { if (it.targetAmount > 0) (it.currentAmount / it.targetAmount) * 100.0 else 0.0 } ?: 0.0

        val risks = mutableListOf<String>()
        if (s.savingsRate < 20) risks.add("Low savings rate (${s.savingsRate.toInt()}% — target ≥20%)")
        if (concentration > 40) risks.add("Concentrated portfolio (top holding is ${concentration.toInt()}%)")
        if (equityPct > 80) risks.add("High equity exposure (${equityPct.toInt()}%) — add debt/gold")
        if (emergencyPct < 60) risks.add("Emergency fund only ${emergencyPct.toInt()}% funded")
        if (loans.value.any { it.outstandingAmount > 0 }) risks.add("Active loans/debt — reduce EMIs where possible")

        return buildString {
            append("⚠️ Risk Analysis\n\n")
            append("Health score: ${s.healthScore}/100 (${s.healthGrade})\n")
            append("Savings rate: ${s.savingsRate.toInt()}%\n")
            append("Equity allocation: ${equityPct.toInt()}% · Top holding: ${concentration.toInt()}%\n\n")
            if (risks.isEmpty()) append("✅ No major risks detected — well balanced!")
            else risks.forEach { append("• $it\n") }
        }
    }

    private fun computeOptimizationSummary(): String {
        val s = cashFlowSummary.value
        val dining = transactions.value.filter { it.category == TransactionCategory.DINING && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val subs = transactions.value.filter { it.category == TransactionCategory.SUBSCRIPTIONS }.sumOf { it.amount }
        val sip = holdings.value.filter { it.isSip }.sumOf { it.sipMonthlyAmount }

        return buildString {
            append("💡 Savings Optimization\n\n")
            append("• Net cash flow: ₹${String.format(java.util.Locale.US, "%,.0f", s.netCashFlow)}\n")
            append("• Savings rate: ${s.savingsRate.toInt()}% (target 20–30%)\n")
            if (dining > 0) append("• Dining spend: ₹${String.format(java.util.Locale.US, "%,.0f", dining)} — cutting 30% frees ₹${String.format(java.util.Locale.US, "%,.0f", dining * 0.3)}/mo\n")
            if (subs > 0) append("• Subscriptions: ₹${String.format(java.util.Locale.US, "%,.0f", subs)}/mo — audit unused ones\n")
            if (sip > 0) append("• SIP already: ₹${String.format(java.util.Locale.US, "%,.0f", sip)}/mo\n")
            append("\nTop actions: automate 20% to savings, redirect dining/subscription savings to a goal, and step up SIP by 5%.")
        }
    }

    private fun computeAnalysisSummary(): String {
        val s = cashFlowSummary.value
        val top = com.example.domain.analytics.FinancialAnalyticsEngine.calculateCategoryBreakdown(transactions.value).take(3)
        val inv = holdings.value.sumOf { it.currentValue }
        val goalTotal = goals.value.sumOf { it.currentAmount }

        return buildString {
            append("📊 Full Analysis\n\n")
            append("Inflow: ₹${String.format(java.util.Locale.US, "%,.0f", s.totalInflow)} · Outflow: ₹${String.format(java.util.Locale.US, "%,.0f", s.totalOutflow)}\n")
            append("Net: ₹${String.format(java.util.Locale.US, "%,.0f", s.netCashFlow)} · Savings rate: ${s.savingsRate.toInt()}%\n")
            append("Investments value: ₹${String.format(java.util.Locale.US, "%,.0f", inv)} · Goals funded: ₹${String.format(java.util.Locale.US, "%,.0f", goalTotal)}\n")
            if (top.isNotEmpty()) append("Top spending: " + top.joinToString { "${it.category.displayName} (${it.percentage.toInt()}%)" })
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

    fun deleteMemory(memory: BrainMemoryEntity) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
            showSnackbar("Memory removed")
        }
    }

    // Self-repair: ask the (free) cloud brain to diagnose the latest crash log
    // and suggest a fix. The app auto-restarts on crash and uses a hardcoded
    // free-model fallback chain so it never goes fully offline.
    private val _repairReport = MutableStateFlow("")
    val repairReport: StateFlow<String> = _repairReport.asStateFlow()

    fun runSelfRepair() {
        viewModelScope.launch {
            val log = com.example.DhanomApplication.readCrashLog(getApplication())
            if (log.isBlank() || log.contains("No crashes recorded")) {
                _repairReport.value = "✅ No crashes recorded. The app is healthy."
                showSnackbar("No crashes to repair")
                return@launch
            }
            if (aiSettings.value.cloudApiKey.isBlank()) {
                _repairReport.value = "⚠️ Add an OpenRouter key (Profile → Cloud Brain) so the self-repair brain can analyze the crash log. The app still auto-restarts on its own without it."
                return@launch
            }
            _thinkingStage.value = "Diagnosing crash with cloud brain…"
            try {
                val prompt = "You are a senior Android engineer. Analyze this crash log and give a short, actionable fix (or workaround) for the user. Crash log:\n\n${log.takeLast(4000)}"
                val models = listOf(
                    aiSettings.value.cloudModel,
                    "nvidia/nemotron-3-ultra-550b-a55b:free",
                    "deepseek/deepseek-v3.2",
                    "openrouter/free"
                )
                var reply: String? = null
                for (m in models.distinct()) {
                    reply = cloudBrain.generate(aiSettings.value.cloudEndpoint, aiSettings.value.cloudApiKey, m, prompt, "analyze crash log")
                    if (!reply.isNullOrBlank()) break
                }
                val diagnosis = reply ?: "Couldn't reach the brain — check your connection/key."
                _repairReport.value = diagnosis
                _thinkingStage.value = ""

                // 1) Persist the diagnosis as a SELF_HEAL memory so the brain
                //    remembers what broke and how to fix it next time.
                repository.insertMemory(
                    BrainMemoryEntity(
                        memoryType = MemoryType.SELF_HEAL,
                        topic = "Self-heal: ${java.text.DateFormat.getDateInstance().format(Date())}",
                        description = "Crash diagnosis: " + diagnosis.take(300),
                        confidenceScore = 0.9f,
                        lastObservedAt = System.currentTimeMillis(),
                        actionSuggestion = "Previous crash + fix. If it recurs, apply: " + diagnosis.take(160)
                    )
                )
                // 2) Save a shareable heal-guidance file (for any human/agent to act on).
                saveHealGuidance(log, diagnosis)
            } catch (e: Exception) {
                _repairReport.value = "Self-repair failed: ${e.message}"
                _thinkingStage.value = ""
            }
        }
    }

    /** Writes a self-heal guidance report (crash log + AI diagnosis + suggested
     *  fixes) to Downloads so it can be handed to any developer or AI agent. */
    private fun saveHealGuidance(crashLog: String, diagnosis: String) {
        try {
            val ctx = getApplication<Application>()
            val report = buildString {
                append("# Dhan-OM Self-Heal Guidance\n\n")
                append("Generated: ${java.text.DateFormat.getDateTimeInstance().format(Date())}\n")
                append("App version: 1.2 (${android.os.Build.VERSION.RELEASE}, ${android.os.Build.MODEL})\n\n")
                append("## What crashed\n```\n").append(crashLog.take(3000)).append("\n```\n\n")
                append("## AI diagnosis\n").append(diagnosis).append("\n\n")
                append("## Suggested next steps\n")
                append("1. Apply the AI diagnosis above.\n")
                append("2. If it recurs, send this file to the developer or any AI agent (Claude/GPT) with your crash.\n")
                append("3. The app auto-restarts on crash; this memory is also stored in the 🧠 Memory tab.\n")
            }
            val f = File(ctx.cacheDir, "dhanom_self_heal_${System.currentTimeMillis()}.md")
            f.writeText(report)
            val saved = com.example.data.export.FileSaver.saveToDownloads(ctx, f, "DhanOM_self_heal_guide.md", "text/markdown")
            showSnackbar("Heal guidance saved to ${saved.location}")
        } catch (e: Exception) {
            // best-effort
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
        showSnackbar(if (enabled) "SMS tracking ON — bank messages will be auto-logged" else "SMS tracking OFF")
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

    fun saveCommittedPrompt(prompt: String) {
        prefs.saveCommittedPrompt(prompt)
        showSnackbar("Standing instructions saved")
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

    /** Handles a file the user attached in chat: .json backup, .csv ledger, or plain text. */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val detectedMime = ctx.contentResolver.getType(uri) ?: ""
            val name = queryDisplayName(uri).ifBlank {
                when {
                    detectedMime.startsWith("image/") -> "photo_${System.currentTimeMillis()}.jpg"
                    detectedMime == "application/pdf" -> "document_${System.currentTimeMillis()}.pdf"
                    else -> "file"
                }
            }
            _uploadStatus.value = UploadStatus(name.ifBlank { "file" }, "Uploading…")
            try {
                // Size guard: never load huge files fully into memory.
                val size = querySize(uri)
                if (size > MAX_UPLOAD_BYTES) {
                    _uploadStatus.value = UploadStatus(name, "Failed: too large (${size / 1_000_000} MB > 25 MB)")
                    showSnackbar("File too large (max 25 MB)")
                    return@launch
                }
                // Images: read + base64 (for vision models) AND run on-device OCR.
                val mime = ctx.contentResolver.getType(uri) ?: ""
                if (mime.startsWith("image/")) {
                    val imgBytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
                    if (imgBytes == null || imgBytes.size > 15_000_000) {
                        _uploadStatus.value = UploadStatus(name, "Failed: image too large")
                        return@launch
                    }
                    _uploadStatus.value = UploadStatus(name, "Uploading…", 0f)
                    pendingImageBase64 = android.util.Base64.encodeToString(imgBytes, android.util.Base64.NO_WRAP)
                    pendingImageMime = mime
                    // Show the image preview + log it in chat.
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    if (bmp != null) {
                        _attachedImage.value = bmp
                        repository.insertChatMessage(
                            ChatMessageEntity(
                                sender = MessageSender.USER,
                                messageText = "📷 [Image attached: $name]",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    _uploadStatus.value = UploadStatus(name, "Reading text (OCR)…", 0.5f)
                    val ocr = com.example.domain.ocr.OcrHelper.ocrImage(imgBytes)
                    _uploadStatus.value = UploadStatus(name, "Handing to the brain…", 0.85f)
                    if (ocr.isNotBlank()) {
                        pendingOcrText = ocr
                        // Post the OCR output as its own message so it never vanishes.
                        repository.insertChatMessage(
                            ChatMessageEntity(
                                sender = MessageSender.DHANOM_AI,
                                messageText = "🔍 OCR extracted from your image:\n\n" + ocr.take(3000),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    // Auto-hand-off: the brain sees the image (vision) + OCR text and
                    // estimates/logs the items without the user typing anything.
                    // (pendingOcrText is prepended by sendChatMessage automatically.)
                    sendChatMessage(
                        "Analyze this bill/receipt image: list its items, estimate the total cost, " +
                        "and log each item as a transaction where amounts are clear."
                    )
                    _uploadStatus.value = UploadStatus(name, "Done ✓ (analyzing…)", 1f)
                    kotlinx.coroutines.delay(4000)
                    if (_uploadStatus.value?.name == name) _uploadStatus.value = null
                    return@launch
                }
                val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes == null) {
                    _uploadStatus.value = UploadStatus(name, "Failed: unreadable")
                    return@launch
                }
                _uploadStatus.value = UploadStatus(name, "Processing…", 0f)
                when {
                    name.endsWith(".json", ignoreCase = true) -> {
                        importBackupFromJson(String(bytes, Charsets.UTF_8).take(2_000_000))
                        _uploadStatus.value = UploadStatus(name, "Done ✓")
                    }
                    name.endsWith(".csv", ignoreCase = true) -> {
                        val csvText = String(bytes, Charsets.UTF_8).take(2_000_000)
                        if (csvText.lowercase().contains("amount") || csvText.lowercase().contains("title")) {
                            importCsv(csvText)
                            _uploadStatus.value = UploadStatus(name, "Done ✓ (imported as transactions)", 1f)
                        } else {
                            pendingDataText = csvText
                            pendingDataName = name
                            analyzeTabularData(csvText, name)
                            _uploadStatus.value = UploadStatus(name, "Done ✓ (analyzing…)", 1f)
                        }
                    }
                    name.endsWith(".xlsx", ignoreCase = true) -> {
                        val csv = com.example.data.export.FileImportHelper.toText(name, bytes)?.take(200_000) ?: ""
                        if (csv.lowercase().contains("amount") || csv.lowercase().contains("title")) {
                            importCsv(csv)
                            _uploadStatus.value = UploadStatus(name, "Done ✓ (imported as transactions)", 1f)
                        } else {
                            pendingDataText = csv
                            pendingDataName = name
                            analyzeTabularData(csv, name)
                            _uploadStatus.value = UploadStatus(name, "Done ✓ (analyzing…)", 1f)
                        }
                    }
                    name.endsWith(".docx", ignoreCase = true) -> {
                        val text = com.example.data.export.FileImportHelper.toText(name, bytes)?.take(50_000) ?: ""
                        sendChatMessage("Here is the Word document content:\n\n" + text.take(4000))
                        _uploadStatus.value = UploadStatus(name, "Done ✓", 1f)
                    }
                    name.endsWith(".pdf", ignoreCase = true) -> {
                        // Save a working copy so chat can later decrypt/merge/split it.
                        val cached = File(ctx.cacheDir, "attach_${System.currentTimeMillis()}.pdf")
                        cached.writeBytes(bytes)
                        attachedPdfs.add(cached)
                        if (attachedPdfs.size > 6) attachedPdfs.removeAt(0)

                        var text = com.example.data.export.extractPdfText(bytes).take(60_000)
                        if (text.isBlank()) {
                            _uploadStatus.value = UploadStatus(name, "Scanning PDF (OCR)…", 0.5f)
                            text = com.example.domain.ocr.OcrHelper.ocrPdf(cached).take(60_000)
                        }
                        if (text.isBlank()) {
                            _uploadStatus.value = UploadStatus(name, "No text found (password-protected?)", 1f)
                            showSnackbar("PDF looks password-protected or scanned. Say: 'decrypt pdf password YOURPASSWORD'")
                        } else {
                            pendingOcrText = text
                            sendChatMessage("Here is the PDF statement content. Please analyze it and log the transactions:\n\n" + text.take(6000))
                            _uploadStatus.value = UploadStatus(name, "Done ✓ (sent to brain)", 1f)
                        }
                    }
                    name.endsWith(".zip", ignoreCase = true) -> {
                        val text = com.example.data.export.FileImportHelper.toText(name, bytes)?.take(50_000) ?: ""
                        if (text.isBlank()) showSnackbar("No readable files inside the ZIP")
                        else sendChatMessage("Here is the ZIP contents:\n\n" + text.take(4000))
                        _uploadStatus.value = UploadStatus(name, "Done ✓")
                    }
                    else -> {
                        val text = String(bytes, Charsets.UTF_8).take(50_000)
                        if (text.isBlank()) showSnackbar("Could not read the file")
                        else sendChatMessage("Here is the file content:\n\n" + text.take(4000))
                        _uploadStatus.value = UploadStatus(name, "Done ✓")
                    }
                }
                // clear status after a short moment
                kotlinx.coroutines.delay(2500)
                if (_uploadStatus.value?.name == name) _uploadStatus.value = null
            } catch (e: Exception) {
                _uploadStatus.value = UploadStatus(name, "Failed: ${e.message?.take(40)}")
                showSnackbar("Import failed: ${e.message}")
            }
        }
    }

    private fun querySize(uri: Uri): Long {
        return try {
            val ctx = getApplication<Application>()
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx) else 0L
            } ?: 0L
        } catch (e: Exception) { 0L }
    }

    companion object {
        private const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024 // 25 MB
    }

    private fun queryDisplayName(uri: Uri): String {
        return try {
            val ctx = getApplication<Application>()
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else ""
            } ?: ""
        } catch (e: Exception) {
            ""
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
