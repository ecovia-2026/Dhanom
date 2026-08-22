package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Currency(val code: String, val symbol: String, val label: String) {
    INR("INR", "₹", "Indian Rupee"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    AED("AED", "د.إ", "UAE Dirham");

    companion object {
        fun fromCode(code: String?): Currency = entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
    }
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    INVESTMENT_BUY,
    INVESTMENT_SELL
}

enum class ExpenseNecessity {
    NEED,       // Rent, groceries, utilities, basic healthcare
    WANT,       // Dining out, entertainment, gadgets, shopping
    SAVINGS     // Emergency fund, investments, retirement
}

enum class TransactionCategory(val displayName: String, val defaultNecessity: ExpenseNecessity) {
    HOUSING("Housing & Rent", ExpenseNecessity.NEED),
    GROCERIES("Groceries", ExpenseNecessity.NEED),
    UTILITIES("Utilities & Bills", ExpenseNecessity.NEED),
    TRANSPORTATION("Transportation", ExpenseNecessity.NEED),
    HEALTHCARE("Healthcare", ExpenseNecessity.NEED),
    DINING("Dining & Takeout", ExpenseNecessity.WANT),
    ENTERTAINMENT("Entertainment", ExpenseNecessity.WANT),
    SHOPPING("Shopping & Goods", ExpenseNecessity.WANT),
    TRAVEL("Travel & Vacation", ExpenseNecessity.WANT),
    EDUCATION("Education & Self-Care", ExpenseNecessity.NEED),
    INVESTMENT("Investments", ExpenseNecessity.SAVINGS),
    SAVINGS_TRANSFER("Savings Deposit", ExpenseNecessity.SAVINGS),
    SALARY("Salary & Wages", ExpenseNecessity.NEED),
    FREELANCE("Freelance & Business", ExpenseNecessity.NEED),
    INVESTMENT_RETURN("Dividends & Returns", ExpenseNecessity.NEED),
    INSURANCE("Insurance Premium", ExpenseNecessity.NEED),
    TAX("Tax & TDS", ExpenseNecessity.NEED),
    MUTUAL_FUND("Mutual Fund & SIP", ExpenseNecessity.SAVINGS),
    GOLD("Gold & Commodities", ExpenseNecessity.SAVINGS),
    CRYPTO("Crypto Assets", ExpenseNecessity.SAVINGS),
    GIFTS_DONATIONS("Gifts & Donations", ExpenseNecessity.WANT),
    SUBSCRIPTIONS("Subscriptions", ExpenseNecessity.WANT),
    OTHER("Other", ExpenseNecessity.WANT);

    companion object {
        fun fromString(value: String): TransactionCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val necessity: ExpenseNecessity = category.defaultNecessity,
    val account: String = "Main Checking",
    val merchant: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val tags: String = "",
    val isRecurring: Boolean = false,
    val currency: String = Currency.INR.code
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val periodMonth: Int, // 1-12
    val periodYear: Int,  // e.g. 2026
    val alertThreshold: Double = 0.85 // Alert at 85%
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long = System.currentTimeMillis() + (90L * 24 * 3600 * 1000), // default 3 months
    val categoryTag: String = "General",
    val isCompleted: Boolean = false
)

enum class MemoryType {
    HABIT_LEARNED,
    MERCHANT_PATTERN,
    SPENDING_SURGE,
    SAVINGS_VELOCITY,
    RECOMMENDATION_ACTIVE,
    MARKET_CONTEXT,
    GOAL_STRATEGY,
    FACT,          // a durable fact about the user (assets, salary, debts, life events)
    PREFERENCE,    // a user preference / standing style rule
    TASK,          // a scheduled/recurring task the brain must follow (until expiry)
    SELF_HEAL      // a crash / repair diagnosis kept so the brain can heal itself
}

@Entity(tableName = "brain_memories")
data class BrainMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryType: MemoryType,
    val topic: String,
    val description: String,
    val confidenceScore: Float = 0.85f,
    val detectedCount: Int = 1,
    val lastObservedAt: Long = System.currentTimeMillis(),
    val actionSuggestion: String = ""
)

enum class MessageSender {
    USER,
    DHANOM_AI,
    SYSTEM
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: MessageSender,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // e.g. "LOG_EXPENSE", "FLOWCHART_SHOWN", "BUDGET_ALERT"
    val actionPayload: String? = null
)

// Investment portfolio holdings
enum class AssetClass(val displayName: String) {
    LARGE_CAP("Large Cap Equity"),
    MID_SMALL_CAP("Mid & Small Cap"),
    INDEX_ETF("Index ETF"),
    MUTUAL_FUND("Mutual Fund / SIP"),
    DEBT_FD("Debt / Fixed Deposit"),
    GOLD("Gold / Sovereign Gold Bond"),
    INTERNATIONAL("International Equity"),
    CRYPTO("Cryptocurrency"),
    REIT("REIT / Real Estate"),
    PPF_EPF("PPF / EPF / NPS"),
    BONDS("Bonds")
}

enum class InvestmentRegion(val displayName: String) {
    INDIA("India"),
    US("United States"),
    GLOBAL("Global / International")
}

@Entity(tableName = "portfolio_holdings")
data class PortfolioHoldingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instrumentName: String,
    val symbol: String = "",
    val assetClass: AssetClass,
    val region: InvestmentRegion = InvestmentRegion.INDIA,
    val quantity: Double = 0.0,
    val avgBuyPrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val investedAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val currency: String = Currency.INR.code,
    val purchaseDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isSip: Boolean = false,
    val sipMonthlyAmount: Double = 0.0
) {
    val unrealizedPnl: Double get() = currentValue - investedAmount
    val unrealizedPnlPercent: Double get() = if (investedAmount > 0) (unrealizedPnl / investedAmount) * 100.0 else 0.0
}

