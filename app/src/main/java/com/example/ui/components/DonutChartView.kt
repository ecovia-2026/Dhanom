package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.CategoryExpense
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun CategoryDonutChartView(
    expenses: List<CategoryExpense>,
    modifier: Modifier = Modifier,
    onCategorySelected: ((CategoryExpense) -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf<CategoryExpense?>(null) }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart_card"),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Expense Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BentoDeepPurple
            )
            Text(
                text = "Tap a slice or category to isolate spending metrics",
                style = MaterialTheme.typography.bodySmall,
                color = BentoSecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 24.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                            val arcSize = Size(diameter, diameter)

                            var startAngle = -90f

                            expenses.forEach { item ->
                                val sweepAngle = ((item.amount / totalExpense) * 360f).toFloat()
                                val isSelected = selectedCategory?.category == item.category
                                val currentStroke = if (isSelected) strokeWidth * 1.3f else strokeWidth

                                drawArc(
                                    color = item.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 2f, // small gap
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Center Label
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val displayAmount = selectedCategory?.amount ?: totalExpense
                            val displayLabel = selectedCategory?.category?.displayName ?: "Total Spent"
                            Text(
                                text = "$${String.format(Locale.US, "%.0f", displayAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Legend List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expenses.take(5).forEach { item ->
                            val isSelected = selectedCategory?.category == item.category
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCategory = if (selectedCategory?.category == item.category) null else item
                                        selectedCategory?.let { onCategorySelected?.invoke(it) }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) item.color.copy(alpha = 0.2f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.category.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", item.percentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = item.color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
