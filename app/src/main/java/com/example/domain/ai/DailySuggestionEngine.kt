package com.example.domain.ai

import com.example.data.model.*
import com.example.domain.analytics.CashFlowSummary
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.analytics.PortfolioAnalyticsEngine
import com.example.domain.ml.PersonalFinanceMlEngine
import java.util.Calendar
import java.util.Locale

data class DailySuggestion(
    val id: String,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val category: String, // "SPENDING", "SAVINGS", "INVESTMENT", "GOAL", "TAX", "HABIT"
    val title: String,
    val message: String,
    val actionText: String? = null,
    val potentialSaving: Double = 0.0
)

object DailySuggestionEngine {

    fun generateDailySuggestions(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        holdings: List<PortfolioHoldingEntity>,
        summary: CashFlowSummary
    ): List<DailySuggestion> {
        val suggestions = mutableListOf<DailySuggestion>()
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)

        val budgetProgress = FinancialAnalyticsEngine.calculateBudgetProgress(budgets, transactions)
        budgetProgress.filter { it.isNearLimit }.forEach { bp ->
            suggestions.add(
                DailySuggestion(
                    id = "budget_${bp.category.name}",
                    priority = "HIGH",
                    category = "SPENDING",
                    title = "${bp.category.displayName} budget at ${(bp.progressFraction * 100).toInt()}%",
                    message = "You've spent ${formatMoney(bp.spentAmount)} of ${formatMoney(bp.monthlyLimit)}. Only ${formatMoney(bp.remainingAmount)} left this month. Consider pausing discretionary spending in this category.",
                    actionText = "Review spending",
                    potentialSaving = bp.remainingAmount
                )
            )
        }

        budgetProgress.filter { it.isOverBudget }.forEach { bp ->
            suggestions.add(
                DailySuggestion(
                    id = "overbudget_${bp.category.name}",
                    priority = "HIGH",
                    category = "SPENDING",
                    title = "${bp.category.displayName} budget exceeded!",
                    message = "You've overspent by ${formatMoney(bp.spentAmount - bp.monthlyLimit)}. Review recent transactions and adjust your budget or cut back next month.",
                    actionText = "View ledger",
                    potentialSaving = bp.spentAmount - bp.monthlyLimit
                )
            )
        }

        val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(transactions)
        if (forecast.isBurnVelocityHigh) {
            suggestions.add(
                DailySuggestion(
                    id = "burn_rate_alert",
                    priority = "HIGH",
                    category = "SPENDING",
                    title = "Spending burn rate is high",
                    message = "At ${formatMoney(forecast.dailyBurnRate)}/day, you're projected to spend ${formatMoney(forecast.projectedMonthEndExpenses)} by month-end. ${forecast.forecastSummary}",
                    actionText = "See forecast"
                )
            )
        } else if (forecast.projectedMonthEndNetSavings > 0 && dayOfMonth > 15) {
            suggestions.add(
                DailySuggestion(
                    id = "surplus_invest",
                    priority = "MEDIUM",
                    category = "INVESTMENT",
                    title = "Projected surplus available to invest",
                    message = "You're on track to save ${formatMoney(forecast.projectedMonthEndNetSavings)} this month. Consider investing the surplus in your SIP or a lump sum index fund.",
                    actionText = "Invest now",
                    potentialSaving = forecast.projectedMonthEndNetSavings
                )
            )
        }

        val diningTx = transactions.filter {
            it.category == TransactionCategory.DINING && it.type == TransactionType.EXPENSE
        }
        if (diningTx.size >= 4) {
            val monthlyDining = diningTx.sumOf { it.amount }
            val potentialSave = monthlyDining * 0.3
            suggestions.add(
                DailySuggestion(
                    id = "dining_optimize",
                    priority = "MEDIUM",
                    category = "HABIT",
                    title = "Optimize food delivery spending",
                    message = "You've ordered food ${diningTx.size} times totaling ${formatMoney(monthlyDining)}. Cooking 2 extra meals/week could save ~${formatMoney(potentialSave)}/month — redirect it to a goal!",
                    actionText = "Set dining cap",
                    potentialSaving = potentialSave
                )
            )
        }

