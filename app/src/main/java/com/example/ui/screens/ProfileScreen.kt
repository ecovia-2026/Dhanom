package com.example.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.DhanomApplication
import com.example.data.prefs.AiSettings
import com.example.data.prefs.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    aiSettings: AiSettings,
    themeId: String,
    modelStatus: String,
    downloadProgress: Float,
    serverStatus: String,
    transactionsCount: Int,
    holdingsCount: Int,
    onSaveProfile: (String) -> Unit,
    onSaveAi: (AiSettings) -> Unit,
    onSaveWelcomeVoice: (Boolean) -> Unit,
    onSaveSmsTracking: (Boolean) -> Unit,
    onSavePan: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onGenerateApiKey: () -> Unit,
    onDownloadModel: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    onToggleServer: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var gemmaUrl by remember(aiSettings.gemmaModelUrl) { mutableStateOf(aiSettings.gemmaModelUrl) }
    var serverPort by remember(aiSettings.serverPort) { mutableStateOf(aiSettings.serverPort.toString()) }
    var welcomeVoice by remember(profile.welcomeVoice) { mutableStateOf(profile.welcomeVoice) }
    var smsTracking by remember(profile.smsTracking) { mutableStateOf(profile.smsTracking) }
    var panNumber by remember(profile.panNumber) { mutableStateOf(profile.panNumber) }
    var cloudEnabled by remember(aiSettings.cloudEnabled) { mutableStateOf(aiSettings.cloudEnabled) }
    var cloudEndpoint by remember(aiSettings.cloudEndpoint) { mutableStateOf(aiSettings.cloudEndpoint) }
    var cloudModel by remember(aiSettings.cloudModel) { mutableStateOf(aiSettings.cloudModel) }
    var cloudApiKey by remember(aiSettings.cloudApiKey) { mutableStateOf(aiSettings.cloudApiKey) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var crashRefresh by remember { mutableStateOf(0) }
    val crashLog = remember(crashRefresh) {
        DhanomApplication.readCrashLog(context.applicationContext as Application)
    }

    fun currentSettings() = AiSettings(
        gemmaModelUrl = gemmaUrl,
        serverPort = serverPort.toIntOrNull() ?: 8080,
        serverToken = aiSettings.serverToken,
        serverApiKey = aiSettings.serverApiKey,
        cloudEnabled = cloudEnabled,
        cloudEndpoint = cloudEndpoint,
        cloudApiKey = cloudApiKey,
        cloudModel = cloudModel
    )

    fun copyKey() {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Dhan-OM API key", aiSettings.serverApiKey))
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        // result ignored; the toggle below reflects intent
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently deletes ALL your transactions, budgets, goals, holdings, loans, memories and chat from this device.") },
            confirmButton = { TextButton(onClick = { showClearConfirm = false; onClearAllData() }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
    }

    val palette = LocalAppPalette.current

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Profile & Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.accent)
            Text("Identity, themes, the on-device Gemma brain, and data control", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryText)
        }

        // ---- THEME ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    ThemePalettes.all.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { theme ->
                                val selected = theme.id == themeId
                                Surface(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)),
                                    onClick = { onSelectTheme(theme.id) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) theme.primaryContainer else theme.background,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) theme.primary else theme.border)
                                ) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(22.dp).clip(CircleShape).background(theme.primary))
                                        Spacer(Modifier.width(8.dp))
                                        Text(theme.name, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = theme.accent)
                                    }
                                }
                            }
                        }
                    }
                    Text("Tip: say \"change theme to ocean\" in chat to switch instantly.", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                }
            }
        }

        // ---- PROFILE ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("$transactionsCount transactions · $holdingsCount holdings", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = welcomeVoice, onCheckedChange = { welcomeVoice = it; onSaveWelcomeVoice(it) })
                        Spacer(Modifier.width(8.dp))
                        Text("Welcome voice on launch", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSaveProfile(name) }, shape = RoundedCornerShape(12.dp)) { Text("Save Profile") }
                }
            }
        }

        // ---- AUTO TRACKING (SMS + PAN) ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Auto Tracking (bank SMS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("When enabled, Dhan-OM reads your SMS inbox (last 45 days) and every new bank / UPI / card message, then auto-logs debit & credit with merchant, card/UPI and amount. Give READ SMS permission when asked.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = smsTracking, onCheckedChange = { v ->
                            smsTracking = v
                            if (v) {
                                val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(
                                    "android.permission.RECEIVE_SMS",
                                    "android.permission.READ_SMS",
                                    "android.permission.POST_NOTIFICATIONS"
                                ) else arrayOf("android.permission.RECEIVE_SMS", "android.permission.READ_SMS")
                                smsPermissionLauncher.launch(perms)
                            }
                            onSaveSmsTracking(v)
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Track bank SMS automatically", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = panNumber,
                        onValueChange = { panNumber = it.uppercase() },
                        label = { Text("PAN number (optional)") },
                        placeholder = { Text("ABCDE1234F") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Your PAN is stored only on-device and used to tag PAN-related alerts. (Full CIBIL/credit tracing needs a licensed bureau API + your consent.)", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSavePan(panNumber) }, shape = RoundedCornerShape(12.dp)) { Text("Save PAN") }
                }
            }
        }

        // ---- GEMMA BRAIN ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = palette.primaryContainer, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = palette.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Gemma 4 E4B Brain", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("The brain runs fully on-device. Model status: $modelStatus", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.onPrimaryContainer)
                    OutlinedTextField(
                        gemmaUrl, { gemmaUrl = it },
                        label = { Text("Model URL (.litertlm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (downloadProgress > 0f && downloadProgress < 1f) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = downloadProgress, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text("${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = palette.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDownloadModel, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download")
                        }
                        OutlinedButton(onClick = onCancelDownload, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                        OutlinedButton(onClick = onDeleteModel, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                    }
                }
            }
        }

        // ---- CLOUD BRAIN (more accurate backend) ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Cloud Brain (most accurate)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Paste your API key here for fast, ChatGPT-class advice. Your ledger, PAN, SMS, chat, files and Gemma model NEVER leave this phone — the cloud only receives the current question (PAN/account digits stripped). Totals are always calculated on-device. Keys: groq.com/keys · aistudio.google.com/apikey · openrouter.ai/keys", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = cloudEnabled, onCheckedChange = { cloudEnabled = it; onSaveAi(currentSettings()) })
                        Spacer(Modifier.width(8.dp))
                        Text("Use cloud brain", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Provider presets
                    com.example.domain.brain.CloudBrainClient.presets.forEach { preset ->
                        val selected = cloudEndpoint == preset.second && cloudModel == preset.third
                        Surface(
                            onClick = { cloudEndpoint = preset.second; cloudModel = preset.third },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) palette.primaryContainer else palette.surfaceVariant
                        ) {
                            Text(preset.first, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = palette.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(cloudApiKey, { cloudApiKey = it }, label = { Text("API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(cloudModel, { cloudModel = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cloudEndpoint, { cloudEndpoint = it }, label = { Text("Endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSaveAi(currentSettings()) }, shape = RoundedCornerShape(12.dp)) { Text("Save Cloud Brain") }
                }
            }
        }

        // ---- BRAIN PLUGIN SERVER + API KEY ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeviceHub, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Brain (API)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Expose the Gemma brain to other apps/phones via an OpenAI-compatible API. Share your API key and they connect to your IP — like the OpenAI platform.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(serverPort, { serverPort = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("Port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiSettings.serverApiKey,
                        onValueChange = {},
                        label = { Text("API key") },
                        readOnly = true,
                        visualTransformation = PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { copyKey() }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp)) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onGenerateApiKey() }, shape = RoundedCornerShape(12.dp)) { Text("Generate new key") }
                        Button(onClick = { onSaveAi(currentSettings()); onToggleServer() }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (serverStatus.startsWith("Running")) "Restart" else "Start server")
                        }
                        OutlinedButton(onClick = onToggleServer, shape = RoundedCornerShape(12.dp)) { Text("Stop") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Status: $serverStatus", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.accent)
                    Text("Connect: POST http://<your-ip>:$serverPort/v1/chat/completions  ·  Authorization: Bearer <key>", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                    Text("For internet access from anywhere, forward the port on your router or use a tunnel (ngrok/Cloudflare).", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                }
            }
        }

        // ---- SELF-HEAL ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Healing, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Self-heal & Crash Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = palette.surfaceVariant) {
                        Text(crashLog, style = MaterialTheme.typography.labelSmall, color = palette.secondaryText, modifier = Modifier.padding(12.dp).fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { crashRefresh++ }, shape = RoundedCornerShape(12.dp)) { Text("Refresh") }
                        OutlinedButton(onClick = { DhanomApplication.clearCrashLog(context.applicationContext as Application); crashRefresh++ }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear log") }
                    }
                }
            }
        }

        // ---- DATA ----
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Data Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onLoadDemoData, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Load demo data (optional)")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showClearConfirm = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Clear ALL data")
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(24.dp), color = palette.surfaceVariant, border = BorderStroke(1.dp, palette.border)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Dhan-OM v1.4", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.accent)
                    Text("Private by default · ledger never uploaded · speak any language", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                }
            }
        }
    }
}
