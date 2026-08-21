package com.example.domain.ai

import com.example.data.model.*
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.nlp.NaturalLanguageFinanceParser
import com.example.domain.nlp.ParsedFinanceCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DhanomAiResponse(
    val replyText: String,
    val parsedCommand: ParsedFinanceCommand?,
    val brain: String // "gemma" | "cloud" | "local"
)

/**
 * Dhan-OM's brain is layered for maximum accuracy:
 *   1. local commands (add/delete/budget/goal/navigation) execute instantly,
 *   2. a high-accuracy CLOUD LLM answers everything else when configured,
 *   3. the on-device Gemma 4 E4B model answers offline,
 *   4. curated small-talk answers for greetings/meta questions.
 * The LLM never touches the database directly — it only proposes structured
 * JSON tool-calls that the app validates and executes.
 */
class DhanomAiService {

    suspend fun processUserMessage(
        userMessage: String,
        currentTransactions: List<TransactionEntity>,
        currentBudgets: List<BudgetEntity>,
        currentGoals: List<GoalEntity> = emptyList(),
        learnedMemories: List<BrainMemoryEntity> = emptyList(),
        cloudGenerate: (suspend (String) -> String?)? = null,
        gemmaGenerate: (suspend (String) -> String?)? = null,
        gemmaStatus: () -> String = { "Gemma brain not ready yet." }
    ): DhanomAiResponse = withContext(Dispatchers.IO) {

        val summary = FinancialAnalyticsEngine.calculateCashFlowSummary(currentTransactions)

        // 1) Local command understanding (actions the tracker must perform).
        val localParsed = NaturalLanguageFinanceParser.parseCommandWithContext(
            input = userMessage,
            transactions = currentTransactions,
            budgets = currentBudgets,
            goals = currentGoals,
            summary = summary
        )

        val isAction = localParsed is ParsedFinanceCommand.AddTransactionCommand ||
                localParsed is ParsedFinanceCommand.SetBudgetCommand ||
                localParsed is ParsedFinanceCommand.AddGoalCommand ||
                localParsed is ParsedFinanceCommand.DeleteTransactionCommand

        // Complex / multi-intent requests must go to a real brain, not the simple parser.
        val complex = isComplexRequest(userMessage.lowercase())

        if (!complex && isAction) {
            val confirmation = when (localParsed) {
                is ParsedFinanceCommand.AddTransactionCommand -> localParsed.confirmationMessage
                is ParsedFinanceCommand.SetBudgetCommand -> localParsed.confirmationMessage
                is ParsedFinanceCommand.AddGoalCommand -> localParsed.confirmationMessage
                is ParsedFinanceCommand.DeleteTransactionCommand -> localParsed.confirmationMessage
                else -> ""
            }
            return@withContext DhanomAiResponse(confirmation, localParsed, "local")
        }

        if (!complex && localParsed is ParsedFinanceCommand.ShowAnalyticsCommand) {
            return@withContext DhanomAiResponse(localParsed.responseMessage, localParsed, "local")
        }

        // Number questions are ALWAYS answered by the on-device analytics engine
        // (never Gemma) so totals/lakh/crore math cannot be invented.
        if (localParsed is ParsedFinanceCommand.QueryResponseCommand &&
            localParsed.queryTopic in ACCURATE_LOCAL_TOPICS
        ) {
            return@withContext DhanomAiResponse(localParsed.responseText, localParsed, "local")
        }

        // 2) Small talk & meta questions get deterministic, helpful answers.
        handleSmallTalk(userMessage)?.let {
            return@withContext DhanomAiResponse(it, null, "local")
        }

        val safeQuestion = com.example.domain.privacy.PrivacyGuard.sanitizeOutgoingQuestion(userMessage)

        // 3) Cloud brain — question only. Ledger / PAN / SMS / chat / memories
        // never leave the device.
        if (cloudGenerate != null) {
            try {
                val sys = com.example.domain.privacy.PrivacyGuard.cloudSystemPrompt()
                if (com.example.domain.privacy.PrivacyGuard.isSafeForCloud(sys) &&
                    com.example.domain.privacy.PrivacyGuard.isSafeForCloud(safeQuestion)
                ) {
                    val reply = cloudGenerate(sys)
                    if (!reply.isNullOrBlank()) {
                        return@withContext DhanomAiResponse(reply.trim(), null, "cloud")
                    }
                }
            } catch (_: Exception) { /* fall back */ }
        }

        // 4) On-device Gemma brain (offline). Keep the prompt SHORT — a 4B
        // model on-device is slow when the context is thousands of tokens.
        if (gemmaGenerate != null) {
            try {
                val compact = compactGemmaPrompt(summary) + "\nUSER: " + userMessage
                val reply = gemmaGenerate(compact)
                if (!reply.isNullOrBlank()) {
                    return@withContext DhanomAiResponse(reply.trim(), null, "gemma")
                }
            } catch (_: Exception) { /* fall through */ }
        }

        // 5) Not ready — say exactly what's wrong.
        DhanomAiResponse(
            replyText = "🧠 ${gemmaStatus()}\n\nInstall the on-device Gemma brain (Profile → AI Brain → Download), or add a cloud brain API key in Profile for the most accurate answers.",
            parsedCommand = null,
            brain = "local"
        )
    }

