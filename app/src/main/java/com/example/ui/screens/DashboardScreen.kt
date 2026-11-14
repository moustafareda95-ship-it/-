package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.EducationItemType
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.TransactionType
import com.example.domain.ai.DailyPlanSlot
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.AiPreviewBanner
import com.example.ui.components.CalendarEventFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.HomeworkExamFormDialog
import com.example.ui.components.KpiMetricTile
import com.example.ui.components.LocalStorageBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskFormDialog
import com.example.ui.components.TransactionFormDialog
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyMid
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorSoft
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.S
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: HayatiUiState,
    dailyPlanSlots: List<DailyPlanSlot>,
    viewModel: MainViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val currency = state.prefs.currency
    val now = System.currentTimeMillis()

    var showTaskDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showPlannerDialog by remember { mutableStateOf(false) }
    var plannerTargetDay by remember { mutableStateOf(DateTimeUtils.startOfDayMillis(now, tz)) }

    val overdueTasks = remember(state.tasks, tz) {
        state.tasks.filter {
            DateTimeUtils.isOverdue(it.dueDateMillis, it.status == TaskStatus.COMPLETED.name, now, tz)
        }
    }
    val todayTasks = remember(state.tasks, tz) {
        state.tasks.filter { DateTimeUtils.isSameDay(it.dueDateMillis, now, tz) }
    }
    val completedTodayCount = todayTasks.count { it.status == TaskStatus.COMPLETED.name }
    val unpaidBills = remember(state.bills) { state.bills.filter { !it.isPaid } }
    val upcomingEvents = remember(state.calendarEvents, tz) {
        state.calendarEvents.filter { it.endMillis >= DateTimeUtils.startOfDayMillis(now, tz) }.take(4)
    }
    val allPendingEduCount = remember(state.homeworkExams) { state.homeworkExams.count { !it.isCompleted } }
    val pendingEdu = remember(state.homeworkExams) { state.homeworkExams.filter { !it.isCompleted }.take(4) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Hero Welcome Card + "Plan My Day with AI"
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(NavyDeep),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dashboard_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.42f,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(NavyDeep.copy(alpha = 0.55f), NavyMid.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "أهلاً بك، ${state.prefs.userName}" else "Welcome, ${state.prefs.userName}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = DateTimeUtils.formatDate(now, isAr, tz) + " • $tz",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        if (state.tasks.any { it.isSample } || state.transactions.any { it.isSample }) {
                            StatusBadge(
                                text = s.sampleDataBadge,
                                containerColor = GoldAccent.copy(alpha = 0.25f),
                                contentColor = GoldAccent,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            plannerTargetDay = DateTimeUtils.startOfDayMillis(now, tz)
                            viewModel.buildDailyPlan(plannerTargetDay)
                            showPlannerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurquoisePrimary,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("plan_my_day_button"),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = GoldAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(s.planMyDayButton, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Quick Action Buttons (above the fold right under Hero)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = s.quickActions)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionChip(
                        label = s.addTask,
                        icon = Icons.Outlined.AddTask,
                        testTag = "quick_add_task",
                        onClick = { showTaskDialog = true },
                    )
                    QuickActionChip(
                        label = s.addExpense,
                        icon = Icons.Outlined.AttachMoney,
                        testTag = "quick_add_expense",
                        onClick = { showExpenseDialog = true },
                    )
                    QuickActionChip(
                        label = s.addAppointment,
                        icon = Icons.Outlined.Event,
                        testTag = "quick_add_event",
                        onClick = { showEventDialog = true },
                    )
                    QuickActionChip(
                        label = s.addHomework,
                        icon = Icons.Outlined.School,
                        testTag = "quick_add_homework",
                        onClick = { showHomeworkDialog = true },
                    )
                }
            }
        }

        // Empty welcome helper if user hasn't added any data yet
        if (!state.isLoading && !state.hasAnyRecords) {
            item {
                HayatiCard {
                    Text(
                        text = if (isAr) "ابدأ بتنظيم يومك في حياتي AI" else "Start organizing your life with HAYATI AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isAr) {
                            "يمكنك إضافة مهامك ومصروفاتك مباشرة من الأزرار أعلاه، أو تحميل بيانات تجريبية واضحة لتجربة كل شاشات التطبيق."
                        } else {
                            "Add your own tasks and expenses using the quick actions above, or load labeled sample data to preview every module."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        enabled = !state.isSaving,
                        onClick = { viewModel.loadSampleData() },
                        modifier = Modifier.testTag("load_sample_data_btn"),
                    ) {
                        Text(s.loadSampleData)
                    }
                }
            }
        }

        // 2b. Interactive Daily KPI Summary Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiMetricTile(
                        label = s.todayTasks,
                        value = "$completedTodayCount/${todayTasks.size}",
                        icon = Icons.Outlined.TaskAlt,
                        accent = TurquoisePrimary,
                        onClick = { onNavigateToRoute("work") },
                        modifier = Modifier.weight(1f),
                    )
                    KpiMetricTile(
                        label = s.overdueTasks,
                        value = overdueTasks.size.toString(),
                        icon = Icons.Outlined.WarningAmber,
                        accent = if (overdueTasks.isNotEmpty()) StatusError else StatusSuccess,
                        onClick = { onNavigateToRoute(if (overdueTasks.isNotEmpty()) "notifications" else "work") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiMetricTile(
                        label = s.upcomingBills,
                        value = unpaidBills.size.toString(),
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        accent = if (unpaidBills.isNotEmpty()) GoldAccent else StatusSuccess,
                        onClick = { onNavigateToRoute("family") },
                        modifier = Modifier.weight(1f),
                    )
                    KpiMetricTile(
                        label = s.childrenHomeworkExams,
                        value = allPendingEduCount.toString(),
                        icon = Icons.Outlined.School,
                        accent = StatusInfo,
                        onClick = { onNavigateToRoute("education") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 3. Overdue Alert Card
        if (overdueTasks.isNotEmpty()) {
            item {
                HayatiCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    borderColor = StatusError.copy(alpha = 0.4f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusError)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${s.overdueTasks} (${overdueTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusError,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onNavigateToRoute("work") }) {
                            Text(if (isAr) "عرض الكل" else "View All")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    overdueTasks.take(3).forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTaskCompleted(t) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = t.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = DateTimeUtils.formatShortDate(t.dueDateMillis, isAr, tz),
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError,
                            )
                        }
                    }
                }
            }
        }

        // 4. Financial Overview Card
        item {
            val fin = state.financeSummary
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = if (isAr) "الملخص المالي للشهر" else "Monthly Financial Overview",
                    icon = Icons.Outlined.AttachMoney,
                    actionLabel = if (isAr) "المالية" else "Finance",
                    onActionClick = { onNavigateToRoute("finance") },
                )
                Spacer(Modifier.height(8.dp))
                HayatiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricPill(
                        label = s.monthlyIncome,
                        value = DateTimeUtils.formatCurrency(fin.totalIncome, currency, isAr),
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        accent = StatusSuccess,
                        modifier = Modifier.weight(1f),
                    )
                    MetricPill(
                        label = s.monthlyExpenses,
                        value = DateTimeUtils.formatCurrency(fin.totalExpenses, currency, isAr),
                        icon = Icons.AutoMirrored.Outlined.TrendingDown,
                        accent = StatusError,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.remainingBudget, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = DateTimeUtils.formatCurrency(fin.remainingBalance, currency, isAr),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (fin.remainingBalance >= 0) TurquoisePrimary else StatusError,
                    )
                }
                if (fin.monthlyBudgetLimit > 0) {
                    Spacer(Modifier.height(8.dp))
                    val ratio = (fin.totalExpenses / fin.monthlyBudgetLimit).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (fin.budgetUsedPercent > 100) StatusError else TurquoisePrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isAr) "تم استهلاك ${fin.budgetUsedPercent}% من حدود الميزانية الشهرية"
                        else "${fin.budgetUsedPercent}% of monthly budget limits used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }

        // 5. Today's Tasks
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "${s.todayTasks} ($completedTodayCount/${todayTasks.size})",
                    icon = Icons.Outlined.CheckCircle,
                    actionLabel = if (isAr) "إدارة المهام" else "Manage",
                    onActionClick = { onNavigateToRoute("work") },
                )
                Spacer(Modifier.height(8.dp))
                if (todayTasks.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Outlined.AddTask,
                        title = if (isAr) "لا توجد مهام لليوم" else "No tasks for today",
                        subtitle = if (isAr) "أضف مهام عملك أو منزلك لمتابعة الإنجاز" else "Add today's tasks to track completion",
                        actionLabel = s.addTask,
                        onAction = { showTaskDialog = true },
                    )
                } else {
                    HayatiCard {
                        todayTasks.forEach { task ->
                            DashboardTaskRow(
                                task = task,
                                isArabic = isAr,
                                onToggle = { viewModel.toggleTaskCompleted(task) },
                            )
                        }
                    }
                }
            }
        }

        // 6. Upcoming Bills & Deadlines
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "${s.upcomingBills} (${unpaidBills.size})",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    actionLabel = if (isAr) "الفواتير" else "Bills",
                    onActionClick = { onNavigateToRoute("family") },
                )
                Spacer(Modifier.height(8.dp))
                if (unpaidBills.isEmpty()) {
                    HayatiCard {
                        Text(
                            text = if (isAr) "لا توجد فواتير غير مدفوعة حالياً" else "No unpaid bills right now",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    HayatiCard {
                        unpaidBills.take(4).forEach { bill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bill.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = DateTimeUtils.formatShortDate(bill.dueDateMillis, isAr, tz) +
                                            " • " + DateTimeUtils.formatCurrency(bill.amount, currency, isAr),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.toggleBillPaid(bill) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(if (isAr) "تم الدفع" else "Mark Paid", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Children's Homework & Upcoming Exams
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = s.childrenHomeworkExams,
                    icon = Icons.Outlined.School,
                    actionLabel = if (isAr) "التعليم" else "Education",
                    onActionClick = { onNavigateToRoute("education") },
                )
                Spacer(Modifier.height(8.dp))
                if (pendingEdu.isEmpty()) {
                    HayatiCard {
                        Text(
                            text = if (isAr) "لا توجد واجبات أو امتحانات قادمة" else "No upcoming homework or exams",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    HayatiCard {
                        pendingEdu.forEach { hw ->
                            val childName = state.children.firstOrNull { it.id == hw.childId }?.name ?: ""
                            val isExam = hw.type == EducationItemType.EXAM.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = hw.isCompleted,
                                    onCheckedChange = { viewModel.toggleHomeworkCompleted(hw) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hw.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "$childName • ${hw.subject} • ${DateTimeUtils.formatShortDate(hw.dueDateMillis, isAr, tz)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                StatusBadge(
                                    text = if (isExam) (if (isAr) "امتحان" else "Exam") else (if (isAr) "واجب" else "HW"),
                                    containerColor = if (isExam) StatusErrorSoft else StatusWarningSoft,
                                    contentColor = if (isExam) StatusError else StatusWarning,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Upcoming Appointments
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = s.upcomingAppointments,
                    icon = Icons.Outlined.CalendarMonth,
                    actionLabel = s.navCalendar,
                    onActionClick = { onNavigateToRoute("calendar") },
                )
                Spacer(Modifier.height(8.dp))
                if (upcomingEvents.isEmpty()) {
                    HayatiCard {
                        Text(
                            text = if (isAr) "لا توجد مواعيد قادمة مسجلة" else "No upcoming appointments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    HayatiCard {
                        upcomingEvents.forEach { ev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Event, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ev.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${DateTimeUtils.formatShortDate(ev.startMillis, isAr, tz)} • ${DateTimeUtils.formatTime(ev.startMillis, isAr, tz)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                LocalStorageBanner()
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTaskDialog) {
        TaskFormDialog(
            isArabic = isAr,
            tzId = tz,
            onDismiss = { showTaskDialog = false },
            onSave = {
                viewModel.saveTask(it)
                showTaskDialog = false
            },
        )
    }
    if (showExpenseDialog) {
        TransactionFormDialog(
            defaultType = TransactionType.EXPENSE,
            isArabic = isAr,
            tzId = tz,
            currency = currency,
            onDismiss = { showExpenseDialog = false },
            onSave = {
                viewModel.saveTransaction(it)
                showExpenseDialog = false
            },
        )
    }
    if (showEventDialog) {
        CalendarEventFormDialog(
            isArabic = isAr,
            tzId = tz,
            onDismiss = { showEventDialog = false },
            onSave = {
                viewModel.saveCalendarEvent(it)
                showEventDialog = false
            },
        )
    }
    if (showHomeworkDialog) {
        HomeworkExamFormDialog(
            children = state.children,
            isArabic = isAr,
            tzId = tz,
            onDismiss = { showHomeworkDialog = false },
            onSave = {
                viewModel.saveHomeworkExam(it)
                showHomeworkDialog = false
            },
        )
    }
    if (showPlannerDialog) {
        SmartDailyPlannerDialog(
            slots = dailyPlanSlots,
            targetDayMillis = plannerTargetDay,
            isArabic = isAr,
            tzId = tz,
            isSaving = state.isSaving,
            onShiftTargetDay = { delta ->
                plannerTargetDay = DateTimeUtils.addDays(plannerTargetDay, delta, tz)
                viewModel.buildDailyPlan(plannerTargetDay)
            },
            onShiftSlotHour = { id, delta -> viewModel.shiftPlanSlotHour(id, delta) },
            onRemoveSlot = { id -> viewModel.removePlanSlot(id) },
            onAcceptPlan = {
                viewModel.acceptDailyPlan(plannerTargetDay)
                showPlannerDialog = false
            },
            onDismiss = { showPlannerDialog = false },
        )
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.28f)),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(testTag),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = TurquoisePrimary)
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.08f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun DashboardTaskRow(
    task: TaskEntity,
    isArabic: Boolean,
    onToggle: () -> Unit,
) {
    val completed = task.status == TaskStatus.COMPLETED.name
    val priority = runCatching { TaskPriority.valueOf(task.priority) }.getOrElse { TaskPriority.MEDIUM }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = completed, onCheckedChange = { onToggle() })
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
            color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StatusBadge(
            text = priority.label(isArabic),
            containerColor = when (priority) {
                TaskPriority.HIGH -> StatusErrorSoft
                TaskPriority.MEDIUM -> StatusWarningSoft
                TaskPriority.LOW -> StatusSuccessSoft
            },
            contentColor = when (priority) {
                TaskPriority.HIGH -> StatusError
                TaskPriority.MEDIUM -> StatusWarning
                TaskPriority.LOW -> StatusSuccess
            },
        )
    }
}

@Composable
private fun SmartDailyPlannerDialog(
    slots: List<DailyPlanSlot>,
    targetDayMillis: Long,
    isArabic: Boolean,
    tzId: String,
    isSaving: Boolean,
    onShiftTargetDay: (Int) -> Unit,
    onShiftSlotHour: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onAcceptPlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = S()
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = {
            Column {
                Text(s.planMyDayButton, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onShiftTargetDay(-1) }) { Text(if (isArabic) "اليوم السابق" else "Prev") }
                    Text(
                        text = DateTimeUtils.formatShortDate(targetDayMillis, isArabic, tzId),
                        style = MaterialTheme.typography.labelLarge,
                        color = TurquoisePrimary,
                    )
                    TextButton(onClick = { onShiftTargetDay(1) }) { Text(if (isArabic) "اليوم التالي" else "Next") }
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(340.dp),
            ) {
                item { AiPreviewBanner() }
                items(slots.size) { idx ->
                    val slot = slots[idx]
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (slot.isUrgentOrOverdue) StatusErrorSoft.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TurquoisePrimary.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", slot.startHour, slot.startMinute),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoisePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(slot.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(slot.sourceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!slot.isLockedEvent && slot.id != "empty_tip") {
                                IconButton(onClick = { onShiftSlotHour(slot.id, -1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = if (isArabic) "تقديم ساعة" else "Earlier",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(onClick = { onShiftSlotHour(slot.id, 1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = if (isArabic) "تأخير ساعة" else "Later",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(onClick = { onRemoveSlot(slot.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAcceptPlan,
                enabled = !isSaving && slots.any { !it.isLockedEvent && it.id != "empty_tip" },
            ) {
                Text(if (isArabic) "اعتماد وإضافة للتقويم" else "Apply to Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.close) }
        },
    )
}
