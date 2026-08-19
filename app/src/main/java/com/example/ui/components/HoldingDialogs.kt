package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHoldingDialog(
    existing: PortfolioHoldingEntity?,
    onDismiss: () -> Unit,
    onSave: (PortfolioHoldingEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.instrumentName ?: "") }
    var symbol by remember { mutableStateOf(existing?.symbol ?: "") }
    var assetClass by remember { mutableStateOf(existing?.assetClass ?: AssetClass.MUTUAL_FUND) }
    var region by remember { mutableStateOf(existing?.region ?: InvestmentRegion.INDIA) }
    var qtyStr by remember { mutableStateOf(existing?.quantity?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var avgPriceStr by remember { mutableStateOf(existing?.avgBuyPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var currentPriceStr by remember { mutableStateOf(existing?.currentPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var isSip by remember { mutableStateOf(existing?.isSip ?: false) }
    var sipAmountStr by remember { mutableStateOf(existing?.sipMonthlyAmount?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    var assetExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Holding" else "Add Holding", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Instrument Name") },
                    placeholder = { Text("e.g. Nifty 50 Index Fund") },
                    modifier = Modifier.fillMaxWidth().testTag("holding_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = symbol, onValueChange = { symbol = it },
                    label = { Text("Symbol/Ticker (Optional)") },
                    placeholder = { Text("e.g. NIFTYBEES") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(expanded = assetExpanded, onExpandedChange = { assetExpanded = it }) {
                    OutlinedTextField(
                        value = assetClass.displayName, onValueChange = {}, readOnly = true,
                        label = { Text("Asset Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = assetExpanded, onDismissRequest = { assetExpanded = false }) {
                        AssetClass.entries.forEach { ac ->
                            DropdownMenuItem(text = { Text(ac.displayName) }, onClick = { assetClass = ac; assetExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = regionExpanded, onExpandedChange = { regionExpanded = it }) {
                    OutlinedTextField(
                        value = region.displayName, onValueChange = {}, readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                        InvestmentRegion.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(r.displayName) }, onClick = { region = r; regionExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = qtyStr, onValueChange = { qtyStr = it },
                    label = { Text("Quantity / Units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = avgPriceStr, onValueChange = { avgPriceStr = it },
                    label = { Text("Avg Buy Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = currentPriceStr, onValueChange = { currentPriceStr = it },
                    label = { Text("Current Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSip, onCheckedChange = { isSip = it })
                    Text("This is a SIP (Systematic Investment Plan)")
                }

                if (isSip) {
                    OutlinedTextField(
                        value = sipAmountStr, onValueChange = { sipAmountStr = it },
                        label = { Text("Monthly SIP Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val avg = avgPriceStr.toDoubleOrNull() ?: 0.0
                    val curr = currentPriceStr.toDoubleOrNull() ?: avg
                    val invested = qty * avg
                    val current = qty * curr
                    val sipAmt = if (isSip) sipAmountStr.toDoubleOrNull() ?: 0.0 else 0.0
                    if (name.isNotBlank() && qty > 0) {
                        val holding = PortfolioHoldingEntity(
                            id = existing?.id ?: 0,
                            instrumentName = name.trim(),
                            symbol = symbol.trim(),
                            assetClass = assetClass,
                            region = region,
                            quantity = qty,
                            avgBuyPrice = avg,
                            currentPrice = curr,
                            investedAmount = if (existing != null) existing.investedAmount else invested,
                            currentValue = current,
                            currency = Currency.INR.code,
                            purchaseDate = existing?.purchaseDate ?: System.currentTimeMillis(),
                            notes = notes.trim(),
                            isSip = isSip,
                            sipMonthlyAmount = sipAmt
                        )
                        onSave(holding)
                    }
                },
                modifier = Modifier.testTag("holding_save_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
