package com.duoji.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("确定清空所有账单吗？", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    "这会删除本地保存的所有记录，操作后无法恢复。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAllTransactions()
                }) {
                    Text("确认清空", color = WarmAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
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
                        "设置",
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

            // Success/error messages
            if (uiState.exportMessage != null || uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.errorMessage != null) WarningLight else IncomeLight
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.errorMessage != null) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (uiState.errorMessage != null) WarmWarning else WarmIncome,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage ?: uiState.exportMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = WarmTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- Data Management ----
            SettingsSectionHeader("数据管理")
            Spacer(Modifier.height(8.dp))

            SettingsActionCard(
                icon = Icons.Rounded.FileDownload,
                iconTint = WarmIncome,
                iconBg = IncomeLight,
                title = "导出 CSV",
                subtitle = "导出后可以自己备份或进一步分析。",
                enabled = !uiState.isExporting,
                onClick = { viewModel.exportCsv() }
            )

            Spacer(Modifier.height(8.dp))

            SettingsActionCard(
                icon = Icons.Rounded.DataObject,
                iconTint = WarmPrimary,
                iconBg = ExpenseLight,
                title = "导出 JSON",
                subtitle = "导出后可以自己备份或进一步分析。",
                enabled = !uiState.isExporting,
                onClick = { viewModel.exportJson() }
            )

            Spacer(Modifier.height(8.dp))

            SettingsActionCard(
                icon = Icons.Rounded.DeleteForever,
                iconTint = WarmAccent,
                iconBg = ExpenseLight,
                title = "清空本地账本",
                subtitle = "清空后无法恢复，请确认已经备份。",
                enabled = !uiState.isClearing,
                onClick = { showClearDialog = true }
            )

            Spacer(Modifier.height(24.dp))

            // ---- AI Settings ----
            SettingsSectionHeader("AI 设置")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "当前解析模式",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmTextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (uiState.apiKey.isNotBlank()) IncomeLight else ExpenseLight
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (uiState.apiKey.isNotBlank()) "真实 AI" else "本地模拟",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.apiKey.isNotBlank()) WarmIncome else WarmAccent
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.apiBaseUrl,
                        onValueChange = { viewModel.updateApiBaseUrl(it) },
                        label = { Text("API Base URL") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = settingsFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        colors = settingsFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = WarmTextSecondary
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.modelName,
                        onValueChange = { viewModel.updateModelName(it) },
                        label = { Text("模型名称") },
                        placeholder = { Text("gpt-4o-mini") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = settingsFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary)
                    ) {
                        Text("保存设置")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Preferences ----
            SettingsSectionHeader("使用偏好")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PreferenceRow(label = "默认货币", value = "CNY")
                    Spacer(Modifier.height(12.dp))
                    PreferenceRow(label = "默认首页", value = "首页")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "温和提醒",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextPrimary
                            )
                            Text(
                                text = "使用温和的消费提示语",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.useWarmReminder,
                            onCheckedChange = {
                                viewModel.updateWarmReminder(it)
                                viewModel.saveSettings()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WarmPrimary,
                                checkedTrackColor = WarmPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- About ----
            SettingsSectionHeader("关于多记")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PreferenceRow(label = "App 名称", value = "多记 / duoji")
                    Spacer(Modifier.height(12.dp))
                    PreferenceRow(label = "版本", value = "0.1.0 MVP")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "定位",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "AI 自然语言记账助手",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "数据存储",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "账单默认保存在本机，不会自动上传到云端。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "隐私说明",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmTextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "只有进行 AI 解析时才会上传必要文本；本地模拟模式不上传数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextPrimary
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = WarmTextPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
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

@Composable
private fun PreferenceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmTextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmTextSecondary
        )
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WarmPrimary,
    unfocusedBorderColor = WarmSecondary.copy(alpha = 0.4f),
    focusedContainerColor = WarmCardAlt,
    unfocusedContainerColor = WarmCardAlt,
    cursorColor = WarmPrimary,
    focusedLabelColor = WarmPrimary,
    unfocusedLabelColor = WarmTextSecondary
)
