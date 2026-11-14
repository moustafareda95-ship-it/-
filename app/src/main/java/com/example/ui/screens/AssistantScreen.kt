package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.domain.ai.PendingAiAction
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.AiPreviewBanner
import com.example.ui.components.HayatiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSoft
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.S

@Composable
fun AssistantScreen(
    state: HayatiUiState,
    pendingAction: PendingAiAction?,
    aiBusy: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = remember(isAr) {
        if (isAr) {
            listOf(
                "سجل مصروف 150 جنيه مشتريات للبيت",
                "فكرني بدفع فاتورة الإنترنت يوم 20",
                "أضف امتحان علوم يوم الأحد",
                "كم صرفت على البيت هذا الشهر؟",
                "ما المهام المتأخرة عندي؟",
                "اعمل لي جدول مذاكرة للأطفال هذا الأسبوع",
                "خطط لي يوم الغد",
            )
        } else {
            listOf(
                "Add expense 150 EGP groceries",
                "Remind me internet bill day 20",
                "How much did I spend this month?",
                "What are my overdue tasks?",
                "Plan tomorrow",
            )
        }
    }

    LaunchedEffect(state.chatMessages.size, pendingAction != null) {
        if (pendingAction != null) {
            listState.animateScrollToItem(state.chatMessages.size)
        } else if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TurquoisePrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isAr) "مساعد حياتي الشخصي" else "HAYATI AI Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (state.chatMessages.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearChatHistory() }) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isAr) "مسح" else "Clear")
                }
            }
        }

        AiPreviewBanner(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        // Quick Prompts Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(quickPrompts) { prompt ->
                AssistChip(
                    enabled = !aiBusy,
                    onClick = { viewModel.sendAssistantMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        if (aiBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = TurquoisePrimary)
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.chatMessages.isEmpty()) {
                item {
                    HayatiCard(borderColor = TurquoisePrimary.copy(alpha = 0.35f)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = GoldAccent)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isAr) "مرحباً ${state.prefs.userName}!" else "Hello ${state.prefs.userName}!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) {
                                        "يمكنك التحدث معي بالعامية المصرية أو العربية الفصحى لتسجيل مصروفات، ضبط فواتير، إضافة واجبات، أو الاستعلام عن بياناتك الحقيقية."
                                    } else {
                                        "Ask me in Arabic or English to log expenses, set bill reminders, check overdue tasks, or plan your day."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.chatMessages, key = { it.id }) { msg ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                bottomEnd = if (msg.isUser) 4.dp else 16.dp,
                            ),
                            color = if (msg.isUser) NavyDeep else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (msg.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.widthIn(max = 340.dp),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (msg.isUser) Icons.Outlined.Person else Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (msg.isUser) GoldAccent else TurquoisePrimary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (msg.isUser) state.prefs.userName else s.appName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isUser) Color.White.copy(alpha = 0.85f) else TurquoisePrimary,
                                        )
                                    }
                                    Text(
                                        text = DateTimeUtils.formatTime(msg.timestampMillis, isAr, tz),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (msg.isUser) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (msg.actionTag == "SAVED") {
                                    Spacer(Modifier.height(6.dp))
                                    StatusBadge(
                                        text = if (isAr) "تم الحفظ في التطبيق" else "Saved to App",
                                        containerColor = StatusSuccessSoft,
                                        contentColor = StatusSuccess,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (pendingAction != null) {
                item {
                    HayatiCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        borderColor = TurquoisePrimary,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isAr) "تأكيد الإجراء قبل الحفظ" else "Confirm Action Before Saving",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TurquoisePrimary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(pendingAction.confirmationSummary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !state.isSaving,
                                onClick = { viewModel.confirmPendingAiAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                modifier = Modifier.testTag("ai_confirm_action_btn"),
                            ) {
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.confirm)
                            }
                            OutlinedButton(onClick = { viewModel.dismissPendingAiAction() }) {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.cancel)
                            }
                        }
                    }
                }
            }
        }

        // Input Row
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(if (isAr) "اكتب أمرك بالعربية أو الإنجليزية..." else "Type a command...")
                    },
                    maxLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    enabled = inputText.isNotBlank() && !aiBusy,
                    onClick = {
                        val toSend = inputText
                        inputText = ""
                        viewModel.sendAssistantMessage(toSend)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (inputText.isNotBlank()) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("ai_send_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isAr) "إرسال" else "Send",
                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
