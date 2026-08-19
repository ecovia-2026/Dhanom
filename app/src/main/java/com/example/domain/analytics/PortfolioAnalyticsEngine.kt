package com.example.domain.analytics

import com.example.data.model.*
import java.util.Locale

data class PortfolioSummary(
    val totalInvested: Double,
    val totalCurrentValue: Double,
    val totalPnl: Double,
    val totalPnlPercent: Double,
    val monthlySipTotal: Double,
    val indiaAllocation: Double,
    val internationalAllocation: Double,
    val assetClassBreakdown: Map<AssetClass, Double>
) {
    val indiaAllocationPercent: Double
        get() = if (totalCurrentValue > 0) (indiaAllocation / totalCurrentValue) * 100.0 else 0.0
    val internationalAllocationPercent: Double
        get() = if (totalCurrentValue > 0) (internationalAllocation / totalCurrentValue) * 100.0 else 0.0
}

data class AssetClassAllocation(
    val assetClass: AssetClass,
    val investedAmount: Double,
    val currentValue: Double,
    val percentage: Double,
    val pnlPercent: Double
)

object PortfolioAnalyticsEngine {

    fun calculatePortfolioSummary(holdings: List<PortfolioHoldingEntity>): PortfolioSummary {
        val totalInvested = holdings.sumOf { it.investedAmount }
        val totalCurrent = holdings.sumOf { it.currentValue }
        val totalPnl = totalCurrent - totalInvested
        val totalPnlPct = if (totalInvested > 0) (totalPnl / totalInvested) * 100.0 else 0.0
        val monthlySip = holdings.filter { it.isSip }.sumOf { it.sipMonthlyAmount }

        val indiaAlloc = holdings.filter { it.region == InvestmentRegion.INDIA }.sumOf { it.currentValue }
        val intlAlloc = holdings.filter { it.region != InvestmentRegion.INDIA }.sumOf { it.currentValue }

        val breakdown = holdings.groupBy { it.assetClass }
            .mapValues { it.value.sumOf { h -> h.currentValue } }

        return PortfolioSummary(
            totalInvested = totalInvested,
            totalCurrentValue = totalCurrent,
            totalPnl = totalPnl,
            totalPnlPercent = totalPnlPct,
            monthlySipTotal = monthlySip,
            indiaAllocation = indiaAlloc,
            internationalAllocation = intlAlloc,
            assetClassBreakdown = breakdown
        )
    }

    fun calculateAssetClassAllocations(holdings: List<PortfolioHoldingEntity>): List<AssetClassAllocation> {
        val totalCurrent = holdings.sumOf { it.currentValue }.coerceAtLeast(1.0)
        return holdings.groupBy { it.assetClass }
            .map { (assetClass, list) ->
                val invested = list.sumOf { it.investedAmount }
                val current = list.sumOf { it.currentValue }
                AssetClassAllocation(
                    assetClass = assetClass,
                    investedAmount = invested,
                    currentValue = current,
                    percentage = (current / totalCurrent) * 100.0,
                    pnlPercent = if (invested > 0) ((current - invested) / invested) * 100.0 else 0.0
                )
            }
            .sortedByDescending { it.currentValue }
    }

    fun generateDiversificationAdvice(summary: PortfolioSummary): String {
        val sb = StringBuilder()
        sb.append("Portfolio Diversification Analysis:\n\n")

        val goldPct = summary.assetClassBreakdown[AssetClass.GOLD]?.let { (it / summary.totalCurrentValue.coerceAtLeast(1.0)) * 100.0 } ?: 0.0
        val debtPct = (summary.assetClassBreakdown[AssetClass.DEBT_FD] ?: 0.0).let {
            (it / summary.totalCurrentValue.coerceAtLeast(1.0)) * 100.0
        }
        val equityPct = listOf(AssetClass.LARGE_CAP, AssetClass.MID_SMALL_CAP, AssetClass.INDEX_ETF, AssetClass.MUTUAL_FUND)
            .sumOf { summary.assetClassBreakdown[it] ?: 0.0 }
            .let { (it / summary.totalCurrentValue.coerceAtLeast(1.0)) * 100.0 }

        sb.append("Equity: ${String.format(Locale.US, "%.0f", equityPct)}%\n")
        sb.append("Gold: ${String.format(Locale.US, "%.0f", goldPct)}%\n")
        sb.append("Debt/Fixed: ${String.format(Locale.US, "%.0f", debtPct)}%\n")
        sb.append("India: ${String.format(Locale.US, "%.0f", summary.indiaAllocationPercent)}% | International: ${String.format(Locale.US, "%.0f", summary.internationalAllocationPercent)}%\n\n")

        when {
            equityPct > 80 -> sb.append("High equity concentration. Consider rebalancing toward debt/gold for stability.")
            equityPct < 30 -> sb.append("Low equity allocation. For long-term wealth, consider increasing equity SIPs.")
            goldPct > 20 -> sb.append("Gold allocation above 20% may limit growth. Ideal range is 5-15%.")
            summary.internationalAllocationPercent < 5 -> sb.append("Consider adding international exposure (5-15%) for geographic diversification.")
            else -> sb.append("Well-diversified portfolio across asset classes and geographies.")
        }

        return sb.toString()
    }
}
