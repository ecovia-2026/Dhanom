package com.example

import com.example.data.model.*
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.ml.PersonalFinanceMlEngine
import com.example.domain.nlp.NaturalLanguageFinanceParser
import com.example.domain.nlp.ParsedFinanceCommand
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class FinancialLogicTest {

    @Test
    fun testCashFlowSummaryCalculation() {
        val transactions = listOf(
            TransactionEntity(
                title = "Primary Salary",
                amount = 5000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                necessity = ExpenseNecessity.NEED
            ),
            TransactionEntity(
                title = "Apartment Rent",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HOUSING,
                necessity = ExpenseNecessity.NEED
            ),
            TransactionEntity(
                title = "Fine Dining",
                amount = 300.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.DINING,
                necessity = ExpenseNecessity.WANT
            ),
            TransactionEntity(
                title = "Index Fund ETF",
                amount = 1000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.INVESTMENT,
                necessity = ExpenseNecessity.SAVINGS
            )
        )

        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(transactions)

        assertEquals(5000.0, summary.totalInflow, 0.01)
        assertEquals(2800.0, summary.totalOutflow, 0.01)
        assertEquals(2200.0, summary.netCashFlow, 0.01)
        assertEquals(1500.0, summary.needsAmount, 0.01)
        assertEquals(300.0, summary.wantsAmount, 0.01)
        assertEquals(1000.0, summary.savingsAmount, 0.01)
        assertTrue("Savings rate should be > 20%", summary.savingsRate >= 20.0)
        assertTrue("Health score should be >= 70", summary.healthScore >= 70)
    }

    @Test
    fun testZeroIncomeEdgeCaseDoesNotDivideByZero() {
        val expensesOnly = listOf(
            TransactionEntity(
                title = "Groceries",
                amount = 250.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES,
                necessity = ExpenseNecessity.NEED
            )
        )

        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(expensesOnly)

        assertEquals(0.0, summary.totalInflow, 0.01)
        assertEquals(250.0, summary.totalOutflow, 0.01)
        assertEquals(-250.0, summary.netCashFlow, 0.01)
        assertEquals(0.0, summary.savingsRate, 0.01)
        assertTrue(summary.healthScore in 0..100)
    }

    @Test
    fun testFlowchartDataGeneration() {
        val transactions = listOf(
            TransactionEntity(
                title = "Tech Salary",
                amount = 4000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY
            ),
            TransactionEntity(
                title = "Groceries",
                amount = 400.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES,
                necessity = ExpenseNecessity.NEED
            )
        )

        val flowchart = FinancialAnalyticsEngine.generateCashFlowchartData(transactions)

        assertNotNull(flowchart)
        assertTrue(flowchart.incomeNodes.isNotEmpty())
        assertTrue(flowchart.allocationNodes.isNotEmpty())
        assertTrue(flowchart.categoryNodes.isNotEmpty())
        assertEquals(4000.0, flowchart.totalInflow, 0.01)
        assertEquals(400.0, flowchart.totalOutflow, 0.01)
    }

    @Test
    fun testMlCategoryPredictionLearnsFromHistory() {
        val history = listOf(
            TransactionEntity(
                title = "Blue Bottle Coffee",
                amount = 6.50,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.DINING,
                merchant = "Blue Bottle Coffee"
            ),
            TransactionEntity(
                title = "Trader Joe's Market",
                amount = 65.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES,
                merchant = "Trader Joe's"
            )
        )

        val prediction1 = PersonalFinanceMlEngine.predictCategory("Blue Bottle", 6.50, history)
        assertEquals(TransactionCategory.DINING, prediction1.category)
        assertTrue(prediction1.confidence >= 0.80)

        val prediction2 = PersonalFinanceMlEngine.predictCategory("Trader Joe's groceries", 45.0, history)
        assertEquals(TransactionCategory.GROCERIES, prediction2.category)
    }

    @Test
    fun testMlCashFlowForecastBurnRate() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val transactions = listOf(
            TransactionEntity(
                title = "Salary",
                amount = 6000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                timestamp = nowCal.timeInMillis
            ),
            TransactionEntity(
                title = "Mid-month Expenses",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HOUSING,
                timestamp = nowCal.timeInMillis
            )
        )

        val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(transactions, nowCal)

        assertEquals(15, forecast.daysPassedInMonth)
        assertEquals(100.0, forecast.dailyBurnRate, 0.01)
        assertTrue("Projected expenses should be ~3000", forecast.projectedMonthEndExpenses in 2800.0..3200.0)
        assertTrue("Projected net savings should be positive", forecast.projectedMonthEndNetSavings > 0)
    }

    @Test
    fun testMlAnomalyDetectionZScore() {
        val normalTx = (1..5).map {
            TransactionEntity(
                title = "Daily Lunch",
                amount = 15.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.DINING
            )
        }
        val spikeTx = TransactionEntity(
            title = "Luxury Steakhouse",
            amount = 280.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.DINING
        )

        val all = normalTx + spikeTx
        val anomalies = PersonalFinanceMlEngine.detectAnomalies(all)

        assertTrue("Should detect luxury steakhouse as anomaly", anomalies.isNotEmpty())
        assertEquals("Luxury Steakhouse", anomalies.first().transaction.title)
        assertTrue("Z-score should be > 2.0", anomalies.first().zScore >= 2.0)
    }

    @Test
    fun testMlRecurringSubscriptionDetection() {
        val now = System.currentTimeMillis()
        val oneMonth = 30L * 24 * 3600 * 1000
        val netflix = listOf(
            TransactionEntity(
                title = "Netflix",
                amount = 19.99,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                merchant = "Netflix",
                timestamp = now - (oneMonth * 2)
            ),
            TransactionEntity(
                title = "Netflix",
                amount = 19.99,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                merchant = "Netflix",
                timestamp = now - oneMonth
            ),
            TransactionEntity(
                title = "Netflix",
                amount = 19.99,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                merchant = "Netflix",
                timestamp = now
            )
        )

        val recurring = PersonalFinanceMlEngine.detectRecurringPatterns(netflix)

        assertTrue(recurring.isNotEmpty())
        val sub = recurring.first()
        assertTrue(sub.isSubscription)
        assertEquals(19.99, sub.averageAmount, 0.01)
        assertTrue("Projected annual cost should be ~240", sub.projectedAnnualCost in 230.0..250.0)
    }

    @Test
    fun testNluSpendingQueries() {
        val transactions = listOf(
            TransactionEntity(
                title = "Whole Foods",
                amount = 85.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES
            ),
            TransactionEntity(
                title = "Safeway",
                amount = 65.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES
            )
        )

        val command = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = "Show me my spending on groceries",
            transactions = transactions
        )

        assertTrue(command is ParsedFinanceCommand.QueryResponseCommand)
        val queryCmd = command as ParsedFinanceCommand.QueryResponseCommand
        assertTrue(queryCmd.responseText.contains("150.00") || queryCmd.responseText.contains("150"))
    }

    @Test
    fun testNluInvestmentReturnQuery() {
        val transactions = listOf(
            TransactionEntity(
                title = "Vanguard Total Stock ETF",
                amount = 1200.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.INVESTMENT
            ),
            TransactionEntity(
                title = "Quarterly Dividend",
                amount = 48.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.INVESTMENT_RETURN
            )
        )

        val command = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = "What's my current investment return?",
            transactions = transactions
        )

        assertTrue(command is ParsedFinanceCommand.QueryResponseCommand)
        val queryCmd = command as ParsedFinanceCommand.QueryResponseCommand
        assertTrue(queryCmd.responseText.contains("1,200.00") || queryCmd.responseText.contains("1200"))
        assertTrue(queryCmd.responseText.contains("48.00") || queryCmd.responseText.contains("48"))
    }

    @Test
    fun testNluCategorizeQuery() {
        val command = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = "Categorize this transaction: Starbucks $6.50"
        )

        assertTrue(command is ParsedFinanceCommand.QueryResponseCommand)
        val queryCmd = command as ParsedFinanceCommand.QueryResponseCommand
        assertTrue(queryCmd.responseText.contains("Dining"))
    }

    @Test
    fun testNluPredictMonthEndBalance() {
        val command = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = "Predict my end of month balance"
        )

        assertTrue(command is ParsedFinanceCommand.QueryResponseCommand)
        val queryCmd = command as ParsedFinanceCommand.QueryResponseCommand
        assertTrue(queryCmd.responseText.contains("End-of-Month Predictive Cash Forecast"))
    }

    @Test
    fun testHinglishSpendingQueryUsesLocalMath() {
        val transactions = listOf(
            TransactionEntity(
                title = "BigBasket",
                amount = 3200.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.GROCERIES
            )
        )
        val command = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = "kitna kharcha groceries",
            transactions = transactions
        )
        assertTrue(command is ParsedFinanceCommand.QueryResponseCommand)
        val q = command as ParsedFinanceCommand.QueryResponseCommand
        assertTrue(q.responseText.contains("3,200") || q.responseText.contains("3200"))
    }

    @Test
    fun testNaturalLanguageExpenseLogging() {
        val command = NaturalLanguageFinanceParser.parseCommand("Spent $45 on groceries at Trader Joe's")

        assertTrue(command is ParsedFinanceCommand.AddTransactionCommand)
        val addTx = command as ParsedFinanceCommand.AddTransactionCommand
        assertEquals(45.0, addTx.transaction.amount, 0.01)
        assertEquals(TransactionCategory.GROCERIES, addTx.transaction.category)
        assertEquals(TransactionType.EXPENSE, addTx.transaction.type)
    }

    @Test
    fun testNaturalLanguageIncomeLogging() {
        val command = NaturalLanguageFinanceParser.parseCommand("Received freelance payment $850")

        assertTrue(command is ParsedFinanceCommand.AddTransactionCommand)
        val addTx = command as ParsedFinanceCommand.AddTransactionCommand
        assertEquals(850.0, addTx.transaction.amount, 0.01)
        assertEquals(TransactionCategory.FREELANCE, addTx.transaction.category)
        assertEquals(TransactionType.INCOME, addTx.transaction.type)
    }

    @Test
    fun testNaturalLanguageNavigationCommand() {
        val command = NaturalLanguageFinanceParser.parseCommand("Show cash flow flowchart")

        assertTrue(command is ParsedFinanceCommand.ShowAnalyticsCommand)
        val navCmd = command as ParsedFinanceCommand.ShowAnalyticsCommand
        assertEquals("FLOWCHART", navCmd.targetTab)
    }
}
