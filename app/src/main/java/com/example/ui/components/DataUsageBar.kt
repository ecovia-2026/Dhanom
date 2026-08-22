package com.example.ui.components

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.TrafficStats
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppPalette
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Persistent bottom bar showing the app's REAL network data usage
 * (bytes received / sent since boot, per Android TrafficStats).
 */
@Composable
fun DataUsageBar(modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val uid = android.os.Process.myUid()

    var rx by remember { mutableStateOf(TrafficStats.getUidRxBytes(uid)) }
    var tx by remember { mutableStateOf(TrafficStats.getUidTxBytes(uid)) }

    // Refresh every 2 seconds so downloads show live movement.
    LaunchedEffect(Unit) {
        while (true) {
            rx = TrafficStats.getUidRxBytes(uid)
            tx = TrafficStats.getUidTxBytes(uid)
            delay(2000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NetworkCell, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Data usage", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.secondaryText, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = palette.positiveGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatBytes(rx), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.accent, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = palette.expenseRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatBytes(tx), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.accent, fontSize = 14.sp)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024.0
        i++
    }
    return String.format(Locale.US, "%.1f %s", value, units[i])
}
