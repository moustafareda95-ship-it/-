package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CalendarEventType
import com.example.data.local.EducationItemType
import com.example.data.local.HomeEventType
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.CalendarEventFormDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EventBillColor
import com.example.ui.theme.EventExamColor
import com.example.ui.theme.EventFamilyColor
import com.example.ui.theme.EventHomeworkColor
import com.example.ui.theme.EventPersonalColor
import com.example.ui.theme.EventReminderColor
import com.example.ui.theme.EventWorkColor
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.S
import java.util.Calendar

private enum class CalendarViewMode { DAILY, WEEKLY, MONTHLY }

private data class UnifiedCalendarItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val millis: Long,
    val type: CalendarEventType,
    val editableEvent: CalendarEventEntity? = null,
)

@Composable
fun CalendarScreen(
    state: HayatiUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val todayMillis = remember(tz) { DateTimeUtils.startOfDayMillis(tzId = tz) }

    var viewMode by remember { mutableStateOf(CalendarViewMode.DAILY) }
    var selectedDateMillis by remember { mutableStateOf(todayMillis) }
    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    val allUnifiedItems = remember(
        state.tasks,
        state.bills,
        state.homeworkExams,
        state.homeEvents,
        state.calendarEvents,
        state.children,
        isAr,
    ) {
        val items = mutableListOf<UnifiedCalendarItem>()
        state.tasks.forEach { t ->
            items.add(
                UnifiedCalendarItem(
                    key = "task_${t.id}",
                    title = t.title,
                    subtitle = if (t.isMeeting) (if (isAr) "اجتماع عمل" else "Work Meeting") else (if (isAr) "مهمة" else "Task"),
                    millis = t.dueDateMillis,
                    type = CalendarEventType.WORK,
                )
            )
        }
        state.bills.forEach { b ->
            items.add(
                UnifiedCalendarItem(
                    key = "bill_${b.id}",
                    title = b.title,
                    subtitle = DateTimeUtils.formatCurrency(b.amount, state.prefs.currency, isAr),
                    millis = b.dueDateMillis,
                    type = CalendarEventType.BILL,
                )
            )
        }
        state.homeworkExams.forEach { hw ->
            val childName = state.children.firstOrNull { it.id == hw.childId }?.name ?: ""
            val isExam = hw.type == EducationItemType.EXAM.name
            items.add(
                UnifiedCalendarItem(
                    key = "hw_${hw.id}",
                    title = "${hw.subject}: ${hw.title}",
                    subtitle = childName,
                    millis = hw.dueDateMillis,
                    type = if (isExam) CalendarEventType.EXAM else CalendarEventType.HOMEWORK,
                )
            )
        }
        state.homeEvents.forEach { he ->
            val heType = runCatching { HomeEventType.valueOf(he.type) }.getOrElse { HomeEventType.OCCASION }
            items.add(
                UnifiedCalendarItem(
                    key = "home_${he.id}",
                    title = he.title,
                    subtitle = heType.label(isAr),
                    millis = he.dateMillis,
                    type = CalendarEventType.FAMILY,
                )
            )
        }
        state.calendarEvents.forEach { ce ->
            val et = runCatching { CalendarEventType.valueOf(ce.eventType) }.getOrElse { CalendarEventType.PERSONAL }
            items.add(
                UnifiedCalendarItem(
                    key = "cal_${ce.id}",
                    title = ce.title,
                    subtitle = DateTimeUtils.formatTime(ce.startMillis, isAr, tz) + if (ce.description.isNotBlank()) " • ${ce.description}" else "",
                    millis = ce.startMillis,
                    type = et,
                    editableEvent = ce,
                )
            )
        }
        items.sortedBy { it.millis }
    }

    val visibleItems = remember(allUnifiedItems, viewMode, selectedDateMillis, tz) {
        when (viewMode) {
            CalendarViewMode.DAILY -> allUnifiedItems.filter { DateTimeUtils.isSameDay(it.millis, selectedDateMillis, tz) }
            CalendarViewMode.WEEKLY -> {
                val start = DateTimeUtils.startOfDayMillis(selectedDateMillis, tz)
                val end = DateTimeUtils.endOfDayMillis(DateTimeUtils.addDays(start, 6, tz), tz)
                allUnifiedItems.filter { it.millis in start..end }
            }
            CalendarViewMode.MONTHLY -> {
                val targetMonth = DateTimeUtils.currentMonthKey(selectedDateMillis, tz)
                allUnifiedItems.filter { DateTimeUtils.currentMonthKey(it.millis, tz) == targetMonth }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEventDialog = true },
                containerColor = TurquoisePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("calendar_add_event_fab"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = s.addAppointment)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("calendar_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            CalendarViewMode.DAILY to (if (isAr) "يومي" else "Daily"),
                            CalendarViewMode.WEEKLY to (if (isAr) "أسبوعي" else "Weekly"),
                            CalendarViewMode.MONTHLY to (if (isAr) "شهري" else "Monthly"),
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = viewMode == mode,
                                onClick = { viewMode = mode },
                                label = { Text(label) },
                            )
                        }
                    }
                    if (!DateTimeUtils.isSameDay(selectedDateMillis, todayMillis, tz)) {
                        TextButton(onClick = { selectedDateMillis = DateTimeUtils.startOfDayMillis(tzId = tz) }) {
                            Text(if (isAr) "اليوم" else "Today", color = TurquoisePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Date Navigator + Aligned 7-column Month Grid in Monthly mode
            item {
                HayatiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            val delta = when (viewMode) {
                                CalendarViewMode.DAILY -> -1
                                CalendarViewMode.WEEKLY -> -7
                                CalendarViewMode.MONTHLY -> -30
                            }
                            selectedDateMillis = DateTimeUtils.addDays(selectedDateMillis, delta, tz)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = if (isAr) "الفترة السابقة" else "Previous",
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatDate(selectedDateMillis, isAr, tz),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = {
                            val delta = when (viewMode) {
                                CalendarViewMode.DAILY -> 1
                                CalendarViewMode.WEEKLY -> 7
                                CalendarViewMode.MONTHLY -> 30
                            }
                            selectedDateMillis = DateTimeUtils.addDays(selectedDateMillis, delta, tz)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = if (isAr) "الفترة التالية" else "Next",
                            )
                        }
                    }

                    if (viewMode == CalendarViewMode.MONTHLY) {
                        Spacer(Modifier.height(10.dp))
                        val cal = Calendar.getInstance(DateTimeUtils.timeZone(tz)).apply {
                            timeInMillis = selectedDateMillis
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val firstWeekDay = if (isAr) Calendar.SATURDAY else Calendar.SUNDAY
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        val leadingBlanks = ((dayOfWeek - firstWeekDay) + 7) % 7
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val weekdayHeaders = if (isAr) {
                            listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
                        } else {
                            listOf("S", "M", "T", "W", "T", "F", "S")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            weekdayHeaders.forEach { header ->
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        val cells = buildList<Int?> {
                            repeat(leadingBlanks) { add(null) }
                            for (d in 1..daysInMonth) add(d)
                            while (size % 7 != 0) add(null)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            cells.chunked(7).forEach { weekCells ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    weekCells.forEach { day ->
                                        if (day == null) {
                                            Spacer(modifier = Modifier.weight(1f).height(44.dp))
                                        } else {
                                            val dayCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                                            val dayMillis = dayCal.timeInMillis
                                            val isSelected = DateTimeUtils.isSameDay(dayMillis, selectedDateMillis, tz)
                                            val isToday = DateTimeUtils.isSameDay(dayMillis, todayMillis, tz)
                                            val hasEvent = allUnifiedItems.any { DateTimeUtils.isSameDay(it.millis, dayMillis, tz) }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        selectedDateMillis = dayMillis
                                                        viewMode = CalendarViewMode.DAILY
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) TurquoisePrimary else Color.Transparent)
                                                            .border(
                                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                                color = if (isToday && !isSelected) GoldAccent else Color.Transparent,
                                                                shape = CircleShape,
                                                            ),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = day.toString(),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                            fontWeight = if (hasEvent || isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        )
                                                    }
                                                    if (hasEvent) {
                                                        Spacer(Modifier.height(2.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) GoldAccent else TurquoisePrimary),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (visibleItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.CalendarMonth,
                        title = if (isAr) "لا توجد أحداث في هذه الفترة" else "No events in this period",
                        subtitle = if (isAr) "أضف موعداً شخصياً أو مهمة أو فاتورة وستظهر هنا تلقائياً" else "Add appointments, tasks, bills, or homework to see them here",
                        actionLabel = s.addAppointment,
                        onAction = { showEventDialog = true },
                    )
                }
            } else {
                items(visibleItems, key = { it.key }) { item ->
                    val badgeColor = when (item.type) {
                        CalendarEventType.WORK -> EventWorkColor
                        CalendarEventType.FAMILY -> EventFamilyColor
                        CalendarEventType.BILL -> EventBillColor
                        CalendarEventType.HOMEWORK -> EventHomeworkColor
                        CalendarEventType.EXAM -> EventExamColor
                        CalendarEventType.PERSONAL -> EventPersonalColor
                        CalendarEventType.REMINDER -> EventReminderColor
                    }
                    HayatiCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(42.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    StatusBadge(
                                        text = item.type.label(isAr),
                                        containerColor = badgeColor.copy(alpha = 0.14f),
                                        contentColor = badgeColor,
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = DateTimeUtils.formatShortDate(item.millis, isAr, tz) +
                                        if (item.subtitle.isNotBlank()) " • ${item.subtitle}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (item.editableEvent != null) {
                                IconButton(onClick = { editingEvent = item.editableEvent }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = s.edit, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteCalendarEvent(item.editableEvent) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(64.dp)) }
        }
    }

    if (showEventDialog) {
        CalendarEventFormDialog(
            defaultDateMillis = selectedDateMillis,
            isArabic = isAr,
            tzId = tz,
            onDismiss = { showEventDialog = false },
            onSave = {
                viewModel.saveCalendarEvent(it)
                showEventDialog = false
            },
        )
    }

    editingEvent?.let { ev ->
        CalendarEventFormDialog(
            initial = ev,
            isArabic = isAr,
            tzId = tz,
            onDismiss = { editingEvent = null },
            onSave = {
                viewModel.saveCalendarEvent(it)
                editingEvent = null
            },
        )
    }
}
