package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.AiPreviewBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskFormDialog
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorSoft
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoSoft
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.S

private enum class TaskFilterMode { ALL, TODAY, WEEK, OVERDUE, COMPLETED }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkScreen(
    state: HayatiUiState,
    workAiInsight: String?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val now = System.currentTimeMillis()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(TaskFilterMode.ALL) }
    var priorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val overdueCount = remember(state.tasks, tz) {
        state.tasks.count { DateTimeUtils.isOverdue(it.dueDateMillis, it.status == TaskStatus.COMPLETED.name, now, tz) }
    }

    val filteredTasks = remember(state.tasks, searchQuery, filterMode, priorityFilter, tz) {
        val todayStart = DateTimeUtils.startOfDayMillis(now, tz)
        val weekEnd = DateTimeUtils.endOfDayMillis(DateTimeUtils.addDays(todayStart, 7, tz), tz)
        state.tasks.filter { t ->
            val matchesQuery = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.description.contains(searchQuery, ignoreCase = true) ||
                t.labels.contains(searchQuery, ignoreCase = true)
            val matchesPriority = priorityFilter == null || t.priority == priorityFilter!!.name
            val matchesMode = when (filterMode) {
                TaskFilterMode.ALL -> true
                TaskFilterMode.TODAY -> DateTimeUtils.isSameDay(t.dueDateMillis, now, tz)
                TaskFilterMode.WEEK -> t.dueDateMillis in todayStart..weekEnd
                TaskFilterMode.OVERDUE -> DateTimeUtils.isOverdue(t.dueDateMillis, t.status == TaskStatus.COMPLETED.name, now, tz)
                TaskFilterMode.COMPLETED -> t.status == TaskStatus.COMPLETED.name
            }
            matchesQuery && matchesPriority && matchesMode
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TurquoisePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("work_add_task_fab"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = s.addTask)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("work_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Overdue alert banner
            if (overdueCount > 0) {
                item {
                    HayatiCard(
                        containerColor = StatusErrorSoft.copy(alpha = 0.55f),
                        borderColor = StatusError.copy(alpha = 0.4f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusError)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "تنبيه: لديك $overdueCount مهام متأخرة عن موعدها"
                                else "Alert: $overdueCount task(s) are overdue",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusError,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { filterMode = TaskFilterMode.OVERDUE }) {
                                Text(if (isAr) "تصفية" else "Filter")
                            }
                        }
                    }
                }
            }

            // AI Work Summary & Prioritization
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isAr) "مساعد العمل والمهام" else "Work & Tasks Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedButton(onClick = { viewModel.generateWorkInsight() }) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = TurquoisePrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAr) "تلخيص وترتيب" else "AI Prioritize")
                    }
                }
            }

            if (workAiInsight != null) {
                item {
                    HayatiCard(borderColor = TurquoisePrimary.copy(alpha = 0.5f)) {
                        AiPreviewBanner()
                        Spacer(Modifier.height(8.dp))
                        Text(workAiInsight, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { viewModel.dismissWorkInsight() }) {
                            Text(s.close)
                        }
                    }
                }
            }

            // Search & Filters
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(s.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = if (isAr) "مسح البحث" else "Clear search",
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().testTag("work_search_input"),
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val modes = listOf(
                            TaskFilterMode.ALL to (if (isAr) "الكل" else "All"),
                            TaskFilterMode.TODAY to (if (isAr) "اليوم" else "Today"),
                            TaskFilterMode.WEEK to (if (isAr) "الأسبوع" else "Week"),
                            TaskFilterMode.OVERDUE to (if (isAr) "متأخرة" else "Overdue"),
                            TaskFilterMode.COMPLETED to (if (isAr) "مكتملة" else "Done"),
                        )
                        modes.forEach { (mode, label) ->
                            FilterChip(
                                selected = filterMode == mode,
                                onClick = { filterMode = mode },
                                label = { Text(label) },
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priorityFilter == p,
                                onClick = { priorityFilter = if (priorityFilter == p) null else p },
                                label = { Text(p.label(isAr)) },
                            )
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                val hasActiveFilters = searchQuery.isNotBlank() || filterMode != TaskFilterMode.ALL || priorityFilter != null
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.WorkOutline,
                        title = if (hasActiveFilters) {
                            if (isAr) "لا توجد مهام مطابقة للتصفية" else "No matching tasks"
                        } else {
                            s.noRecordsYet
                        },
                        subtitle = if (hasActiveFilters) {
                            if (isAr) "جرّب مسح البحث أو إعادة ضبط الفلاتر لعرض جميع مهامك" else "Try clearing search or resetting filters to see all tasks"
                        } else {
                            if (isAr) "أضف مهمة عمل أو اجتماع لبدء التنظيم" else "Add a work task or meeting to get started"
                        },
                        actionLabel = if (hasActiveFilters) {
                            if (isAr) "إعادة ضبط الفلاتر" else "Reset Filters"
                        } else {
                            s.addTask
                        },
                        onAction = {
                            if (hasActiveFilters) {
                                searchQuery = ""
                                filterMode = TaskFilterMode.ALL
                                priorityFilter = null
                            } else {
                                showCreateDialog = true
                            }
                        },
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    WorkTaskCard(
                        task = task,
                        isArabic = isAr,
                        tzId = tz,
                        nowMillis = now,
                        onToggle = { viewModel.toggleTaskCompleted(task) },
                        onEdit = { editingTask = task },
                        onDelete = { viewModel.deleteTask(task) },
                    )
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showCreateDialog) {
        TaskFormDialog(
            isArabic = isAr,
            tzId = tz,
            onDismiss = { showCreateDialog = false },
            onSave = {
                viewModel.saveTask(it)
                showCreateDialog = false
            },
        )
    }

    editingTask?.let { task ->
        TaskFormDialog(
            initial = task,
            isArabic = isAr,
            tzId = tz,
            onDismiss = { editingTask = null },
            onSave = {
                viewModel.saveTask(it)
                editingTask = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkTaskCard(
    task: TaskEntity,
    isArabic: Boolean,
    tzId: String,
    nowMillis: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val completed = task.status == TaskStatus.COMPLETED.name
    val isOverdue = DateTimeUtils.isOverdue(task.dueDateMillis, completed, nowMillis, tzId)
    val priority = runCatching { TaskPriority.valueOf(task.priority) }.getOrElse { TaskPriority.MEDIUM }
    val status = runCatching { TaskStatus.valueOf(task.status) }.getOrElse { TaskStatus.PENDING }
    val category = runCatching { TaskCategory.valueOf(task.category) }.getOrElse { TaskCategory.WORK }
    val accentColor = when {
        completed -> StatusSuccess
        isOverdue || priority == TaskPriority.HIGH -> StatusError
        priority == TaskPriority.MEDIUM -> StatusWarning
        else -> TurquoisePrimary
    }

    HayatiCard(
        borderColor = if (isOverdue) StatusError.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
            Spacer(Modifier.width(4.dp))
            Checkbox(checked = completed, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f),
                    )
                    if (task.isMeeting) {
                        Icon(Icons.Outlined.Videocam, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(18.dp))
                    }
                }
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (task.isMeeting && task.meetingNotes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = (if (isArabic) "ملاحظات الاجتماع: " else "Meeting notes: ") + task.meetingNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusInfo,
                    )
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
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
                    StatusBadge(
                        text = if (isOverdue) (if (isArabic) "متأخرة" else "Overdue") else status.label(isArabic),
                        containerColor = if (isOverdue) StatusErrorSoft else StatusInfoSoft,
                        contentColor = if (isOverdue) StatusError else StatusInfo,
                    )
                    StatusBadge(
                        text = category.label(isArabic),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = DateTimeUtils.formatDate(task.dueDateMillis, isArabic, tzId),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) StatusError else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = S().edit, tint = TurquoisePrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = S().delete, tint = StatusError)
                }
            }
        }
    }
}
