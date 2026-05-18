package com.duoji.app.ui.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.duoji.app.ui.components.animation.AnimatedAmountText
import com.duoji.app.ui.components.animation.AnimatedProgressBar
import com.duoji.app.ui.components.animation.AnimatedSection
import com.duoji.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecord: () -> Unit = {},
    viewModel: StatisticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "统计",
                        style = MaterialTheme.typography.headlineLarge,
                        color = WarmTextPrimary
                    )
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

            // Month selector
            AnimatedSection(delayMillis = 0, animDuration = 350) {
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WarmPrimary)
                }
            } else {
                val stats = uiState.statistics

                if (stats == null || stats.transactionCount == 0) {
                    AnimatedSection(delayMillis = 80, animDuration = 400) {
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
                } else {
                    // Monthly overview card
                    AnimatedSection(delayMillis = 80, animDuration = 400) {
                        MonthlyOverviewCard(stats)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Category breakdown card
                    AnimatedSection(delayMillis = 140, animDuration = 400) {
                        CategoryBreakdownCard(stats)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Daily trend card
                    AnimatedSection(delayMillis = 200, animDuration = 400) {
                        DailyTrendCard(stats)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Top expense card
                    AnimatedSection(delayMillis = 260, animDuration = 400) {
                        TopExpenseCard(stats)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Frequent small expenses card
                    AnimatedSection(delayMillis = 320, animDuration = 400) {
                        FrequentSmallExpensesCard(stats)
                    }

                    Spacer(Modifier.height(16.dp))

                    // AI monthly advice card
                    AnimatedSection(delayMillis = 380, animDuration = 400) {
                        AiMonthlyAdviceCard(
                            adviceState = uiState.adviceState,
                            onGenerate = { viewModel.generateMonthlyAdvice() },
                            onClearError = { viewModel.clearAdviceError() }
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
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
    val now = LocalDate.now()
    val isCurrentMonth = year == now.year && month == now.monthValue

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = "上个月",
                tint = WarmTextPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(
            targetState = Pair(year, month),
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "monthLabel"
        ) { (y, m) ->
            Text(
                text = "${y}年${m}月",
                style = MaterialTheme.typography.titleLarge,
                color = WarmTextPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
        IconButton(
            onClick = onNext,
            enabled = !isCurrentMonth
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "下个月",
                tint = if (isCurrentMonth) WarmTextSecondary.copy(alpha = 0.3f) else WarmTextPrimary
            )
        }
    }
}

@Composable
private fun MonthlyOverviewCard(stats: MonthlyStatistics?) {
    val hasData = stats != null && stats.transactionCount > 0

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

            if (!hasData) {
                Text(
                    text = "这个月还没有记录，记一笔后这里会展示月度概况。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
                return@Column
            }

            Text(
                text = "本月已支出",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            AnimatedAmountText(
                amount = stats!!.totalExpense,
                prefix = "¥ ",
                color = WarmExpense,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedAmountText(
                        amount = stats.totalIncome,
                        prefix = "¥ ",
                        color = WarmIncome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = "结余",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedAmountText(
                        amount = stats.balance,
                        prefix = "¥ ",
                        color = if (stats.balance >= 0) WarmIncome else WarmAccent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = "笔数",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedContent(
                        targetState = stats.transactionCount,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "txCount"
                    ) { count ->
                        Text(
                            text = "${count}笔",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(stats: MonthlyStatistics?) {
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

            val categories = stats?.categorySummaries
            if (categories.isNullOrEmpty()) {
                Text(
                    text = "还没有支出分类数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
                return@Column
            }

            categories.forEachIndexed { index, cat ->
                CategoryBar(
                    cat,
                    categories.first().amount,
                    delay = index * 80
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CategoryBar(category: CategorySummary, maxAmount: Double, delay: Int = 0) {
    val color = categoryColor(category.category)
    val fraction = if (maxAmount > 0) (category.amount / maxAmount).toFloat().coerceIn(0.05f, 1f) else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextPrimary
                )
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
        AnimatedProgressBar(
            progress = fraction,
            barColor = color,
            trackColor = WarmBackground,
            animDelay = delay,
            animDuration = 500
        )
    }
}

@Composable
private fun DailyTrendCard(stats: MonthlyStatistics?) {
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

            val days = stats?.dailySummaries?.filter { it.expense > 0 }
            if (days.isNullOrEmpty()) {
                Text(
                    text = "还没有支出记录，记一笔后这里会展示每日趋势。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
                return@Column
            }

            val maxExpense = days.maxOf { it.expense }
            val showDays = if (days.size > 14) days.takeLast(14) else days

            Column {
                showDays.forEachIndexed { index, day ->
                    DailyTrendRow(day, maxExpense, delay = index * 20)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyTrendRow(day: DailySummary, maxExpense: Double, delay: Int = 0) {
    val fraction = if (maxExpense > 0) (day.expense / maxExpense).toFloat().coerceIn(0.03f, 1f) else 0f

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
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WarmBackground)
        ) {
            // Use AnimatedProgressBar directly
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(500, delayMillis = delay),
                label = "dailyBar"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
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

@Composable
private fun TopExpenseCard(stats: MonthlyStatistics?) {
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

            val top = stats?.topExpense
            if (top == null) {
                AnimatedSection(delayMillis = 100) {
                    Text(
                        text = "还没有明显的大额支出记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextSecondary
                    )
                }
                return@Column
            }

            AnimatedContent(
                targetState = top.amount,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "topExpense"
            ) { _ ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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

@Composable
private fun FrequentSmallExpensesCard(stats: MonthlyStatistics?) {
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

            val freqExpenses = stats?.frequentSmallExpenses
            if (freqExpenses.isNullOrEmpty()) {
                Text(
                    text = "小额消费还不多，节奏挺轻松。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
                return@Column
            }

            freqExpenses.forEachIndexed { index, fse ->
                AnimatedSection(delayMillis = index * 60, animDuration = 350) {
                    FrequentSmallRow(fse)
                }
                Spacer(Modifier.height(8.dp))
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
            modifier = Modifier
                .size(8.dp)
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
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = adviceState,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "adviceState",
                modifier = Modifier.fillMaxWidth()
            ) { state ->
                when {
                    state.isLoading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WarmPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "正在生成建议…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary
                            )
                        }
                    }
                    state.content != null -> {
                        Text(
                            text = state.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmTextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmAccent
                        )
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
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("生成本月建议")
                        }
                    }
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.1f", amount)
    }
}
