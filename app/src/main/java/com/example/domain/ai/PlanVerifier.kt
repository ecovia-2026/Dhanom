package com.example.domain.ai

import com.example.data.model.*
import com.example.domain.analytics.CashFlowSummary
import java.util.Locale

/**
 * Self-verification for any plan/strategy the brain produces: before a plan is
 * shown, it is stress-tested against the user's REAL numbers (net cash flow,
 * savings rate, outstanding debt, emergency fund, investments) so infeasible
 * advice is caught and flagged — the brain "tests itself against reality".
 */
object PlanVerifier {

    private fun money(v: Double): String = String.format(Locale.US, "%,.0f", v)

    /** Returns null when the reply is not a plan, otherwise a verification block. */
    fun verify(
        reply: String,
        summary: CashFlowSummary,
        transactions: List<TransactionEntity>,
        goals: List<GoalEntity>,
        loans: List<LoanEntity>,
        holdings: List<PortfolioHoldingEntity>
    ): String? {
        val lower = reply.lowercase()
        // Skip anything that is a task/reminder/EMI confirmation — the reality
        // check is only for actual financial plans/strategies.
        if (lower.contains("task") || lower.contains("remind") || lower.contains("reminder") ||
            lower.contains("emi") || lower.contains("due on the") || lower.contains("updated") ||
            lower.contains("added task")) return null

        val planSignals = listOf(
            "plan", "strategy", "you should", "recommend", "consider", "reduce", "cut ",
            "automate", "allocate", "invest", "sip", "save more", "step up", "rebalance",
            "redirect", "target"
        )
        val looksLikePlan = planSignals.count { lower.contains(it) } >= 1
        if (!looksLikePlan || reply.length < 60) return null

        val net = summary.netCashFlow
        val savingsRate = summary.savingsRate
        val emergencyGoal = goals.firstOrNull { it.title.contains("emergency", true) }
        val emergencyFunded = emergencyGoal?.let {
            if (it.targetAmount > 0) it.currentAmount / it.targetAmount * 100.0 else 100.0
        } ?: 100.0
        val debt = loans.sumOf { it.outstandingAmount }
        val investable = holdings.sumOf { it.currentValue }

        // Extract the largest monthly amount the plan suggests saving/investing.
        val suggestedMonthly = Regex("""(?i)(?:save|sip|invest|sip|automate|redirect|step up)[^₹$0-9]{0,24}(?:₹|rs\.?|inr|\$)?\s*([\d,]+(?:\.[\d]+)?)\s*(?:/mo|/month|monthly|per month|/month)?""")
            .findAll(reply).mapNotNull { m ->
                m.groupValues[1].replace(",", "").toDoubleOrNull()
            }.maxOrNull()

        val flags = mutableListOf<String>()
        val checks = mutableListOf<String>()

        if (net < 0) {
            flags.add("Your net cash flow is negative (₹${money(net)}) — any new saving is not affordable right now.")
        } else {
            checks.add("Net cash flow ₹${money(net)}/month can absorb new commitments.")
        }

        suggestedMonthly?.let { m ->
            if (m > net && net > 0) {
                flags.add("The plan suggests ~₹${money(m)}/month, but your real net cash flow is only ₹${money(net)} — it exceeds what you actually have.")
            } else if (m > 0 && net > 0) {
                val pct = m / summary.totalInflow * 100.0
                checks.add("Suggested ₹${money(m)}/month ≈ ${pct.toInt()}% of your inflow — feasible against your real numbers.")
            }
        }

        if (savingsRate < 15) {
            flags.add("Your current savings rate is only ${savingsRate.toInt()}% — the plan must first build a buffer, not stretch further.")
        } else {
            checks.add("Savings rate ${savingsRate.toInt()}% is a healthy base for the plan.")
        }

        if (debt > 0) {
            flags.add("You still have ₹${money(debt)} outstanding debt — verify the plan pays high-interest debt before investing.")
        }

        if (emergencyFunded < 100) {
            flags.add("Emergency fund is only ${emergencyFunded.toInt()}% funded — the plan should secure it before optional goals.")
        } else {
            checks.add("Emergency fund is fully funded (${emergencyFunded.toInt()}%).")
        }

        if (investable > 0) checks.add("Existing investments ₹${money(investable)} can be rebalanced instead of adding fresh capital.")

        return buildString {
            append("\n\n🔍 REALITY CHECK (self-verified against your data):\n")
            if (flags.isEmpty()) {
                append("✅ The plan is feasible with your current numbers.\n")
                checks.take(3).forEach { append("• $it\n") }
            } else {
                flags.take(4).forEach { append("⚠️ $it\n") }
                if (checks.isNotEmpty()) checks.take(2).forEach { append("• $it\n") }
            }
        }
    }
}
