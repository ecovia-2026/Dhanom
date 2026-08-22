package com.example.domain.memory

import com.example.data.model.BrainMemoryEntity
import com.example.data.model.MemoryType
import com.example.data.model.TaskEntity
import com.example.data.model.TaskRecurrence
import java.util.Calendar
import java.util.Locale

/**
 * On-device memory layer ported from the ideas in mem0 / cognee / mem4ai / Graphify:
 *
 *  1. TWO-PHASE EXTRACTION (mem0)  — pull durable "facts", "preferences" and
 *     "tasks" out of each exchange (rules below, no external LLM needed offline).
 *  2. HIERARCHICAL MEMORY (cognee) — working buffer (recent messages) +
 *     rolling summary (UNBOUNDED — grows for the app's lifetime, stored in a
 *     file) + long-term fact store (BrainMemoryEntity in Room).
 *  3. MULTI-SIGNAL RETRIEVAL (mem0) — fuse keyword score + recency + entity
 *     match, then WALK THE FACT GRAPH (Graphify idea) one hop so "each dot
 *     connects to dot": the context stays small and only the relevant memories
 *     for the current question are loaded — never the whole brain.
 *  4. CONFLICT RESOLUTION (mem0)    — update an existing fact of the same
 *     topic instead of duplicating it.
 *  5. TASK MEMORY (Graphify-style)  — recurring/one-off tasks with expiry and
 *     usage counts that the brain keeps nudging until they expire.
 *
 * This is intentionally lightweight (regex/NLP heuristics) so it runs 100%
 * offline; the cloud brain adds the semantic layer on top via chatHistory.
 */
object MemoryEngine {

    /** Extracts durable facts/preferences/tasks from a user message (single exchange). */
    fun extractFacts(userText: String, replyText: String = ""): List<BrainMemoryEntity> {
        val facts = mutableListOf<BrainMemoryEntity>()
        val t = userText.trim()
        val lower = t.lowercase()
        val now = System.currentTimeMillis()

        fun fact(type: MemoryType, topic: String, desc: String, suggestion: String = "") {
            facts.add(
                BrainMemoryEntity(
                    memoryType = type, topic = topic, description = desc,
                    confidenceScore = 0.8f, detectedCount = 1,
                    lastObservedAt = now, actionSuggestion = suggestion
                )
            )
        }

        // Preferences / style rules ("always reply in hinglish", "keep short")
        if (lower.contains("always") || lower.contains("remember") || lower.contains("prefer") ||
            lower.contains("from now") || lower.contains("note that")) {
            fact(MemoryType.PREFERENCE, "Preference: ${t.take(40)}", t.take(200),
                "Standing preference — apply it in future answers.")
        }

        // Income / salary fact
        Regex("""(?i)(salary|income|kamai|kamaya|tankhwah)[^\d₹]{0,20}(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""").find(lower)?.let {
            val amt = normalizeAmount(it.groupValues[2])
            if (amt > 0) fact(MemoryType.FACT, "Monthly income", "User's stated income/salary is about ₹${fmt(amt)}", "Use this for budgeting & savings-rate calculations.")
        }

        // Emergency fund / savings / FD
        Regex("""(?i)(emergency|savings?|saved|fixed deposit|fd)[^\d₹]{0,20}(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""").find(lower)?.let {
            val amt = normalizeAmount(it.groupValues[2])
            if (amt > 0) fact(MemoryType.FACT, "Emergency fund / savings", "User mentioned ~₹${fmt(amt)} in savings/emergency fund", "Track this against the 6-month-expenses goal.")
        }

        // Outstanding / debt / loan
        Regex("""(?i)(outstanding|debt|loan|emi|due)[^\d₹]{0,20}(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""").find(lower)?.let {
            val amt = normalizeAmount(it.groupValues[2])
            if (amt > 0) fact(MemoryType.FACT, "Outstanding / debt", "User mentioned ~₹${fmt(amt)} outstanding/debt", "Track repayments and reduce high-interest debt first.")
        }

        // Investment mention
        if (lower.contains("invest") || lower.contains("sip") || lower.contains("mutual fund") || lower.contains("stock")) {
            fact(MemoryType.FACT, "Investment intent", t.take(160), "Consider suggesting SIP / index funds / asset allocation.")
        }

        // Goal / target
        Regex("""(?i)(goal|target|save for|buy|house|home|car|vacation|wedding)[^\d₹]{0,30}""").find(lower)?.let {
            fact(MemoryType.GOAL_STRATEGY, "Goal: ${it.value.take(30)}", t.take(160), "Factor into goal pacing & strategy suggestions.")
        }

        // Life event
        if (lower.contains("married") || lower.contains("baby") || lower.contains("child") || lower.contains("retire")) {
            fact(MemoryType.FACT, "Life event", t.take(160), "Adjust financial planning for this life stage.")
        }

        return facts
    }

