package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.viewmodel.LedgerSort
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerTableScreen(
    transactions: List<TransactionEntity>,
    searchQuery: String,
    filterCategory: TransactionCategory?,
    filterType: TransactionType?,
    sortOrder: LedgerSort,
    onSearchChange: (String) -> Unit,
    onFilterCategoryChange: (TransactionCategory?) -> Unit,
    onFilterTypeChange: (TransactionType?) -> Unit,
    onSortChange: (LedgerSort) -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onResetSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("ledger_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("ledger_table_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Stats Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Financial Ledger",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${transactions.size} records match active filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Ledger")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date: Newest First") },
                                onClick = { onSortChange(LedgerSort.DATE_DESC); sortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Date: Oldest First") },
                                onClick = { onSortChange(LedgerSort.DATE_ASC); sortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount: Highest First") },
                                onClick = { onSortChange(LedgerSort.AMOUNT_DESC); sortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount: Lowest First") },
                                onClick = { onSortChange(LedgerSort.AMOUNT_ASC); sortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Category Name") },
                                onClick = { onSortChange(LedgerSort.CATEGORY); sortMenuExpanded = false }
                            )
                        }

                        IconButton(onClick = onResetSampleData) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = "Reset sample data")
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search transactions, merchants, accounts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_field"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Type Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { onFilterTypeChange(null) },
                        label = { Text("All Types") }
                    )
                    FilterChip(
                        selected = filterType == TransactionType.EXPENSE,
                        onClick = { onFilterTypeChange(TransactionType.EXPENSE) },
                        label = { Text("Expenses") }
                    )
                    FilterChip(
                        selected = filterType == TransactionType.INCOME,
                        onClick = { onFilterTypeChange(TransactionType.INCOME) },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = filterType == TransactionType.TRANSFER,
                        onClick = { onFilterTypeChange(TransactionType.TRANSFER) },
                        label = { Text("Transfers") }
                    )
                }
            }

            // Category Filter Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { onFilterCategoryChange(null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(TransactionCategory.entries) { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { onFilterCategoryChange(if (filterCategory == cat) null else cat) },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }

            // Transactions Table List
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try clearing search or filter parameters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    LedgerCardItem(
                        transaction = tx,
                        onClick = { selectedTxForDetails = tx },
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }

    // Detail BottomSheet or Dialog
    selectedTxForDetails?.let { tx ->
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { selectedTxForDetails = null },
            title = { Text(tx.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Amount: $${String.format(Locale.US, "%.2f", tx.amount)} (${tx.type.name})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == TransactionType.INCOME) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                    Text("Category: ${tx.category.displayName} (${tx.necessity.name})")
                    if (tx.merchant.isNotBlank()) Text("Merchant: ${tx.merchant}")
                    Text("Account: ${tx.account}")
                    Text("Recorded: ${dateFormat.format(Date(tx.timestamp))}")
                    if (tx.notes.isNotBlank()) Text("Notes: ${tx.notes}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = tx
                    selectedTxForDetails = null
                    onEditTransaction(target)
                }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun LedgerCardItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.type == TransactionType.TRANSFER

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("ledger_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIncome) Color(0xFF10B981).copy(alpha = 0.15f)
                        else if (isTransfer) Color(0xFF06B6D4).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp
                    else if (isTransfer) Icons.Default.SwapHoriz
                    else Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = if (isIncome) Color(0xFF10B981)
                    else if (isTransfer) Color(0xFF06B6D4)
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${transaction.category.displayName} • ${dateFormat.format(Date(transaction.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.merchant.isNotBlank() && transaction.merchant != transaction.title) {
                    Text(
                        text = "Store: ${transaction.merchant}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}$${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.account,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