enum class LoanType(val displayName: String) {
    LOAN("Loan (Borrowed)"),
    DEBT("Debt (Owed)")
}

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: LoanType = LoanType.LOAN,
    val principalAmount: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val interestRate: Double = 0.0, // % per annum
    val monthlyEmi: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A money account (cash, bank, credit card, wallet, investment). Transactions
 * map onto accounts by name — this gives the "perfect mapping" from the
 * reference apps (Monefy / Wallet / Actual / Firefly III).
 */
enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    CREDIT_CARD("Credit Card"),
    WALLET("Wallet / UPI"),
    INVESTMENT("Investment")
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.BANK,
    val initialBalance: Double = 0.0,
    val colorArgb: Long = 0xFF6750A4,
    val icon: String = "wallet",
    val isArchived: Boolean = false
)

/**
 * A scheduled/recurring transaction (Actual/Firefly-style): the app auto-posts
 * a real transaction each time it comes due, then rolls the schedule forward.
 */
@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val necessity: ExpenseNecessity = ExpenseNecessity.NEED,
    val account: String = "Bank Account",
    val recurrence: TaskRecurrence = TaskRecurrence.MONTHLY,
    val nextDueDateMillis: Long = 0L,
    val endDateMillis: Long = 0L, // 0 = forever
    val notes: String = "",
    val isActive: Boolean = true
) {
    fun scheduleLabel(): String = when (recurrence) {
        TaskRecurrence.ONCE -> "Once"
        TaskRecurrence.DAILY -> "Daily"
        TaskRecurrence.WEEKLY -> "Weekly"
        TaskRecurrence.MONTHLY -> "Monthly"
        TaskRecurrence.QUARTERLY -> "Quarterly"
        TaskRecurrence.YEARLY -> "Yearly"
    }

    fun nextOccurrence(): Long {
        val cal = Calendar.getInstance()
        val base = if (nextDueDateMillis > 0 && nextDueDateMillis > System.currentTimeMillis())
            nextDueDateMillis else System.currentTimeMillis()
        cal.timeInMillis = base
        when (recurrence) {
            TaskRecurrence.ONCE -> return Long.MAX_VALUE
            TaskRecurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TaskRecurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TaskRecurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
            TaskRecurrence.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            TaskRecurrence.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
