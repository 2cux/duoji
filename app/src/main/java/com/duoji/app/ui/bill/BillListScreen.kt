package com.duoji.app.ui.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToRecord: () -> Unit,
    viewModel: BillListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog && deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("这笔记录要删除吗？", style = MaterialTheme.typography.titleLarge) },
            text = { Text("删除后无法恢复，确认删除吗？", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTargetId?.let { viewModel.deleteTransaction(it) }
                    showDeleteDialog = false
                    deleteTargetId = null
                }) {
                    Text("删除", color = WarmAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = WarmTextSecondary)
                }
            },
            containerColor = WarmCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "账单",
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
        },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WarmPrimary)
                    }
                }
            } else if (uiState.isEmpty) {
                item {
                    EmptyBillState(onClick = onNavigateToRecord)
                }
            } else {
                item {
                    MonthSummaryCard(
                        expense = uiState.monthExpense,
                        income = uiState.monthIncome,
                        balance = uiState.monthBalance
                    )
                    Spacer(Modifier.height(16.dp))
                }

                items(uiState.groupedTransactions) { group ->
                    DateGroup(
                        group = group,
                        onItemClick = onNavigateToEdit,
                        onDeleteRequest = { id ->
                            deleteTargetId = id
                            showDeleteDialog = true
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
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
        Text(
            text = "${year}年${month}月",
            style = MaterialTheme.typography.titleLarge,
            color = WarmTextPrimary
        )
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
private fun MonthSummaryCard(expense: Double, income: Double, balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(label = "本月支出", amount = expense, color = WarmExpense)
            SummaryItem(label = "本月收入", amount = income, color = WarmIncome)
            SummaryItem(
                label = "结余",
                amount = balance,
                color = if (balance >= 0) WarmIncome else WarmAccent
            )
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WarmTextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "¥${formatAmount(amount)}",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DateGroup(
    group: GroupedTransaction,
    onItemClick: (String) -> Unit,
    onDeleteRequest: (String) -> Unit
) {
    val date = group.date
    val now = LocalDate.now()
    val prefix = when {
        date == now -> "今天"
        date == now.minusDays(1) -> "昨天"
        date == now.minusDays(2) -> "前天"
        else -> ""
    }
    val dateLabel = if (prefix.isNotEmpty()) {
        "$prefix · ${date.format(DateTimeFormatter.ofPattern("M月d日"))}"
    } else {
        date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINESE))
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextSecondary,
                fontWeight = FontWeight.Medium
            )
            if (group.totalExpense > 0) {
                Text(
                    text = "总支出 ¥${formatAmount(group.totalExpense)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmExpense
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                group.transactions.forEachIndexed { index, tx ->
                    TransactionItem(
                        transaction = tx,
                        onClick = { onItemClick(tx.id) },
                        onDelete = { onDeleteRequest(tx.id) }
                    )
                    if (index < group.transactions.lastIndex) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(WarmSecondary.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot and category
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (transaction.type == "income") IncomeLight
                    else if (transaction.type == "expense") ExpenseLight
                    else WarningLight
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForCategory(transaction.category),
                contentDescription = null,
                tint = if (transaction.type == "income") WarmIncome
                       else if (transaction.type == "expense") WarmExpense
                       else WarmWarning,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (transaction.note.isNotBlank()) transaction.note else transaction.category,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmTextSecondary
                )
                if (transaction.source == "ai_parse") {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmPrimary
                    )
                }
            }
        }

        Text(
            text = "${if (transaction.type == "income") "+" else ""}¥${formatAmount(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (transaction.type == "income") WarmIncome else WarmExpense,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyBillState(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = WarmTextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "这个月还没有记录，去记一笔吧",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("记一笔")
            }
        }
    }
}

private fun iconForCategory(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "餐饮" -> Icons.Rounded.Restaurant
        "交通" -> Icons.Rounded.DirectionsCar
        "购物" -> Icons.Rounded.ShoppingCart
        "居住" -> Icons.Rounded.Home
        "娱乐" -> Icons.Rounded.Movie
        "学习" -> Icons.Rounded.School
        "医疗" -> Icons.Rounded.LocalHospital
        "通讯" -> Icons.Rounded.Phone
        "人情" -> Icons.Rounded.Favorite
        "旅行" -> Icons.Rounded.Flight
        "工资" -> Icons.Rounded.Business
        "副业" -> Icons.Rounded.Computer
        "红包" -> Icons.Rounded.CardGiftcard
        "退款" -> Icons.Rounded.Reply
        "其他收入", "其他" -> Icons.Rounded.ReceiptLong
        else -> Icons.Rounded.ReceiptLong
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format("%.1f", amount)
    }
}
