package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val brainMemoryDao: BrainMemoryDao,
    private val chatMessageDao: ChatMessageDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    val allMemories: Flow<List<BrainMemoryEntity>> = brainMemoryDao.getAllMemories()
    val chatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun clearTransactions() =
        transactionDao.clearAllTransactions()

    suspend fun insertBudget(budget: BudgetEntity): Long =
        budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    suspend fun insertGoal(goal: GoalEntity): Long =
        goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) =
        goalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: GoalEntity) =
        goalDao.deleteGoal(goal)

    suspend fun insertMemory(memory: BrainMemoryEntity): Long =
        brainMemoryDao.insertMemory(memory)

    suspend fun updateMemory(memory: BrainMemoryEntity) =
        brainMemoryDao.updateMemory(memory)

    suspend fun deleteMemory(memory: BrainMemoryEntity) =
        brainMemoryDao.deleteMemory(memory)

    suspend fun clearMemories() =
        brainMemoryDao.clearAllMemories()

    suspend fun insertChatMessage(message: ChatMessageEntity): Long =
        chatMessageDao.insertMessage(message)

    suspend fun getRecentChatHistory(limit: Int = 15): List<ChatMessageEntity> =
        chatMessageDao.getRecentMessages(limit)

    suspend fun clearChatHistory() =
        chatMessageDao.clearChatHistory()

    suspend fun checkAndSeedInitialData() {
        val count = transactionDao.getTransactionCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 3600 * 1000

            // Sample Incomes & Expenses
            val initialTransactions = listOf(
                TransactionEntity(
                    title = "Monthly Salary Direct Deposit",
                    amount = 4850.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALARY,
                    necessity = ExpenseNecessity.NEED,
                    account = "Main Checking",
                    merchant = "TechCorp Global",
                    timestamp = now - (12 * dayMillis)
                ),
                TransactionEntity(
                    title = "Apartment Rent & Lease",
                    amount = 1450.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.HOUSING,
                    necessity = ExpenseNecessity.NEED,
                    account = "Main Checking",
                    merchant = "Skyline Residences",
                    timestamp = now - (11 * dayMillis),
                    isRecurring = true
                ),
                TransactionEntity(
                    title = "Whole Foods Organic Market",
                    amount = 142.50,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.GROCERIES,
                    necessity = ExpenseNecessity.NEED,
                    account = "Credit Card",
                    merchant = "Whole Foods Market",
                    timestamp = now - (9 * dayMillis)
                ),
                TransactionEntity(
                    title = "Trader Joe's Weekly Groceries",
                    amount = 88.20,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.GROCERIES,
                    necessity = ExpenseNecessity.NEED,
                    account = "Credit Card",
                    merchant = "Trader Joe's",
                    timestamp = now - (3 * dayMillis)
                ),
                TransactionEntity(
                    title = "City Electricity & Water",
                    amount = 115.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.UTILITIES,
                    necessity = ExpenseNecessity.NEED,
                    account = "Main Checking",
                    merchant = "Metro Power & Water",
                    timestamp = now - (8 * dayMillis),
                    isRecurring = true
                ),
                TransactionEntity(
                    title = "Subway & Metro Transit Pass",
                    amount = 75.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.TRANSPORTATION,
                    necessity = ExpenseNecessity.NEED,
                    account = "Credit Card",
                    merchant = "MTA Transit",
                    timestamp = now - (7 * dayMillis)
                ),
                TransactionEntity(
                    title = "Bistro Italia Dining Out",
                    amount = 68.40,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.DINING,
                    necessity = ExpenseNecessity.WANT,
                    account = "Credit Card",
                    merchant = "Bistro Italia",
                    timestamp = now - (6 * dayMillis)
                ),
                TransactionEntity(
                    title = "Blue Bottle Coffee",
                    amount = 6.75,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.DINING,
                    necessity = ExpenseNecessity.WANT,
                    account = "Credit Card",
                    merchant = "Blue Bottle Coffee",
                    timestamp = now - (5 * dayMillis)
                ),
                TransactionEntity(
                    title = "Starbucks Morning Espresso",
                    amount = 5.90,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.DINING,
                    necessity = ExpenseNecessity.WANT,
                    account = "Credit Card",
                    merchant = "Starbucks",
                    timestamp = now - (2 * dayMillis)
                ),
                TransactionEntity(
                    title = "Netflix & Spotify Subscriptions",
                    amount = 32.98,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.ENTERTAINMENT,
                    necessity = ExpenseNecessity.WANT,
                    account = "Credit Card",
                    merchant = "Streaming Services",
                    timestamp = now - (4 * dayMillis),
                    isRecurring = true
                ),
                TransactionEntity(
                    title = "Index Fund ETF Investment",
                    amount = 600.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.INVESTMENT,
                    necessity = ExpenseNecessity.SAVINGS,
                    account = "Investment Account",
                    merchant = "Vanguard Index",
                    timestamp = now - (10 * dayMillis)
                ),
                TransactionEntity(
                    title = "High-Yield Savings Transfer",
                    amount = 400.0,
                    type = TransactionType.TRANSFER,
                    category = TransactionCategory.SAVINGS_TRANSFER,
                    necessity = ExpenseNecessity.SAVINGS,
                    account = "Emergency Savings",
                    merchant = "Marcus HYSA",
                    timestamp = now - (10 * dayMillis)
                ),
                TransactionEntity(
                    title = "Freelance UI Consulting",
                    amount = 750.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.FREELANCE,
                    necessity = ExpenseNecessity.NEED,
                    account = "Main Checking",
                    merchant = "Fintech Client",
                    timestamp = now - (2 * dayMillis)
                )
            )
            transactionDao.insertTransactions(initialTransactions)

            // Initial Budgets
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            val initialBudgets = listOf(
                BudgetEntity(category = TransactionCategory.GROCERIES, monthlyLimit = 400.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.DINING, monthlyLimit = 250.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.HOUSING, monthlyLimit = 1500.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.UTILITIES, monthlyLimit = 180.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.TRANSPORTATION, monthlyLimit = 150.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.ENTERTAINMENT, monthlyLimit = 120.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.SHOPPING, monthlyLimit = 200.0, periodMonth = month, periodYear = year)
            )
            budgetDao.insertBudgets(initialBudgets)

            // Initial Goals
            val initialGoals = listOf(
                GoalEntity(
                    title = "Emergency Fund (6 Months)",
                    targetAmount = 10000.0,
                    currentAmount = 6400.0,
                    targetDateMillis = now + (180L * dayMillis),
                    categoryTag = "Security"
                ),
                GoalEntity(
                    title = "Japan Autumn Vacation",
                    targetAmount = 3200.0,
                    currentAmount = 1850.0,
                    targetDateMillis = now + (90L * dayMillis),
                    categoryTag = "Travel"
                ),
                GoalEntity(
                    title = "New MacBook Pro Workstation",
                    targetAmount = 2400.0,
                    currentAmount = 900.0,
                    targetDateMillis = now + (60L * dayMillis),
                    categoryTag = "Tech"
                )
            )
            initialGoals.forEach { goalDao.insertGoal(it) }

            // Initial Brain Memories (Dhanom learned patterns)
            val initialMemories = listOf(
                BrainMemoryEntity(
                    memoryType = MemoryType.HABIT_LEARNED,
                    topic = "Morning Coffee Routine",
                    description = "Consistently spends $6 - $7 on weekday mornings at Blue Bottle or Starbucks (~$130/month).",
                    confidenceScore = 0.94f,
                    detectedCount = 8,
                    lastObservedAt = now - (2 * dayMillis),
                    actionSuggestion = "Brewing at home 3 days/week could divert $75/mo directly into your Japan Travel Goal."
                ),
                BrainMemoryEntity(
                    memoryType = MemoryType.SAVINGS_VELOCITY,
                    topic = "Strong Savings Rate",
                    description = "Maintaining a 31.8% average savings rate (investments + emergency fund transfer).",
                    confidenceScore = 0.98f,
                    detectedCount = 4,
                    lastObservedAt = now - (1 * dayMillis),
                    actionSuggestion = "Well ahead of the standard 20% benchmark! You are pacing to hit your emergency fund in 3.4 months."
                ),
                BrainMemoryEntity(
                    memoryType = MemoryType.MERCHANT_PATTERN,
                    topic = "Grocery Splitting",
                    description = "Bulk grocery trips at Whole Foods ($140+) balanced by mid-week pantry top-ups at Trader Joe's.",
                    confidenceScore = 0.89f,
                    detectedCount = 5,
                    lastObservedAt = now - (3 * dayMillis),
                    actionSuggestion = "Groceries are currently at 57.6% of your monthly $400 limit, in safe green territory."
                )
            )
            brainMemoryDao.insertMemories(initialMemories)

            // Initial Welcome Chat Message from Dhanom
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    sender = MessageSender.DHANOM_AI,
                    messageText = "Hello! I am Dhanom, your personal finance AI. I keep track of your cash flows, interactive flowcharts, budget alerts, and spending habits locally and securely on your device. You can type commands like 'Spent $45 at Trader Joe's', 'Show cash flow chart', or ask me for financial advice anytime!",
                    timestamp = now
                )
            )
        }
    }
}
