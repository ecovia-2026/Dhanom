package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.domain.ai.DhanomAiService
import com.example.domain.analytics.*
import com.example.domain.ml.PersonalFinanceMlEngine
import com.example.domain.ml.PersonalizedFinancialInsight
import com.example.domain.nlp.ParsedFinanceCommand
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FinanceTab(val title: String) {
    DASHBOARD("Dashboard"),
    FLOW_ANALYTICS("Flow & Charts"),
    LEDGER("Ledger Table"),
    DHANOM_AI("Dhanom AI"),
    BUDGETS_GOALS("Budgets & Goals")
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
    val statusSnackbarMessage: String? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val aiService = DhanomAiService()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            goalDao = db.goalDao(),
            brainMemoryDao = db.brainMemoryDao(),
            chatMessageDao = db.chatMessageDao()
        )

        // Seed initial rich data on first launch
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
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

    // Derived Financial Analytics
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

    // On-Device Machine Learning Insights & Predictions
    val personalizedInsights: StateFlow<List<PersonalizedFinancialInsight>> = combine(
        transactions,
        budgets,
        goals,
        cashFlowSummary
    ) { txList, bList, gList, summary ->
        PersonalFinanceMlEngine.generatePersonalizedInsights(txList, bList, gList, summary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Ledger Transactions
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
            // Also log savings transfer transaction
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
            showSnackbar("Deposited $${String.format(java.util.Locale.US, "%.2f", depositAmount)} towards ${goal.title}!")
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            showSnackbar("Goal removed")
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userText = text.trim()

        viewModelScope.launch {
            // Save user message
            repository.insertChatMessage(
                ChatMessageEntity(
                    sender = MessageSender.USER,
                    messageText = userText,
                    timestamp = System.currentTimeMillis()
                )
            )

            _uiState.update { it.copy(isChatLoading = true) }

            // Process via Dhanom AI Service
            val aiResponse = aiService.processUserMessage(
                userMessage = userText,
                currentTransactions = transactions.value,
                currentBudgets = budgets.value,
                currentGoals = goals.value,
                learnedMemories = brainMemories.value,
                enableInternetKnowledge = _uiState.value.enableInternetKnowledge
            )

            // If command produced side effects, execute them
            when (val cmd = aiResponse.parsedCommand) {
                is ParsedFinanceCommand.AddTransactionCommand -> {
                    repository.insertTransaction(cmd.transaction)
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
                else -> {}
            }

            // Save AI reply message
            repository.insertChatMessage(
                ChatMessageEntity(
                    sender = MessageSender.DHANOM_AI,
                    messageText = aiResponse.replyText,
                    timestamp = System.currentTimeMillis(),
                    actionType = if (aiResponse.internetInsightsUsed) "INTERNET_INSIGHT" else null
                )
            )

            _uiState.update { it.copy(isChatLoading = false) }
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

            // Combine rule-based detections + ML patterns
            val detected = FinancialAnalyticsEngine.detectHabitsAndAnomalies(txList)
            detected.forEach {
                repository.insertMemory(it)
            }

            // ML Forecast Memory
            val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(txList)
            repository.insertMemory(
                BrainMemoryEntity(
                    memoryType = MemoryType.SAVINGS_VELOCITY,
                    topic = "End-of-Month Run Rate Forecast",
                    description = forecast.forecastSummary,
                    confidenceScore = 0.94f,
                    actionSuggestion = "Burn rate: $${String.format(java.util.Locale.US, "%.0f", forecast.dailyBurnRate)}/day; Projected Net: $${String.format(java.util.Locale.US, "%,.0f", forecast.projectedMonthEndNetSavings)}."
                )
            )

            // ML Subscriptions
            val recurring = PersonalFinanceMlEngine.detectRecurringPatterns(txList)
            recurring.filter { it.isSubscription }.forEach { sub ->
                repository.insertMemory(
                    BrainMemoryEntity(
                        memoryType = MemoryType.MERCHANT_PATTERN,
                        topic = "Subscription: ${sub.merchantOrTitle}",
                        description = "Recurring payment of ~$${String.format(java.util.Locale.US, "%.2f", sub.averageAmount)} every ${sub.intervalDays.toInt()} days (Annual: ~$${String.format(java.util.Locale.US, "%,.0f", sub.projectedAnnualCost)}).",
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
            showSnackbar("Dhanom Brain memory reset")
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.clearTransactions()
            repository.checkAndSeedInitialData()
            showSnackbar("Reset to rich sample portfolio")
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(statusSnackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(statusSnackbarMessage = null) }
    }
}
