package com.duoji.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRecord: () -> Unit,
    onNavigateToBillList: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = WarmBackground,
        floatingActionButton = {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Header()
            Spacer(Modifier.height(20.dp))
            HeroCard(uiState)
            Spacer(Modifier.height(16.dp))
            AiTipCard(uiState)
            Spacer(Modifier.height(16.dp))
            StatisticsEntryCard(onClick = onNavigateToStatistics)
            Spacer(Modifier.height(16.dp))
            if (uiState.topCategories.isNotEmpty()) {
                TopCategoriesSection(uiState)
                Spacer(Modifier.height(16.dp))
            }
            if (uiState.transactionCount > 0) {
                RecentTransactionsSection(
                    recentTransactions = uiState.recentTransactions,
                    onClick = onNavigateToBillList
                )
                Spacer(Modifier.height(16.dp))
            } else {
                EmptyBillEntry(onClick = onNavigateToBillList)
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(100.dp)) // Space for FAB
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
                state.topCategories.forEach { (category, amount) ->
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WarmBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
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
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                recentTransactions.take(3).forEach { tx ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "月度统计",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "看看这个月的消费情况",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmTextSecondary
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = WarmTextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyBillEntry(onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
            Icon(
                Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = WarmTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "还没有记录，去轻松记一笔吧",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextSecondary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = WarmTextSecondary
            )
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
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.1f", amount)
    }
}