    /** Extracts scheduled/recurring tasks the brain should follow (Graphify-style). */
    fun extractTasks(userText: String, replyText: String = ""): List<TaskEntity> {
        val out = mutableListOf<TaskEntity>()
        val lower = userText.lowercase().trim()
        // Only treat as a task if the user is clearly asking the brain to remember/schedule/remind.
        val isTaskIntent = lower.contains("remind") || lower.contains("remember to") ||
            lower.contains("schedule") || lower.contains("every") || lower.contains("recurring") ||
            lower.contains("task") || lower.contains("due") || lower.contains("todo") || lower.contains("to-do")
        if (!isTaskIntent) return out

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Recurrence
        val recurrence = when {
            lower.contains("quarterly") || lower.contains("every 3 month") || lower.contains("every three month") || lower.contains("every 3rd month") -> TaskRecurrence.QUARTERLY
            lower.contains("yearly") || lower.contains("annual") || lower.contains("every year") -> TaskRecurrence.YEARLY
            lower.contains("monthly") || lower.contains("every month") || lower.contains("every 1st") -> TaskRecurrence.MONTHLY
            lower.contains("weekly") || lower.contains("every week") -> TaskRecurrence.WEEKLY
            lower.contains("daily") || lower.contains("every day") || lower.contains("everyday") -> TaskRecurrence.DAILY
            else -> TaskRecurrence.ONCE
        }

        // Amount involved (if any)
        val amount = Regex("""(?i)(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""")
            .find(userText)?.groupValues?.get(1)?.let { normalizeAmount(it) } ?: 0.0

        // Expiry ("until 2027", "till March", "until next year")
        val expiresAt = when {
            Regex("""(?i)until|till|before\s+(\d{4})""").containsMatchIn(lower) -> {
                val y = Regex("""(?i)(?:until|till|before)\s+(\d{4})""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                if (y != null && y > 2000) {
                    val c = Calendar.getInstance(); c.set(y, 11, 31); c.timeInMillis
                } else 0L
            }
            lower.contains("this year") -> { val c = Calendar.getInstance(); c.set(c.get(Calendar.YEAR), 11, 31); c.timeInMillis }
            else -> 0L
        }

        // Due day-of-month (e.g. "on the 5th", "every month on 1st")
        val dueDay = Regex("""(?i)(?:on\s+the?\s+|on\s+)(\d{1,2})(?:st|nd|rd|th)?\b""")
            .find(lower)?.groupValues?.get(1)?.toIntOrNull()

        val nextDue = if (dueDay != null && recurrence == TaskRecurrence.MONTHLY) {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, 28))
            if (c.timeInMillis < now) c.add(Calendar.MONTH, 1)
            c.timeInMillis
        } else now + 24L * 3600 * 1000

        // Derive a short title from the sentence.
        val title = userText
            .replace(Regex("(?i)please|kindly|can you|could you|remind me to|remember to|don't forget to"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
            .ifBlank { "Task" }

        out.add(
            TaskEntity(
                title = title,
                amount = amount,
                recurrence = recurrence,
                nextDueDateMillis = nextDue,
                expiresAtMillis = expiresAt,
                category = "Task",
                notes = "Learned from: \"${userText.take(120)}\"",
                timesDone = 0,
                isActive = true
            )
        )
        return out
    }

    /**
     * Rolling summary consolidation — UNBOUNDED. The summary is the brain's
     * lifetime memory; it is stored in a file and only the relevant parts are
     * loaded per message (see selectRelevantSummary). A safety ceiling of
     * 5000 lines protects against pathological growth while remaining
     * effectively "unlimited" for real-world usage.
     */
    fun consolidateSummary(previous: String, userText: String, replyText: String): String {
        val newLine = "• ${userText.take(160)} → ${replyText.take(160)}"
        val lines = (previous.split("\n") + newLine).filter { it.isNotBlank() }
        return lines.takeLast(5000).joinToString("\n")
    }

    /**
     * Selects the most relevant lines of the (possibly huge) summary for a
     * query — so the context sent to the brain stays small and focused, while
     * the full summary remains stored on-device for the app's lifetime.
     */
    fun selectRelevantSummary(summary: String, query: String, maxLines: Int = 25): String {
        if (query.isBlank()) return summary.lines().takeLast(maxLines).joinToString("\n")
        val tokens = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
        if (tokens.isEmpty()) return summary.lines().takeLast(maxLines).joinToString("\n")
        val scored = summary.lines().mapNotNull { line ->
            val l = line.lowercase()
            val score = tokens.count { l.contains(it) }
            if (score > 0) score to line else null
        }.sortedByDescending { it.first }.map { it.second }
        val picked = if (scored.size >= maxLines) scored.take(maxLines)
        else (scored + summary.lines().takeLast(maxLines - scored.size))
        return picked.distinct().joinToString("\n")
    }

    /** Multi-signal retrieval: keyword + recency + entity boost, top-k. */
    fun retrieve(memories: List<BrainMemoryEntity>, query: String, k: Int = 6): List<BrainMemoryEntity> {
        if (query.isBlank()) return memories.take(k)
        val q = query.lowercase()
        val tokens = q.split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
        val now = System.currentTimeMillis()
        return memories.map { mem ->
            val m = (mem.topic + " " + mem.description).lowercase()
            var score = 0.0
            tokens.forEach { if (m.contains(it)) score += 1.5 }
            // entity boost: topic title match
            if (q.contains(mem.topic.lowercase()) || mem.topic.lowercase().contains(q)) score += 2.0
            // recency decay (half-life ~7 days)
            val ageDays = (now - mem.lastObservedAt) / (1000.0 * 3600 * 24)
            score += (mem.confidenceScore * 1.0) * kotlin.math.exp(-ageDays / 7.0)
            score to mem
        }.sortedByDescending { it.first }.take(k).map { it.second }
    }

    /**
     * Graphify-style retrieval: seed with the top keyword matches, then walk
     * one hop through the fact graph (memories sharing significant words) so
     * connected memories are recalled together — "each dot connects to dot".
     */
    fun retrieveWithGraph(memories: List<BrainMemoryEntity>, query: String, k: Int = 8): List<BrainMemoryEntity> {
        if (memories.isEmpty()) return emptyList()
        val seeds = retrieve(memories, query, k = 4)
        val result = LinkedHashSet<BrainMemoryEntity>()
        result.addAll(seeds)
        // Build neighbor index once
        val stopwords = setOf("user", "about", "this", "that", "with", "your", "the", "for", "and")
        fun significantWords(m: BrainMemoryEntity): Set<String> =
            (m.topic + " " + m.description).lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 4 && it !in stopwords }
                .toSet()
        val wordIndex = memories.map { significantWords(it) }
        for (seed in seeds) {
            val seedWords = significantWords(seed)
            if (seedWords.isEmpty()) continue
            memories.forEachIndexed { i, other ->
                if (result.size >= k) return@forEachIndexed
                if (other.id == seed.id) return@forEachIndexed
                val overlap = wordIndex[i].count { seedWords.contains(it) }
                if (overlap >= 2) result.add(other)
            }
        }
        // Ensure at least k by filling with top-scored remaining
        if (result.size < k) {
            val rest = memories.filter { it !in result }
            val extra = retrieve(rest, query, k = k - result.size)
            result.addAll(extra)
        }
        return result.toList().take(k)
    }

    fun normalizeAmount(s: String): Double {
        val clean = s.lowercase().trim().replace(",", "").replace("₹", "").replace("rs", "").replace("inr", "")
        return when {
            clean.contains("lakh") || clean.contains("lac") ->
                (clean.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0) * 100_000.0
            clean.contains("crore") || clean.contains("cr") ->
                (clean.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0) * 10_000_000.0
            else -> clean.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        }
    }

    fun fmt(v: Double): String = String.format(Locale.US, "%,.0f", v)

    /**
     * Correction detection: when the user says "actually my salary is
     * 50000 not 60000" / "correct that to ..." / "no, it's X", return the
     * topic keyword + new amount so the app can update the stored fact and
     * regenerate strategies.
     */
    fun detectCorrection(text: String): Pair<String, Double>? {
        val lower = text.lowercase()
        val isCorrection = lower.contains("actually") || lower.contains("correct") ||
                lower.contains("not") || lower.contains("no, it") || lower.contains("i meant") ||
                lower.contains("change that") || lower.contains("update that")
        if (!isCorrection) return null
        val topic = when {
            lower.contains("salary") || lower.contains("income") -> "Monthly income"
            lower.contains("emergency") || lower.contains("savings") || lower.contains("fd") -> "Emergency fund / savings"
            lower.contains("outstanding") || lower.contains("debt") || lower.contains("loan") || lower.contains("emi") -> "Outstanding / debt"
            else -> return null
        }
        val amt = Regex("""(?i)(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d+)?|[\d.]+ lakh|lakhs|lac|lacs|crore)""").find(lower)
            ?.groupValues?.get(1)?.let { normalizeAmount(it) } ?: return null
        if (amt <= 0) return null
        return topic to amt
    }
}