        if (dayOfMonth <= 3) {
            val recurring = PersonalFinanceMlEngine.detectRecurringPatterns(transactions).filter { it.isSubscription }
            if (recurring.isNotEmpty()) {
                val totalMonthly = recurring.sumOf { it.averageAmount }
                suggestions.add(
                    DailySuggestion(
                        id = "subscription_audit",
                        priority = "MEDIUM",
                        category = "SPENDING",
                        title = "Review your ${recurring.size} active subscriptions",
                        message = "You have ${recurring.size} recurring subscriptions (~${formatMoney(totalMonthly)}/month, ${formatMoney(totalMonthly * 12)}/year). Cancel any unused ones to save.",
                        actionText = "Review subscriptions",
                        potentialSaving = totalMonthly * 0.2
                    )
                )
            }
        }

        goals.filter { !it.isCompleted }.forEach { goal ->
            val remaining = goal.targetAmount - goal.currentAmount
            val daysLeft = ((goal.targetDateMillis - System.currentTimeMillis()) / (1000L * 3600 * 24)).toInt()
            if (daysLeft > 0) {
                val requiredMonthly = (remaining / (daysLeft / 30.0))
                val currentMonthlyVelocity = summary.netCashFlow.coerceAtLeast(0.0) / goals.size.coerceAtLeast(1)
                if (requiredMonthly > currentMonthlyVelocity && daysLeft < 180) {
                    suggestions.add(
                        DailySuggestion(
                            id = "goal_accelerate_${goal.id}",
                            priority = "MEDIUM",
                            category = "GOAL",
                            title = "Accelerate: ${goal.title}",
                            message = "To reach this goal on time, you need ${formatMoney(requiredMonthly)}/month. Currently pacing at ${formatMoney(currentMonthlyVelocity)}/month. Increase deposits or extend the deadline.",
                            actionText = "Deposit now",
                            potentialSaving = requiredMonthly - currentMonthlyVelocity
                        )
                    )
                }
            }
        }

        if (dayOfWeek == Calendar.SUNDAY && holdings.isNotEmpty()) {
            val portfolioSummary = PortfolioAnalyticsEngine.calculatePortfolioSummary(holdings)
            val advice = PortfolioAnalyticsEngine.generateDiversificationAdvice(portfolioSummary)
            suggestions.add(
                DailySuggestion(
                    id = "portfolio_rebalance",
                    priority = "MEDIUM",
                    category = "INVESTMENT",
                    title = "Weekly portfolio review",
                    message = advice,
                    actionText = "View portfolio"
                )
            )
        }

        val emergencyGoal = goals.find { it.title.contains("emergency", ignoreCase = true) }
        if (emergencyGoal != null) {
            val pct = (emergencyGoal.currentAmount / emergencyGoal.targetAmount * 100).toInt()
            if (pct < 100) {
                val monthlyExpenses = summary.needsAmount + summary.wantsAmount
                val targetEmergency = monthlyExpenses * 6
                if (emergencyGoal.targetAmount < targetEmergency * 0.8) {
                    suggestions.add(
                        DailySuggestion(
                            id = "emergency_fund_check",
                            priority = "HIGH",
                            category = "SAVINGS",
                            title = "Build your emergency fund",
                            message = "Aim for 6 months of expenses (${formatMoney(targetEmergency)}) in your emergency fund. Currently at $pct% of ${formatMoney(emergencyGoal.targetAmount)}. This is your first line of defense against financial shocks.",
                            actionText = "Deposit to emergency fund",
                            potentialSaving = emergencyGoal.targetAmount - emergencyGoal.currentAmount
                        )
                    )
                }
            }
        }

        if (dayOfMonth <= 5 && now.get(Calendar.MONTH) in 0..2) {
            suggestions.add(
                DailySuggestion(
                    id = "tax_80c",
                    priority = "MEDIUM",
                    category = "TAX",
                    title = "Maximize Section 80C tax savings",
                    message = "You can save up to 46,800 in taxes by investing 1.5L under Section 80C (PPF, ELSS, NPS, life insurance). With the financial year ending March 31, review your investments now.",
                    actionText = "Plan tax savings"
                )
            )
        }

