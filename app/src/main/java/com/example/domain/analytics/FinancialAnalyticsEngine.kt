package com.example.domain.analytics

import androidx.compose.ui.graphics.Color
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CashFlowSummary(
    val totalInflow: Double,
    val totalOutflow: Double,
    val netCashFlow: Double,
    val savingsRate: Double,
    val needsAmount: Double,
    val wantsAmount: Double,
    val savingsAmount: Double,
    val needsPercentage: Double,
    val wantsPercentage: Double,
    val savingsPercentage: Double,
    val dailyBurnRate: Double,
    val healthScore: Int,
    val healthGrade: String,
    val healthSummary: String
)

data class CategoryExpense(
    val category: TransactionCategory,
    val amount: Double,
    val count: Int,
    val percentage: Double,
    val color: Color
)

data class DailyTrendPoint(
    val dayLabel: String,
    val timestamp: Long,
    val inflow: Double,
    val outflow: Double,
    val net: Double
)

data class CategoryBudgetProgress(
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressFraction: Float, // 0.0f to 1.0f+
    val isOverBudget: Boolean,
    val isNearLimit: Boolean
)

// Data structure for the Interactive Cash Flow Flowchart (Sankey-style)
data class FlowNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val percentageOfInflow: Double,
    val color: Color,
    val category: String // "INCOME", "ALLOCATION", "CATEGORY"
)

data class FlowLink(
    val sourceId: String,
    val targetId: String,
    val amount: Double,
    val flowPercentage: Double,
    val color: Color
)

data class CashFlowchartData(
    val incomeNodes: List<FlowNode>,
    val allocationNodes: List<FlowNode>,
    val categoryNodes: List<FlowNode>,
    val links: List<FlowLink>,
    val totalInflow: Double,
    val totalOutflow: Double,
    val retainedBuffer: Double
)

object FinancialAnalyticsEngine {

    val CategoryColors = mapOf(
        TransactionCategory.HOUSING to Color(0xFF3B82F6), // Blue
        TransactionCategory.GROCERIES to Color(0xFF10B981), // Emerald
        TransactionCategory.UTILITIES to Color(0xFF06B6D4), // Cyan
        TransactionCategory.TRANSPORTATION to Color(0xFF8B5CF6), // Violet
        TransactionCategory.HEALTHCARE to Color(0xFFEC4899), // Pink
        TransactionCategory.DINING to Color(0xFFF59E0B), // Amber
        TransactionCategory.ENTERTAINMENT to Color(0xFFF97316), // Orange
        TransactionCategory.SHOPPING to Color(0xFFEF4444), // Red
        TransactionCategory.TRAVEL to Color(0xFF14B8A6), // Teal
        TransactionCategory.EDUCATION to Color(0xFF6366F1), // Indigo
        TransactionCategory.INVESTMENT to Color(0xFF10B981), // Emerald Green
        TransactionCategory.SAVINGS_TRANSFER to Color(0xFF059669), // Dark Emerald
        TransactionCategory.SALARY to Color(0xFF22C55E), // Green
        TransactionCategory.FREELANCE to Color(0xFF84CC16), // Lime
        TransactionCategory.INVESTMENT_RETURN to Color(0xFF10B981),
        TransactionCategory.INSURANCE to Color(0xFF6366F1), // Indigo
        TransactionCategory.TAX to Color(0xFFEF4444), // Red
        TransactionCategory.MUTUAL_FUND to Color(0xFF10B981), // Emerald
        TransactionCategory.GOLD to Color(0xFFEAB308), // Yellow
        TransactionCategory.CRYPTO to Color(0xFFF97316), // Orange
        TransactionCategory.GIFTS_DONATIONS to Color(0xFFEC4899), // Pink
        TransactionCategory.SUBSCRIPTIONS to Color(0xFF8B5CF6), // Violet
        TransactionCategory.OTHER to Color(0xFF94A3B8) // Slate
    )

    fun calculateCashFlowSummary(transactions: List<TransactionEntity>): CashFlowSummary {
        var totalInflow = 0.0
        var totalOutflow = 0.0
        var needsTotal = 0.0
        var wantsTotal = 0.0
        var savingsTotal = 0.0

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Filter transactions for current month if possible, or all if small
        val monthTransactions = transactions.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }.ifEmpty { transactions }

