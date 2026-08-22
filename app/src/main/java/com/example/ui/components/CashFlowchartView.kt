package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.CashFlowchartData
import com.example.domain.analytics.FlowNode
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun CashFlowchartView(
    data: CashFlowchartData,
    modifier: Modifier = Modifier,
    onCategoryClick: ((String) -> Unit)? = null
) {
    var selectedNode by remember { mutableStateOf<FlowNode?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_flowchart_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cash Flow Flowchart",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sankey Flow: Inflow → 50/30/20 Allocations → Expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Flow",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stage 1: Inflow Root Stage
            Text(
                text = "STAGE 1: TOTAL INFLOW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.incomeNodes.forEach { node ->
                    FlowchartNodeCard(
                        node = node,
                        isSelected = selectedNode?.id == node.id,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedNode = if (selectedNode?.id == node.id) null else node }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Flow Connector Arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midX = size.width / 2f
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.5f),
                        start = Offset(midX, 0f),
                        end = Offset(midX, size.height),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Flow downward",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage 2: 50/30/20 Strategic Allocations
            Text(
                text = "STAGE 2: 50 / 30 / 20 ALLOCATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.allocationNodes.chunked(2).forEach { rowNodes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowNodes.forEach { node ->
                            FlowchartNodeCard(
                                node = node,
                                isSelected = selectedNode?.id == node.id,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedNode = if (selectedNode?.id == node.id) null else node }
                            )
                        }
                        if (rowNodes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Flow Connector Arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midX = size.width / 2f
                    drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = 0.5f),
                        start = Offset(midX, 0f),
                        end = Offset(midX, size.height),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Flow downward",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage 3: Category Endpoints
            Text(
                text = "STAGE 3: ACTIVE EXPENSE STREAMS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                data.categoryNodes.take(6).forEach { node ->
                    FlowchartCategoryRow(
                        node = node,
                        isSelected = selectedNode?.id == node.id,
                        onClick = {
                            selectedNode = if (selectedNode?.id == node.id) null else node
                            onCategoryClick?.invoke(node.title)
                        }
                    )
                }
            }

            // Node Inspector Sheet
            AnimatedVisibility(visible = selectedNode != null) {
                selectedNode?.let { node ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = node.color.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, node.color.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(node.color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${node.subtitle} • Represents ${String.format(Locale.US, "%.1f", node.percentageOfInflow)}% of total monthly inflows (₹${String.format(Locale.US, "%.2f", node.amount)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { selectedNode = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowchartNodeCard(
    node: FlowNode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) node.color else node.color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) node.color.copy(alpha = 0.18f) else node.color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(node.color)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "₹${String.format(Locale.US, "%.2f", node.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = node.color
            )

            Text(
                text = "${String.format(Locale.US, "%.1f", node.percentageOfInflow)}% of Inflow",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FlowchartCategoryRow(
    node: FlowNode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) node.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(node.color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = node.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "₹${String.format(Locale.US, "%.2f", node.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = node.color.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", node.percentageOfInflow)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = node.color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
