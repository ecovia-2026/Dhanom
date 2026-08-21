package com.example.domain.nlp

import com.example.data.model.*
import com.example.domain.analytics.CashFlowSummary
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.ml.PersonalFinanceMlEngine
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

sealed class ParsedFinanceCommand {
    data class AddTransactionCommand(
        val transaction: TransactionEntity,
        val confirmationMessage: String
    ) : ParsedFinanceCommand()

    data class DeleteTransactionCommand(
        val transactionsToDelete: List<TransactionEntity>,
        val confirmationMessage: String
    ) : ParsedFinanceCommand()

    data class SetBudgetCommand(
        val category: TransactionCategory,
        val limit: Double,
        val confirmationMessage: String
    ) : ParsedFinanceCommand()

    data class AddGoalCommand(
        val goal: GoalEntity,
        val confirmationMessage: String
    ) : ParsedFinanceCommand()

    data class ShowAnalyticsCommand(
        val targetTab: String, // "FLOWCHART", "GRAPHS", "TABLE", "HABITS", "BUDGETS"
        val responseMessage: String
    ) : ParsedFinanceCommand()

    data class QueryResponseCommand(
        val queryTopic: String,
        val responseText: String,
        val categoryBreakdown: Map<String, Double>? = null
    ) : ParsedFinanceCommand()

    /** App-customization commands spoken in chat: THEME / SORT. */
    data class CustomizeCommand(
        val action: String, // "THEME" | "SORT"
        val payload: String,
        val message: String
    ) : ParsedFinanceCommand()
}

/**
 * Advanced Natural Language Understanding (NLU) Engine for Personal Finance.
 * Interprets user questions, temporal filters, category queries, investment returns,
 * ML predictions, and automated actions with live database context.
 */