        val sipHoldings = holdings.filter { it.isSip }
        if (sipHoldings.isNotEmpty()) {
            val totalSip = sipHoldings.sumOf { it.sipMonthlyAmount }
            val sipRatio = if (summary.totalInflow > 0) (totalSip / summary.totalInflow) * 100.0 else 0.0
            if (sipRatio < 10.0 && summary.totalInflow > 0) {
                suggestions.add(
                    DailySuggestion(
                        id = "sip_increase",
                        priority = "LOW",
                        category = "INVESTMENT",
                        title = "Consider increasing your SIP",
                        message = "Your SIP commitment is ${String.format(Locale.US, "%.0f", sipRatio)}% of income (${formatMoney(totalSip)}/month). Financial planners recommend 15-20% for long-term wealth building. Even a 5% increase compounds significantly over time.",
                        actionText = "Increase SIP"
                    )
                )
            }
        }

        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            val weekendTx = transactions.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                val d = cal.get(Calendar.DAY_OF_WEEK)
                it.type == TransactionType.EXPENSE && (d == Calendar.SATURDAY || d == Calendar.SUNDAY)
            }
            if (weekendTx.isNotEmpty()) {
                val avgWeekendSpend = weekendTx.sumOf { it.amount } / (weekendTx.size.coerceAtLeast(1))
                suggestions.add(
                    DailySuggestion(
                        id = "weekend_awareness",
                        priority = "LOW",
                        category = "HABIT",
                        title = "Weekend spending reminder",
                        message = "Your average weekend spend is ${formatMoney(avgWeekendSpend)}. Set a weekend budget cap to enjoy guilt-free while staying on track.",
                        actionText = "Set weekend cap"
                    )
                )
            }
        }

        return suggestions.sortedByDescending { it.priority == "HIGH" }
    }

    fun generateGoalStrategy(goal: GoalEntity, summary: CashFlowSummary, allGoals: List<GoalEntity>): String {
        val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val daysLeft = ((goal.targetDateMillis - System.currentTimeMillis()) / (1000L * 3600 * 24)).toInt()
        val pct = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).toInt() else 0

        return buildString {
            append("Goal Strategy: ${goal.title}\n\n")
            append("Progress: $pct% (${formatMoney(goal.currentAmount)} / ${formatMoney(goal.targetAmount)})\n")
            append("Remaining: ${formatMoney(remaining)}\n")
            append("Time left: $daysLeft days\n\n")

            if (remaining <= 0) {
                append("Congratulations! You've achieved this goal. Consider setting a new one.")
                return@buildString
            }

            if (daysLeft > 0) {
                val requiredMonthly = remaining / (daysLeft / 30.0)
                append("To reach on time:\n")
                append("Required: ${formatMoney(requiredMonthly)}/month\n")
                append("Or ${formatMoney(requiredMonthly / 4)}/week\n\n")
            }

            append("Strategies to achieve this faster:\n")
            append("1. Automate a monthly transfer of ${formatMoney(remaining / maxOf(1, daysLeft / 30))} to this goal.\n")
            append("2. Redirect savings from dining/budget cuts (see daily suggestions).\n")
            append("3. Allocate bonuses, tax refunds, or freelance income directly here.\n")
            append("4. If invested, the corpus can grow via compounding — consider a liquid fund for short-term goals.\n")

            if (goal.categoryTag.equals("Travel", ignoreCase = true)) {
                append("5. Book flights/hotels early for better rates — save 15-20% on the total.\n")
            } else if (goal.categoryTag.equals("Security", ignoreCase = true)) {
                append("5. Keep this in a High-Yield Savings Account or liquid fund — earn 6-7% while staying accessible.\n")
            } else if (goal.categoryTag.equals("Property", ignoreCase = true)) {
                append("5. Park funds in arbitrage or liquid funds to earn while you save — avoid equity for <3 year horizons.\n")
            }
        }
    }

    private fun formatMoney(amount: Double): String {
        return "${Currency.INR.symbol}${String.format(Locale.US, "%,.0f", amount)}"
    }
}
