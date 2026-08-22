package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupBundle(
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val memories: List<BrainMemoryEntity>,
    val chatMessages: List<ChatMessageEntity>,
    val holdings: List<PortfolioHoldingEntity>,
    val loans: List<LoanEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val recurring: List<RecurringTransactionEntity> = emptyList(),
    // device-transfer extras (full context, not just financial data)
    val prefs: Map<String, String> = emptyMap()
)

object ExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportTransactionsCsv(context: Context, transactions: List<TransactionEntity>): File {
        val fileName = "dhanom_transactions_${fileDateFormat.format(Date())}.csv"
        val file = File(context.cacheDir, fileName)
        FileWriter(file).use { writer ->
            writer.write("Date,Title,Amount,Type,Category,Necessity,Account,Merchant,Currency,Notes\n")
            transactions.forEach { tx ->
                writer.write(
                    listOf(
                        csvSafe(dateFormat.format(Date(tx.timestamp))),
                        csvSafe(tx.title),
                        tx.amount,
                        tx.type.name,
                        tx.category.displayName,
                        tx.necessity.name,
                        csvSafe(tx.account),
                        csvSafe(tx.merchant),
                        tx.currency,
                        csvSafe(tx.notes)
                    ).joinToString(",") { it.toString() }
                )
                writer.write("\n")
            }
        }
        return file
    }

    fun exportPortfolioCsv(context: Context, holdings: List<PortfolioHoldingEntity>): File {
        val fileName = "dhanom_portfolio_${fileDateFormat.format(Date())}.csv"
        val file = File(context.cacheDir, fileName)
        FileWriter(file).use { writer ->
            writer.write("Instrument,Symbol,AssetClass,Region,Quantity,AvgBuyPrice,CurrentPrice,Invested,CurrentValue,PnL,PnL%,Currency\n")
            holdings.forEach { h ->
                writer.write(
                    listOf(
                        csvSafe(h.instrumentName),
                        csvSafe(h.symbol),
                        h.assetClass.displayName,
                        h.region.displayName,
                        h.quantity,
                        h.avgBuyPrice,
                        h.currentPrice,
                        h.investedAmount,
                        h.currentValue,
                        h.unrealizedPnl,
                        String.format(Locale.US, "%.2f", h.unrealizedPnlPercent),
                        h.currency
                    ).joinToString(",") { it.toString() }
                )
                writer.write("\n")
            }
        }
        return file
    }

    fun exportPdfReport(
        context: Context,
        summary: com.example.domain.analytics.CashFlowSummary,
        transactions: List<TransactionEntity>,
        holdings: List<PortfolioHoldingEntity>,
        goals: List<GoalEntity>
    ): File {
        val fileName = "dhanom_report_${fileDateFormat.format(Date())}.pdf"
        val file = File(context.cacheDir, fileName)

        val pdfContent = buildPdfContent(summary, transactions, holdings, goals)
        val header = buildPdfHeader(pdfContent.toByteArray(Charsets.ISO_8859_1).size)
        file.outputStream().use { out ->
            out.write(header.toByteArray(Charsets.ISO_8859_1))
            out.write(pdfContent.toByteArray(Charsets.ISO_8859_1))
            out.write("endstream\nendobj\nstartxref\n0\n%%EOF".toByteArray(Charsets.ISO_8859_1))
        }
        return file
    }

    fun exportBackupJson(context: Context, bundle: BackupBundle): File {
        val fileName = "dhanom_backup_${fileDateFormat.format(Date())}.json"
        val file = File(context.cacheDir, fileName)
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val txArray = JSONArray()
        bundle.transactions.forEach { tx ->
            val o = JSONObject()
            o.put("title", tx.title); o.put("amount", tx.amount); o.put("type", tx.type.name)
            o.put("category", tx.category.name); o.put("necessity", tx.necessity.name)
            o.put("account", tx.account); o.put("merchant", tx.merchant)
            o.put("timestamp", tx.timestamp); o.put("notes", tx.notes)
            o.put("tags", tx.tags); o.put("isRecurring", tx.isRecurring)
            o.put("currency", tx.currency)
            txArray.put(o)
        }
        root.put("transactions", txArray)

        val budgetArray = JSONArray()
        bundle.budgets.forEach { b ->
            val o = JSONObject()
            o.put("category", b.category.name); o.put("monthlyLimit", b.monthlyLimit)
            o.put("periodMonth", b.periodMonth); o.put("periodYear", b.periodYear)
            o.put("alertThreshold", b.alertThreshold)
            budgetArray.put(o)
        }
        root.put("budgets", budgetArray)

        val goalArray = JSONArray()
        bundle.goals.forEach { g ->
            val o = JSONObject()
            o.put("title", g.title); o.put("targetAmount", g.targetAmount)
            o.put("currentAmount", g.currentAmount); o.put("targetDateMillis", g.targetDateMillis)
            o.put("categoryTag", g.categoryTag); o.put("isCompleted", g.isCompleted)
            goalArray.put(o)
        }
        root.put("goals", goalArray)

        val memArray = JSONArray()
        bundle.memories.forEach { m ->
            val o = JSONObject()
            o.put("memoryType", m.memoryType.name); o.put("topic", m.topic)
            o.put("description", m.description); o.put("confidenceScore", m.confidenceScore)
            o.put("detectedCount", m.detectedCount); o.put("lastObservedAt", m.lastObservedAt)
            o.put("actionSuggestion", m.actionSuggestion)
            memArray.put(o)
        }
        root.put("memories", memArray)

        val chatArray = JSONArray()
        bundle.chatMessages.forEach { c ->
            val o = JSONObject()
            o.put("sender", c.sender.name); o.put("messageText", c.messageText)
            o.put("timestamp", c.timestamp); o.put("actionType", c.actionType ?: "")
            o.put("actionPayload", c.actionPayload ?: "")
            chatArray.put(o)
        }
        root.put("chatMessages", chatArray)

        val holdingArray = JSONArray()
        bundle.holdings.forEach { h ->
            val o = JSONObject()
            o.put("instrumentName", h.instrumentName); o.put("symbol", h.symbol)
            o.put("assetClass", h.assetClass.name); o.put("region", h.region.name)
            o.put("quantity", h.quantity); o.put("avgBuyPrice", h.avgBuyPrice)
            o.put("currentPrice", h.currentPrice); o.put("investedAmount", h.investedAmount)
            o.put("currentValue", h.currentValue); o.put("currency", h.currency)
            o.put("purchaseDate", h.purchaseDate); o.put("notes", h.notes)
            o.put("isSip", h.isSip); o.put("sipMonthlyAmount", h.sipMonthlyAmount)
            holdingArray.put(o)
        }
        root.put("holdings", holdingArray)

        // Loans
        val loanArray = JSONArray()
        bundle.loans.forEach { l ->
            val o = JSONObject()
            o.put("title", l.title); o.put("type", l.type.name)
            o.put("principalAmount", l.principalAmount); o.put("outstandingAmount", l.outstandingAmount)
            o.put("interestRate", l.interestRate); o.put("monthlyEmi", l.monthlyEmi)
            o.put("notes", l.notes)
            loanArray.put(o)
        }
        root.put("loans", loanArray)

        // Scheduled / recurring tasks (the brain's "daily fix task" memory)
        val taskArray = JSONArray()
        bundle.tasks.forEach { t ->
            val o = JSONObject()
            o.put("title", t.title); o.put("amount", t.amount)
            o.put("recurrence", t.recurrence.name); o.put("nextDueDateMillis", t.nextDueDateMillis)
            o.put("expiresAtMillis", t.expiresAtMillis); o.put("category", t.category)
            o.put("notes", t.notes); o.put("timesDone", t.timesDone)
            o.put("isActive", t.isActive)
            taskArray.put(o)
        }
        root.put("tasks", taskArray)

        // Accounts (money accounts for mapping)
        val accountArray = JSONArray()
        bundle.accounts.forEach { a ->
            val o = JSONObject()
            o.put("name", a.name); o.put("type", a.type.name)
            o.put("initialBalance", a.initialBalance); o.put("colorArgb", a.colorArgb)
            o.put("icon", a.icon); o.put("isArchived", a.isArchived)
            accountArray.put(o)
        }
        root.put("accounts", accountArray)

        // Recurring transactions
        val recurringArray = JSONArray()
        bundle.recurring.forEach { r ->
            val o = JSONObject()
            o.put("title", r.title); o.put("amount", r.amount); o.put("type", r.type.name)
            o.put("category", r.category.name); o.put("necessity", r.necessity.name)
            o.put("account", r.account); o.put("recurrence", r.recurrence.name)
            o.put("nextDueDateMillis", r.nextDueDateMillis); o.put("endDateMillis", r.endDateMillis)
            o.put("notes", r.notes); o.put("isActive", r.isActive)
            recurringArray.put(o)
        }
        root.put("recurring", recurringArray)

        // Device-transfer extras: prefs (committed prompt, memory summary, theme, profile, AI settings)
        val prefsObj = JSONObject()
        bundle.prefs.forEach { (k, v) -> prefsObj.put(k, v) }
        root.put("prefs", prefsObj)

        file.writeText(root.toString(2))
        return file
    }

    fun parseBackupJson(json: String): BackupBundle {
        val root = JSONObject(json)
        val txList = mutableListOf<TransactionEntity>()
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                txList.add(
                    TransactionEntity(
                        title = o.optString("title"),
                        amount = o.optDouble("amount"),
                        type = TransactionType.valueOf(o.optString("type", "EXPENSE")),
                        category = TransactionCategory.fromString(o.optString("category", "OTHER")),
                        necessity = ExpenseNecessity.valueOf(o.optString("necessity", "WANT")),
                        account = o.optString("account", "Main Checking"),
                        merchant = o.optString("merchant", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        notes = o.optString("notes", ""),
                        tags = o.optString("tags", ""),
                        isRecurring = o.optBoolean("isRecurring", false),
                        currency = o.optString("currency", Currency.INR.code)
                    )
                )
            }
        }

        val budgetList = mutableListOf<BudgetEntity>()
        root.optJSONArray("budgets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                budgetList.add(
                    BudgetEntity(
                        category = TransactionCategory.fromString(o.optString("category")),
                        monthlyLimit = o.optDouble("monthlyLimit"),
                        periodMonth = o.optInt("periodMonth"),
                        periodYear = o.optInt("periodYear"),
                        alertThreshold = o.optDouble("alertThreshold", 0.85)
                    )
                )
            }
        }

        val goalList = mutableListOf<GoalEntity>()
        root.optJSONArray("goals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                goalList.add(
                    GoalEntity(
                        title = o.optString("title"),
                        targetAmount = o.optDouble("targetAmount"),
                        currentAmount = o.optDouble("currentAmount"),
                        targetDateMillis = o.optLong("targetDateMillis"),
                        categoryTag = o.optString("categoryTag", "General"),
                        isCompleted = o.optBoolean("isCompleted", false)
                    )
                )
            }
        }

        val memList = mutableListOf<BrainMemoryEntity>()
        root.optJSONArray("memories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                memList.add(
                    BrainMemoryEntity(
                        memoryType = MemoryType.valueOf(o.optString("memoryType", "HABIT_LEARNED")),
                        topic = o.optString("topic"),
                        description = o.optString("description"),
                        confidenceScore = o.optDouble("confidenceScore", 0.85).toFloat(),
                        detectedCount = o.optInt("detectedCount", 1),
                        lastObservedAt = o.optLong("lastObservedAt", System.currentTimeMillis()),
                        actionSuggestion = o.optString("actionSuggestion", "")
                    )
                )
            }
        }

        val chatList = mutableListOf<ChatMessageEntity>()
        root.optJSONArray("chatMessages")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                chatList.add(
                    ChatMessageEntity(
                        sender = MessageSender.valueOf(o.optString("sender", "SYSTEM")),
                        messageText = o.optString("messageText"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        actionType = o.optString("actionType").ifBlank { null },
                        actionPayload = o.optString("actionPayload").ifBlank { null }
                    )
                )
            }
        }

        val holdingList = mutableListOf<PortfolioHoldingEntity>()
        root.optJSONArray("holdings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                holdingList.add(
                    PortfolioHoldingEntity(
                        instrumentName = o.optString("instrumentName"),
                        symbol = o.optString("symbol", ""),
                        assetClass = AssetClass.valueOf(o.optString("assetClass", "MUTUAL_FUND")),
                        region = InvestmentRegion.valueOf(o.optString("region", "INDIA")),
                        quantity = o.optDouble("quantity"),
                        avgBuyPrice = o.optDouble("avgBuyPrice"),
                        currentPrice = o.optDouble("currentPrice"),
                        investedAmount = o.optDouble("investedAmount"),
                        currentValue = o.optDouble("currentValue"),
                        currency = o.optString("currency", Currency.INR.code),
                        purchaseDate = o.optLong("purchaseDate", System.currentTimeMillis()),
                        notes = o.optString("notes", ""),
                        isSip = o.optBoolean("isSip", false),
                        sipMonthlyAmount = o.optDouble("sipMonthlyAmount", 0.0)
                    )
                )
            }
        }

        val loanList = mutableListOf<LoanEntity>()
        root.optJSONArray("loans")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                loanList.add(
                    LoanEntity(
                        title = o.optString("title"),
                        type = runCatching { LoanType.valueOf(o.optString("type")) }.getOrDefault(LoanType.LOAN),
                        principalAmount = o.optDouble("principalAmount"),
                        outstandingAmount = o.optDouble("outstandingAmount"),
                        interestRate = o.optDouble("interestRate"),
                        monthlyEmi = o.optDouble("monthlyEmi"),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        val taskList = mutableListOf<TaskEntity>()
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                taskList.add(
                    TaskEntity(
                        title = o.optString("title"),
                        amount = o.optDouble("amount"),
                        recurrence = runCatching { TaskRecurrence.valueOf(o.optString("recurrence")) }.getOrDefault(TaskRecurrence.ONCE),
                        nextDueDateMillis = o.optLong("nextDueDateMillis"),
                        expiresAtMillis = o.optLong("expiresAtMillis"),
                        category = o.optString("category", "General"),
                        notes = o.optString("notes", ""),
                        timesDone = o.optInt("timesDone"),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
        }

        val accountList = mutableListOf<AccountEntity>()
        root.optJSONArray("accounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                accountList.add(
                    AccountEntity(
                        name = o.optString("name"),
                        type = runCatching { AccountType.valueOf(o.optString("type")) }.getOrDefault(AccountType.BANK),
                        initialBalance = o.optDouble("initialBalance"),
                        colorArgb = o.optLong("colorArgb", 0xFF6750A4),
                        icon = o.optString("icon", ""),
                        isArchived = o.optBoolean("isArchived", false)
                    )
                )
            }
        }

        val recurringList = mutableListOf<RecurringTransactionEntity>()
        root.optJSONArray("recurring")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                recurringList.add(
                    RecurringTransactionEntity(
                        title = o.optString("title"),
                        amount = o.optDouble("amount"),
                        type = runCatching { TransactionType.valueOf(o.optString("type")) }.getOrDefault(TransactionType.EXPENSE),
                        category = TransactionCategory.fromString(o.optString("category")),
                        necessity = runCatching { ExpenseNecessity.valueOf(o.optString("necessity")) }.getOrDefault(ExpenseNecessity.NEED),
                        account = o.optString("account", "Bank Account"),
                        recurrence = runCatching { TaskRecurrence.valueOf(o.optString("recurrence")) }.getOrDefault(TaskRecurrence.MONTHLY),
                        nextDueDateMillis = o.optLong("nextDueDateMillis"),
                        endDateMillis = o.optLong("endDateMillis"),
                        notes = o.optString("notes", ""),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
        }

        val prefsMap = mutableMapOf<String, String>()
        root.optJSONObject("prefs")?.let { pobj ->
            val keys = pobj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                prefsMap[k] = pobj.optString(k, "")
            }
        }

        return BackupBundle(txList, budgetList, goalList, memList, chatList, holdingList, loanList, taskList, accountList, recurringList, prefsMap)
    }

    fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createShareChooser(intent: Intent): Intent {
        return Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Parses a Dhan-OM transactions CSV export back into entities.
     * Header: Date,Title,Amount,Type,Category,Necessity,Account,Merchant,Currency,Notes
     * Also tolerates a minimal "Title,Amount,Category" CSV.
     */
    fun parseTransactionsCsv(csv: String): List<TransactionEntity> {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val out = mutableListOf<TransactionEntity>()
        var start = 0
        if (lines[0].contains("Title") && lines[0].contains("Amount")) start = 1
        for (line in lines.drop(start)) {
            val cols = splitCsvLine(line)
            if (cols.size < 3) continue
            // Determine columns: if first column looks like a date (contains '-') and 2nd is title
            val hasHeader = start == 1
            val amount = when {
                hasHeader && cols.size >= 3 -> cols[2].toDoubleOrNull()
                else -> cols[1].toDoubleOrNull()
            } ?: continue
            if (hasHeader && cols.size >= 10) {
                out.add(
                    TransactionEntity(
                        title = cols[1],
                        amount = amount,
                        type = runCatching { TransactionType.valueOf(cols[3].trim()) }.getOrDefault(TransactionType.EXPENSE),
                        category = TransactionCategory.fromString(cols[4]),
                        necessity = runCatching { ExpenseNecessity.valueOf(cols[5].trim()) }.getOrDefault(TransactionCategory.fromString(cols[4]).defaultNecessity),
                        account = cols[6].ifBlank { "Main" },
                        merchant = cols[7],
                        notes = cols.getOrElse(9) { "" }
                    )
                )
            } else {
                out.add(
                    TransactionEntity(
                        title = cols[0],
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.fromString(cols.getOrElse(2) { "" }),
                        necessity = TransactionCategory.fromString(cols.getOrElse(2) { "" }).defaultNecessity,
                        account = "Main",
                        merchant = ""
                    )
                )
            }
        }
        return out
    }

    private fun splitCsvLine(line: String): List<String> {
        val cols = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { cur.append('"'); i++ }
                    else inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> { cols.add(cur.toString()); cur.setLength(0) }
                else -> cur.append(c)
            }
            i++
        }
        cols.add(cur.toString())
        return cols
    }

    private fun csvSafe(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private fun buildPdfContent(
        summary: com.example.domain.analytics.CashFlowSummary,
        transactions: List<TransactionEntity>,
        holdings: List<PortfolioHoldingEntity>,
        goals: List<GoalEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("BT\n/F1 16 Tf\n72 760 Td\n(Dhan-OM - Financial Report) Tj\nET\n")
        sb.append("BT\n/F1 10 Tf\n72 740 Td\n(Generated: ${dateFormat.format(Date())}) Tj\nET\n\n")

        var y = 710
        sb.append("BT\n/F1 12 Tf\n72 $y Td\n(Cash Flow Summary) Tj\nET\n")
        y -= 18
        val summaryLines = listOf(
            "Total Inflow: ${formatMoney(summary.totalInflow)}",
            "Total Outflow: ${formatMoney(summary.totalOutflow)}",
            "Net Cash Flow: ${formatMoney(summary.netCashFlow)}",
            "Savings Rate: ${String.format(Locale.US, "%.1f", summary.savingsRate)}%",
            "Health Score: ${summary.healthScore}/100 (${summary.healthGrade})",
            "Needs / Wants / Savings: ${summary.needsPercentage.toInt()}% / ${summary.wantsPercentage.toInt()}% / ${summary.savingsPercentage.toInt()}%"
        )
        summaryLines.forEach { line ->
            sb.append("BT\n/F1 10 Tf\n72 $y Td\n(${escapePdf(line)}) Tj\nET\n")
            y -= 14
        }

        y -= 10
        sb.append("BT\n/F1 12 Tf\n72 $y Td\n(Recent Transactions) Tj\nET\n")
        y -= 16
        transactions.take(15).forEach { tx ->
            val line = "${dateFormat.format(Date(tx.timestamp))} | ${tx.type.name} | ${formatMoney(tx.amount)} | ${tx.title}"
            sb.append("BT\n/F1 9 Tf\n72 $y Td\n(${escapePdf(line)}) Tj\nET\n")
            y -= 12
        }

        if (holdings.isNotEmpty()) {
            y -= 10
            sb.append("BT\n/F1 12 Tf\n72 $y Td\n(Portfolio Holdings) Tj\nET\n")
            y -= 16
            holdings.take(15).forEach { h ->
                val line = "${h.instrumentName} | ${h.assetClass.displayName} | Invested: ${formatMoney(h.investedAmount)} | Current: ${formatMoney(h.currentValue)} | PnL: ${String.format(Locale.US, "%.1f", h.unrealizedPnlPercent)}%"
                sb.append("BT\n/F1 9 Tf\n72 $y Td\n(${escapePdf(line)}) Tj\nET\n")
                y -= 12
            }
        }

        if (goals.isNotEmpty()) {
            y -= 10
            sb.append("BT\n/F1 12 Tf\n72 $y Td\n(Savings Goals) Tj\nET\n")
            y -= 16
            goals.forEach { g ->
                val pct = if (g.targetAmount > 0) ((g.currentAmount / g.targetAmount) * 100).toInt() else 0
                val line = "${g.title} | ${formatMoney(g.currentAmount)} / ${formatMoney(g.targetAmount)} ($pct%)"
                sb.append("BT\n/F1 9 Tf\n72 $y Td\n(${escapePdf(line)}) Tj\nET\n")
                y -= 12
            }
        }

        return sb.toString()
    }

    /** Generates a simple PDF invoice/bill (used by the "create bill/invoice" chat command). */
    fun exportInvoicePdf(
        context: Context,
        billTitle: String,
        amount: Double,
        payee: String,
        notes: String
    ): File {
        val fileName = "dhanom_invoice_${fileDateFormat.format(Date())}.pdf"
        val file = File(context.cacheDir, fileName)
        val sb = StringBuilder()
        var y = 760
        sb.append("BT\n/F1 18 Tf\n72 $y Td\n(INVOICE) Tj\nET\n")
        y -= 30
        sb.append("BT\n/F1 12 Tf\n72 $y Td\n(${escapePdf(billTitle)}) Tj\nET\n")
        y -= 22
        sb.append("BT\n/F1 10 Tf\n72 $y Td\n(Date: ${dateFormat.format(Date())}) Tj\nET\n")
        y -= 18
        sb.append("BT\n/F1 10 Tf\n72 $y Td\n(Payee: ${escapePdf(payee)}) Tj\nET\n")
        y -= 22
        sb.append("BT\n/F1 16 Tf\n72 $y Td\n(Amount Due: ${formatMoney(amount)}) Tj\nET\n")
        y -= 22
        sb.append("BT\n/F1 9 Tf\n72 $y Td\n(${escapePdf(notes)}) Tj\nET\n")
        val header = buildPdfHeader(sb.toString().toByteArray(Charsets.ISO_8859_1).size)
        file.outputStream().use { out ->
            out.write(header.toByteArray(Charsets.ISO_8859_1))
            out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
            out.write("endstream\nendobj\nstartxref\n0\n%%EOF".toByteArray(Charsets.ISO_8859_1))
        }
        return file
    }

    private fun buildPdfHeader(streamLength: Int): String {
        return """
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
5 0 obj
<< /Length $streamLength >>
stream
""".trimIndent() + "\n"
    }

    private fun escapePdf(text: String): String =
        text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    private fun formatMoney(amount: Double): String {
        val symbol = Currency.INR.symbol
        return "$symbol${String.format(Locale.US, "%,.2f", amount)}"
    }
}
