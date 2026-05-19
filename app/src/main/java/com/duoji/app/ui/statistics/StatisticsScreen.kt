package com.duoji.app.ui.statistics

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.domain.statistics.CategorySummary
import com.duoji.app.domain.statistics.DailySummary
import com.duoji.app.domain.statistics.FrequentSmallExpense
import com.duoji.app.domain.statistics.MonthlyAdviceState
import com.duoji.app.domain.statistics.MonthlyStatistics
import com.duoji.app.ui.theme.*
import java.time.LocalDate

private const val TAG = "StatisticsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecord: () -> Unit = {},
    viewModel: StatisticsViewModel = viewModel()
) {
    Log.d(TAG, "StatisticsScreen entered")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Log.d(TAG, "StatisticsScreen: LaunchedEffect initial load, " +
                "isLoading=${uiState.isLoading}, hasStats=${uiState.statistics != null}")
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("统计",
                        style = MaterialTheme.typography.headlineLarge,
                        color = WarmTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = WarmTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            MonthSelector(
                year = uiState.selectedYear,
                month = uiState.selectedMonth,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() }
            )
            Spacer(Modifier.height(16.dp))

            val stats = uiState.statistics

            when {
                uiState.isLoading -> {
                    Log.d(TAG, "render: loading")
                    LoadingContent()
                }
                uiState.errorMessage != null -> {
                    Log.d(TAG, "render: error=${uiState.errorMessage}")
                    ErrorContent(uiState.errorMessage!!)
                }
                stats == null || stats.transactionCount == 0 -> {
                    Log.d(TAG, "render: empty")
                    EmptyContent(onNavigateToRecord)
                }
                else -> {
                    Log.d(TAG, "render: content, txCount=${stats.transactionCount}, " +
                            "expense=${stats.totalExpense}, " +
                            "categories=${stats.categorySummaries.size}, " +
                            "dailyDays=${stats.dailySummaries.size}")
                    MonthlyOverviewCard(stats)
                    Spacer(Modifier.height(16.dp))
                    CategoryBreakdownCard(stats)
                    Spacer(Modifier.height(16.dp))
                    DailyTrendCard(stats)
                    Spacer(Modifier.height(16.dp))
                    TopExpenseCard(stats)
                    Spacer(Modifier.height(16.dp))
                    FrequentSmallExpensesCard(stats)
                    Spacer(Modifier.height(16.dp))
                    AiMonthlyAdviceCard(
                        adviceState = uiState.adviceState,
                        onGenerate = { viewModel.generateMonthlyAdvice() },
                        onClearError = { viewModel.clearAdviceError() }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
            Log.d(TAG, "StatisticsScreen render complete")
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = WarmPrimary)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = WarmAccent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmAccent
            )
        }
    }
}

@Composable
private fun EmptyContent(onNavigateToRecord: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Assessment,
                contentDescription = null,
                tint = WarmTextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "这个月还没有足够的记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "先记几笔，月底我再帮你看看钱主要花在哪里。",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onNavigateToRecord,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("去记一笔")
            }
        }
    }
}

@Composable
private fun MonthSelector(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val isCurrentMonth = year == LocalDate.now().year && month == LocalDate.now().monthValue
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "上个月", tint = WarmTextPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${year}年${month}月",
            style = MaterialTheme.typography.titleLarge,
            color = WarmTextPrimary
        )
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onNext, enabled = !isCurrentMonth) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "下个月",
                tint = if (isCurrentMonth) WarmTextSecondary.copy(alpha = 0.3f) else WarmTextPrimary
            )
        }
    }
}

// ── MonthlyOverviewCard ──

