package com.example.domain.agent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.db.AppDatabase
import com.example.data.export.FileSaver
import com.example.data.model.*
import com.example.data.prefs.AppPrefs
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.brain.CloudBrainClient
import com.example.domain.ml.PersonalFinanceMlEngine
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The in-app "autopilot" agent: every month it automatically analyzes the
 * user's transactions, budgets, goals, loans and savings, writes a report,
 * saves it (chat + memory + Downloads), and nudges with a notification.
 *
 * This runs entirely ON the phone (no GitHub Actions needed) — a periodic
 * WorkManager job calls [run] daily, and [run] only produces a report when a
 * new calendar month has started (or when [force] is true).
 */
object MonthlyAutopilot {

    private const val PREFS = "dhanom_prefs"
    private const val KEY_LAST_MONTH = "last_monthly_analysis_month" // "yyyy-MM"
    private const val CHANNEL_ID = "dhanom_monthly_report"

    fun monthKey(now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun monthLabel(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(now))

    fun shouldRun(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_MONTH, "") != monthKey()
    }

    /** Runs the monthly analysis. Returns the report text, or null when there
     *  is nothing new this month (and force == false). */
    suspend fun run(context: Context, force: Boolean = false): String? {
        val app = context.applicationContext
        if (!force && !shouldRun(app)) return null

        val db = AppDatabase.getDatabase(app)
        val tx = db.transactionDao().getAllTransactions().first()
        val goals = db.goalDao().getAllGoals().first()
        val loans = db.loanDao().getAllLoans().first()
        val holdings = db.portfolioDao().getAllHoldings().first()

        // No data yet — don't "spend" this month's report on an empty account.
        if (tx.isEmpty() && !force) return null

        val report = buildReport(app, tx, goals, loans, holdings)

        // 1) Save into chat so it is waiting for the user when they open the app.
        db.chatMessageDao().insertMessage(
            ChatMessageEntity(
                sender = MessageSender.DHANOM_AI,
                messageText = report,
                timestamp = System.currentTimeMillis(),
                actionType = "MONTHLY_AUTOPILOT"
            )
        )

        // 2) Persist a compact memory of this month's key numbers.
        db.brainMemoryDao().insertMemory(
            BrainMemoryEntity(
                memoryType = MemoryType.FACT,
                topic = "Monthly Autopilot · ${monthLabel()}",
                description = shortDigest(tx, goals, loans),
                confidenceScore = 0.97f,
                lastObservedAt = System.currentTimeMillis(),
                actionSuggestion = "Generated automatically by the monthly analysis agent."
            )
        )

        // 3) Save the full report to Downloads/Dhan-OM.
        try {
            val f = File(app.cacheDir, "dhanom_monthly_${monthKey()}.md")
            f.writeText(report)
            FileSaver.saveToDownloads(app, f, "DhanOM_Monthly_Report_${monthKey()}.md", "text/markdown")
        } catch (_: Exception) {
        }

        // 4) Mark this month as analyzed.
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_MONTH, monthKey()).apply()

        // 5) Nudge with a notification (silently no-ops on devices without permission).
        notify(app, report.lines().take(3).joinToString("\n"))

