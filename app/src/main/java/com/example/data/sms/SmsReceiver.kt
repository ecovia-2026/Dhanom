package com.example.data.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Auto-logs incoming bank / UPI / card transaction SMS as transactions
 * (when SMS tracking is enabled in Profile). Runs off the main thread.
 */
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("dhanom_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sms_tracking", false)) return
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val messages = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages?.joinToString(" ") { it.displayMessageBody } ?: return

        val parsed = BankSmsParser.parse(body) ?: return

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.transactionDao().insertTransaction(BankSmsParser.toEntity(parsed))
                notify(context, parsed)
            } catch (_: Exception) {
            }
        }
    }

    private fun notify(context: Context, tx: BankSmsParser.SmsTx) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("dhanom_sms", "Dhan-OM bank alerts", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        val symbol = if (tx.type == com.example.data.model.TransactionType.INCOME) "↑" else "↓"
        val n = NotificationCompat.Builder(context, "dhanom_sms")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("$symbol ₹${"%.2f".format(tx.amount)} · ${tx.merchant}")
            .setContentText("${tx.category.displayName} via ${tx.accountHint} — logged to Dhan-OM")
            .setAutoCancel(true)
            .build()
        try { nm.notify(tx.hashCode(), n) } catch (_: Exception) {}
    }
}
