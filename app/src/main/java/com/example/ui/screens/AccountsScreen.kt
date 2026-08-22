package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.theme.LocalAppPalette
import java.util.Locale

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    balances: Map<Long, Double>,
    onAdd: () -> Unit,
    onEdit: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val total = balances.values.sum()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Net worth / total balance header card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("TOTAL BALANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer, letterSpacing = 1.sp)
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", total)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onPrimaryContainer
                    )
                    Text("across ${accounts.size} account(s)", style = MaterialTheme.typography.bodySmall, color = palette.onPrimaryContainer)
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
                    Text("Your Accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.accent)
                    Text("Transactions map onto these accounts", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                }
                FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add account")
                }
            }
        }

        if (accounts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No accounts yet. Add a Cash, Bank, Credit Card or Wallet account to start mapping your money.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }
            }
        } else {
            items(accounts, key = { it.id }) { acc ->
                AccountRow(
                    account = acc,
                    balance = balances[acc.id] ?: acc.initialBalance,
                    onEdit = { onEdit(acc) },
                    onDelete = { onDelete(acc) }
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountEntity,
    balance: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalAppPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(account.colorArgb)),
                contentAlignment = Alignment.Center
            ) {
                Icon(accountTypeIcon(account.type), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = palette.accent)
                Text(account.type.displayName, style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format(Locale.US, "%,.2f", balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) palette.positiveGreen else palette.expenseRed
                )
                Text("balance", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = palette.primary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Default.Payments
    AccountType.BANK -> Icons.Default.AccountBalance
    AccountType.CREDIT_CARD -> Icons.Default.CreditCard
    AccountType.WALLET -> Icons.Default.AccountBalanceWallet
    AccountType.INVESTMENT -> Icons.Default.ShowChart
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    existing: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: AccountType.BANK) }
    var initialStr by remember { mutableStateOf(existing?.initialBalance?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Account" else "Add Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account name") },
                    placeholder = { Text("e.g. HDFC Savings") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        AccountType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = initialStr,
                    onValueChange = { initialStr = it },
                    label = { Text("Starting balance (₹)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initial = initialStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(
                            AccountEntity(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                type = type,
                                initialBalance = initial,
                                colorArgb = existing?.colorArgb ?: defaultAccountColor(type),
                                icon = existing?.icon ?: ""
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun defaultAccountColor(type: AccountType): Long = when (type) {
    AccountType.CASH -> 0xFF0E9F6E
    AccountType.BANK -> 0xFF0B6BCB
    AccountType.CREDIT_CARD -> 0xFFD6336C
    AccountType.WALLET -> 0xFFE8590C
    AccountType.INVESTMENT -> 0xFF6750A4
}
