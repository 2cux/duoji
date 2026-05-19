package com.duoji.app.ui.bill

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.ui.components.animation.AnimatedSection
import com.duoji.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEditScreen(
    transactionId: String,
    onNavigateBack: () -> Unit,
    viewModel: BillEditViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            android.widget.Toast.makeText(context, "保存成功", android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            android.widget.Toast.makeText(context, "账单已删除", android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.deleteError) {
        state.deleteError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearDeleteError()
        }
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.loadError) {
        state.loadError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("这笔记录要删除吗？", style = MaterialTheme.typography.titleLarge) },
            text = { Text("删除后无法恢复，确认删除吗？", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) {
                    Text("删除", color = WarmAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = WarmTextSecondary)
                }
            },
            containerColor = WarmCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.occurredAtMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.updateOccurredAt(date)
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

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "账单详情",
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WarmPrimary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Type
            AnimatedSection(delayMillis = 0, animDuration = 350) {
                Column {
                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    TypeChipRow(
                        selectedType = state.type,
                        onTypeSelected = { viewModel.updateType(it) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Amount
            AnimatedSection(delayMillis = 40, animDuration = 350) {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { viewModel.updateAmount(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        color = if (state.type == "income") WarmIncome else WarmExpense,
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
                        Text(
                            "¥ ",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (state.type == "income") WarmIncome else WarmExpense,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmPrimary,
                        unfocusedBorderColor = WarmSecondary.copy(alpha = 0.4f),
                        focusedContainerColor = WarmCardAlt,
                        unfocusedContainerColor = WarmCardAlt,
                        cursorColor = WarmPrimary
                    ),
                    isError = state.amount == null && state.amountText.isNotBlank()
                )
            }
            Spacer(Modifier.height(16.dp))

            // Category
            AnimatedSection(delayMillis = 80, animDuration = 350) {
                Column {
                    Text(
                        text = "分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    val categories = viewModel.categoriesForType(state.type)
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = state.category,
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
                                        viewModel.updateCategory(category)
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Time
            AnimatedSection(delayMillis = 120, animDuration = 350) {
                Column {
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
                            text = com.duoji.app.data.repository.TransactionRepository.millisToDateTimeString(state.occurredAtMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmTextPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Note
            AnimatedSection(delayMillis = 160, animDuration = 350) {
                Column {
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { viewModel.updateNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "添加备注",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary.copy(alpha = 0.4f)
                            )
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
                }
            }
            Spacer(Modifier.height(16.dp))

            // Merchant
            AnimatedSection(delayMillis = 200, animDuration = 350) {
                Column {
                    Text(
                        text = "商户或消费对象",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.merchantOrItem,
                        onValueChange = { viewModel.updateMerchantOrItem(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "选填",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary.copy(alpha = 0.4f)
                            )
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
                }
            }
            Spacer(Modifier.height(24.dp))

            // Source info
            if (state.source.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = WarmTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "来源: ${sourceLabel(state.source)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmPrimary,
                    disabledContainerColor = WarmSecondary.copy(alpha = 0.5f)
                )
            ) {
                AnimatedContent(
                    targetState = state.isSaving,
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
                            Text("保存", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Delete button
            Button(
                onClick = { showDeleteConfirm = true },
                enabled = !state.isSaving && !state.isDeleting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmCard,
                    contentColor = WarmAccent,
                    disabledContainerColor = WarmCard.copy(alpha = 0.6f),
                    disabledContentColor = WarmAccent.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, WarmAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("删除这笔记录", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeChipRow(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val types = listOf("expense" to "支出", "income" to "收入", "refund" to "退款", "transfer" to "转账", "repayment" to "还款")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { (value, label) ->
            val isSelected = selectedType == value
            val chipColor by animateColorAsState(
                targetValue = if (isSelected) WarmPrimary else WarmCard,
                animationSpec = tween(300),
                label = "chipColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) WarmOnPrimary else WarmTextSecondary,
                animationSpec = tween(300),
                label = "chipTextColor"
            )

            FilterChip(
                onClick = { onTypeSelected(value) },
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = chipColor,
                    selectedContainerColor = WarmPrimary
                ),
                border = null,
                shape = RoundedCornerShape(50)
            )
        }
    }
}

private fun sourceLabel(source: String): String {
    return when (source) {
        "ai_parse" -> "AI 识别"
        "manual_text" -> "手动输入"
        "manual_form" -> "手动填写"
        else -> source
    }
}
