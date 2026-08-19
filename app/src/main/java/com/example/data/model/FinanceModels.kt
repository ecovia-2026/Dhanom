package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
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
    val isRecurring: Boolean = false
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
    RECOMMENDATION_ACTIVE
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
