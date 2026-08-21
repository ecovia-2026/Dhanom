package com.example.data.sms

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.model.TransactionEntity

/**
 * Back-fills the ledger from the SMS inbox (last [days] days of bank / UPI / card
 * messages). Requires READ_SMS. Dedup is the caller's job.
 */
object SmsInboxScanner {

    fun scan(context: Context, days: Int = 45, limit: Int = 400): List<Pair<TransactionEntity, String>> {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val cutoff = System.currentTimeMillis() - days.toLong() * 24L * 3600L * 1000L
        val out = mutableListOf<Pair<TransactionEntity, String>>()
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(cutoff.toString()),
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                var n = 0
                while (cursor.moveToNext() && n < limit) {
                    n++
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: continue else continue
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val parsed = BankSmsParser.parse(body) ?: continue
                    out += BankSmsParser.toEntity(parsed, date) to body
                }
            }
        } catch (_: Throwable) {
        }
        return out
    }
}
