package com.duoji.app.ui.confirm

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.data.model.TransactionDraft
import com.duoji.app.data.model.TransactionType
import com.duoji.app.data.store.ParseResultStore
import com.duoji.app.ui.components.animation.AnimatedAmountText
import com.duoji.app.ui.components.animation.AnimatedSection
import com.duoji.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBillList: () -> Unit,
    viewModel: ConfirmViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactions = viewModel.transactions
    val errors = viewModel.errors
    val context = LocalContext.current

    // Track which indices are being deleted (for exit animation)
    val deletingIndices = remember { mutableStateOf(setOf<Int>()) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.widget.Toast.makeText(context, "已记录到本地账本", android.widget.Toast.LENGTH_SHORT).show()
            onNavigateToBillList()
        }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSaveError()
        }
    }

    BackHandler(enabled = transactions.isNotEmpty()) {
        showDiscardDialog = true
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = {
                Text("删除这笔账单？", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text("删除后无法恢复，确认删除吗？", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetIndex = uiState.deleteTargetIndex
                    if (targetIndex != null) {
                        deletingIndices.value = deletingIndices.value + targetIndex
                    }
                    viewModel.confirmDelete()
                }) {
                    Text("删除", color = WarmAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("取消", color = WarmTextSecondary)
                }
            },
            containerColor = WarmCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text("放弃保存？", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text("当前识别结果还未保存，确定不保存并返回吗？", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text("不保存返回", color = WarmAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("继续编辑", color = WarmTextSecondary)
                }
            },
            containerColor = WarmCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(WarmBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = WarmTextSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("没有需要保存的账单", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary)) {
                    Text("返回")
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI 识别确认",
                        style = MaterialTheme.typography.headlineLarge,
                        color = WarmTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (transactions.isNotEmpty()) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = WarmTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
            )
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
                // Header
                AnimatedSection(delayMillis = 0, animDuration = 350) {
                    Column {
                        Text(
                            text = "AI识别到 ${transactions.size} 笔账单",
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmTextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "请确认以下信息，可以修改后保存",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmTextSecondary
                        )
                    }
                }

                // Local fallback warning with specific reason
                val fallbackReason = ParseResultStore.localFallbackReason
                if (fallbackReason != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = WarmWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = fallbackReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmTextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            itemsIndexed(transactions) { index, draft ->
                val isDeleting = index in deletingIndices.value

                AnimatedVisibility(
                    visible = !isDeleting,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = index * 50)) +
                            slideInVertically(animationSpec = tween(400, delayMillis = index * 50)) { it / 2 },
                    exit = fadeOut(animationSpec = tween(300)) +
                            shrinkVertically(animationSpec = tween(300))
                ) {
                    TransactionEditCard(
                        index = index,
                        draft = draft,
                        error = errors[index],
                        totalTransactions = transactions.size,
                        onUpdateAmount = { viewModel.updateAmount(index, it) },
                        onUpdateCategory = { viewModel.updateCategory(index, it) },
                        onUpdateNote = { viewModel.updateNote(index, it) },
                        onUpdateOccurredAt = { viewModel.updateOccurredAt(index, it) },
                        onDelete = {
                            deletingIndices.value = deletingIndices.value + index
                            viewModel.requestDelete(index)
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                Spacer(Modifier.height(8.dp))

                // Summary
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount ?: 0.0 }
                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount ?: 0.0 }

                if (totalExpense > 0 || totalIncome > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (totalExpense > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("总支出", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary)
                                    AnimatedAmountText(
                                        amount = totalExpense,
                                        prefix = "¥ ",
                                        color = WarmExpense,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (totalIncome > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("总收入", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary)
                                    AnimatedAmountText(
                                        amount = totalIncome,
                                        prefix = "¥ ",
                                        color = WarmIncome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Confirm button
                Button(
                    onClick = {
                        val success = viewModel.confirmAll()
                        if (!success && viewModel.errors.isEmpty()) {
                            // transactions list is empty after deletion
                        }
                    },
                    enabled = !uiState.isSaving && transactions.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmPrimary,
                        disabledContainerColor = WarmSecondary.copy(alpha = 0.5f)
                    )
                ) {
                    AnimatedContent(
                        targetState = uiState.isSaving,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "saveBtn"
                    ) { saving ->
                        if (saving) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = WarmOnPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("保存中...", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("确认保存全部", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Secondary discard button
                OutlinedButton(
                    onClick = { onNavigateBack() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WarmTextSecondary
                    ),
                    border = BorderStroke(1.dp, WarmSecondary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("不保存，返回修改", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditCard(
    index: Int,
    draft: TransactionDraft,
    error: String?,
    totalTransactions: Int,
    onUpdateAmount: (String) -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onUpdateOccurredAt: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val typeColor = when (draft.type) {
        TransactionType.EXPENSE -> WarmExpense
        TransactionType.INCOME -> WarmIncome
        TransactionType.REFUND -> WarmIncome
        TransactionType.TRANSFER -> WarmWarning
        TransactionType.REPAYMENT -> WarmAccent
    }

    val typeBgColor = when (draft.type) {
        TransactionType.EXPENSE -> ExpenseLight
        TransactionType.INCOME -> IncomeLight
        TransactionType.REFUND -> IncomeLight
        TransactionType.TRANSFER -> WarningLight
        TransactionType.REPAYMENT -> ExpenseLight
    }

    val categories = when (draft.type) {
        TransactionType.INCOME -> listOf("工资", "副业", "红包", "退款", "其他收入")
        else -> listOf("餐饮", "交通", "购物", "居住", "娱乐", "学习", "医疗", "通讯", "人情", "旅行", "其他")
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateFromIso(draft.occurredAt)?.let {
                it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val originalTime = parseTimeFromIso(draft.occurredAt)
                        val newDateTime = "${date}T${originalTime}+08:00"
                        onUpdateOccurredAt(newDateTime)
                    }
                    showDatePicker = false
                }) {
                    Text("确定", color = WarmPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消", color = WarmTextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = WarmCard,
                    selectedDayContainerColor = WarmPrimary
                )
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: type badge + confidence + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(typeBgColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (draft.type) {
                                TransactionType.EXPENSE -> Icons.Rounded.TrendingDown
                                TransactionType.INCOME -> Icons.Rounded.TrendingUp
                                TransactionType.REFUND -> Icons.Rounded.Reply
                                TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                                TransactionType.REPAYMENT -> Icons.Rounded.CurrencyExchange
                            },
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = draft.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                ConfidenceBadge(confidence = draft.confidence)

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "删除",
                        tint = WarmTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Amount field with animated content
            val displayAmount = draft.amount ?: 0.0
            OutlinedTextField(
                value = draft.amountText,
                onValueChange = onUpdateAmount,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    color = if (draft.type == TransactionType.INCOME) WarmIncome else WarmExpense,
                    fontWeight = FontWeight.Bold
                ),
                placeholder = {
                    Text(
                        "输入金额",
                        style = MaterialTheme.typography.displayMedium,
                        color = WarmTextSecondary.copy(alpha = 0.3f)
                    )
                },
                prefix = {
                    AnimatedContent(
                        targetState = displayAmount,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "amountPrefix"
                    ) { _ ->
                        Text(
                            "¥ ",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (draft.type == TransactionType.INCOME) WarmIncome else WarmExpense,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmPrimary,
                    unfocusedBorderColor = WarmSecondary.copy(alpha = 0.4f),
                    focusedContainerColor = WarmCardAlt,
                    unfocusedContainerColor = WarmCardAlt,
                    cursorColor = WarmPrimary,
                    errorBorderColor = WarmAccent
                ),
                isError = error != null,
                supportingText = error?.let {
                    { Text(it, color = WarmAccent, style = MaterialTheme.typography.bodySmall) }
                }
            )

            Spacer(Modifier.height(12.dp))

            // Category dropdown
            Text(
                text = "分类",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = draft.category,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmPrimary,
                        unfocusedBorderColor = WarmSecondary.copy(alpha = 0.4f),
                        focusedContainerColor = WarmCardAlt,
                        unfocusedContainerColor = WarmCardAlt
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onUpdateCategory(category)
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Time
            Text(
                text = "时间",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarmCardAlt)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = WarmTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatOccurredAt(draft.occurredAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextPrimary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Note
            Text(
                text = "备注",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.note,
                onValueChange = onUpdateNote,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("添加备注", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary.copy(alpha = 0.4f))
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmPrimary,
                    unfocusedBorderColor = WarmSecondary.copy(alpha = 0.4f),
                    focusedContainerColor = WarmCardAlt,
                    unfocusedContainerColor = WarmCardAlt,
                    cursorColor = WarmPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (draft.merchantOrItem.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ShoppingBag,
                        contentDescription = null,
                        tint = WarmTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = draft.merchantOrItem,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Double) {
    val (text, color, bgColor) = when {
        confidence >= 0.9 -> Triple("高置信度", WarmIncome, IncomeLight)
        confidence >= 0.7 -> Triple("中置信度", WarmWarning, WarningLight)
        else -> Triple("需要确认", WarmAccent, ExpenseLight)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun formatOccurredAt(iso: String): String {
    if (iso.isBlank()) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }
    return try {
        val dt = LocalDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    } catch (e: Exception) {
        try {
            val d = LocalDate.parse(iso, DateTimeFormatter.ISO_DATE)
            d.format(DateTimeFormatter.ofPattern("M月d日"))
        } catch (e2: Exception) {
            iso
        }
    }
}

private fun parseDateFromIso(iso: String): LocalDate? {
    if (iso.isBlank()) return LocalDate.now()
    return try {
        LocalDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME).toLocalDate()
    } catch (e: Exception) {
        try {
            LocalDate.parse(iso, DateTimeFormatter.ISO_DATE)
        } catch (e2: Exception) {
            null
        }
    }
}

private fun parseTimeFromIso(iso: String): String {
    if (iso.isBlank()) return "12:00"
    return try {
        val dt = LocalDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        "12:00"
    }
}
