package com.example.domain.ai

import com.example.BuildConfig
import com.example.data.model.*
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.ml.PersonalFinanceMlEngine
import com.example.domain.nlp.NaturalLanguageFinanceParser
import com.example.domain.nlp.ParsedFinanceCommand
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DhanomAiResponse(
    val replyText: String,
    val parsedCommand: ParsedFinanceCommand?,
    val isFromGemini: Boolean,
    val internetInsightsUsed: Boolean
)

class DhanomAiService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun processUserMessage(
        userMessage: String,
        currentTransactions: List<TransactionEntity>,
        currentBudgets: List<BudgetEntity>,
        currentGoals: List<GoalEntity> = emptyList(),
        learnedMemories: List<BrainMemoryEntity> = emptyList(),
        enableInternetKnowledge: Boolean = false
    ): DhanomAiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(currentTransactions)

        // Context-aware NLU parsing with live on-device ML
        val localParsed = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = userMessage,
            transactions = currentTransactions,
            budgets = currentBudgets,
            goals = currentGoals,
            summary = summary
        )

        // If it's a direct mutating command (add transaction, set budget, add goal), execute it directly
        if (localParsed is ParsedFinanceCommand.AddTransactionCommand ||
            localParsed is ParsedFinanceCommand.SetBudgetCommand ||
            localParsed is ParsedFinanceCommand.AddGoalCommand
        ) {
            val responseText = when (localParsed) {
                is ParsedFinanceCommand.AddTransactionCommand -> localParsed.confirmationMessage
                is ParsedFinanceCommand.SetBudgetCommand -> localParsed.confirmationMessage
                is ParsedFinanceCommand.AddGoalCommand -> localParsed.confirmationMessage
                else -> ""
            }
            return@withContext DhanomAiResponse(
                replyText = responseText,
                parsedCommand = localParsed,
                isFromGemini = false,
                internetInsightsUsed = false
            )
        }

        // If Gemini API Key is present and valid, call Gemini 3.5 Flash for conversational reasoning
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && !apiKey.startsWith("YOUR_")) {
            try {
                val geminiReply = callGeminiApi(
                    apiKey = apiKey,
                    prompt = userMessage,
                    transactions = currentTransactions,
                    budgets = currentBudgets,
                    goals = currentGoals,
                    memories = learnedMemories,
                    enableInternetKnowledge = enableInternetKnowledge
                )
                if (geminiReply.isNotBlank()) {
                    return@withContext DhanomAiResponse(
                        replyText = geminiReply,
                        parsedCommand = localParsed,
                        isFromGemini = true,
                        internetInsightsUsed = enableInternetKnowledge
                    )
                }
            } catch (e: Exception) {
                // Graceful fallback to offline NLP
            }
        }

        // Offline Context-Aware NLU Response
        val fallbackText = when (localParsed) {
            is ParsedFinanceCommand.ShowAnalyticsCommand -> localParsed.responseMessage
            is ParsedFinanceCommand.QueryResponseCommand -> localParsed.responseText
            else -> "I have analyzed your financial records and noted your input. You can review your interactive Flowchart, Spending Graphs, or Ledger anytime."
        }

        DhanomAiResponse(
            replyText = fallbackText,
            parsedCommand = localParsed,
            isFromGemini = false,
            internetInsightsUsed = false
        )
    }

    private fun callGeminiApi(
        apiKey: String,
        prompt: String,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        memories: List<BrainMemoryEntity>,
        enableInternetKnowledge: Boolean
    ): String {
        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(transactions)
        val forecast = PersonalFinanceMlEngine.forecastMonthEndCashFlow(transactions)
        val topCategories = FinancialAnalyticsEngine.calculateCategoryBreakdown(transactions).take(4)
            .joinToString { "${it.category.displayName}: $${String.format(Locale.US, "%.0f", it.amount)} (${it.percentage.toInt()}%)" }

        val habitsContext = memories.take(4)
            .joinToString("; ") { "${it.topic}: ${it.description}" }

        val systemContext = """
            You are Dhanom, an elite, empathetic, privacy-first personal finance AI advisor.
            User Financial Snapshot & ML Intelligence:
            - Monthly Total Inflow: $${String.format(Locale.US, "%.2f", summary.totalInflow)}
            - Monthly Outflow: $${String.format(Locale.US, "%.2f", summary.totalOutflow)}
            - Net Cash Flow: $${String.format(Locale.US, "%.2f", summary.netCashFlow)}
            - Savings Rate: ${summary.savingsRate.toInt()}%
            - Financial Health Score: ${summary.healthScore}/100 (${summary.healthGrade})
            - ML End-of-Month Projected Outflows: $${String.format(Locale.US, "%.2f", forecast.projectedMonthEndExpenses)} (Burn: $${String.format(Locale.US, "%.2f", forecast.dailyBurnRate)}/day)
            - Top Expense Categories: $topCategories
            - Learned Habit Brain Memories: $habitsContext
            ${if (enableInternetKnowledge) "Internet Knowledge Mode: ACTIVE. Provide actionable benchmarks, economic context, tax/interest rate rules, and modern personal finance best practices." else ""}
            
            Style instructions:
            - Give direct, warm, concise, and mathematically sharp advice.
            - Relate answers directly to the user's specific cash flows, goals, and habits.
            - Keep responses scannable with bullet points and bold highlights where helpful. Max 3 short paragraphs.
        """.trimIndent()

        val jsonPayload = JSONObject()

        // System Instruction
        val systemInstructionObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", systemContext))
        systemInstructionObj.put("parts", sysParts)
        jsonPayload.put("systemInstruction", systemInstructionObj)

        // Contents
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", prompt))
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        jsonPayload.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        genConfig.put("maxOutputTokens", 800)
        jsonPayload.put("generationConfig", genConfig)

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return ""
        }

        val jsonResponse = JSONObject(bodyStr)
        val candidates = jsonResponse.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""

        val firstCand = candidates.getJSONObject(0)
        val content = firstCand.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""

        return parts.getJSONObject(0).optString("text", "")
    }
}
