package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportManager
import com.example.data.model.*
import com.example.domain.analytics.CashFlowSummary
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ReportsExportScreen(
    transactions: List<TransactionEntity>,
    holdings: List<PortfolioHoldingEntity>,
    budgets: List<BudgetEntity>,
    goals: List<GoalEntity>,
    memories: List<BrainMemoryEntity>,
    chatMessages: List<ChatMessageEntity>,
    cashFlowSummary: CashFlowSummary,
    onExportBackup: () -> Unit,
    onImportBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)
                    ?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                if (!json.isNullOrBlank()) {
                    onImportBackup(json)
                    statusMessage = "Importing backup from ${it.lastPathSegment ?: "file"}..."
                } else {
                    statusMessage = "Could not read the selected file."
                }
            } catch (e: Exception) {
                statusMessage = "Import failed: ${e.message}"
            }
        }
    }

    fun shareFile(file: java.io.File, mimeType: String) {
        try {
            val shareIntent = ExportManager.createShareIntent(context, file, mimeType)
            val chooser = ExportManager.createShareChooser(shareIntent)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            statusMessage = "Sharing ${file.name}..."
        } catch (e: Exception) {
            statusMessage = "Share failed: ${e.message}"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_export_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Export & Reports",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Export your financial data or share to another device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Export Data Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ExportOptionRow(
                        icon = Icons.Default.TableChart,
                        title = "Transactions CSV (Excel)",
                        subtitle = "${transactions.size} transactions - Opens in Excel/Sheets",
                        onClick = {
                            val file = ExportManager.exportTransactionsCsv(context, transactions)
                            shareFile(file, "text/csv")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExportOptionRow(
                        icon = Icons.Default.ShowChart,
                        title = "Portfolio CSV (Excel)",
                        subtitle = "${holdings.size} holdings - With P&L calculations",
                        onClick = {
                            val file = ExportManager.exportPortfolioCsv(context, holdings)
                            shareFile(file, "text/csv")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExportOptionRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Full Financial Report (PDF)",
                        subtitle = "Summary + transactions + portfolio + goals",
                        onClick = {
                            val file = ExportManager.exportPdfReport(context, cashFlowSummary, transactions, holdings, goals)
                            shareFile(file, "application/pdf")
                        }
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Device Transfer & Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export a full backup JSON file and share it via QuickShare, Nearby Share, Bluetooth, or email to move all your data to a new device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onExportBackup,
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create & Share Full Backup")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("import_backup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Backup From Device")
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Data Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DataSummaryRow("Transactions", transactions.size.toString())
                    DataSummaryRow("Portfolio Holdings", holdings.size.toString())
                    DataSummaryRow("Budgets", budgets.size.toString())
                    DataSummaryRow("Savings Goals", goals.size.toString())
                    DataSummaryRow("AI Brain Memories", memories.size.toString())
                    DataSummaryRow("Chat Messages", chatMessages.size.toString())
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DataSummaryRow("Total Portfolio Value", "${Currency.INR.symbol}${String.format(Locale.US, "%,.0f", holdings.sumOf { it.currentValue })}")
                    DataSummaryRow("Monthly Net Cash Flow", "${Currency.INR.symbol}${String.format(Locale.US, "%,.0f", cashFlowSummary.netCashFlow)}")
                    DataSummaryRow("Financial Health Score", "${cashFlowSummary.healthScore}/100 (${cashFlowSummary.healthGrade})")
                }
            }
        }

        statusMessage?.let { msg ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DataSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
