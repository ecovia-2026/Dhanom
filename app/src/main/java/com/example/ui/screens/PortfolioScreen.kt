package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.domain.analytics.AssetClassAllocation
import com.example.domain.analytics.PortfolioSummary
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    holdings: List<PortfolioHoldingEntity>,
    portfolioSummary: PortfolioSummary,
    assetAllocations: List<AssetClassAllocation>,
    onAddHoldingClick: () -> Unit,
    onEditHolding: (PortfolioHoldingEntity) -> Unit,
    onDeleteHolding: (PortfolioHoldingEntity) -> Unit,
    onUpdatePrices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHoldingClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("portfolio_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Holding")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("portfolio_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PortfolioSummaryCard(summary = portfolioSummary, onUpdatePrices = onUpdatePrices)
            }

            if (assetAllocations.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Asset Allocation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoDeepPurple
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                assetAllocations.forEach { alloc ->
                                    if (alloc.percentage > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(max(0.01f, alloc.percentage.toFloat()))
                                                .fillMaxHeight()
                                                .background(getAssetClassColor(alloc.assetClass))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            assetAllocations.forEach { alloc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getAssetClassColor(alloc.assetClass))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = alloc.assetClass.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", alloc.percentage)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getAssetClassColor(alloc.assetClass)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${if (alloc.pnlPercent >= 0) "+" else ""}${String.format(Locale.US, "%.1f", alloc.pnlPercent)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (alloc.pnlPercent >= 0) BentoActiveGreen else BentoExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Holdings (${holdings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoDeepPurple
                )
            }

            if (holdings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No holdings yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Add stocks, mutual funds, gold, FDs to track your portfolio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(holdings, key = { it.id }) { holding ->
                    HoldingCard(
                        holding = holding,
                        onEdit = { onEditHolding(holding) },
                        onDelete = { onDeleteHolding(holding) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    onUpdatePrices: () -> Unit
) {
    val isProfit = summary.totalPnl >= 0
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("portfolio_summary_card"),
        shape = RoundedCornerShape(24.dp),
        color = BentoDarkCard
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL PORTFOLIO VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${Currency.INR.symbol}${String.format(Locale.US, "%,.2f", summary.totalCurrentValue)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onUpdatePrices) {
                    Icon(Icons.Default.Refresh, contentDescription = "Update Prices", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatBox(
                    label = "Invested",
                    value = "${Currency.INR.symbol}${String.format(Locale.US, "%,.0f", summary.totalInvested)}",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    label = "P&L",
                    value = "${if (isProfit) "+" else ""}${Currency.INR.symbol}${String.format(Locale.US, "%,.0f", summary.totalPnl)}",
                    valueColor = if (isProfit) BentoActiveGreen else BentoExpenseRed,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    label = "Returns",
                    value = "${if (isProfit) "+" else ""}${String.format(Locale.US, "%.1f", summary.totalPnlPercent)}%",
                    valueColor = if (isProfit) BentoActiveGreen else BentoExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary.monthlySipTotal > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, tint = BentoActiveGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Monthly SIP: ${Currency.INR.symbol}${String.format(Locale.US, "%,.0f", summary.monthlySipTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun HoldingCard(
    holding: PortfolioHoldingEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isProfit = holding.unrealizedPnl >= 0
    val pnlColor = if (isProfit) BentoActiveGreen else BentoExpenseRed
    val currencySymbol = Currency.fromCode(holding.currency).symbol

    Card(
        modifier = Modifier.fillMaxWidth().testTag("holding_card_${holding.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(getAssetClassColor(holding.assetClass).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getAssetClassIcon(holding.assetClass),
                            contentDescription = null,
                            tint = getAssetClassColor(holding.assetClass),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = holding.instrumentName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${holding.assetClass.displayName} - ${holding.region.displayName}${if (holding.isSip) " - SIP" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$currencySymbol${String.format(Locale.US, "%,.0f", holding.investedAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$currencySymbol${String.format(Locale.US, "%,.0f", holding.currentValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${if (isProfit) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", holding.unrealizedPnl)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                    Text(
                        "${if (isProfit) "+" else ""}${String.format(Locale.US, "%.1f", holding.unrealizedPnlPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }
            }
        }
    }
}

private fun getAssetClassColor(assetClass: AssetClass): Color = when (assetClass) {
    AssetClass.LARGE_CAP -> Color(0xFF3B82F6)
    AssetClass.MID_SMALL_CAP -> Color(0xFF8B5CF6)
    AssetClass.INDEX_ETF -> Color(0xFF06B6D4)
    AssetClass.MUTUAL_FUND -> Color(0xFF10B981)
    AssetClass.DEBT_FD -> Color(0xFFF59E0B)
    AssetClass.GOLD -> Color(0xFFEAB308)
    AssetClass.INTERNATIONAL -> Color(0xFFEC4899)
    AssetClass.CRYPTO -> Color(0xFFEF4444)
    AssetClass.REIT -> Color(0xFF14B8A6)
    AssetClass.PPF_EPF -> Color(0xFF6366F1)
    AssetClass.BONDS -> Color(0xFF94A3B8)
}

private fun getAssetClassIcon(assetClass: AssetClass) = when (assetClass) {
    AssetClass.LARGE_CAP -> Icons.Default.TrendingUp
    AssetClass.MID_SMALL_CAP -> Icons.Default.ShowChart
    AssetClass.INDEX_ETF -> Icons.Default.StackedBarChart
    AssetClass.MUTUAL_FUND -> Icons.Default.Savings
    AssetClass.DEBT_FD -> Icons.Default.AccountBalance
    AssetClass.GOLD -> Icons.Default.Stars
    AssetClass.INTERNATIONAL -> Icons.Default.Public
    AssetClass.CRYPTO -> Icons.Default.CurrencyBitcoin
    AssetClass.REIT -> Icons.Default.Apartment
    AssetClass.PPF_EPF -> Icons.Default.Savings
    AssetClass.BONDS -> Icons.Default.Receipt
}
