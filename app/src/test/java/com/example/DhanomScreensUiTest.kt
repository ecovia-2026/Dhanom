package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.*
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.analytics.PortfolioAnalyticsEngine
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerSort
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DhanomScreensUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = 1L,
            title = "Monthly Salary Direct Deposit",
            amount = 5000.0,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            necessity = ExpenseNecessity.NEED,
            account = "Main Checking",
            merchant = "TechCorp",
            timestamp = System.currentTimeMillis()
        ),
        TransactionEntity(
            id = 2L,
            title = "Whole Foods Organic Market",
            amount = 120.0,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            necessity = ExpenseNecessity.NEED,
            account = "Credit Card",
            merchant = "Whole Foods",
            timestamp = System.currentTimeMillis()
        )
    )

    @Test
    fun testDashboardScreenRendersSuccessfully() {
        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(sampleTransactions)
        val flowchart = FinancialAnalyticsEngine.generateCashFlowchartData(sampleTransactions)
        val categoryExpenses = FinancialAnalyticsEngine.calculateCategoryBreakdown(sampleTransactions)

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    summary = summary,
                    flowchartData = flowchart,
                    categoryExpenses = categoryExpenses,
                    recentTransactions = sampleTransactions,
                    onNavigateTab = {},
                    onAddTransactionClick = {},
                    onTransactionClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_screen").assertExists()
        composeTestRule.onNodeWithTag("bento_insight_card").assertExists()
    }

    @Test
    fun testFlowAnalyticsScreenRendersSuccessfully() {
        val flowchart = FinancialAnalyticsEngine.generateCashFlowchartData(sampleTransactions)
        val categoryExpenses = FinancialAnalyticsEngine.calculateCategoryBreakdown(sampleTransactions)
        val trends = FinancialAnalyticsEngine.calculateDailyTrends(sampleTransactions)

        composeTestRule.setContent {
            MyApplicationTheme {
                FlowAnalyticsScreen(
                    flowchartData = flowchart,
                    categoryExpenses = categoryExpenses,
                    dailyTrends = trends
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("flow_analytics_screen").assertExists()
        composeTestRule.onNodeWithTag("cash_flowchart_card").assertExists()
    }

    @Test
    fun testLedgerTableScreenRendersSuccessfully() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LedgerTableScreen(
                    transactions = sampleTransactions,
                    searchQuery = "",
                    filterCategory = null,
                    filterType = null,
                    sortOrder = LedgerSort.DATE_DESC,
                    onSearchChange = {},
                    onFilterCategoryChange = {},
                    onFilterTypeChange = {},
                    onSortChange = {},
                    onAddTransactionClick = {},
                    onEditTransaction = {},
                    onDeleteTransaction = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ledger_table_screen").assertExists()
    }

    @Test
    fun testDhanomChatScreenRendersSuccessfully() {
        val messages = listOf(
            ChatMessageEntity(
                id = 1L,
                sender = MessageSender.DHANOM_AI,
                messageText = "Hello! I am Dhanom AI.",
                timestamp = System.currentTimeMillis()
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                DhanomChatScreen(
                    messages = messages,
                    memories = emptyList(),
                    insights = emptyList(),
                    isChatLoading = false,
                    onSendMessage = {},
                    onAttachFile = {},
                    onQuickAdd = {},
                    onDeleteLast = {},
                    onClearChat = {},
                    onRefreshBrain = {},
                    onClearBrain = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dhanom_chat_screen").assertIsDisplayed()
    }

    @Test
    fun testBudgetsGoalsScreenRendersSuccessfully() {
        val budgets = listOf(
            BudgetEntity(
                id = 1L,
                category = TransactionCategory.GROCERIES,
                monthlyLimit = 400.0,
                periodMonth = 8,
                periodYear = 2026
            )
        )
        val goals = listOf(
            GoalEntity(
                id = 1L,
                title = "Emergency Fund",
                targetAmount = 10000.0,
                currentAmount = 6400.0,
                targetDateMillis = System.currentTimeMillis() + (90L * 86400000),
                categoryTag = "Security"
            )
        )
        val progress = FinancialAnalyticsEngine.calculateBudgetProgress(budgets, sampleTransactions)

        composeTestRule.setContent {
            MyApplicationTheme {
                BudgetsGoalsScreen(
                    budgetProgressList = progress,
                    rawBudgets = budgets,
                    goals = goals,
                    onAddBudgetClick = {},
                    onAddGoalClick = {},
                    onDepositGoalClick = {},
                    onDeleteBudget = {},
                    onDeleteGoal = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("budgets_goals_screen").assertIsDisplayed()
    }

    @Test
    fun testPortfolioScreenRendersSuccessfully() {
        val holdings = listOf(
            PortfolioHoldingEntity(
                id = 1L,
                instrumentName = "Nifty 50 Index Fund",
                symbol = "NIFTYBEES",
                assetClass = AssetClass.INDEX_ETF,
                region = InvestmentRegion.INDIA,
                quantity = 120.0,
                avgBuyPrice = 210.0,
                currentPrice = 245.0,
                investedAmount = 25200.0,
                currentValue = 29400.0,
                isSip = true,
                sipMonthlyAmount = 5000.0
            )
        )
        val summary = PortfolioAnalyticsEngine.calculatePortfolioSummary(holdings)
        val allocations = PortfolioAnalyticsEngine.calculateAssetClassAllocations(holdings)

        composeTestRule.setContent {
            MyApplicationTheme {
                PortfolioScreen(
                    holdings = holdings,
                    portfolioSummary = summary,
                    assetAllocations = allocations,
                    onAddHoldingClick = {},
                    onEditHolding = {},
                    onDeleteHolding = {},
                    onUpdatePrices = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("portfolio_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_summary_card").assertExists()
    }

    @Test
    fun testReportsExportScreenRendersSuccessfully() {
        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(sampleTransactions)

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsExportScreen(
                    transactions = sampleTransactions,
                    holdings = emptyList(),
                    budgets = emptyList(),
                    goals = emptyList(),
                    memories = emptyList(),
                    chatMessages = emptyList(),
                    cashFlowSummary = summary,
                    onExportBackup = {},
                    onImportBackup = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reports_export_screen").assertIsDisplayed()
    }
}
