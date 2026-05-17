package com.duoji.app.ui.record

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoji.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfirm: () -> Unit,
    onNavigateToManualRecord: () -> Unit = {},
    viewModel: RecordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.parsedSuccessfully) {
        if (uiState.parsedSuccessfully) {
            onNavigateToConfirm()
        }
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "记一笔",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "像发消息一样记一笔",
                style = MaterialTheme.typography.bodySmall,
                color = WarmTextSecondary
            )

            Spacer(Modifier.height(16.dp))

            // Input area
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInput(it) },
                placeholder = {
                    Text(
                        "描述你的消费，例如：\n午饭35，咖啡18，地铁6",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextSecondary.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = WarmTextPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmPrimary,
                    unfocusedBorderColor = WarmSecondary,
                    focusedContainerColor = WarmCard,
                    unfocusedContainerColor = WarmCard,
                    cursorColor = WarmPrimary
                ),
                maxLines = 10
            )

            Spacer(Modifier.height(20.dp))

            // AI Process Button
            Button(
                onClick = { viewModel.process() },
                enabled = !uiState.isProcessing && uiState.inputText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmPrimary,
                    disabledContainerColor = WarmSecondary.copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = WarmOnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "AI 识别中...",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "帮我记账",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Error state
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                text = "识别失败，可以稍后再试，或者手动记一笔。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToManualRecord,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("手动记一笔")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Suggestions
            SuggestionsSection(
                onExampleClick = { example -> viewModel.updateInput(example) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SuggestionsSection(
    onExampleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "试试这样说",
            style = MaterialTheme.typography.bodySmall,
            color = WarmTextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("午饭35", "工资8000", "滴滴打车20").forEach { example ->
                SuggestionChip(
                    onClick = { onExampleClick(example) },
                    label = {
                        Text(
                            example,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmTextSecondary
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = WarmCard,
                        labelColor = WarmTextSecondary
                    ),
                    border = null
                )
            }
        }
    }
}
