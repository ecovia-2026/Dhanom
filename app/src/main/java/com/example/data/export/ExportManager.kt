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
    val holdings: List<PortfolioHoldingEntity>
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

        return BackupBundle(txList, budgetList, goalList, memList, chatList, holdingList)
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
        sb.append("BT\n/F1 16 Tf\n72 760 Td\n(Dhanom AI - Financial Report) Tj\nET\n")
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
