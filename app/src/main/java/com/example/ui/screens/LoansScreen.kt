package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.model.LoanEntity
import com.example.data.model.LoanType
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loans: List<LoanEntity>,
    onAddClick: () -> Unit,
    onEdit: (LoanEntity) -> Unit,
    onDelete: (LoanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val totalOutstanding = loans.sumOf { it.outstandingAmount }
    val totalDebt = loans.filter { it.type == LoanType.DEBT }.sumOf { it.outstandingAmount }
    val totalBorrowed = loans.filter { it.type == LoanType.LOAN }.sumOf { it.outstandingAmount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = palette.primary, contentColor = palette.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Add loan")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding).testTag("loans_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = palette.primaryContainer) {
                    Column(Modifier.padding(20.dp)) {
                        Text("TOTAL OUTSTANDING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                        Text("₹${String.format(Locale.US, "%,.0f", totalOutstanding)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("Borrowed", style = MaterialTheme.typography.labelSmall, color = palette.onPrimaryContainer)
                                Text("₹${String.format(Locale.US, "%,.0f", totalBorrowed)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Owed (debts)", style = MaterialTheme.typography.labelSmall, color = palette.onPrimaryContainer)
                                Text("₹${String.format(Locale.US, "%,.0f", totalDebt)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.expenseRed)
                            }
                        }
                    }
                }
            }

            if (loans.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No loans or debts yet. Tap + to add a home loan, personal loan, credit-card debt, etc.", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryText)
                    }
                }
            } else {
                items(loans, key = { it.id }) { loan ->
                    LoanCard(loan = loan, onEdit = { onEdit(loan) }, onDelete = { onDelete(loan) })
                }
            }
        }
    }
}

@Composable
private fun LoanCard(loan: LoanEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalAppPalette.current
    val isDebt = loan.type == LoanType.DEBT
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDebt) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = if (isDebt) palette.expenseRed else palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(loan.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${loan.type.displayName} · ${String.format(Locale.US, "%.1f", loan.interestRate)}% p.a.", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                    }
                }
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = palette.expenseRed, modifier = Modifier.size(18.dp)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                Text("₹${String.format(Locale.US, "%,.0f", loan.outstandingAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isDebt) palette.expenseRed else palette.accent)
            }
            if (loan.monthlyEmi > 0) {
                Text("EMI: ₹${String.format(Locale.US, "%,.0f", loan.monthlyEmi)}/month", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanDialog(
    existing: LoanEntity?,
    onDismiss: () -> Unit,
    onSave: (LoanEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: LoanType.LOAN) }
    var principal by remember { mutableStateOf(existing?.principalAmount?.toString() ?: "") }
    var outstanding by remember { mutableStateOf(existing?.outstandingAmount?.toString() ?: "") }
    var rate by remember { mutableStateOf(existing?.interestRate?.toString() ?: "") }
    var emi by remember { mutableStateOf(existing?.monthlyEmi?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add loan / debt" else "Edit loan / debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title (e.g. Home Loan, Credit Card)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoanType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.displayName) }
                        )
                    }
                }
                OutlinedTextField(principal, { principal = it }, label = { Text("Principal amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(outstanding, { outstanding = it }, label = { Text("Outstanding amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rate, { rate = it }, label = { Text("Interest rate (% p.a.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(emi, { emi = it }, label = { Text("Monthly EMI") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val principalD = principal.toDoubleOrNull() ?: 0.0
                val outstandingD = outstanding.toDoubleOrNull() ?: principalD
                onSave(
                    LoanEntity(
                        id = existing?.id ?: 0,
                        title = title.ifBlank { "Untitled" },
                        type = type,
                        principalAmount = principalD,
                        outstandingAmount = outstandingD,
                        interestRate = rate.toDoubleOrNull() ?: 0.0,
                        monthlyEmi = emi.toDoubleOrNull() ?: 0.0
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