    /** True when the message mixes multiple financial intents or is very long. */
    private fun isComplexRequest(lower: String): Boolean {
        val markers = listOf("add", "spent", "spend", "salary", "income", "expense", "goal",
            "budget", "delete", "remove", "deposit", "save", "invest", "pay", "paid")
        val hits = markers.count { lower.contains(it) }
        if (hits >= 2) return true
        return lower.trim().split(Regex("\\s+")).size > 14
    }

    /** Curated answers for greetings, small talk, and meta questions. */
    private fun handleSmallTalk(message: String): String? {
        val m = message.lowercase().trim()
        if (m in listOf("hi", "hello", "hey", "hii", "namaste", "namaskar", "hola",
                "vanakkam", "namaskar", "sat sri akal", "assalamualaikum", "adaab") ||
            m.startsWith("hi ") || m.startsWith("hello ") || m.startsWith("hey ") ||
            m == "good morning" || m == "good afternoon" || m == "good evening" ||
            m == "सुप्रभात" || m == "नमस्ते" || m == "नमस्कार") {
            return "Namaste 🙏 I'm Dhan-OM, your personal finance AI.\n\nYou can tell me things like:\n• \"Spent ₹450 on Swiggy\"\n• \"Add income 50000 salary\"\n• \"Delete my last transaction\"\n• \"How much did I spend on dining this month?\"\n\nHow can I help you today?"
        }
        if (m.contains("who are you") || m.contains("what can you do") || m.contains("help")) {
            return "I'm Dhan-OM, an offline-first personal finance AI. I can log & delete transactions by talking, track budgets/goals/loans, analyze spending, manage investments, and export CSV/PDF/backup. Just talk to me in plain English (or Hinglish)."
        }
        if (m.contains("install") || m.contains("download") || m.contains("gemma") || m.contains("model") || m.contains("brain") || m.contains("setup")) {
            return "🧠 The brain runs on your phone. Install the on-device Gemma model in Profile → AI Brain → Download (~3.7 GB, resumes if it drops). For the most accurate answers, also add a cloud brain API key (OpenAI/Groq/etc.) in Profile → Cloud Brain."
        }
        if (m.contains("how are you") || m.contains("kaise ho") || m.contains("kya haal")) {
            return "I'm doing great, thank you! 😊 What would you like to do with your money today?"
        }
        if (m.contains("thank") || m.contains("thanks") || m.contains("shukriya") || m.contains("dhanyavad")) {
            return "You're welcome! 🙏 I'm always here for your money questions."
        }
        return null
    }

    private fun compactGemmaPrompt(summary: com.example.domain.analytics.CashFlowSummary): String =
        "You are Dhan-OM. Reply in the user's language. 2-4 short sentences using ₹. Never invent numbers. " +
            "On-device snapshot only: inflow ₹${summary.totalInflow.toInt()} outflow ₹${summary.totalOutflow.toInt()} " +
            "net ₹${summary.netCashFlow.toInt()} savings ${summary.savingsRate.toInt()}%."

    companion object {
        private val ACCURATE_LOCAL_TOPICS = setOf(
            "SPENDING_QUERY", "PREDICTION", "INVESTMENTS", "HEALTH_SCORE",
            "GOALS_PACING", "CATEGORIZE", "DELETE"
        )
    }
}