object NaturalLanguageFinanceParser {

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:₹|\\$|USD\\s*|INR\\s*|EUR\\s*)?(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d{1,2})?)(?:\\s*(?:dollars|bucks|k|grand|lakh|lakhs|lac|lacs|crore|crores))?")

    fun parseCommandWithContext(
        input: String,
        transactions: List<TransactionEntity> = emptyList(),
        budgets: List<BudgetEntity> = emptyList(),
        goals: List<GoalEntity> = emptyList(),
        summary: CashFlowSummary? = null
    ): ParsedFinanceCommand {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Check for Flowchart / Sankey navigation requests
        if (lower.contains("flowchart") || lower.contains("cash flow chart") || lower.contains("flow chart") || lower.contains("sankey") || lower.contains("show flow")) {
            return ParsedFinanceCommand.ShowAnalyticsCommand(
                targetTab = "FLOWCHART",
                responseMessage = "Here is your interactive Cash Flow Flowchart. It maps your total monthly inflows into 50/30/20 core allocations (Needs, Wants, Savings) and detailed category streams."
            )
        }

        // 2. Check for Graph / Analytics / Donut requests
        if (lower.contains("graph") || lower.contains("chart") || lower.contains("breakdown") || lower.contains("pie") || lower.contains("velocity")) {
            return ParsedFinanceCommand.ShowAnalyticsCommand(
                targetTab = "GRAPHS",
                responseMessage = "Opening your Spending Analytics, Category Donut Breakdown, and 10-day Cash Velocity charts."
            )
        }

        // 3. Check for Ledger / Transactions Table requests
        if (lower.contains("table") || lower.contains("ledger") || lower.contains("history") || lower.contains("show all transactions") || lower.contains("records") || lower.contains("list transactions")) {
            return ParsedFinanceCommand.ShowAnalyticsCommand(
                targetTab = "TABLE",
                responseMessage = "Opening your searchable Transaction Ledger and data table."
            )
        }

        // 4. Check for Habit / Brain / ML Learning requests
        if (lower.contains("habit") || lower.contains("spending pattern") || lower.contains("brain") || lower.contains("what did you learn") || lower.contains("my patterns") || lower.contains("insights")) {
            return ParsedFinanceCommand.ShowAnalyticsCommand(
                targetTab = "HABITS",
                responseMessage = "Here is what Dhan-OM Brain has learned about your spending patterns, recurring routines, and ML-detected optimizations."
            )
        }

        // 4b. Delete / remove transactions by talking
        parseDeleteCommand(trimmed, lower, transactions)?.let { return it }

        // 4c. Customize the app by talking: change theme, sort ledger
        parseCustomizeCommand(trimmed, lower)?.let { return it }

        // 5. Check for ML End-of-Month Forecast & Prediction Queries: "Predict my end of month balance", "Will I run out of money?", "Forecast"
        if (lower.contains("predict") || lower.contains("forecast") || lower.contains("run out of money") || lower.contains("end of month") || lower.contains("month end balance")) {
            val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(transactions)
            val response = buildString {
                append("🔮 **End-of-Month Predictive Cash Forecast**:\n\n")
                append("• **Current Burn Velocity**: ₹${String.format(Locale.US, "%.2f", forecast.dailyBurnRate)} / day (${forecast.daysPassedInMonth} of ${forecast.totalDaysInMonth} days passed).\n")
                append("• **Projected Month-End Expenses**: ₹${String.format(Locale.US, "%,.2f", forecast.projectedMonthEndExpenses)}.\n")
                append("• **Projected Net Savings**: ₹${String.format(Locale.US, "%,.2f", forecast.projectedMonthEndNetSavings)} (${forecast.projectedSavingsRate.toInt()}% savings rate).\n\n")
                append(forecast.forecastSummary)
            }
            return ParsedFinanceCommand.QueryResponseCommand("PREDICTION", response)
        }

        // 6. Check for Investment & Return Queries: "What's my current investment return?", "Show investments"
        if (lower.contains("investment return") || lower.contains("return on investment") || lower.contains("investments") || lower.contains("portfolio") || lower.contains("my stocks")) {
            val investments = transactions.filter { it.category == TransactionCategory.INVESTMENT }
            val investmentReturns = transactions.filter { it.category == TransactionCategory.INVESTMENT_RETURN }
            val totalInvested = investments.sumOf { it.amount }
            val totalReturns = investmentReturns.sumOf { it.amount }
            val netReturnPct = if (totalInvested > 0) ((totalReturns / totalInvested) * 100.0) else 0.0

            val response = buildString {
                append("📈 **Investment & Portfolio Performance**:\n\n")
                append("• **Total Capital Invested**: ₹${String.format(Locale.US, "%,.2f", totalInvested)} across ${investments.size} allocations.\n")
                append("• **Dividends & Capital Returns**: ₹${String.format(Locale.US, "%,.2f", totalReturns)}.\n")
                if (totalInvested > 0) {
                    append("• **Realized Yield**: ${String.format(Locale.US, "%.2f", netReturnPct)}%.\n\n")
                }
                append("💡 *Recommendation*: Maintain consistent DCA (Dollar Cost Averaging) into broad-market index ETFs while ensuring your liquid emergency fund is fully capitalized.")
            }
            return ParsedFinanceCommand.QueryResponseCommand("INVESTMENTS", response)
        }

        // 7. Check for Categorization Queries: "Categorize this transaction: Starbucks ₹6.50", "What category is Uber?"
        if (lower.contains("categorize") || lower.contains("what category") || lower.contains("which category")) {
            val cleanSubject = trimmed.replace(Regex("(?i)(?:categorize|this transaction|what category is|which category is|for|\\:)"), "").trim()
            val amount = extractAmount(cleanSubject)
            val prediction = PersonalFinanceMlEngine.predictCategory(cleanSubject, amount, transactions)

            val response = buildString {
                append("🏷️ **AI Category Prediction**:\n\n")
                append("• **Predicted Category**: **${prediction.category.displayName}**\n")
                append("• **Classification Confidence**: ${(prediction.confidence * 100).toInt()}%\n")
                append("• **Reasoning**: ${prediction.explanation}\n\n")
                append("Would you like me to log this transaction under **${prediction.category.displayName}**?")
            }
            return ParsedFinanceCommand.QueryResponseCommand("CATEGORIZE", response)
        }

        // 8. Check if this is an explicit expense logging command with an amount (e.g. "Spent ₹45 on groceries", "Paid 50 for dinner", "Bought coffee ₹6")
        val isExplicitExpenseLog = (lower.startsWith("spent ") || lower.startsWith("spend ") || lower.startsWith("paid ") || lower.startsWith("bought ") || lower.startsWith("charged ") || lower.startsWith("log ") || lower.startsWith("add expense") || lower.startsWith("expense ") || lower.startsWith("kharcha") || lower.startsWith("kharida") || lower.startsWith("kharch hua")) &&
                AMOUNT_PATTERN.matcher(trimmed).find()

        // 9. Check for Temporal / Specific Spending Queries: "Show me my spending last month", "How much did I spend this week?", "What did I spend on groceries?"
        if (!isExplicitExpenseLog && (lower.contains("spending") || lower.contains("how much did i spend") || lower.contains("how much have i spent") || lower.contains("what did i spend") || lower.contains("total expenses") || lower.contains("spending on") || (lower.contains("spent") && lower.contains("how much")))) {
            val response = handleSpendingQuery(lower, transactions, summary)
            return ParsedFinanceCommand.QueryResponseCommand("SPENDING_QUERY", response)
        }

        // 9. Check for Savings Rate & Health Score: "What is my savings rate?", "Health score"
        if (lower.contains("savings rate") || lower.contains("health score") || lower.contains("financial health") || lower.contains("how am i doing")) {
            val calcSummary = summary ?: FinancialAnalyticsEngine.calculateCashFlowSummary(transactions)
            val response = buildString {
                append("📊 **Financial Health Assessment**:\n\n")
                append("• **Financial Health Score**: **${calcSummary.healthScore}/100** (Grade: **${calcSummary.healthGrade}**)\n")
                append("• **Monthly Savings Rate**: **${String.format(Locale.US, "%.1f", calcSummary.savingsRate)}%** (Target: ≥20%)\n")
                append("• **Net Cash Flow**: **₹${String.format(Locale.US, "%,.2f", calcSummary.netCashFlow)}**\n")
                append("• **Needs / Wants / Savings**: ${calcSummary.needsPercentage.toInt()}% / ${calcSummary.wantsPercentage.toInt()}% / ${calcSummary.savingsPercentage.toInt()}%\n\n")
                append("💡 ${calcSummary.healthSummary}")
            }
            return ParsedFinanceCommand.QueryResponseCommand("HEALTH_SCORE", response)
        }

        // 10. Check for Goals Pacing & ETA: "When will I reach my goal?", "Goal progress"
        if (lower.contains("when will i reach") || lower.contains("goal progress") || lower.contains("vacation goal") || lower.contains("emergency goal")) {
            val calcSummary = summary ?: FinancialAnalyticsEngine.calculateCashFlowSummary(transactions)
            val projections = PersonalFinanceMlEngine.projectGoals(goals, calcSummary)
            val response = if (projections.isEmpty()) {
                "You haven't set up any savings goals yet. You can create one by typing 'Add goal Emergency Fund 5000'!"
            } else {
                buildString {
                    append("🎯 **Goal Completion Projections**:\n\n")
                    projections.forEach { p ->
                        val pct = if (p.targetAmount > 0) ((p.currentAmount / p.targetAmount) * 100).toInt() else 0
                        append("• **${p.goalTitle}**: ₹${String.format(Locale.US, "%,.0f", p.currentAmount)} / ₹${String.format(Locale.US, "%,.0f", p.targetAmount)} ($pct%)\n")
                        append("  - Projected Completion: **${p.projectedCompletionDate}** (${p.projectedDaysToComplete} days)\n")
                        append("  - ${p.recommendation}\n\n")
                    }
                }
            }
            return ParsedFinanceCommand.QueryResponseCommand("GOALS_PACING", response)
        }

        // 11. Check for Budget Setup: "Set budget 500 for dining", "Budget for groceries is 400"
        if (lower.startsWith("budget") || lower.contains("set budget") || lower.contains("create budget")) {
            val amount = extractAmount(lower)
            val category = detectCategory(lower)
            if (amount > 0) {
                return ParsedFinanceCommand.SetBudgetCommand(
                    category = category,
                    limit = amount,
                    confirmationMessage = "Updated your monthly budget for ${category.displayName} to ₹${String.format(Locale.US, "%.2f", amount)}."
                )
            }
        }

        // 12. Check for Goal Creation: "Add goal Vacation 3000", "New goal Emergency Fund 5000"
        if (lower.contains("goal") || lower.startsWith("save for") || lower.startsWith("target")) {
            val amount = extractAmount(lower)
            val title = extractGoalTitle(trimmed)
            if (amount > 0) {
                return ParsedFinanceCommand.AddGoalCommand(
                    goal = GoalEntity(
                        title = title,
                        targetAmount = amount,
                        currentAmount = 0.0,
                        categoryTag = "Target"
                    ),
                    confirmationMessage = "Created new financial goal '$title' with target amount of ₹${String.format(Locale.US, "%.2f", amount)}."
                )
            }
        }

        // 13. Check for Income Logging: "Salary 5000", "Received payment 450", "Freelance income ₹800"
        val isIncome = lower.contains("salary") || lower.contains("income") || lower.contains("received") || lower.contains("got paid") || lower.contains("freelance") || lower.contains("dividend") || lower.contains("earned") || lower.contains("aaya") || lower.contains("aay") || lower.contains("kamai") || lower.contains("kamaya") || lower.contains("tankhwah")
        val amount = extractAmount(lower)

        if (isIncome && amount > 0) {
            val category = if (lower.contains("freelance")) TransactionCategory.FREELANCE
            else if (lower.contains("dividend") || lower.contains("return")) TransactionCategory.INVESTMENT_RETURN
            else TransactionCategory.SALARY

            val title = if (lower.contains("freelance")) "Freelance Payment" else if (lower.contains("salary")) "Salary Deposit" else "Income Deposit"

            return ParsedFinanceCommand.AddTransactionCommand(
                transaction = TransactionEntity(
                    title = title,
                    amount = amount,
                    type = TransactionType.INCOME,
                    category = category,
                    necessity = ExpenseNecessity.NEED,
                    account = "Main Checking",
                    merchant = extractMerchant(trimmed)
                ),
                confirmationMessage = "Logged income of ₹${String.format(Locale.US, "%.2f", amount)} under ${category.displayName}."
            )
        }

        // 14. Check for Expense Logging: "Spent ₹45 on groceries at Trader Joe's", "Coffee ₹6.50", "Paid rent 1400"
        if (amount > 0) {
            val mlPredicted = PersonalFinanceMlEngine.predictCategory(trimmed, amount, transactions)
            val category = mlPredicted.category
            val merchant = extractMerchant(trimmed)
            val cleanTitle = buildTransactionTitle(trimmed, category, merchant)

            return ParsedFinanceCommand.AddTransactionCommand(
                transaction = TransactionEntity(
                    title = cleanTitle,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = category,
                    necessity = category.defaultNecessity,
                    account = if (amount > 200) "Main Checking" else "Credit Card",
                    merchant = merchant.ifBlank { cleanTitle }
                ),
                confirmationMessage = "Logged expense of ₹${String.format(Locale.US, "%.2f", amount)} for '$cleanTitle' (${category.displayName}) [Confidence: ${(mlPredicted.confidence * 100).toInt()}%]."
            )
        }

        // 15. Fallback general personal finance query
        return ParsedFinanceCommand.QueryResponseCommand(
            queryTopic = trimmed,
            responseText = generateHeuristicFinanceAdvice(lower)
        )
    }

    fun parseCommand(input: String): ParsedFinanceCommand {
        return parseCommandWithContext(input)
    }

    private fun parseCustomizeCommand(trimmed: String, lower: String): ParsedFinanceCommand? {
        // "change theme to ocean" / "apply emerald theme" / "switch to dark"
        if (lower.contains("theme")) {
            val themeWord = trimmed
                .replace(Regex("(?i).*?theme"), "")
                .replace(Regex("(?i)^\\s*(to|as)\\s*"), "")
                .trim()
                .lowercase()
            if (themeWord.isNotBlank()) {
                return ParsedFinanceCommand.CustomizeCommand(
                    action = "THEME",
                    payload = themeWord,
                    message = "Applying theme: $themeWord"
                )
            }
        }
        // "sort ledger by amount" / "sort transactions newest first"
        if (lower.contains("sort")) {
            val payload = when {
                lower.contains("amount") || lower.contains("highest") || lower.contains("lowest") -> "AMOUNT"
                lower.contains("category") -> "CATEGORY"
                lower.contains("oldest") -> "DATE_ASC"
                else -> "DATE"
            }
            return ParsedFinanceCommand.CustomizeCommand(
                action = "SORT",
                payload = payload,
                message = "Sorting ledger by $payload"
            )
        }
        return null
    }

    private fun parseDeleteCommand(
        trimmed: String,
        lower: String,
        transactions: List<TransactionEntity>
    ): ParsedFinanceCommand? {
        val isDelete = lower.contains("delete") || lower.contains("remove") ||
                lower.contains("undo") || lower.contains("erase") || lower.contains("cancel")
        if (!isDelete) return null
        // Don't hijack budget/goal/holding/chat/memory deletions
        if (lower.contains("budget") || lower.contains("goal") || lower.contains("holding") ||
            lower.contains("chat") || lower.contains("memory") || lower.contains("account")) return null

        val sorted = transactions.sortedByDescending { it.timestamp }

        fun money(v: Double) = "₹${String.format(Locale.US, "%.2f", v)}"

        // "delete all transactions / clear everything"
        if (lower.contains("all") || lower.contains("everything") || lower.contains("history")) {
            if (sorted.isEmpty()) return ParsedFinanceCommand.QueryResponseCommand("DELETE", "You have no transactions to delete yet.")
            return ParsedFinanceCommand.DeleteTransactionCommand(
                transactionsToDelete = sorted,
                confirmationMessage = "🧹 Deleted all ${sorted.size} transactions from your ledger."
            )
        }

        // "delete last transaction / undo / remove latest"
        if (lower.contains("last") || lower.contains("latest") || lower.contains("recent") || lower.contains("undo") || lower.contains("previous")) {
            val target = sorted.firstOrNull()
                ?: return ParsedFinanceCommand.QueryResponseCommand("DELETE", "You have no transactions to delete yet.")
            return ParsedFinanceCommand.DeleteTransactionCommand(
                transactionsToDelete = listOf(target),
                confirmationMessage = "🗑️ Deleted your last transaction: '${target.title}' (${money(target.amount)})."
            )
        }

        // "delete transaction 3 / remove entry #2 / delete number 1"
        val numMatch = Regex("(?i)(?:transaction|entry|record|#|number|no\\.?)\\s*(\\d{1,3})").find(trimmed)
            ?: Regex("(?i)(?:delete|remove|erase|cancel)\\s*(\\d{1,3})\\b").find(trimmed)
        if (numMatch != null) {
            val idx = numMatch.groupValues[1].toIntOrNull() ?: 0
            val target = sorted.getOrNull(idx - 1)
            return if (target != null) {
                ParsedFinanceCommand.DeleteTransactionCommand(
                    transactionsToDelete = listOf(target),
                    confirmationMessage = "🗑️ Deleted transaction #$idx: '${target.title}' (${money(target.amount)})."
                )
            } else {
                ParsedFinanceCommand.QueryResponseCommand("DELETE", "Couldn't find transaction #$idx — you have ${sorted.size} transaction(s).")
            }
        }

        // "delete swiggy / remove the rent entry"
        val subject = trimmed
            .replace(Regex("(?i)\\b(delete|remove|erase|cancel|undo|the|my|transaction|entry|record|expense|all)\\b"), " ")
            .trim()
        if (subject.isNotBlank() && subject.length >= 2) {
            val matches = sorted.filter {
                it.title.contains(subject, ignoreCase = true) ||
                        it.merchant.contains(subject, ignoreCase = true) ||
                        it.category.displayName.contains(subject, ignoreCase = true)
            }
            return when {
                matches.isEmpty() -> ParsedFinanceCommand.QueryResponseCommand("DELETE", "I couldn't find any transaction matching '$subject'.")
                matches.size == 1 -> {
                    val t = matches[0]
                    ParsedFinanceCommand.DeleteTransactionCommand(
                        transactionsToDelete = listOf(t),
                        confirmationMessage = "🗑️ Deleted '${t.title}' (${money(t.amount)})."
                    )
                }
                else -> {
                    val names = matches.take(3).joinToString { "'${it.title}'" } + if (matches.size > 3) "…" else ""
                    ParsedFinanceCommand.DeleteTransactionCommand(
                        transactionsToDelete = matches,
                        confirmationMessage = "🗑️ Deleted ${matches.size} matching transactions: $names."
                    )
                }
            }
        }

        return ParsedFinanceCommand.QueryResponseCommand(
            "DELETE",
            "Tell me exactly what to delete, e.g. 'delete last transaction', 'delete transaction 3', or 'delete Swiggy'."
        )
    }

    private fun handleSpendingQuery(
        lower: String,
        transactions: List<TransactionEntity>,
        summary: CashFlowSummary?
    ): String {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val now = Calendar.getInstance()

        // Check for specific category query (e.g. "groceries", "dining", "uber")
        val queriedCategory = TransactionCategory.entries.find { cat ->
            lower.contains(cat.name.lowercase()) || lower.contains(cat.displayName.lowercase())
        }

        if (queriedCategory != null) {
            val catExpenses = expenses.filter { it.category == queriedCategory }
            val totalCat = catExpenses.sumOf { it.amount }
            val avg = if (catExpenses.isNotEmpty()) totalCat / catExpenses.size else 0.0
            return buildString {
                append("🛒 **${queriedCategory.displayName} Spending Summary**:\n\n")
                append("• **Total Spent**: **₹${String.format(Locale.US, "%,.2f", totalCat)}** across ${catExpenses.size} transactions.\n")
                append("• **Average Ticket**: ₹${String.format(Locale.US, "%.2f", avg)} per transaction.\n")
                if (catExpenses.isNotEmpty()) {
                    append("• **Recent Transactions**:\n")
                    catExpenses.take(3).forEach {
                        append("  - ${it.title}: ₹${String.format(Locale.US, "%.2f", it.amount)}\n")
                    }
                }
            }
        }

        // Check for "last month"
        if (lower.contains("last month") || lower.contains("previous month")) {
            val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            val targetMonth = lastMonthCal.get(Calendar.MONTH)
            val targetYear = lastMonthCal.get(Calendar.YEAR)

            val lastMonthExpenses = expenses.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
            }

            val total = lastMonthExpenses.sumOf { it.amount }
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(lastMonthCal.time)

            return buildString {
                append("📅 **Spending for $monthName**:\n\n")
                append("• **Total Outflows**: **₹${String.format(Locale.US, "%,.2f", total)}** (${lastMonthExpenses.size} transactions).\n")
                val topCats = lastMonthExpenses.groupBy { it.category }
                    .mapValues { it.value.sumOf { tx -> tx.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)
                if (topCats.isNotEmpty()) {
                    append("• **Top Categories**:\n")
                    topCats.forEach { (cat, amt) ->
                        append("  - ${cat.displayName}: ₹${String.format(Locale.US, "%,.2f", amt)}\n")
                    }
                }
            }
        }

        // General current month summary
        val totalSpent = expenses.sumOf { it.amount }
        return buildString {
            append("💳 **Current Period Spending Overview**:\n\n")
            append("• **Total Outflows**: **₹${String.format(Locale.US, "%,.2f", totalSpent)}**\n")
            summary?.let {
                append("• **Net Cash Flow**: ₹${String.format(Locale.US, "%,.2f", it.netCashFlow)}\n")
                append("• **Daily Burn Velocity**: ₹${String.format(Locale.US, "%.2f", it.dailyBurnRate)} / day\n")
            }
            append("\nYou can ask specifically about any category (e.g. *'How much did I spend on dining?'*) or type *'Show cash flow chart'* for interactive visualization.")
        }
    }

    private val wordNumbers = mapOf(
        "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "eleven" to 11.0, "twelve" to 12.0, "thirteen" to 13.0, "fourteen" to 14.0,
        "fifteen" to 15.0, "sixteen" to 16.0, "seventeen" to 17.0, "eighteen" to 18.0,
        "nineteen" to 19.0, "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0,
        "fifty" to 50.0, "sixty" to 60.0, "seventy" to 70.0, "eighty" to 80.0, "ninety" to 90.0
    )

    private fun extractAmount(text: String): Double {
        val lower = text.lowercase()
        // "1.5 lakh" / "20 lakhs" / "2 crore" — Indian multipliers (before plain numbers)
        Regex("""(?i)(\d+(?:[.,]\d+)?)\s*(lakh|lakhs|lac|lacs)\b""").find(lower)?.let {
            val n = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            return n * 100_000.0
        }
        Regex("""(?i)(\d+(?:[.,]\d+)?)\s*(crore|crores|cr)\b""").find(lower)?.let {
            val n = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            return n * 10_000_000.0
        }
        // "62,000" / "₹450" / "5000 k" — plain numbers
        Regex("""(?i)(?:[₹$]|INR|USD|EUR)?\s*(\d{1,3}(?:,\d{3})+|\d+(?:\.\d+)?)\s*(k|thousand|grand)?\b""").find(lower)?.let {
            val n = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val suffix = it.groupValues[2]
            return when {
                suffix == "k" || suffix == "thousand" || suffix == "grand" -> n * 1000.0
                else -> n
            }
        }
        // word numbers: "twenty thousand" -> 20000, "fifty" -> 50, "two lakh" etc.
        Regex("""(?i)\b(twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)?\s*(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen)?\s*(hundred|thousand|lakh|lac|lakhs|lacs|crore|crores)?\b""").find(lower)?.let { m ->
            val tens = m.groupValues[1]
            val ones = m.groupValues[2]
            val scale = m.groupValues[3]
            if (tens.isEmpty() && ones.isEmpty() && scale.isEmpty()) return@let
            var value = 0.0
            if (tens.isNotEmpty()) value += wordNumbers[tens] ?: 0.0
            if (ones.isNotEmpty()) value += wordNumbers[ones] ?: 0.0
            value = when (scale) {
                "hundred" -> (if (value == 0.0) 1.0 else value) * 100.0
                "thousand" -> (if (value == 0.0) 1.0 else value) * 1000.0
                "lakh", "lac", "lakhs", "lacs" -> (if (value == 0.0) 1.0 else value) * 100_000.0
                "crore", "crores" -> (if (value == 0.0) 1.0 else value) * 10_000_000.0
                else -> value
            }
            if (value > 0) return value
        }
        return 0.0
    }

    private fun detectCategory(text: String): TransactionCategory {
        return when {
            text.contains("rent") || text.contains("apartment") || text.contains("mortgage") || text.contains("housing") -> TransactionCategory.HOUSING
            text.contains("grocer") || text.contains("food market") || text.contains("supermarket") || text.contains("trader joe") || text.contains("whole foods") || text.contains("safeway") || text.contains("walmart") || text.contains("bigbasket") || text.contains("dmart") || text.contains("blinkit") || text.contains("zepto") || text.contains("jiomart") || text.contains("food") || text.contains("rashan") -> TransactionCategory.GROCERIES
            text.contains("electric") || text.contains("water bill") || text.contains("wifi") || text.contains("internet") || text.contains("utility") || text.contains("utilities") || text.contains("power") || text.contains("gas bill") || text.contains("recharge") || text.contains("airtel") || text.contains("jio") || text.contains("bsnl") || text.contains("electricity") || text.contains("bill") -> TransactionCategory.UTILITIES
            text.contains("uber") || text.contains("lyft") || text.contains("gas") || text.contains("fuel") || text.contains("metro") || text.contains("transit") || text.contains("subway") || text.contains("bus") || text.contains("train") || text.contains("parking") || text.contains("ola") || text.contains("rapido") || text.contains("rickshaw") || text.contains("petrol") || text.contains("diesel") -> TransactionCategory.TRANSPORTATION
            text.contains("doctor") || text.contains("pharmacy") || text.contains("medicine") || text.contains("hospital") || text.contains("dental") || text.contains("health") -> TransactionCategory.HEALTHCARE
            text.contains("insurance") || text.contains("lic") || text.contains("policybazaar") || text.contains("term plan") -> TransactionCategory.INSURANCE
            text.contains("coffee") || text.contains("starbucks") || text.contains("cafe") || text.contains("lunch") || text.contains("dinner") || text.contains("breakfast") || text.contains("restaurant") || text.contains("dining") || text.contains("pizza") || text.contains("burger") || text.contains("bistro") || text.contains("takeout") || text.contains("swiggy") || text.contains("zomato") || text.contains("dunzo") || text.contains("dominos") || text.contains("khana") || text.contains("khaana") -> TransactionCategory.DINING
            text.contains("netflix") || text.contains("spotify") || text.contains("movie") || text.contains("cinema") || text.contains("game") || text.contains("concert") || text.contains("entertainment") || text.contains("streaming") || text.contains("hotstar") || text.contains("sonyliv") || text.contains("tatasky") -> TransactionCategory.ENTERTAINMENT
            text.contains("amazon") || text.contains("clothes") || text.contains("shopping") || text.contains("shoes") || text.contains("gadget") || text.contains("electronic") || text.contains("flipkart") || text.contains("myntra") || text.contains("ajio") || text.contains("meesho") -> TransactionCategory.SHOPPING
            text.contains("flight") || text.contains("hotel") || text.contains("airbnb") || text.contains("travel") || text.contains("vacation") || text.contains("irctc") || text.contains("indigo") || text.contains("air india") || text.contains("makemytrip") || text.contains("goibibo") -> TransactionCategory.TRAVEL
            text.contains("book") || text.contains("course") || text.contains("tuition") || text.contains("gym") || text.contains("fitness") || text.contains("coaching") -> TransactionCategory.EDUCATION
            text.contains("stock") || text.contains("etf") || text.contains("crypto") || text.contains("vanguard") || text.contains("invest") || text.contains("zerodha") || text.contains("groww") || text.contains("upstox") || text.contains("nifty") || text.contains("sensex") || text.contains("mutual fund") || text.contains("gold bond") || text.contains("sovereign gold") -> TransactionCategory.INVESTMENT
            text.contains("savings") || text.contains("emergency fund") || text.contains("hysa") || text.contains("ppf") || text.contains("epf") || text.contains("nps") -> TransactionCategory.SAVINGS_TRANSFER
            else -> TransactionCategory.OTHER
        }
    }

    private fun extractMerchant(text: String): String {
        val atPattern = Pattern.compile("(?i)(?:at|from|to|via)\\s+([A-Za-z0-9'&\\s]{2,25})(?:\\s+(?:for|on|in|\\$|\\d)|$)")
        val matcher = atPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    private fun buildTransactionTitle(text: String, category: TransactionCategory, merchant: String): String {
        if (merchant.isNotBlank()) return merchant
        val forPattern = Pattern.compile("(?i)(?:for|on)\\s+([A-Za-z0-9'&\\s]{2,25})")
        val matcher = forPattern.matcher(text)
        if (matcher.find()) {
            val item = matcher.group(1)?.trim()
            if (!item.isNullOrBlank()) return item.replaceFirstChar { it.uppercase() }
        }
        return category.displayName
    }

    private fun extractGoalTitle(text: String): String {
        val goalPattern = Pattern.compile("(?i)(?:goal|save for|target)\\s+([A-Za-z0-9'&\\s]{2,30})(?:\\s+(?:for|of|at|\\$|\\d)|$)")
        val matcher = goalPattern.matcher(text)
        if (matcher.find()) {
            val title = matcher.group(1)?.trim()
            if (!title.isNullOrBlank()) return title.replaceFirstChar { it.uppercase() }
        }
        return "Personal Savings Goal"
    }

    private fun generateHeuristicFinanceAdvice(query: String): String {
        return when {
            query.contains("save") || query.contains("saving") ->
                "To optimize savings, follow the 50/30/20 rule: 50% for Needs (rent, groceries), 30% for Wants (dining, hobbies), and 20% dedicated to Savings & Investments. Check the Flowchart tab to see your current distribution!"
            query.contains("invest") || query.contains("stock") ->
                "Building long-term wealth is most consistent through diversified, low-cost broad-market index funds (like total stock market or S&P 500 ETFs) after establishing a 3-6 month emergency cash reserve."
            query.contains("debt") || query.contains("loan") ->
                "For high-interest debt (e.g. credit cards >15%), the Avalanche method (highest interest first) mathematically saves the most in interest charges, while the Snowball method (smallest balance first) builds psychological momentum."
            query.contains("emergency") ->
                "A robust emergency fund covers 3 to 6 months of essential living expenses (Housing, Food, Utilities) kept in a liquid High-Yield Savings Account (HYSA)."
            else ->
                "I am monitoring your transactions and learning your cash flow habits with on-device ML. You can ask *'Predict my end of month balance'*, *'Show spending on groceries'*, or log transactions naturally!"
        }
    }
}
