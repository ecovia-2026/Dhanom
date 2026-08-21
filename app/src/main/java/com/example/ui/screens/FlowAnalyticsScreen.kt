package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.CashFlowchartData
import com.example.domain.analytics.CategoryExpense
import com.example.domain.analytics.DailyTrendPoint
import com.example.ui.components.CashFlowchartView
import com.example.ui.components.CategoryDonutChartView
import com.example.ui.components.DailyTrendChartView
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun FlowAnalyticsScreen(
    flowchartData: CashFlowchartData,
    categoryExpenses: List<CategoryExpense>,
    dailyTrends: List<DailyTrendPoint>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackgroundLight)
            .testTag("flow_analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Full Cash Flowchart
        item {
            CashFlowchartView(data = flowchartData)
        }

        // Section 2: Donut Category Breakdown
        item {
            CategoryDonutChartView(expenses = categoryExpenses)
        }

        // Section 3: Cash Flow Velocity Chart
        item {
            DailyTrendChartView(trends = dailyTrends)
        }

        // Section 4: Category Breakdown Ledger Table
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_table_card"),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Category Analytics Table",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BentoDeepPurple
                    )
                    Text(
                        text = "Complete distribution ranking across active spending",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoSecondaryText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoSurfaceLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoDeepPurple,
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = "Count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoDeepPurple,
                            modifier = Modifier.weight(0.7f)
                        )
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoDeepPurple,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoDeepPurple,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    categoryExpenses.forEach { exp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1.5f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(exp.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = exp.category.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BentoOnBackgroundLight,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${exp.count} tx",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoSecondaryText,
                                modifier = Modifier.weight(0.7f)
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", exp.amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoOnBackgroundLight,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", exp.percentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = exp.color,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
