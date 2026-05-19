package com.duoji.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.ui.components.animation.AnimatedProgressBar
import com.duoji.app.ui.components.animation.AnimatedSection
import com.duoji.app.ui.components.animation.PressableCard
import com.duoji.app.ui.components.animation.StaggeredListItem
import com.duoji.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToRecord: () -> Unit,
    onNavigateToBillList: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToManualRecord: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trendState by viewModel.trendState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "设置",
                            tint = WarmTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                LargeFloatingActionButton(
                    onClick = onNavigateToRecord,
                    containerColor = WarmPrimary,
                    contentColor = WarmOnPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("记一笔", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Page indicator
            PageIndicator(
                currentPage = pagerState.currentPage,
                onPageChange = { scope.launch { pagerState.animateScrollToPage(it) } }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> HomeOverviewPage(
                        uiState = uiState,
                        onNavigateToStatistics = onNavigateToStatistics,
                        onNavigateToManualRecord = onNavigateToManualRecord,
                        onNavigateToBillList = onNavigateToBillList,
                        onNavigateToRecord = onNavigateToRecord
                    )
                    1 -> HomeTrendPage(
                        trendState = trendState,
                        onRangeChange = { viewModel.setTrendRange(it) }
                    )
                }
            }
        }
    }
}

// ── Page Indicator ──────────────────────────────────────────────────────────

@Composable
private fun PageIndicator(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 0.dp),
    ) {
        PageTab(label = "概览", selected = currentPage == 0) { onPageChange(0) }
        Spacer(Modifier.width(16.dp))
        PageTab(label = "趋势", selected = currentPage == 1) { onPageChange(1) }
    }
}

@Composable
private fun PageTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() })
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) WarmTextPrimary else WarmTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 28.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) WarmPrimary else Color.Transparent)
        )
    }
}

// ── Home Overview Page (existing homepage content) ──────────────────────────