        for (tx in monthTransactions) {
            when (tx.type) {
                TransactionType.INCOME, TransactionType.INVESTMENT_SELL -> totalInflow += tx.amount
                TransactionType.EXPENSE, TransactionType.TRANSFER, TransactionType.INVESTMENT_BUY -> {
                    totalOutflow += tx.amount
                    when (tx.necessity) {
                        ExpenseNecessity.NEED -> needsTotal += tx.amount
                        ExpenseNecessity.WANT -> wantsTotal += tx.amount
                        ExpenseNecessity.SAVINGS -> savingsTotal += tx.amount
                    }
                }
            }
        }

        val netCashFlow = totalInflow - totalOutflow
        val savingsRate = if (totalInflow > 0) {
            ((savingsTotal + max(0.0, netCashFlow)) / totalInflow) * 100.0
        } else 0.0

        val totalAllocated = needsTotal + wantsTotal + savingsTotal
        val needsPct = if (totalAllocated > 0) (needsTotal / totalAllocated) * 100.0 else 0.0
        val wantsPct = if (totalAllocated > 0) (wantsTotal / totalAllocated) * 100.0 else 0.0
        val savingsPct = if (totalAllocated > 0) (savingsTotal / totalAllocated) * 100.0 else 0.0

        val dailyBurnRate = if (dayOfMonth > 0) (totalOutflow - savingsTotal) / dayOfMonth else 0.0

        // Financial Health Score calculation (0 - 100)
        var score = 50 // baseline

        // Savings rate factor (+/- 25)
        if (savingsRate >= 20.0) score += 20 else if (savingsRate >= 10.0) score += 10 else score -= 10
        if (savingsRate >= 30.0) score += 5

        // Needs ratio factor (ideal <= 50%) (+/- 15)
        val needsInflowRatio = if (totalInflow > 0) (needsTotal / totalInflow) * 100.0 else 100.0
        if (needsInflowRatio <= 50.0) score += 15 else if (needsInflowRatio <= 65.0) score += 5 else score -= 10

        // Wants ratio factor (ideal <= 30%) (+/- 10)
        val wantsInflowRatio = if (totalInflow > 0) (wantsTotal / totalInflow) * 100.0 else 100.0
        if (wantsInflowRatio <= 30.0) score += 10 else score -= 10

        score = min(100, max(10, score))

        val grade = when {
            score >= 85 -> "A+ Excellent"
            score >= 75 -> "A Strong"
            score >= 65 -> "B Good"
            score >= 50 -> "C Fair"
            else -> "Needs Attention"
        }

        val summary = when {
            score >= 80 -> "Exceptional financial balance. Your savings rate (${savingsRate.roundToInt()}%) and low debt risk position you well above standard targets."
            score >= 65 -> "Solid cash flow. Good control over essential needs, with moderate discretionary flexibility."
            else -> "High outflow velocity detected. Consider trimming dining or leisure subscriptions to boost your safety buffer."
        }

