package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.DailyTrendPoint
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.max

@Composable
fun DailyTrendChartView(
    trends: List<DailyTrendPoint>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_trend_chart_card"),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Flow Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BentoDeepPurple
                    )
                    Text(
                        text = "Inflow vs Outflow over time",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoSecondaryText
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendPill(color = BentoPrimaryPurple, label = "Inflow")
                    LegendPill(color = BentoLilacContainer, label = "Outflow")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (trends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Insufficient trend data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = remember(trends) {
                    max(100.0, trends.maxOfOrNull { max(it.inflow, it.outflow) } ?: 100.0)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = 10.dp.toPx()
                        val groupSpacing = size.width / trends.size.toFloat()
                        val chartHeight = size.height - 24.dp.toPx()

                        // Base grid line
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, chartHeight),
                            end = Offset(size.width, chartHeight),
                            strokeWidth = 1.dp.toPx()
                        )

                        trends.forEachIndexed { index, point ->
                            val xCenter = (index * groupSpacing) + (groupSpacing / 2f)

                            // Inflow Bar (Green)
                            val inflowHeight = ((point.inflow / maxVal) * chartHeight).toFloat()
                            if (inflowHeight > 0) {
                                drawRoundRect(
                                    color = Color(0xFF10B981),
                                    topLeft = Offset(xCenter - barWidth - 2.dp.toPx(), chartHeight - inflowHeight),
                                    size = Size(barWidth, inflowHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }

                            // Outflow Bar (Red)
                            val outflowHeight = ((point.outflow / maxVal) * chartHeight).toFloat()
                            if (outflowHeight > 0) {
                                drawRoundRect(
                                    color = Color(0xFFEF4444),
                                    topLeft = Offset(xCenter + 2.dp.toPx(), chartHeight - outflowHeight),
                                    size = Size(barWidth, outflowHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trends.forEachIndexed { i, pt ->
                        if (i % 2 == 0 || i == trends.lastIndex) {
                            Text(
                                text = pt.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = RoundedCornerShape(2.dp),
            color = color
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
