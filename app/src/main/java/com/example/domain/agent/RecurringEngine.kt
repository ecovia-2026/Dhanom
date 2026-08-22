package com.example.domain.agent

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MessageSender
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Auto-posts scheduled/recurring transactions (Actual/Firefly-style): when a
 * recurring transaction comes due, a real transaction is inserted and the
 * schedule rolls forward. Called on app start and by the daily worker.
 */
object RecurringEngine {

    /** Posts all due recurring transactions. Returns how many were posted. */
    suspend fun processDue(context: Context): Int {
        val app = context.applicationContext
        val db = AppDatabase.getDatabase(app)
        val list = db.recurringTransactionDao().getActiveRecurring()
        val now = System.currentTimeMillis()
        var count = 0
        val posted = mutableListOf<String>()

        for (r in list) {
            // due now (and not yet ended)
            val due = r.nextDueDateMillis in 1..now
            val notEnded = r.endDateMillis == 0L || now <= r.endDateMillis
            if (!due || !notEnded) continue

            db.transactionDao().insertTransaction(
                TransactionEntity(
                    title = r.title,
                    amount = r.amount,
                    type = r.type,
                    category = r.category,
                    necessity = r.necessity,
                    account = r.account,
                    merchant = r.title,
                    notes = r.notes.ifBlank { "Auto-posted (recurring)" },
                    isRecurring = true
                )
            )

            val next = r.nextOccurrence()
            if (next == Long.MAX_VALUE || (r.endDateMillis > 0 && next > r.endDateMillis)) {
                db.recurringTransactionDao().updateRecurring(r.copy(isActive = false))
            } else {
                db.recurringTransactionDao().updateRecurring(r.copy(nextDueDateMillis = next))
            }
            posted.add("• ${r.title} (₹${String.format(Locale.US, "%,.0f", r.amount)})")
            count++
        }

        if (count > 0) {
            db.chatMessageDao().insertMessage(
                ChatMessageEntity(
                    sender = MessageSender.DHANOM_AI,
                    messageText = "🔁 Auto-posted $count recurring transaction(s):\n\n" + posted.joinToString("\n"),
                    timestamp = System.currentTimeMillis(),
                    actionType = "RECURRING_POST"
                )
            )
        }
        return count
    }
}