        return report
    }

    private suspend fun buildReport(
        context: Context,
        tx: List<TransactionEntity>,
        goals: List<GoalEntity>,
        loans: List<LoanEntity>,
        holdings: List<PortfolioHoldingEntity>
    ): String {
        val now = Calendar.getInstance()
        val thisMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val lastMonthStart = Calendar.getInstance().apply {
            timeInMillis = thisMonthStart; add(Calendar.MONTH, -1)
        }.timeInMillis

        val thisMonthTx = tx.filter { it.timestamp >= thisMonthStart }
        val lastMonthTx = tx.filter { it.timestamp in lastMonthStart until thisMonthStart }

        val s = FinancialAnalyticsEngine.calculateCashFlowSummary(thisMonthTx)
        val sPrev = FinancialAnalyticsEngine.calculateCashFlowSummary(lastMonthTx)
        val top = FinancialAnalyticsEngine.calculateCategoryBreakdown(thisMonthTx).take(3)
        val subs = PersonalFinanceMlEngine.detectRecurringPatterns(thisMonthTx).filter { it.isSubscription }
        val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(thisMonthTx)
        val debt = loans.sumOf { it.outstandingAmount }
        val investments = holdings.sumOf { it.currentValue }

        val sb = StringBuilder()
        sb.append("📊 Dhan-OM Monthly Autopilot · ").append(monthLabel()).append("\n\n")
        sb.append("Here's your automatic monthly analysis (I run this myself every month):\n\n")

        sb.append("💰 Cash flow\n")
        sb.append("• Inflow: ₹").append(money(s.totalInflow)).append(" · Outflow: ₹").append(money(s.totalOutflow)).append("\n")
        sb.append("• Net: ₹").append(money(s.netCashFlow)).append(" · Savings rate: ").append(s.savingsRate.toInt()).append("%\n")
        if (lastMonthTx.isNotEmpty()) {
            val diff = s.netCashFlow - sPrev.netCashFlow
            sb.append("• vs last month: ").append(if (diff >= 0) "+" else "").append("₹").append(money(diff)).append("\n")
        }

        if (top.isNotEmpty()) {
            sb.append("\n📈 Top spending\n")
            top.forEach { sb.append("• ").append(it.category.displayName).append(": ₹").append(money(it.amount)).append(" (").append(it.percentage.toInt()).append("%)\n") }
        }

        if (subs.isNotEmpty()) {
            sb.append("\n🔁 Subscriptions to audit\n")
            subs.take(5).forEach { sb.append("• ").append(it.merchantOrTitle).append(" ~₹").append(money(it.averageAmount)).append("/").append(it.intervalDays.toInt()).append("d (≈₹").append(money(it.projectedAnnualCost)).append("/yr)\n") }
        }

        sb.append("\n🎯 Goals\n")
        val activeGoals = goals.filter { !it.isCompleted }
        if (activeGoals.isEmpty()) sb.append("• No active goals — consider setting one.\n")
        else activeGoals.take(4).forEach {
            val pct = if (it.targetAmount > 0) (it.currentAmount / it.targetAmount * 100).toInt() else 0
            sb.append("• ").append(it.title).append(": ").append(pct).append("% funded (₹").append(money(it.currentAmount)).append("/₹").append(money(it.targetAmount)).append(")\n")
        }

        sb.append("\n🏦 Money\n")
        sb.append("• Investments: ₹").append(money(investments)).append(" · Debt: ₹").append(money(debt)).append("\n")
        sb.append("• Projected month-end net: ₹").append(money(forecast.projectedMonthEndNetSavings)).append(" · Daily burn: ₹").append(money(forecast.dailyBurnRate)).append("\n")

        sb.append("\n✅ Suggested actions\n")
        val actions = suggestedActions(s, debt, subs)
        actions.forEach { sb.append("• ").append(it).append("\n") }

        // Optional: a cloud brain adds a short narrative (best accuracy).
        val cloudExtra = cloudInsight(context, sb.toString())
        if (cloudExtra != null) {
            sb.append("\n🧠 Cloud brain view\n").append(cloudExtra).append("\n")
        }

        return sb.toString().trim()
    }

    private suspend fun cloudInsight(context: Context, numbers: String): String? {
        return try {
            val ai = AppPrefs(context).aiSettings.value
            if (!ai.cloudEnabled || ai.cloudApiKey.isBlank()) return null
            val prompt = "You are Dhan-OM. Here are the user's monthly numbers:\n\n${numbers.take(2500)}\n\n" +
                "Write a SHORT 3-bullet review of this month (what went well, what to watch, one action). Keep it in ₹ and plain language."
            CloudBrainClient().generate(ai.cloudEndpoint, ai.cloudApiKey, ai.cloudModel, "You are Dhan-OM, a precise personal finance AI.", prompt)
        } catch (_: Exception) {
            null
        }
    }

    private fun shortDigest(tx: List<TransactionEntity>, goals: List<GoalEntity>, loans: List<LoanEntity>): String {
        val s = FinancialAnalyticsEngine.calculateCashFlowSummary(tx)
        return "Auto monthly review: inflow ₹${money(s.totalInflow)}, outflow ₹${money(s.totalOutflow)}, " +
            "savings rate ${s.savingsRate.toInt()}%, debt ₹${money(loans.sumOf { it.outstandingAmount })}, " +
            "${goals.count { !it.isCompleted }} active goals."
    }

    private fun suggestedActions(s: com.example.domain.analytics.CashFlowSummary, debt: Double, subs: List<com.example.domain.ml.RecurringPattern>): List<String> {
        val actions = mutableListOf<String>()
        if (s.savingsRate < 20) actions.add("Savings rate is ${s.savingsRate.toInt()}% — automate at least 20% of inflow to a goal this month.")
        else actions.add("Savings rate ${s.savingsRate.toInt()}% is healthy — keep it up.")
        if (debt > 0) actions.add("Pay down high-interest debt first (₹${money(debt)} outstanding).")
        if (subs.isNotEmpty()) actions.add("Audit ${subs.size} subscription(s) — cancel the unused ones.")
        if (actions.isEmpty()) actions.add("No urgent actions — stay the course.")
        return actions
    }

    private fun notify(context: Context, body: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Monthly Analysis", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pending = intent?.let {
                PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("Dhan-OM monthly analysis is ready")
                .setContentText(body.take(120))
                .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(400)))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            NotificationManagerCompat.from(context).notify(2001, n)
        } catch (_: Exception) {
            // notifications are best-effort
        }
    }

    private fun money(v: Double): String = String.format(Locale.US, "%,.0f", v)
}
