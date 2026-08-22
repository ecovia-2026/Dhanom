package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: TransactionCategory): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY monthlyLimit DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE periodMonth = :month AND periodYear = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudgetForCategory(category: TransactionCategory): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY targetDateMillis ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun clearAllGoals()
}

@Dao
interface BrainMemoryDao {
    @Query("SELECT * FROM brain_memories ORDER BY confidenceScore DESC, lastObservedAt DESC")
    fun getAllMemories(): Flow<List<BrainMemoryEntity>>

    @Query("SELECT * FROM brain_memories WHERE topic = :topic LIMIT 1")
    suspend fun getMemoryByTopic(topic: String): BrainMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: BrainMemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<BrainMemoryEntity>)

    @Update
    suspend fun updateMemory(memory: BrainMemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: BrainMemoryEntity)

    @Query("DELETE FROM brain_memories")
    suspend fun clearAllMemories()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_holdings ORDER BY investedAmount DESC")
    fun getAllHoldings(): Flow<List<PortfolioHoldingEntity>>

    @Query("SELECT * FROM portfolio_holdings WHERE id = :id")
    suspend fun getHoldingById(id: Long): PortfolioHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: PortfolioHoldingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<PortfolioHoldingEntity>)

    @Update
    suspend fun updateHolding(holding: PortfolioHoldingEntity)

    @Delete
    suspend fun deleteHolding(holding: PortfolioHoldingEntity)

    @Query("DELETE FROM portfolio_holdings")
    suspend fun clearAllHoldings()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY outstandingAmount DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("DELETE FROM loans")
    suspend fun clearAllLoans()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isActive DESC, nextDueDateMillis ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, type ASC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, nextDueDateMillis ASC")
    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDateMillis ASC")
    suspend fun getActiveRecurring(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(r: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(list: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(r: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(r: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAllRecurring()
}