@Composable
private fun HomeOverviewPage(
    uiState: HomeUiState,
    onNavigateToStatistics: () -> Unit,
    onNavigateToManualRecord: () -> Unit,
    onNavigateToBillList: () -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        AnimatedSection(delayMillis = 0, animDuration = 400) {
            Header()
        }

        Spacer(Modifier.height(20.dp))

        AnimatedSection(delayMillis = 80, animDuration = 450) {
            HeroCard(uiState)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedSection(delayMillis = 100, animDuration = 400) {
            AiTipCard(uiState)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedSection(delayMillis = 140, animDuration = 400) {
            ActionRow(
                onNavigateToStatistics = onNavigateToStatistics,
                onNavigateToManualRecord = onNavigateToManualRecord
            )
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.topCategories.isNotEmpty()) {
            AnimatedSection(delayMillis = 180, animDuration = 400) {
                TopCategoriesSection(uiState)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (uiState.transactionCount > 0) {
            AnimatedSection(delayMillis = 220, animDuration = 400) {
                RecentTransactionsSection(
                    recentTransactions = uiState.recentTransactions,
                    onClick = onNavigateToBillList
                )
            }
            Spacer(Modifier.height(16.dp))
        } else {
            EmptyBillEntry(
                onClick = onNavigateToBillList,
                onManualRecord = onNavigateToManualRecord,
                onAiRecord = onNavigateToRecord
            )
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ── Trend Page ──────────────────────────────────────────────────────────────

@Composable
private fun HomeTrendPage(
    trendState: ExpenseTrendUiState,
    onRangeChange: (TrendRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val points = trendState.points

    // Auto-validate selectedIndex when points change
    LaunchedEffect(points) {
        selectedIndex = when {
            points.isEmpty() -> null
            selectedIndex == null || selectedIndex !in points.indices -> points.indices.last
            else -> selectedIndex
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            text = "每日消费趋势",
            style = MaterialTheme.typography.headlineSmall,
            color = WarmTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        TrendRangeSelector(
            currentRange = trendState.range,
            onRangeChange = onRangeChange
        )

        Spacer(Modifier.height(16.dp))

        if (points.isEmpty() || points.all { it.amount <= 0 }) {
            EmptyTrendCard()
        } else {
            InteractiveLineChart(
                points = points,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { selectedIndex = it }
            )
            Spacer(Modifier.height(12.dp))
            TrendSummaryCard(summary = trendState.summary)
            Spacer(Modifier.height(12.dp))
            TrendTipCard(points = points)
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ── Range Selector ──────────────────────────────────────────────────────────

@Composable
private fun TrendRangeSelector(
    currentRange: TrendRange,
    onRangeChange: (TrendRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrendRange.entries.forEach { range ->
            val label = when (range) {
                TrendRange.LAST_7_DAYS -> "近 7 天"
                TrendRange.CURRENT_MONTH -> "本月"
            }
            val isSelected = range == currentRange
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) WarmPrimary else WarmCard)
                    .clickable { onRangeChange(range) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else WarmTextPrimary
                )
            }
        }
    }
}

// ── Interactive Line Chart ──────────────────────────────────────────────────

@Composable
private fun InteractiveLineChart(
    points: List<DailyExpensePoint>,
    modifier: Modifier = Modifier,
    selectedIndex: Int?,
    onSelectedIndexChange: (Int?) -> Unit
) {
    val leftPad = 52.dp
    val rightPad = 16.dp
    val topPad = 12.dp
    val bottomPad = 28.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row with selected info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日支出",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedIndex != null && selectedIndex in points.indices) {
                    val pt = points[selectedIndex]
                    Text(
                        text = "${pt.date.monthValue}月${pt.date.dayOfMonth}日  ¥ ${formatTrendAmount(pt.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            val maxAmount = points.maxOfOrNull { it.amount } ?: 0.0
            val safeMax = if (maxAmount <= 0) 1.0 else maxAmount

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .pointerInput(points) {
                        if (points.isEmpty()) return@pointerInput
                        detectTapGestures { offset ->
                            val idx = pointIndexAt(
                                touchX = offset.x,
                                canvasWidth = size.width.toFloat(),
                                leftPadPx = leftPad.toPx(),
                                rightPadPx = rightPad.toPx(),
                                pointCount = points.size
                            )
                            if (idx != null) onSelectedIndexChange(idx)
                        }
                    }
                    .pointerInput(points) {
                        if (points.isEmpty()) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val idx = pointIndexAt(
                                    touchX = offset.x,
                                    canvasWidth = size.width.toFloat(),
                                    leftPadPx = leftPad.toPx(),
                                    rightPadPx = rightPad.toPx(),
                                    pointCount = points.size
                                )
                                if (idx != null) onSelectedIndexChange(idx)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val idx = pointIndexAt(
                                    touchX = change.position.x,
                                    canvasWidth = size.width.toFloat(),
                                    leftPadPx = leftPad.toPx(),
                                    rightPadPx = rightPad.toPx(),
                                    pointCount = points.size
                                )
                                if (idx != null) onSelectedIndexChange(idx)
                            }
                        )
                    }
            ) {
                val leftPadPx = leftPad.toPx()
                val bottomPadPx = bottomPad.toPx()
                val topPadPx = topPad.toPx()
                val rightPadPx = rightPad.toPx()

                val chartWidth = size.width - leftPadPx - rightPadPx
                val chartHeight = size.height - topPadPx - bottomPadPx

                if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

                val labelColor = 0xFF8A6F61.toInt()
                val labelSize = 10.dp.toPx()
                val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = labelColor
                    this.textSize = labelSize
                }

                // Y-axis grid lines and labels
                val ySteps = 4
                for (i in 0..ySteps) {
                    val y = topPadPx + chartHeight * (1f - i.toFloat() / ySteps)
                    val labelValue = safeMax * i / ySteps
                    drawLine(
                        color = WarmTextSecondary.copy(alpha = 0.12f),
                        start = Offset(leftPadPx, y),
                        end = Offset(leftPadPx + chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        formatChartYLabel(labelValue),
                        leftPadPx - 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        textPaint
                    )
                }

                val pointCount = points.size
                val xStep = if (pointCount > 1) chartWidth / (pointCount - 1) else chartWidth
                val labelInterval = when {
                    pointCount <= 7 -> 1
                    pointCount <= 14 -> 2
                    pointCount <= 20 -> 3
                    else -> if (pointCount / 7 > 0) pointCount / 7 else 1
                }

                // X-axis labels
                for (index in points.indices) {
                    val x = leftPadPx + index * xStep
                    val showLabel = index % labelInterval == 0 || index == pointCount - 1
                    if (showLabel) {
                        val dateText = "${points[index].date.monthValue}/${points[index].date.dayOfMonth}"
                        val textWidth = textPaint.measureText(dateText)
                        drawContext.canvas.nativeCanvas.drawText(
                            dateText,
                            x - textWidth / 2f,
                            size.height - 4.dp.toPx(),
                            textPaint
                        )
                    }
                }

                // Draw line
                if (pointCount >= 2) {
                    val path = Path()
                    for (index in points.indices) {
                        val x = leftPadPx + index * xStep
                        val ratio = (points[index].amount / safeMax).toFloat().coerceIn(0f, 1f)
                        val y = topPadPx + chartHeight * (1f - ratio)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = WarmPrimary,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw all points
                for (index in points.indices) {
                    val x = leftPadPx + index * xStep
                    val ratio = (points[index].amount / safeMax).toFloat().coerceIn(0f, 1f)
                    val y = topPadPx + chartHeight * (1f - ratio)

                    if (selectedIndex == index) {
                        // Selection indicator: vertical line + highlighted point
                        drawLine(
                            color = WarmPrimary.copy(alpha = 0.25f),
                            start = Offset(x, y),
                            end = Offset(x, size.height - bottomPadPx),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(x, y))
                        drawCircle(WarmPrimary, radius = 6.dp.toPx(), center = Offset(x, y))
                        drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(x, y))
                    } else {
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                        drawCircle(WarmPrimary, radius = 3.5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }
    }
}

/**
 * Maps a touch X coordinate to the nearest data point index.
 * Returns null if pointCount is 0, otherwise an index in [0, pointCount-1].
 */
private fun pointIndexAt(
    touchX: Float,
    canvasWidth: Float,
    leftPadPx: Float,
    rightPadPx: Float,
    pointCount: Int
): Int? {
    if (pointCount <= 0) return null
    if (pointCount == 1) return 0
    val chartWidth = canvasWidth - leftPadPx - rightPadPx
    if (chartWidth <= 0 || !chartWidth.isFinite()) return null
    val adjustedX = (touchX - leftPadPx).coerceIn(0f, chartWidth)
    val ratio = adjustedX / chartWidth
    if (!ratio.isFinite()) return null
    val index = (ratio * (pointCount - 1) + 0.5f).toInt()
    return index.coerceIn(0, pointCount - 1)
}

private fun formatChartYLabel(value: Double): String {
    return if (value >= 1000) String.format("%.0f", value)
    else if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.0f", value)
}

// ── Trend Summary ───────────────────────────────────────────────────────────

@Composable
private fun TrendSummaryCard(
    summary: ExpenseTrendSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "汇总",
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            SummaryRow(label = "总消费", value = "¥ ${formatTrendAmount(summary.totalAmount)}")
            Spacer(Modifier.height(8.dp))
            SummaryRow(label = "日均消费", value = "¥ ${formatTrendAmount(summary.averageDailyAmount)}")
            Spacer(Modifier.height(8.dp))
            SummaryRow(
                label = "最高消费日",
                value = if (summary.maxDay != null) {
                    "${summary.maxDay.monthValue}月${summary.maxDay.dayOfMonth}日 · ¥ ${formatTrendAmount(summary.maxAmount)}"
                } else "—"
            )
            Spacer(Modifier.height(8.dp))
            SummaryRow(label = "有消费天数", value = "${summary.recordDays} 天")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Trend Tip ───────────────────────────────────────────────────────────────

@Composable
private fun TrendTipCard(
    points: List<DailyExpensePoint>,
    modifier: Modifier = Modifier
) {
    val tip = remember(points) { generateTrendTip(points) }
    if (tip.isBlank()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarningLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = WarmWarning,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextPrimary
            )
        }
    }
}

private fun generateTrendTip(points: List<DailyExpensePoint>): String {
    if (points.isEmpty()) return ""
    val amounts = points.map { it.amount }
    val total = amounts.sum()
    if (total <= 0) return ""
    if (points.size < 3) return "数据较少，继续记录后可获得更准确的趋势分析。"

    val recent3 = points.takeLast(3)
    val recentAvg = recent3.sumOf { it.amount } / 3.0
    val overallAvg = total / points.size

    return if (recentAvg > overallAvg * 1.15) {
        "最近消费有上升趋势，注意控制预算"
    } else if (recentAvg < overallAvg * 0.85) {
        "近期消费有所下降，继续保持"
    } else {
        "近期消费较平稳"
    }
}

// ── Empty State for Trend ───────────────────────────────────────────────────

@Composable
private fun EmptyTrendCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.BarChart,
                contentDescription = null,
                tint = WarmTextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "暂无消费趋势数据",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "开始记账后即可查看消费趋势",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Existing composables (unchanged, moved from HomeScreen) ─────────────────

@Composable
private fun ActionRow(
    onNavigateToStatistics: () -> Unit,
    onNavigateToManualRecord: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PressableCard(
            onClick = onNavigateToStatistics,
            modifier = Modifier.weight(1f),
            containerColor = WarmCard
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = WarmWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "月度统计",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "消费分析",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmTextSecondary
                    )
                }
            }
        }

        PressableCard(
            onClick = onNavigateToManualRecord,
            modifier = Modifier.weight(1f),
            containerColor = WarmCard
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ExpenseLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = WarmPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "手动记账",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "快速记一笔",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    val now = LocalDate.now()
    val dayOfWeek = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.CHINESE)
    val dateStr = now.format(DateTimeFormatter.ofPattern("M月d日"))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${timeGreeting()}~",
                style = MaterialTheme.typography.headlineLarge,
                color = WarmTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$dateStr 星期${dayOfWeek.replace("星期", "")}",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
        }
        Icon(
            imageVector = Icons.Rounded.Savings,
            contentDescription = null,
            tint = WarmPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun HeroCard(state: HomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "本月支出",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "¥ ${formatAmount(state.monthlyExpense)}",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "今日支出",
                    value = "¥ ${formatAmount(state.todayExpense)}",
                    color = Color.White.copy(alpha = 0.9f)
                )
                StatItem(
                    label = if (state.balance >= 0) "本月结余" else "超出",
                    value = "¥ ${formatAmount(kotlin.math.abs(state.balance))}",
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AiTipCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarningLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = WarmWarning,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = state.aiTip,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextPrimary
            )
        }
    }
}

@Composable
private fun TopCategoriesSection(state: HomeUiState) {
    Column {
        Text(
            text = "本月分类排行",
            style = MaterialTheme.typography.titleLarge,
            color = WarmTextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                state.topCategories.forEachIndexed { index, (category, amount) ->
                    StaggeredListItem(index = index, delayPerItem = 60) {
                        CategoryBar(
                            category = category,
                            amount = amount,
                            maxAmount = state.topCategories.firstOrNull()?.second ?: amount,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(
    category: String,
    amount: Double,
    maxAmount: Double,
    modifier: Modifier = Modifier
) {
    val color = categoryColor(category)
    val fraction = if (maxAmount > 0) (amount / maxAmount).toFloat().coerceIn(0.05f, 1f) else 0f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextPrimary
            )
            Text(
                text = "¥ ${formatAmount(amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        AnimatedProgressBar(
            progress = fraction,
            barColor = color,
            trackColor = WarmBackground,
            animDuration = 500
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentTransactionsSection(
    recentTransactions: List<TransactionEntity>,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = "最近账单",
            style = MaterialTheme.typography.titleLarge,
            color = WarmTextPrimary
        )
        Spacer(Modifier.height(12.dp))
        PressableCard(
            onClick = onClick,
            containerColor = WarmCard
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                recentTransactions.take(3).forEachIndexed { index, tx ->
                    StaggeredListItem(
                        index = index,
                        delayPerItem = 50,
                        animDuration = 350
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (tx.type == "income") WarmIncome else WarmExpense
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = tx.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "¥${formatAmount(tx.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (tx.type == "income") WarmIncome else WarmExpense,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "查看全部账单 →",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmPrimary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun EmptyBillEntry(
    onClick: () -> Unit,
    onManualRecord: () -> Unit,
    onAiRecord: () -> Unit
) {
    Column {
        Text(
            text = "最近账单",
            style = MaterialTheme.typography.titleLarge,
            color = WarmTextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Icon(
                    Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = WarmTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                AnimatedSection(delayMillis = 100) {
                    Text(
                        text = "还没有记录，今天可以先轻松记一笔。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                AnimatedSection(delayMillis = 150) {
                    Text(
                        text = "试试输入：午饭35，咖啡18，地铁6",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(16.dp))
                AnimatedSection(delayMillis = 200) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAiRecord,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmPrimary)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI 记一笔")
                        }
                        Button(
                            onClick = onManualRecord,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary)
                        ) {
                            Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("手动记一笔")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun timeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..5 -> "夜深了"
        in 6..8 -> "早上好"
        in 9..11 -> "上午好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
}

private fun formatAmount(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "0"
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.1f", amount)
    }
}

private fun formatTrendAmount(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "0"
    return String.format("%.2f", amount)
}
