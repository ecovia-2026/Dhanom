package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.dao.*
import com.example.data.model.*

class FinanceTypeConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromExpenseNecessity(necessity: ExpenseNecessity): String = necessity.name

    @TypeConverter
    fun toExpenseNecessity(value: String): ExpenseNecessity = try {
        ExpenseNecessity.valueOf(value)
    } catch (e: Exception) {
        ExpenseNecessity.WANT
    }

    @TypeConverter
    fun fromTransactionCategory(category: TransactionCategory): String = category.name

    @TypeConverter
    fun toTransactionCategory(value: String): TransactionCategory = try {
        TransactionCategory.valueOf(value)
    } catch (e: Exception) {
        TransactionCategory.OTHER
    }

    @TypeConverter
    fun fromMemoryType(type: MemoryType): String = type.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = try {
        MemoryType.valueOf(value)
    } catch (e: Exception) {
        MemoryType.HABIT_LEARNED
    }

    @TypeConverter
    fun fromMessageSender(sender: MessageSender): String = sender.name

    @TypeConverter
    fun toMessageSender(value: String): MessageSender = try {
        MessageSender.valueOf(value)
    } catch (e: Exception) {
        MessageSender.SYSTEM
    }

    @TypeConverter
    fun fromAssetClass(assetClass: AssetClass): String = assetClass.name

    @TypeConverter
    fun toAssetClass(value: String): AssetClass = try {
        AssetClass.valueOf(value)
    } catch (e: Exception) {
        AssetClass.MUTUAL_FUND
    }

    @TypeConverter
    fun fromLoanType(type: LoanType): String = type.name

    @TypeConverter
    fun toLoanType(value: String): LoanType = try {
        LoanType.valueOf(value)
    } catch (e: Exception) {
        LoanType.LOAN
    }

    @TypeConverter
    fun fromInvestmentRegion(region: InvestmentRegion): String = region.name

    @TypeConverter
    fun toInvestmentRegion(value: String): InvestmentRegion = try {
        InvestmentRegion.valueOf(value)
    } catch (e: Exception) {
        InvestmentRegion.INDIA
    }

    @TypeConverter
    fun fromTaskRecurrence(rec: TaskRecurrence): String = rec.name

    @TypeConverter
    fun toTaskRecurrence(value: String): TaskRecurrence = try {
        TaskRecurrence.valueOf(value)
    } catch (e: Exception) {
        TaskRecurrence.ONCE
    }

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = try {
        AccountType.valueOf(value)
    } catch (e: Exception) {
        AccountType.BANK
    }
}

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        BrainMemoryEntity::class,
        ChatMessageEntity::class,
        PortfolioHoldingEntity::class,
        LoanEntity::class,
        TaskEntity::class,
        AccountEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(FinanceTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun brainMemoryDao(): BrainMemoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun loanDao(): LoanDao
    abstract fun taskDao(): TaskDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2 -> v3: adds the loans table (preserves existing data). */
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS loans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "principalAmount REAL NOT NULL, " +
                    "outstandingAmount REAL NOT NULL, " +
                    "interestRate REAL NOT NULL, " +
                    "monthlyEmi REAL NOT NULL, " +
                    "notes TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL)"
                )
            }
        }

        /** v3 -> v4: adds the tasks table (scheduled / recurring brain tasks). */
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "recurrence TEXT NOT NULL, " +
                    "nextDueDateMillis INTEGER NOT NULL, " +
                    "expiresAtMillis INTEGER NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "notes TEXT NOT NULL, " +
                    "timesDone INTEGER NOT NULL, " +
                    "isActive INTEGER NOT NULL)"
                )
            }
        }

        /** v4 -> v5: adds the accounts table (money accounts for mapping). */
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS accounts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "initialBalance REAL NOT NULL, " +
                    "colorArgb INTEGER NOT NULL, " +
                    "icon TEXT NOT NULL, " +
                    "isArchived INTEGER NOT NULL)"
                )
            }
        }

        /** v5 -> v6: adds the recurring transactions table. */
        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recurring_transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "necessity TEXT NOT NULL, " +
                    "account TEXT NOT NULL, " +
                    "recurrence TEXT NOT NULL, " +
                    "nextDueDateMillis INTEGER NOT NULL, " +
                    "endDateMillis INTEGER NOT NULL, " +
                    "notes TEXT NOT NULL, " +
                    "isActive INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dhanom_finance_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
