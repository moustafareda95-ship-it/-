package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.AppLanguage
import com.example.data.local.EducationItemType
import com.example.data.local.TaskStatus
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorSoft
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils

private data class ComputedAlert(
    val id: String,
    val title: String,
    val body: String,
    val badge: String,
    val isUrgent: Boolean,
)

@Composable
fun NotificationsScreen(
    state: HayatiUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val now = System.currentTimeMillis()

    var hasNotifPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        viewModel.setNotificationPrefs(
            state.prefs.morningSummaryEnabled,
            state.prefs.eveningSummaryEnabled,
            granted,
        )
    }

    val computedAlerts = remember(state.tasks, state.bills, state.homeworkExams, state.children, tz) {
        val list = mutableListOf<ComputedAlert>()
        val in5Days = DateTimeUtils.addDays(DateTimeUtils.startOfDayMillis(now, tz), 5, tz)

        state.tasks.filter {
            DateTimeUtils.isOverdue(it.dueDateMillis, it.status == TaskStatus.COMPLETED.name, now, tz)
        }.forEach { t ->
            list.add(
                ComputedAlert(
                    id = "overdue_${t.id}",
                    title = t.title,
                    body = (if (isAr) "تاريخ الاستحقاق كان " else "Was due ") + DateTimeUtils.formatShortDate(t.dueDateMillis, isAr, tz),
                    badge = if (isAr) "مهمة متأخرة" else "Overdue",
                    isUrgent = true,
                )
            )
        }

        state.bills.filter { !it.isPaid && it.dueDateMillis <= in5Days }.forEach { b ->
            list.add(
                ComputedAlert(
                    id = "bill_${b.id}",
                    title = b.title,
                    body = "${DateTimeUtils.formatCurrency(b.amount, state.prefs.currency, isAr)} • ${DateTimeUtils.formatShortDate(b.dueDateMillis, isAr, tz)}",
                    badge = if (isAr) "موعد فاتورة" else "Bill Due",
                    isUrgent = b.dueDateMillis < now,
                )
            )
        }

        state.homeworkExams.filter { !it.isCompleted && it.dueDateMillis <= in5Days }.forEach { hw ->
            val childName = state.children.firstOrNull { it.id == hw.childId }?.name ?: ""
            val isExam = hw.type == EducationItemType.EXAM.name
            list.add(
                ComputedAlert(
                    id = "edu_${hw.id}",
                    title = "${hw.subject}: ${hw.title}",
                    body = "$childName • ${DateTimeUtils.formatShortDate(hw.dueDateMillis, isAr, tz)}",
                    badge = if (isExam) (if (isAr) "امتحان قريب" else "Exam Soon") else (if (isAr) "واجب قادم" else "Homework"),
                    isUrgent = isExam,
                )
            )
        }
        list
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("notifications_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAr) "مركز التنبيهات والتذكيرات" else "Reminders & Alerts",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (computedAlerts.isNotEmpty()) {
                        StatusBadge(
                            text = computedAlerts.size.toString(),
                            containerColor = StatusErrorSoft,
                            contentColor = StatusError,
                        )
                    }
                }
                if (state.notifications.any { !it.isRead }) {
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Text(if (isAr) "تحديد كمقروء" else "Mark Read")
                    }
                }
            }
        }

        // Permission & background limitation card
        item {
            HayatiCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hasNotifPermission) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "تنبيهات النظام داخل التطبيق" else "In-App & Permission Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isAr) {
                                "يتم عرض تنبيهات المهام المتأخرة والفواتير والامتحانات هنا فورياً. الإشعارات المجدولة والتطبيق مغلق تتطلب إعداد خدمة خلفية مخصصة."
                            } else {
                                "Overdue tasks, upcoming bills, and exam alerts are computed live here. Background notifications while the app is closed require a background worker."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(if (isAr) "السماح بإشعارات الجهاز" else "Allow Device Notifications")
                    }
                }
            }
        }

        // Morning / Evening summary card
        if (state.prefs.morningSummaryEnabled || state.prefs.eveningSummaryEnabled) {
            item {
                val openToday = state.tasks.count { it.status != TaskStatus.COMPLETED.name && DateTimeUtils.isSameDay(it.dueDateMillis, now, tz) }
                val unpaid = state.bills.count { !it.isPaid }
                HayatiCard(borderColor = GoldAccent.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = GoldAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "ملخص اليوم السريع" else "Daily Summary Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isAr) {
                            "لديك اليوم $openToday مهام مفتوحة، و$unpaid فواتير غير مدفوعة، والرصيد الشهري المتاح ${DateTimeUtils.formatCurrency(state.financeSummary.remainingBalance, state.prefs.currency, true)}."
                        } else {
                            "Today: $openToday open task(s), $unpaid unpaid bill(s), remaining balance ${DateTimeUtils.formatCurrency(state.financeSummary.remainingBalance, state.prefs.currency, false)}."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (computedAlerts.isEmpty() && state.notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.NotificationsNone,
                    title = if (isAr) "لا توجد تنبيهات عاجلة" else "No urgent alerts",
                    subtitle = if (isAr) "كل مواعيدك وفواتيرك تحت السيطرة" else "All tasks and bills are on track",
                )
            }
        } else {
            items(computedAlerts, key = { it.id }) { alert ->
                val accent = if (alert.isUrgent) StatusError else StatusWarning
                HayatiCard(
                    borderColor = if (alert.isUrgent) StatusError.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(accent),
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = if (alert.isUrgent) Icons.Outlined.WarningAmber else Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(alert.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(
                            text = alert.badge,
                            containerColor = if (alert.isUrgent) StatusErrorSoft else StatusWarningSoft,
                            contentColor = accent,
                        )
                    }
                }
            }
            items(state.notifications, key = { "saved_${it.id}" }) { n ->
                HayatiCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary),
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Outlined.School, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(n.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(
                            text = if (isAr) "تذكير محفوظ" else "Saved",
                            containerColor = StatusInfoSoft,
                            contentColor = StatusInfo,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