        return CashFlowSummary(
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netCashFlow = netCashFlow,
            savingsRate = savingsRate,
            needsAmount = needsTotal,
            wantsAmount = wantsTotal,
            savingsAmount = savingsTotal,
            needsPercentage = needsPct,
            wantsPercentage = wantsPct,
            savingsPercentage = savingsPct,
            dailyBurnRate = dailyBurnRate,
            healthScore = score,
            healthGrade = grade,
            healthSummary = summary
        )
    }

    fun calculateCategoryBreakdown(transactions: List<TransactionEntity>): List<CategoryExpense> {
        val expenseTx = transactions.filter { it.type != TransactionType.INCOME }
        val totalAmount = expenseTx.sumOf { it.amount }

        if (totalAmount <= 0.0) return emptyList()

        val grouped = expenseTx.groupBy { it.category }
        return grouped.map { (cat, list) ->
            val catTotal = list.sumOf { it.amount }
            CategoryExpense(
                category = cat,
                amount = catTotal,
                count = list.size,
                percentage = (catTotal / totalAmount) * 100.0,
                color = CategoryColors[cat] ?: Color.Gray
            )
        }.sortedByDescending { it.amount }
    }

    fun calculateBudgetProgress(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>
    ): List<CategoryBudgetProgress> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val currentMonthExpenses = transactions.filter {
            it.type != TransactionType.INCOME &&
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.let { cal ->
                        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                    }
        }

        val spentByCategory = currentMonthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        return budgets.map { budget ->
            val spent = spentByCategory[budget.category] ?: 0.0
            val remaining = max(0.0, budget.monthlyLimit - spent)
            val fraction = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
            CategoryBudgetProgress(
                category = budget.category,
                monthlyLimit = budget.monthlyLimit,
                spentAmount = spent,
                remainingAmount = remaining,
                progressFraction = fraction,
                isOverBudget = spent > budget.monthlyLimit,
                isNearLimit = fraction >= budget.alertThreshold.toFloat() && spent <= budget.monthlyLimit
            )
        }.sortedByDescending { it.progressFraction }
    }

    fun generateCashFlowchartData(transactions: List<TransactionEntity>): CashFlowchartData {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val monthTx = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.ifEmpty { transactions }

        val incomeTx = monthTx.filter { it.type == TransactionType.INCOME }
        val expenseTx = monthTx.filter { it.type != TransactionType.INCOME }

        val totalInflow = max(1.0, incomeTx.sumOf { it.amount })
        val totalOutflow = expenseTx.sumOf { it.amount }
        val retainedBuffer = max(0.0, totalInflow - totalOutflow)

        // 1. Income Source Nodes
        val incomeGrouped = incomeTx.groupBy { it.category }
        val incomeNodes = incomeGrouped.map { (cat, list) ->
            val amount = list.sumOf { it.amount }
            FlowNode(
                id = "in_${cat.name}",
                title = cat.displayName,
                subtitle = "${list.size} deposit(s)",
                amount = amount,
                percentageOfInflow = (amount / totalInflow) * 100.0,
                color = CategoryColors[cat] ?: Color(0xFF10B981),
                category = "INCOME"
            )
        }.ifEmpty {
            listOf(
                FlowNode(
                    id = "in_default",
                    title = "Total Inflow",
                    subtitle = "Monthly Income",
                    amount = totalInflow,
                    percentageOfInflow = 100.0,
                    color = Color(0xFF10B981),
                    category = "INCOME"
                )
            )
        }

        // 2. 50/30/20 Framework Allocation Nodes (Needs, Wants, Savings, Buffer)
        val needsTx = expenseTx.filter { it.necessity == ExpenseNecessity.NEED }
        val wantsTx = expenseTx.filter { it.necessity == ExpenseNecessity.WANT }
        val savingsTx = expenseTx.filter { it.necessity == ExpenseNecessity.SAVINGS }

        val needsAmount = needsTx.sumOf { it.amount }
        val wantsAmount = wantsTx.sumOf { it.amount }
        val savingsAmount = savingsTx.sumOf { it.amount }

        val allocationNodes = mutableListOf<FlowNode>()
        allocationNodes.add(
            FlowNode(
                id = "alloc_needs",
                title = "Essential Needs (50%)",
                subtitle = "Housing, Groceries, Transit",
                amount = needsAmount,
                percentageOfInflow = (needsAmount / totalInflow) * 100.0,
                color = Color(0xFF3B82F6),
                category = "ALLOCATION"
            )
        )
        allocationNodes.add(
            FlowNode(
                id = "alloc_wants",
                title = "Discretionary Wants (30%)",
                subtitle = "Dining, Fun, Shopping",
                amount = wantsAmount,
                percentageOfInflow = (wantsAmount / totalInflow) * 100.0,
                color = Color(0xFFF59E0B),
                category = "ALLOCATION"
            )
        )
        allocationNodes.add(
            FlowNode(
                id = "alloc_savings",
                title = "Savings & Goals (20%)",
                subtitle = "Investments, Emergency HYSA",
                amount = savingsAmount,
                percentageOfInflow = (savingsAmount / totalInflow) * 100.0,
                color = Color(0xFF10B981),
                category = "ALLOCATION"
            )
        )
        if (retainedBuffer > 0.0) {
            allocationNodes.add(
                FlowNode(
                    id = "alloc_buffer",
                    title = "Unallocated Buffer",
                    subtitle = "Remaining Cash Surplus",
                    amount = retainedBuffer,
                    percentageOfInflow = (retainedBuffer / totalInflow) * 100.0,
                    color = Color(0xFF6366F1),
                    category = "ALLOCATION"
                )
            )
        }

        // 3. Category Nodes
        val categoryExpenseMap = expenseTx.groupBy { it.category }
        val categoryNodes = categoryExpenseMap.map { (cat, list) ->
            val catTotal = list.sumOf { it.amount }
            FlowNode(
                id = "cat_${cat.name}",
                title = cat.displayName,
                subtitle = "${list.size} tx (${cat.defaultNecessity.name})",
                amount = catTotal,
                percentageOfInflow = (catTotal / totalInflow) * 100.0,
                color = CategoryColors[cat] ?: Color.Gray,
                category = "CATEGORY"
            )
        }.sortedByDescending { it.amount }

        // 4. Flow Connections (Links)
        val links = mutableListOf<FlowLink>()

        // Link primary income to allocations
        incomeNodes.forEach { inc ->
            if (needsAmount > 0) {
                links.add(
                    FlowLink(
                        sourceId = inc.id,
                        targetId = "alloc_needs",
                        amount = (inc.amount / totalInflow) * needsAmount,
                        flowPercentage = (needsAmount / totalInflow) * 100.0,
                        color = Color(0xFF3B82F6).copy(alpha = 0.6f)
                    )
                )
            }
            if (wantsAmount > 0) {
                links.add(
                    FlowLink(
                        sourceId = inc.id,
                        targetId = "alloc_wants",
                        amount = (inc.amount / totalInflow) * wantsAmount,
                        flowPercentage = (wantsAmount / totalInflow) * 100.0,
                        color = Color(0xFFF59E0B).copy(alpha = 0.6f)
                    )
                )
            }
            if (savingsAmount > 0) {
                links.add(
                    FlowLink(
                        sourceId = inc.id,
                        targetId = "alloc_savings",
                        amount = (inc.amount / totalInflow) * savingsAmount,
                        flowPercentage = (savingsAmount / totalInflow) * 100.0,
                        color = Color(0xFF10B981).copy(alpha = 0.6f)
                    )
                )
            }
        }

        // Link Allocations to specific Categories
        categoryExpenseMap.forEach { (cat, list) ->
            val catTotal = list.sumOf { it.amount }
            val sourceAllocId = when (cat.defaultNecessity) {
                ExpenseNecessity.NEED -> "alloc_needs"
                ExpenseNecessity.WANT -> "alloc_wants"
                ExpenseNecessity.SAVINGS -> "alloc_savings"
            }
            links.add(
                FlowLink(
                    sourceId = sourceAllocId,
                    targetId = "cat_${cat.name}",
                    amount = catTotal,
                    flowPercentage = (catTotal / totalInflow) * 100.0,
                    color = (CategoryColors[cat] ?: Color.Gray).copy(alpha = 0.7f)
                )
            )
        }

        return CashFlowchartData(
            incomeNodes = incomeNodes,
            allocationNodes = allocationNodes,
            categoryNodes = categoryNodes,
            links = links,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            retainedBuffer = retainedBuffer
        )
    }

    fun calculateDailyTrends(transactions: List<TransactionEntity>, daysCount: Int = 14): List<DailyTrendPoint> {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val dayMillis = 24L * 3600 * 1000
        val now = System.currentTimeMillis()

        val points = mutableListOf<DailyTrendPoint>()

        for (i in (daysCount - 1) downTo 0) {
            val targetDayStart = now - (i * dayMillis)
            val cal = Calendar.getInstance().apply {
                timeInMillis = targetDayStart
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = cal.timeInMillis
            val endMs = startMs + dayMillis

            val dayTransactions = transactions.filter { it.timestamp in startMs until endMs }
            val dayInflow = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val dayOutflow = dayTransactions.filter { it.type != TransactionType.INCOME }.sumOf { it.amount }

            points.add(
                DailyTrendPoint(
                    dayLabel = dateFormat.format(Date(startMs)),
                    timestamp = startMs,
                    inflow = dayInflow,
                    outflow = dayOutflow,
                    net = dayInflow - dayOutflow
                )
            )
        }

        return points
    }

    fun detectHabitsAndAnomalies(transactions: List<TransactionEntity>): List<BrainMemoryEntity> {
        val memories = mutableListOf<BrainMemoryEntity>()
        val now = System.currentTimeMillis()

        // 1. Detect Frequent Recurring Merchants (e.g. coffee, groceries)
        val merchantGroups = transactions
            .filter { it.merchant.isNotBlank() && it.type != TransactionType.INCOME }
            .groupBy { it.merchant.lowercase().trim() }

        merchantGroups.forEach { (rawMerchant, list) ->
            if (list.size >= 3) {
                val totalSpent = list.sumOf { it.amount }
                val avgSpent = totalSpent / list.size
                val merchantName = list.first().merchant
                val isCoffeeOrFood = rawMerchant.contains("coffee") || rawMerchant.contains("starbucks") || rawMerchant.contains("blue bottle") || rawMerchant.contains("cafe")

                memories.add(
                    BrainMemoryEntity(
                        memoryType = if (isCoffeeOrFood) MemoryType.HABIT_LEARNED else MemoryType.MERCHANT_PATTERN,
                        topic = "Regular at $merchantName",
                        description = "Visited $merchantName ${list.size} times, averaging $${String.format(Locale.US, "%.2f", avgSpent)} per visit ($${String.format(Locale.US, "%.2f", totalSpent)} total).",
                        confidenceScore = min(0.99f, 0.75f + (list.size * 0.05f)),
                        detectedCount = list.size,
                        lastObservedAt = list.maxOf { it.timestamp },
                        actionSuggestion = if (isCoffeeOrFood) "Small daily micro-purchases compound to $${String.format(Locale.US, "%.0f", totalSpent * 4)}/quarter. Automate a cap to save effortlessly." else "Track recurring transactions for $merchantName in your monthly budget."
                    )
                )
            }
        }

        // 2. Detect Weekend Surge Spending
        val weekendTx = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val day = cal.get(Calendar.DAY_OF_WEEK)
            day == Calendar.SATURDAY || day == Calendar.SUNDAY
        }
        val weekdayTx = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val day = cal.get(Calendar.DAY_OF_WEEK)
            day != Calendar.SATURDAY && day != Calendar.SUNDAY
        }

        val weekendAvg = if (weekendTx.isNotEmpty()) weekendTx.sumOf { it.amount } / weekendTx.size else 0.0
        val weekdayAvg = if (weekdayTx.isNotEmpty()) weekdayTx.sumOf { it.amount } / weekdayTx.size else 0.0

        if (weekendAvg > weekdayAvg * 1.5 && weekendTx.size >= 2) {
            memories.add(
                BrainMemoryEntity(
                    memoryType = MemoryType.SPENDING_SURGE,
                    topic = "Weekend Spending Surge",
                    description = "Average weekend transaction ($${String.format(Locale.US, "%.2f", weekendAvg)}) is ${((weekendAvg / max(1.0, weekdayAvg) - 1.0) * 100).roundToInt()}% higher than weekday average.",
                    confidenceScore = 0.88f,
                    detectedCount = weekendTx.size,
                    lastObservedAt = now,
                    actionSuggestion = "Set a dedicated 'Weekend Treat' envelope of $100 to prevent weekend blowout without feeling restricted."
                )
            )
        }

        // 3. Detect Savings Velocity
        val summary = calculateCashFlowSummary(transactions)
        if (summary.savingsRate >= 15.0) {
            memories.add(
                BrainMemoryEntity(
                    memoryType = MemoryType.SAVINGS_VELOCITY,
                    topic = "High Savings Discipline",
                    description = "Maintaining a healthy ${summary.savingsRate.roundToInt()}% savings rate across active income.",
                    confidenceScore = 0.96f,
                    detectedCount = 1,
                    lastObservedAt = now,
                    actionSuggestion = "Consider directing half of surplus cash buffer ($${String.format(Locale.US, "%.2f", max(0.0, summary.netCashFlow))}) into broad-market index funds."
                )
            )
        }

        return memories
    }
}
