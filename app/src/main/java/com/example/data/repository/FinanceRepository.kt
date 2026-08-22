package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val brainMemoryDao: BrainMemoryDao,
    private val chatMessageDao: ChatMessageDao,
    private val portfolioDao: PortfolioDao,
    private val loanDao: LoanDao,
    private val taskDao: TaskDao,
    private val accountDao: AccountDao,
    private val recurringTransactionDao: RecurringTransactionDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    val allMemories: Flow<List<BrainMemoryEntity>> = brainMemoryDao.getAllMemories()
    val chatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    val allHoldings: Flow<List<PortfolioHoldingEntity>> = portfolioDao.getAllHoldings()
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allRecurring: Flow<List<RecurringTransactionEntity>> = recurringTransactionDao.getAllRecurring()

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

    suspend fun insertHolding(holding: PortfolioHoldingEntity): Long =
        portfolioDao.insertHolding(holding)

    suspend fun updateHolding(holding: PortfolioHoldingEntity) =
        portfolioDao.updateHolding(holding)

    suspend fun deleteHolding(holding: PortfolioHoldingEntity) =
        portfolioDao.deleteHolding(holding)

    suspend fun clearHoldings() =
        portfolioDao.clearAllHoldings()

    suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)
    suspend fun clearLoans() = loanDao.clearAllLoans()

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun insertTasks(tasks: List<TaskEntity>) = taskDao.insertTasks(tasks)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun clearTasks() = taskDao.clearAllTasks()

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
    suspend fun clearAccounts() = accountDao.clearAllAccounts()

    suspend fun insertRecurring(r: RecurringTransactionEntity): Long = recurringTransactionDao.insertRecurring(r)
    suspend fun updateRecurring(r: RecurringTransactionEntity) = recurringTransactionDao.updateRecurring(r)
    suspend fun deleteRecurring(r: RecurringTransactionEntity) = recurringTransactionDao.deleteRecurring(r)
    suspend fun clearRecurring() = recurringTransactionDao.clearAllRecurring()

    /** Seed sensible default accounts on first run so transactions map cleanly. */
    suspend fun seedDefaultAccounts() {
        val existing = accountDao.getAllAccounts().first()
        if (existing.isEmpty()) {
            accountDao.insertAccounts(
                listOf(
                    AccountEntity(name = "Cash", type = AccountType.CASH, initialBalance = 0.0, colorArgb = 0xFF0E9F6E, icon = "cash"),
                    AccountEntity(name = "Bank Account", type = AccountType.BANK, initialBalance = 0.0, colorArgb = 0xFF0B6BCB, icon = "bank"),
                    AccountEntity(name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = 0.0, colorArgb = 0xFFD6336C, icon = "card")
                )
            )
        }
    }

    /** First-run onboarding: welcome message + default accounts (NO fake transactions). */
    suspend fun onboardIfNeeded() {
        seedDefaultAccounts()
        if (chatMessageDao.getRecentMessages(1).isEmpty()) {
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    sender = MessageSender.DHANOM_AI,
                    messageText = "🙏 Welcome to Dhan-OM, your personal AI.\n\nI run an on-device Gemma 4 E2B (fast) brain — no cloud needed. Try:\n\n• \"Spent ₹450 on Swiggy\"\n• \"Add income 50000 salary\"\n• \"Delete my last transaction\"\n• \"Set budget 8000 groceries\"\n• \"Show my spending on dining\"\n\nTip: if my brain isn't downloaded yet, accept the download prompt (or go to Profile → AI Brain) and every answer becomes real AI reasoning.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /** Wipe all local finance data (explicit user action only). */
    suspend fun clearAllData() {
        transactionDao.clearAllTransactions()
        budgetDao.clearAllBudgets()
        goalDao.clearAllGoals()
        brainMemoryDao.clearAllMemories()
        chatMessageDao.clearChatHistory()
        portfolioDao.clearAllHoldings()
        loanDao.clearAllLoans()
        taskDao.clearAllTasks()
        accountDao.clearAllAccounts()
        recurringTransactionDao.clearAllRecurring()
    }

    /** Explicitly load rich demo/sample data (only when the user asks for it). */
    suspend fun seedSampleData() {
        val count = transactionDao.getTransactionCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 3600 * 1000

            val initialTransactions = listOf(
                TransactionEntity(
                    title = "Monthly Salary Direct Deposit",
                    amount = 85000.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALARY,
                    necessity = ExpenseNecessity.NEED,
                    account = "HDFC Savings",
                    merchant = "TechCorp India",
                    timestamp = now - (12 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Apartment Rent",
                    amount = 22000.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.HOUSING,
                    necessity = ExpenseNecessity.NEED,
                    account = "HDFC Savings",
                    merchant = "Landlord",
                    timestamp = now - (11 * dayMillis),
                    isRecurring = true,
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "BigBasket Grocery Order",
                    amount = 3200.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.GROCERIES,
                    necessity = ExpenseNecessity.NEED,
                    account = "ICICI Credit Card",
                    merchant = "BigBasket",
                    timestamp = now - (9 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "DMart Weekly Groceries",
                    amount = 1850.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.GROCERIES,
                    necessity = ExpenseNecessity.NEED,
                    account = "ICICI Credit Card",
                    merchant = "DMart",
                    timestamp = now - (3 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Electricity Bill - Adani",
                    amount = 2100.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.UTILITIES,
                    necessity = ExpenseNecessity.NEED,
                    account = "HDFC Savings",
                    merchant = "Adani Electricity",
                    timestamp = now - (8 * dayMillis),
                    isRecurring = true,
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Metro Card Recharge",
                    amount = 600.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.TRANSPORTATION,
                    necessity = ExpenseNecessity.NEED,
                    account = "ICICI Credit Card",
                    merchant = "Metro Rail",
                    timestamp = now - (7 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Swiggy Food Order",
                    amount = 450.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.DINING,
                    necessity = ExpenseNecessity.WANT,
                    account = "ICICI Credit Card",
                    merchant = "Swiggy",
                    timestamp = now - (6 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Cafe Coffee Day",
                    amount = 180.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.DINING,
                    necessity = ExpenseNecessity.WANT,
                    account = "ICICI Credit Card",
                    merchant = "CCD",
                    timestamp = now - (5 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Zomato Dinner",
                    amount = 650.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.DINING,
                    necessity = ExpenseNecessity.WANT,
                    account = "ICICI Credit Card",
                    merchant = "Zomato",
                    timestamp = now - (2 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Netflix + Prime + Spotify",
                    amount = 647.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SUBSCRIPTIONS,
                    necessity = ExpenseNecessity.WANT,
                    account = "ICICI Credit Card",
                    merchant = "Streaming Services",
                    timestamp = now - (4 * dayMillis),
                    isRecurring = true,
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Nifty 50 Index Fund SIP",
                    amount = 10000.0,
                    type = TransactionType.INVESTMENT_BUY,
                    category = TransactionCategory.MUTUAL_FUND,
                    necessity = ExpenseNecessity.SAVINGS,
                    account = "HDFC Savings",
                    merchant = "Zerodha Coin",
                    timestamp = now - (10 * dayMillis),
                    isRecurring = true,
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Sovereign Gold Bond",
                    amount = 5000.0,
                    type = TransactionType.INVESTMENT_BUY,
                    category = TransactionCategory.GOLD,
                    necessity = ExpenseNecessity.SAVINGS,
                    account = "HDFC Savings",
                    merchant = "RBI SGB",
                    timestamp = now - (10 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "PPF Deposit",
                    amount = 15000.0,
                    type = TransactionType.TRANSFER,
                    category = TransactionCategory.SAVINGS_TRANSFER,
                    necessity = ExpenseNecessity.SAVINGS,
                    account = "PPF Account",
                    merchant = "SBI PPF",
                    timestamp = now - (10 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Term Insurance Premium",
                    amount = 12000.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.INSURANCE,
                    necessity = ExpenseNecessity.NEED,
                    account = "HDFC Savings",
                    merchant = "HDFC Life",
                    timestamp = now - (14 * dayMillis),
                    isRecurring = true,
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Freelance UI Consulting",
                    amount = 25000.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.FREELANCE,
                    necessity = ExpenseNecessity.NEED,
                    account = "HDFC Savings",
                    merchant = "Fintech Client",
                    timestamp = now - (2 * dayMillis),
                    currency = Currency.INR.code
                ),
                TransactionEntity(
                    title = "Reliance Industries Dividend",
                    amount = 1850.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.INVESTMENT_RETURN,
                    necessity = ExpenseNecessity.NEED,
                    account = "Zerodha Demat",
                    merchant = "Reliance Industries",
                    timestamp = now - (1 * dayMillis),
                    currency = Currency.INR.code
                )
            )
            transactionDao.insertTransactions(initialTransactions)

            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            val initialBudgets = listOf(
                BudgetEntity(category = TransactionCategory.GROCERIES, monthlyLimit = 8000.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.DINING, monthlyLimit = 5000.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.HOUSING, monthlyLimit = 25000.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.UTILITIES, monthlyLimit = 3500.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.TRANSPORTATION, monthlyLimit = 3000.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.ENTERTAINMENT, monthlyLimit = 2000.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.SHOPPING, monthlyLimit = 5000.0, periodMonth = month, periodYear = year),
                BudgetEntity(category = TransactionCategory.SUBSCRIPTIONS, monthlyLimit = 1000.0, periodMonth = month, periodYear = year)
            )
            budgetDao.insertBudgets(initialBudgets)

            val initialGoals = listOf(
                GoalEntity(
                    title = "Emergency Fund (6 Months)",
                    targetAmount = 150000.0,
                    currentAmount = 95000.0,
                    targetDateMillis = now + (180L * dayMillis),
                    categoryTag = "Security"
                ),
                GoalEntity(
                    title = "Goa Beach Vacation",
                    targetAmount = 40000.0,
                    currentAmount = 22000.0,
                    targetDateMillis = now + (90L * dayMillis),
                    categoryTag = "Travel"
                ),
                GoalEntity(
                    title = "New iPhone",
                    targetAmount = 80000.0,
                    currentAmount = 30000.0,
                    targetDateMillis = now + (120L * dayMillis),
                    categoryTag = "Tech"
                ),
                GoalEntity(
                    title = "Home Down Payment",
                    targetAmount = 1500000.0,
                    currentAmount = 450000.0,
                    targetDateMillis = now + (900L * dayMillis),
                    categoryTag = "Property"
                )
            )
            initialGoals.forEach { goalDao.insertGoal(it) }

            val initialMemories = listOf(
                BrainMemoryEntity(
                    memoryType = MemoryType.HABIT_LEARNED,
                    topic = "Frequent Food Delivery Habit",
                    description = "Orders via Swiggy/Zomato 3-4 times per week averaging ₹550 per order (~₹9,000/month).",
                    confidenceScore = 0.94f,
                    detectedCount = 8,
                    lastObservedAt = now - (2 * dayMillis),
                    actionSuggestion = "Cooking at home 2 extra days/week could redirect ₹4,500/mo into your Goa Vacation goal."
                ),
                BrainMemoryEntity(
                    memoryType = MemoryType.SAVINGS_VELOCITY,
                    topic = "Strong Savings Rate",
                    description = "Maintaining a 38% savings rate across SIP, PPF, and gold bonds - well above the 20% benchmark.",
                    confidenceScore = 0.98f,
                    detectedCount = 4,
                    lastObservedAt = now - (1 * dayMillis),
                    actionSuggestion = "Excellent discipline! Consider increasing SIP by 10% to accelerate your home down payment goal."
                ),
                BrainMemoryEntity(
                    memoryType = MemoryType.MERCHANT_PATTERN,
                    topic = "Grocery Splitting",
                    description = "Bulk monthly BigBasket orders (₹3,000+) balanced with weekly DMart top-ups (₹1,500).",
                    confidenceScore = 0.89f,
                    detectedCount = 5,
                    lastObservedAt = now - (3 * dayMillis),
                    actionSuggestion = "Groceries at 63% of your ₹8,000 monthly limit - in safe green territory."
                ),
                BrainMemoryEntity(
                    memoryType = MemoryType.MARKET_CONTEXT,
                    topic = "Indian Market Diversification",
                    description = "Portfolio spans Nifty Index, Sovereign Gold Bonds, PPF, and direct equity (Reliance).",
                    confidenceScore = 0.91f,
                    detectedCount = 3,
                    lastObservedAt = now - (1 * dayMillis),
                    actionSuggestion = "Consider adding a mid/small-cap allocation (5-10%) for higher long-term growth potential."
                )
            )
            brainMemoryDao.insertMemories(initialMemories)

            val initialHoldings = listOf(
                PortfolioHoldingEntity(
                    instrumentName = "Nifty 50 Index Fund",
                    symbol = "NIFTYBEES",
                    assetClass = AssetClass.INDEX_ETF,
                    region = InvestmentRegion.INDIA,
                    quantity = 120.0,
                    avgBuyPrice = 210.0,
                    currentPrice = 245.0,
                    investedAmount = 25200.0,
                    currentValue = 29400.0,
                    currency = Currency.INR.code,
                    isSip = true,
                    sipMonthlyAmount = 5000.0
                ),
                PortfolioHoldingEntity(
                    instrumentName = "Sovereign Gold Bond 2024",
                    symbol = "SGBAUG24",
                    assetClass = AssetClass.GOLD,
                    region = InvestmentRegion.INDIA,
                    quantity = 20.0,
                    avgBuyPrice = 6200.0,
                    currentPrice = 7100.0,
                    investedAmount = 124000.0,
                    currentValue = 142000.0,
                    currency = Currency.INR.code
                ),
                PortfolioHoldingEntity(
                    instrumentName = "Reliance Industries Ltd",
                    symbol = "RELIANCE",
                    assetClass = AssetClass.LARGE_CAP,
                    region = InvestmentRegion.INDIA,
                    quantity = 50.0,
                    avgBuyPrice = 2400.0,
                    currentPrice = 2850.0,
                    investedAmount = 120000.0,
                    currentValue = 142500.0,
                    currency = Currency.INR.code
                ),
                PortfolioHoldingEntity(
                    instrumentName = "Public Provident Fund",
                    symbol = "PPF",
                    assetClass = AssetClass.PPF_EPF,
                    region = InvestmentRegion.INDIA,
                    quantity = 1.0,
                    avgBuyPrice = 1.0,
                    currentPrice = 1.0,
                    investedAmount = 150000.0,
                    currentValue = 168000.0,
                    currency = Currency.INR.code
                ),
                PortfolioHoldingEntity(
                    instrumentName = "HDFC Mid-Cap Opportunities Fund",
                    symbol = "HDFCMID",
                    assetClass = AssetClass.MUTUAL_FUND,
                    region = InvestmentRegion.INDIA,
                    quantity = 5000.0,
                    avgBuyPrice = 95.0,
                    currentPrice = 112.0,
                    investedAmount = 475000.0,
                    currentValue = 560000.0,
                    currency = Currency.INR.code,
                    isSip = true,
                    sipMonthlyAmount = 5000.0
                ),
                PortfolioHoldingEntity(
                    instrumentName = "Vanguard Total US Stock ETF",
                    symbol = "VTI",
                    assetClass = AssetClass.INTERNATIONAL,
                    region = InvestmentRegion.US,
                    quantity = 15.0,
                    avgBuyPrice = 230.0,
                    currentPrice = 265.0,
                    investedAmount = 3450.0,
                    currentValue = 3975.0,
                    currency = Currency.USD.code
                )
            )
            portfolioDao.insertHoldings(initialHoldings)

            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    sender = MessageSender.DHANOM_AI,
                    messageText = "Namaste! I am Dhan-OM, your personal finance AI. This is demo data you chose to load — clear it anytime from Profile → Clear All Data.",
                    timestamp = now
                )
            )
        }
    }
}
