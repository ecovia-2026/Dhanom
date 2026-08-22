package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.data.model.TransactionCategory
import com.example.ui.theme.LocalAppPalette
import com.example.ui.viewmodel.EnvelopeProgress
import java.util.Locale

@Composable
fun CategoriesScreen(
    envelopes: List<EnvelopeProgress>,
    onSetBudget: (TransactionCategory, Double) -> Unit,
    onClearBudget: (TransactionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val totalBudget = envelopes.sumOf { it.monthlyLimit }
    val totalSpent = envelopes.sumOf { it.spentAmount }
    val remaining = totalBudget - totalSpent
    var editing by remember { mutableStateOf<TransactionCategory?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("MONTHLY ENVELOPES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer, letterSpacing = 1.sp)
                    Text(
                        "₹${String.format(Locale.US, "%,.0f", remaining)} left",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onPrimaryContainer
                    )
                    Text(
                        "Spent ₹${String.format(Locale.US, "%,.0f", totalSpent)} of ₹${String.format(Locale.US, "%,.0f", totalBudget)} budgeted",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Text("Categories & Envelopes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.accent)
            Text("Tap a category to set its monthly budget (Goodbudget-style)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
        }

        items(envelopes, key = { it.category.name }) { env ->
            EnvelopeRow(
                envelope = env,
                onClick = { editing = env.category }
            )
        }
    }

    editing?.let { cat ->
        val current = envelopes.firstOrNull { it.category == cat }
        SetEnvelopeDialog(
            category = cat,
            currentLimit = current?.monthlyLimit ?: 0.0,
            onDismiss = { editing = null },
            onSave = { limit -> onSetBudget(cat, limit); editing = null },
            onClear = { onClearBudget(cat); editing = null }
        )
    }
}

@Composable
private fun EnvelopeRow(envelope: EnvelopeProgress, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val progressColor = when {
        envelope.isOverBudget -> Color(0xFFD32F2F)
        envelope.isNearLimit -> Color(0xFFF59E0B)
        else -> palette.positiveGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(palette.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIcon(envelope.category), contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(envelope.category.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.accent)
                    Text(
                        if (envelope.hasBudget) "Spent ₹${String.format(Locale.US, "%,.0f", envelope.spentAmount)} of ₹${String.format(Locale.US, "%,.0f", envelope.monthlyLimit)}"
                        else "No budget set — tap to add one",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                }
                if (envelope.hasBudget) {
                    Text(
                        if (envelope.isOverBudget) "Over by ₹${String.format(Locale.US, "%,.0f", envelope.spentAmount - envelope.monthlyLimit)}"
                        else "₹${String.format(Locale.US, "%,.0f", envelope.remainingAmount)} left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }
            if (envelope.hasBudget) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { envelope.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = palette.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun SetEnvelopeDialog(
    category: TransactionCategory,
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onClear: () -> Unit
) {
    var limitStr by remember { mutableStateOf(if (currentLimit > 0) String.format(Locale.US, "%.0f", currentLimit) else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${category.displayName} envelope", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set the monthly budget (envelope) for this category. You'll see spent vs remaining and a progress bar each month.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monthly limit (₹)") },
                    placeholder = { Text("e.g. 8000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { val l = limitStr.toDoubleOrNull() ?: 0.0; if (l > 0) onSave(l) }) { Text("Set budget") }
        },
        dismissButton = {
            Row {
                if (currentLimit > 0) {
                    TextButton(onClick = onClear) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

fun categoryIcon(cat: TransactionCategory): ImageVector = when (cat) {
    TransactionCategory.HOUSING -> Icons.Default.Home
    TransactionCategory.GROCERIES -> Icons.Default.ShoppingCart
    TransactionCategory.UTILITIES -> Icons.Default.Bolt
    TransactionCategory.TRANSPORTATION -> Icons.Default.DirectionsBus
    TransactionCategory.HEALTHCARE -> Icons.Default.Favorite
    TransactionCategory.DINING -> Icons.Default.Restaurant
    TransactionCategory.ENTERTAINMENT -> Icons.Default.Movie
    TransactionCategory.SHOPPING -> Icons.Default.ShoppingBag
    TransactionCategory.TRAVEL -> Icons.Default.Flight
    TransactionCategory.EDUCATION -> Icons.Default.School
    TransactionCategory.INVESTMENT -> Icons.Default.ShowChart
    TransactionCategory.SAVINGS_TRANSFER -> Icons.Default.Savings
    TransactionCategory.SALARY -> Icons.Default.Payments
    TransactionCategory.FREELANCE -> Icons.Default.Work
    TransactionCategory.INVESTMENT_RETURN -> Icons.Default.TrendingUp
    TransactionCategory.INSURANCE -> Icons.Default.Shield
    TransactionCategory.TAX -> Icons.AutoMirrored.Filled.ReceiptLong
    TransactionCategory.MUTUAL_FUND -> Icons.Default.PieChart
    TransactionCategory.GOLD -> Icons.Default.Diamond
    TransactionCategory.CRYPTO -> Icons.Default.CurrencyBitcoin
    TransactionCategory.GIFTS_DONATIONS -> Icons.Default.CardGiftcard
    TransactionCategory.SUBSCRIPTIONS -> Icons.Default.Subscriptions
    TransactionCategory.OTHER -> Icons.Default.Category
}
