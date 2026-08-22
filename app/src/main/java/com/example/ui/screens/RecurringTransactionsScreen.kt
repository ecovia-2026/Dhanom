package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.theme.LocalAppPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RecurringTransactionsScreen(
    recurring: List<RecurringTransactionEntity>,
    accounts: List<AccountEntity>,
    onAdd: () -> Unit,
    onEdit: (RecurringTransactionEntity) -> Unit,
    onDelete: (RecurringTransactionEntity) -> Unit,
    onProcessNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = palette.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("RECURRING TRANSACTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer, letterSpacing = 1.sp)
                    Text(
                        "${recurring.count { it.isActive }} active",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onPrimaryContainer
                    )
                    Text(
                        "Salaries, rent, EMIs and subscriptions are auto-posted when they come due.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Scheduled payments", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.accent)
                    Text("Auto-posts the real transaction each due date", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                }
                FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }

        if (recurring.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No recurring transactions yet.\n\nAdd your salary, rent, EMI or subscription and Dhan-OM will post it automatically when due.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }
            }
        } else {
            items(recurring, key = { it.id }) { r ->
                RecurringRow(r, onEdit = { onEdit(r) }, onDelete = { onDelete(r) })
            }
        }

        if (recurring.any { it.isActive }) {
            item {
                OutlinedButton(onClick = onProcessNow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Post due now")
                }
            }
        }
    }
}

@Composable
private fun RecurringRow(r: RecurringTransactionEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalAppPalette.current
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isIncome = r.type == TransactionType.INCOME
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (isIncome) Color(0x332E7D32) else palette.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) Color(0xFF2E7D32) else palette.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.accent)
                Text(
                    "${r.scheduleLabel()} · ${r.category.displayName} · ${r.account}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
                Text(
                    "Next: " + (if (r.nextDueDateMillis > 0) fmt.format(Date(r.nextDueDateMillis)) else "today"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (r.isActive) palette.positiveGreen else palette.secondaryText
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "-"}₹${String.format(Locale.US, "%,.0f", r.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF2E7D32) else palette.accent
                )
                Text(if (r.isActive) "active" else "paused", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = palette.primary, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringDialog(
    existing: RecurringTransactionEntity?,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (RecurringTransactionEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountStr by remember { mutableStateOf(existing?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(existing?.category ?: TransactionCategory.OTHER) }
    var account by remember { mutableStateOf(existing?.account ?: accounts.firstOrNull()?.name ?: "Bank Account") }
    var recurrence by remember { mutableStateOf(existing?.recurrence ?: TaskRecurrence.MONTHLY) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var recurrenceExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Recurring" else "Add Recurring", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, placeholder = { Text("e.g. Monthly Rent") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                OutlinedTextField(amountStr, { amountStr = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Type
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        onClick = { type = TransactionType.EXPENSE },
                        selected = type == TransactionType.EXPENSE
                    ) { Text("Expense") }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        onClick = { type = TransactionType.INCOME },
                        selected = type == TransactionType.INCOME
                    ) { Text("Income") }
                }

                // Recurrence dropdown
                ExposedDropdownMenuBox(expanded = recurrenceExpanded, onExpandedChange = { recurrenceExpanded = it }) {
                    OutlinedTextField(
                        value = recurrenceLabel(recurrence),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repeats") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = recurrenceExpanded, onDismissRequest = { recurrenceExpanded = false }) {
                        TaskRecurrence.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(recurrenceLabel(r)) }, onClick = { recurrence = r; recurrenceExpanded = false })
                        }
                    }
                }

                // Category dropdown
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        TransactionCategory.entries.forEach { c ->
                            DropdownMenuItem(text = { Text(c.displayName) }, onClick = { category = c; categoryExpanded = false })
                        }
                    }
                }

                // Account dropdown
                ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        singleLine = true
                    )
                    if (accounts.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                            accounts.forEach { a ->
                                DropdownMenuItem(text = { Text(a.name) }, onClick = { account = a.name; accountExpanded = false })
                            }
                        }
                    }
                }

                Text(
                    "First post: today. I'll roll the schedule forward automatically after each post.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && amt > 0) {
                    onSave(
                        RecurringTransactionEntity(
                            id = existing?.id ?: 0,
                            title = title.trim(),
                            amount = amt,
                            type = type,
                            category = category,
                            necessity = category.defaultNecessity,
                            account = account.ifBlank { "Bank Account" },
                            recurrence = recurrence,
                            nextDueDateMillis = existing?.nextDueDateMillis ?: Calendar.getInstance().timeInMillis,
                            endDateMillis = existing?.endDateMillis ?: 0L,
                            notes = existing?.notes ?: "",
                            isActive = true
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun recurrenceLabel(r: TaskRecurrence): String = when (r) {
    TaskRecurrence.ONCE -> "Once"
    TaskRecurrence.DAILY -> "Daily"
    TaskRecurrence.WEEKLY -> "Weekly"
    TaskRecurrence.MONTHLY -> "Monthly"
    TaskRecurrence.QUARTERLY -> "Quarterly (3 months)"
    TaskRecurrence.YEARLY -> "Yearly"
}
