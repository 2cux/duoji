package com.duoji.app.ui.manual

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
fun ManualRecordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualRecordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val typeOptions = listOf(
        "expense" to "支出",
        "income" to "收入",
        "refund" to "退款",
        "transfer" to "转账",
        "repayment" to "还款"
    )

    val typeColors = mapOf(
        "expense" to WarmExpense,
        "income" to WarmIncome,
        "refund" to WarmIncome,
        "transfer" to WarmWarning,
        "repayment" to WarmAccent
    )

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            android.widget.Toast.makeText(context, "已记录到本地账本。", android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.occurredAt
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateDate(date)
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
                        "手动记一笔",
                        style = MaterialTheme.typography.headlineLarge,
                        color = WarmTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = WarmTextPrimary
                        )
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

            // Type
            AnimatedSection(delayMillis = 0, animDuration = 350) {
                Column {
                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = showTypeDropdown,
                        onExpandedChange = { showTypeDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = typeOptions.first { it.first == uiState.type }.second,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
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
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false },
                            containerColor = WarmCard
                        ) {
                            typeOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(typeColors[value]!!)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateType(value)
                                        showTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Amount
            AnimatedSection(delayMillis = 40, animDuration = 350) {
                Column {
                    Text(
                        text = "金额 *",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.amountText,
                        onValueChange = { viewModel.updateAmount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = typeColors[uiState.type] ?: WarmExpense,
                            fontWeight = FontWeight.Bold
                        ),
                        placeholder = {
                            Text("0", style = MaterialTheme.typography.displaySmall, color = WarmTextSecondary.copy(alpha = 0.3f))
                        },
                        prefix = {
                            Text(
                                "¥ ",
                                style = MaterialTheme.typography.displaySmall,
                                color = typeColors[uiState.type] ?: WarmExpense,
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
                        )
                    )
                }
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
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.category,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
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
                            onDismissRequest = { showCategoryDropdown = false },
                            containerColor = WarmCard
                        ) {
                            viewModel.getCategoriesForCurrentType().forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(categoryColor(category))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(category, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
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

            // Date
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
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = WarmTextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = uiState.occurredAt.format(DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINESE)),
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
                        value = uiState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("可选", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary.copy(alpha = 0.4f)) },
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
                        value = uiState.merchantOrItem,
                        onValueChange = { viewModel.updateMerchantOrItem(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("可选", style = MaterialTheme.typography.bodyMedium, color = WarmTextSecondary.copy(alpha = 0.4f)) },
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

            // Error message
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(animationSpec = tween(300)) { -it / 4 },
                exit = fadeOut(animationSpec = tween(200)) +
                        shrinkVertically(animationSpec = tween(200))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = WarmWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmTextPrimary
                        )
                    }
                }
            }

            // Save button
            Button(
                onClick = { viewModel.save() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmPrimary,
                    disabledContainerColor = WarmSecondary.copy(alpha = 0.5f)
                )
            ) {
                AnimatedContent(
                    targetState = uiState.isSaving,
                    transitionSpec = { Crossfade(tween(300)) },
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

            Spacer(Modifier.height(32.dp))
        }
    }
}
