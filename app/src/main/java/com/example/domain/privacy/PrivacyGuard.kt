package com.example.domain.privacy

/**
 * Hard privacy boundary: on-device brain data (ledger, PAN, SMS, chat,
 * memories, files, merchants, account numbers) must never be placed in a
 * payload that leaves the phone.
 *
 * The cloud LLM may receive only:
 *  - the user's current question (PAN / account digits stripped),
 *  - a generic multilingual system instruction with NO personal records.
 */
object PrivacyGuard {

    private val pan = Regex("""\b[A-Z]{5}\d{4}[A-Z]\b""")
    private val aadhaar = Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b""")
    private val longAccount = Regex("""\b\d{9,18}\b""")
    private val cardMask = Regex("""\b(?:x{2,}\d{2,}|\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4})\b""", RegexOption.IGNORE_CASE)

    /** Forbidden markers that mean a prompt dumped the on-device ledger. */
    private val leakMarkers = listOf(
        "REAL DATA:",
        "Recent:",
        "Learned Habit",
        "brain_memories",
        "Auto-logged from bank SMS",
        "PAN number",
        "chat_uploads"
    )

    fun sanitizeOutgoingQuestion(text: String): String {
        var s = text
        s = pan.replace(s, "[PAN]")
        s = aadhaar.replace(s, "[ID]")
        s = cardMask.replace(s, "[CARD]")
        s = longAccount.replace(s, "[ACCT]")
        return s.take(4000)
    }

    fun isSafeForCloud(payload: String): Boolean {
        if (payload.length > 8_000) return false
        val lower = payload.lowercase()
        if (leakMarkers.any { payload.contains(it) }) return false
        if (pan.containsMatchIn(payload)) return false
        if (lower.contains("hdfc savings") || lower.contains("icici credit")) return false
        return true
    }

    /**
     * System prompt for the cloud model. Contains zero user records —
     * numbers live on-device and are answered locally.
     */
    fun cloudSystemPrompt(): String = """
You are Dhan-OM, a precise personal finance advisor.
Reply in the SAME language the user wrote or spoke (Hindi, Hinglish, Marathi, Gujarati, Tamil, Telugu, Kannada, Malayalam, Bengali, Punjabi, Urdu, English, or mixed).
Understand Indian number words: 1.5 lakh = 150000, 20 lacs = 2000000, 2 crore = 20000000, 62,000 = 62000.
Never invent the user's balances, merchants, PAN, or account numbers — you do not have their ledger.
If they ask "how much did I spend", say those totals are calculated on the phone from their private ledger.
Keep answers 2–5 short sentences unless they ask for detail.
When they clearly ask to record money, you may append one JSON line:
{"action":"add_expense","amount":450,"merchant":"Swiggy","category":"dining"}
{"action":"add_income","amount":62000,"merchant":"Salary","category":"salary"}
{"action":"add_goal","title":"Save","amount":2000000,"days":365}
{"action":"set_budget","category":"groceries","limit":8000}
{"action":"delete_last":true}
For questions output NO JSON.
""".trimIndent()
}