@Composable
private fun MonthlyOverviewCard(stats: MonthlyStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "这个月的消费记录",
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "本月已支出",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "¥ ${formatAmount(stats.totalExpense)}",
                style = MaterialTheme.typography.displayLarge,
                color = WarmExpense,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text("收入", style = MaterialTheme.typography.labelSmall, color = WarmTextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "¥ ${formatAmount(stats.totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmIncome,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("结余", style = MaterialTheme.typography.labelSmall, color = WarmTextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "¥ ${formatAmount(stats.balance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (stats.balance >= 0) WarmIncome else WarmAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("笔数", style = MaterialTheme.typography.labelSmall, color = WarmTextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${stats.transactionCount}笔",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── CategoryBreakdownCard ──

@Composable
private fun CategoryBreakdownCard(stats: MonthlyStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "分类支出",
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            val categories = stats.categorySummaries
            if (categories.isEmpty()) {
                EmptyCategoryText()
            } else {
                Log.d(TAG, "CategoryBreakdownCard: ${categories.size} categories")
                val maxAmount = categories.first().amount
                categories.forEachIndexed { index, cat ->
                    val pct = if (maxAmount > 0) (cat.amount / maxAmount).toFloat().coerceIn(0.05f, 1f) else 0f
                    CategoryBar(cat, pct)
                    if (index < categories.size - 1) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCategoryText() {
    Text(
        text = "还没有支出分类数据。",
        style = MaterialTheme.typography.bodyMedium,
        color = WarmTextSecondary
    )
}

@Composable
private fun CategoryBar(category: CategorySummary, fraction: Float) {
    val color = categoryColor(category.category)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.category, style = MaterialTheme.typography.bodyMedium, color = WarmTextPrimary)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${String.format("%.0f", category.percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmTextSecondary
                )
            }
            Text(
                text = "¥ ${formatAmount(category.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WarmBackground)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

// ── DailyTrendCard ──

@Composable
private fun DailyTrendCard(stats: MonthlyStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "每日支出趋势",
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            val days = stats.dailySummaries.filter { it.expense > 0 }
            if (days.isEmpty()) {
                Text(
                    text = "还没有支出记录，记一笔后这里会展示每日趋势。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
            } else {
                Log.d(TAG, "DailyTrendCard: ${days.size} days")
                val maxExpense = days.maxOf { it.expense }
                val showDays = if (days.size > 14) days.takeLast(14) else days
                showDays.forEachIndexed { index, day ->
                    val fraction = if (maxExpense > 0) (day.expense / maxExpense).toFloat().coerceIn(0.03f, 1f) else 0f
                    DailyTrendRow(day, fraction)
                    if (index < showDays.size - 1) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTrendRow(day: DailySummary, fraction: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = day.date,
            style = MaterialTheme.typography.labelSmall,
            color = WarmTextSecondary,
            modifier = Modifier.width(36.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f).height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WarmBackground)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(WarmPrimary.copy(alpha = 0.7f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "¥${formatAmount(day.expense)}",
            style = MaterialTheme.typography.labelSmall,
            color = WarmTextPrimary,
            modifier = Modifier.widthIn(min = 48.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── TopExpenseCard ──

@Composable
private fun TopExpenseCard(stats: MonthlyStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "最大单笔支出",
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            val top = stats.topExpense
            if (top == null) {
                Text(
                    text = "还没有明显的大额支出记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
            } else {
                Log.d(TAG, "TopExpenseCard: category=${top.category}, amount=${top.amount}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForCategory(top.category),
                            contentDescription = null,
                            tint = WarmWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¥${formatAmount(top.amount)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                append(top.category)
                                if (top.note.isNotBlank()) {
                                    append(" · ${top.note}")
                                }
                                append(" · ${TransactionRepository.millisToDateString(top.occurredAt)}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ── FrequentSmallExpensesCard ──

@Composable
private fun FrequentSmallExpensesCard(stats: MonthlyStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "高频小额消费",
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            val freq = stats.frequentSmallExpenses
            if (freq.isEmpty()) {
                Text(
                    text = "小额消费还不多，节奏挺轻松。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
            } else {
                Log.d(TAG, "FrequentSmallExpensesCard: ${freq.size} items")
                freq.forEachIndexed { index, fse ->
                    FrequentSmallRow(fse)
                    if (index < freq.size - 1) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequentSmallRow(fse: FrequentSmallExpense) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WarmWarning)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${fse.name}出现 ${fse.count} 次，共 ¥${formatAmount(fse.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmTextPrimary
        )
    }
}

// ── AiMonthlyAdviceCard ──

@Composable
private fun AiMonthlyAdviceCard(
    adviceState: MonthlyAdviceState,
    onGenerate: () -> Unit,
    onClearError: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI 月度建议",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Text("✨", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))

            when {
                adviceState.isLoading -> {
                    Log.d(TAG, "AiMonthlyAdviceCard: loading")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = WarmPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("正在生成建议…", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary)
                    }
                }
                adviceState.content != null -> {
                    Log.d(TAG, "AiMonthlyAdviceCard: content, length=${adviceState.content.length}")
                    val segments = remember(adviceState.content) {
                        adviceState.content!!.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }
                    segments.forEachIndexed { index, segment ->
                        if (index > 0) Spacer(Modifier.height(10.dp))
                        if (index == 0) {
                            Text(
                                text = segment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(WarmPrimary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarmTextPrimary,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                adviceState.errorMessage != null -> {
                    Log.d(TAG, "AiMonthlyAdviceCard: error=${adviceState.errorMessage}")
                    Text(adviceState.errorMessage, style = MaterialTheme.typography.bodyMedium, color = WarmAccent)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClearError) {
                        Text("知道了", color = WarmPrimary)
                    }
                }
                else -> {
                    Text(
                        text = "生成后，我会根据本月分类、趋势和小额消费，给你一段具体建议。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onGenerate,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("生成本月建议")
                    }
                }
            }
        }
    }
}

// ── Utilities ──

private fun formatAmount(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "0"
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.1f", amount)
    }
}
