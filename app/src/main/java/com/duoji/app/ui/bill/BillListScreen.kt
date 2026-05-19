package com.duoji.app.ui.bill

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.data.local.entity.TransactionEntity
import com.duoji.app.ui.components.animation.AnimatedAmountText
import com.duoji.app.ui.components.animation.AnimatedSection
import com.duoji.app.ui.components.animation.StaggeredListItem
import com.duoji.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToRecord: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToManualRecord: () -> Unit = {},
    viewModel: BillListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    LaunchedEffect(uiState.batchDeleteCount) {
        uiState.batchDeleteCount?.let { count ->
            android.widget.Toast.makeText(context, "已删除 $count 笔账单", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearBatchMessages()
        }
    }

    LaunchedEffect(uiState.batchDeleteError) {
        uiState.batchDeleteError?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearBatchMessages()
        }
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("确认删除", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "将删除已选择的 ${uiState.selectedIds.size} 笔账单，此操作不可恢复。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchDeleteConfirm = false
                        viewModel.deleteSelected()
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("删除", color = WarmAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
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
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "已选择 ${uiState.selectedIds.size} 笔",
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "取消选择", tint = WarmTextPrimary)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showBatchDeleteConfirm = true },
                            enabled = uiState.selectedIds.isNotEmpty() && !uiState.isDeleting
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "删除",
                                tint = if (uiState.selectedIds.isNotEmpty() && !uiState.isDeleting) WarmAccent
                                       else WarmTextSecondary.copy(alpha = 0.4f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
                )
            } else {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground),
                    actions = {
                        IconButton(onClick = onNavigateToStatistics) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = "统计",
                                tint = WarmTextPrimary
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
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

            // Animated content keyed by month
            item(key = "content_${uiState.selectedYear}_${uiState.selectedMonth}") {
                AnimatedContent(
                    targetState = Pair(uiState.selectedYear, uiState.selectedMonth),
                    transitionSpec = {
                        val direction = if (targetState.second > initialState.second) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(350),
                            initialOffsetX = { fullWidth -> direction * fullWidth / 4 }
                        ) + fadeIn(animationSpec = tween(350)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(250),
                                    targetOffsetX = { fullWidth -> -direction * fullWidth / 4 }
                                ) + fadeOut(animationSpec = tween(250))
                            )
                    },
                    label = "monthContent"
                ) { _ ->
                    Column {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = WarmPrimary)
                            }
                        } else if (uiState.isEmpty) {
                            EmptyBillState(
                                onAiRecord = onNavigateToRecord,
                                onManualRecord = onNavigateToManualRecord
                            )
                        } else {
                            MonthSummaryCard(
                                expense = uiState.monthExpense,
                                income = uiState.monthIncome,
                                balance = uiState.monthBalance
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (!uiState.isLoading && !uiState.isEmpty) {
                items(
                    items = uiState.groupedTransactions,
                    key = { it.date.toEpochDay() }
                ) { group ->
                    DateGroup(
                        group = group,
                        onItemClick = onNavigateToEdit,
                        onItemLongClick = { viewModel.enterSelectionMode(it) },
                        isSelectionMode = uiState.isSelectionMode,
                        selectedIds = uiState.selectedIds
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
        AnimatedContent(
            targetState = Pair(year, month),
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
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
        AnimatedAmountText(
            amount = amount,
            prefix = "¥",
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DateGroup(
    group: GroupedTransaction,
    onItemClick: (String) -> Unit,
    onItemLongClick: (String) -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<String>
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
                    StaggeredListItem(
                        index = index,
                        delayPerItem = 40,
                        animDuration = 300
                    ) {
                        TransactionItem(
                            transaction = tx,
                            onClick = { onItemClick(tx.id) },
                            onLongClick = { onItemLongClick(tx.id) },
                            isSelectionMode = isSelectionMode,
                            isSelected = tx.id in selectedIds
                        )
                    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) WarmSecondary.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(200),
        label = "itemBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onLongClick()
                    else onClick()
                },
                onLongClick = {
                    if (!isSelectionMode) onLongClick()
                }
            )
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = scaleIn(animationSpec = tween(200)) + fadeIn(tween(200)),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "已选中",
                        tint = WarmPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "未选中",
                        tint = WarmTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

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
private fun EmptyBillState(onAiRecord: () -> Unit, onManualRecord: () -> Unit) {
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
            AnimatedSection(delayMillis = 100) {
                Text(
                    text = "这个月还没有记录，去记一笔吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary
                )
            }
            Spacer(Modifier.height(6.dp))
            AnimatedSection(delayMillis = 150) {
                Text(
                    text = "试试输入：午饭35，咖啡18，地铁6",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmTextSecondary.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(20.dp))
            AnimatedSection(delayMillis = 200) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onAiRecord,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseLight,
                            contentColor = WarmPrimary
                        ),
                        border = BorderStroke(1.5.dp, WarmPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
