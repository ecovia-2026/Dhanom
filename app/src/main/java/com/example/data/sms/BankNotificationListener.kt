package com.example.data.sms

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Auto-logs bank / UPI / card **notifications** (GPay, PhonePe, banks).
 * Does not need READ_SMS — sideloading SMS apps is blocked as a "security"
 * reason on Xiaomi / Vivo / Oppo / Samsung. Notification access is granted
 * in system settings after install.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("dhanom_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("sms_tracking", false)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val big = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val body = "$title $text $big".trim()
        if (body.length < 8) return

        val parsed = BankSmsParser.parse(body) ?: return

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.transactionDao().insertTransaction(
                    BankSmsParser.toEntity(parsed).copy(notes = "Auto-logged from bank notification")
                )
            } catch (_: Exception) {
            }
        }
    }
}
