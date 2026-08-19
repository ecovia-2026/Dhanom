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
    fun fromInvestmentRegion(region: InvestmentRegion): String = region.name

    @TypeConverter
    fun toInvestmentRegion(value: String): InvestmentRegion = try {
        InvestmentRegion.valueOf(value)
    } catch (e: Exception) {
        InvestmentRegion.INDIA
    }
}

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        BrainMemoryEntity::class,
        ChatMessageEntity::class,
        PortfolioHoldingEntity::class
    ],
    version = 2,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dhanom_finance_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
