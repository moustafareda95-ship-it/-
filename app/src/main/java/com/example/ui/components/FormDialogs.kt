package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BillCategory
import com.example.data.local.BillEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CalendarEventType
import com.example.data.local.ChildEntity
import com.example.data.local.EducationItemType
import com.example.data.local.FinanceCategory
import com.example.data.local.HomeworkExamEntity
import com.example.data.local.PaymentMethod
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.DateTimeUtils
import com.example.util.S

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskFormDialog(
    initial: TaskEntity? = null,
    isArabic: Boolean,
    tzId: String,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
) {
    val s = S()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember {
        mutableStateOf(
            initial?.priority?.let { runCatching { TaskPriority.valueOf(it) }.getOrNull() } ?: TaskPriority.MEDIUM
        )
    }
    var status by remember {
        mutableStateOf(
            initial?.status?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() } ?: TaskStatus.PENDING
        )
    }
    var category by remember {
        mutableStateOf(
            initial?.category?.let { runCatching { TaskCategory.valueOf(it) }.getOrNull() } ?: TaskCategory.WORK
        )
    }
    var labels by remember { mutableStateOf(initial?.labels ?: "") }
    var dueDateMillis by remember { mutableStateOf(initial?.dueDateMillis ?: System.currentTimeMillis()) }
    var isMeeting by remember { mutableStateOf(initial?.isMeeting ?: false) }
    var meetingNotes by remember { mutableStateOf(initial?.meetingNotes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = {
            Text(
                text = if (initial == null) s.addTask else (if (isArabic) "تعديل المهمة" else "Edit Task"),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(if (isArabic) "عنوان المهمة *" else "Task Title *") },
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isArabic) "الوصف" else "Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(if (isArabic) "الأولوية" else "Priority", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label(isArabic)) },
                        )
                    }
                }
                Text(if (isArabic) "التصنيف" else "Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskCategory.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.label(isArabic)) },
                        )
                    }
                }
                if (initial != null) {
                    Text(if (isArabic) "الحالة" else "Status", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label(isArabic)) },
                            )
                        }
                    }
                }
                DatePickerField(
                    label = if (isArabic) "تاريخ الاستحقاق" else "Due Date",
                    selectedMillis = dueDateMillis,
                    onDateSelected = { dueDateMillis = it },
                    isArabic = isArabic,
                    tzId = tzId,
                )
                OutlinedTextField(
                    value = labels,
                    onValueChange = { labels = it },
                    label = { Text(if (isArabic) "وسوم (اختياري)" else "Labels (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isMeeting, onCheckedChange = { isMeeting = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (isArabic) "اجتماع / موعد عمل" else "Work Meeting", style = MaterialTheme.typography.bodyMedium)
                }
                if (isMeeting) {
                    OutlinedTextField(
                        value = meetingNotes,
                        onValueChange = { meetingNotes = it },
                        label = { Text(if (isArabic) "ملاحظات الاجتماع وبنود التنفيذ" else "Meeting Notes & Action Items") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitted,
                onClick = {
                    if (title.isBlank()) {
                        errorMsg = if (isArabic) "يرجى إدخال عنوان المهمة" else "Please enter a task title"
                    } else {
                        submitted = true
                        onSave(
                            (initial ?: TaskEntity(title = title.trim(), dueDateMillis = dueDateMillis)).copy(
                                title = title.trim(),
                                description = description.trim(),
                                priority = priority.name,
                                status = status.name,
                                category = category.name,
                                labels = labels.trim(),
                                dueDateMillis = dueDateMillis,
                                isMeeting = isMeeting,
                                meetingNotes = meetingNotes.trim(),
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_task_button"),
            ) {
                Text(s.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFormDialog(
    initial: TransactionEntity? = null,
    defaultType: TransactionType = TransactionType.EXPENSE,
    isArabic: Boolean,
    tzId: String,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
) {
    val s = S()
    var type by remember {
        mutableStateOf(
            initial?.type?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() } ?: defaultType
        )
    }
    var amountText by remember { mutableStateOf(initial?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    val validCategories = if (type == TransactionType.INCOME) FinanceCategory.incomeCategories else FinanceCategory.expenseCategories
    var category by remember(type) {
        mutableStateOf(
            initial?.category?.let { runCatching { FinanceCategory.valueOf(it) }.getOrNull() }
                ?.takeIf { it.isIncome == (type == TransactionType.INCOME) }
                ?: validCategories.first()
        )
    }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var paymentMethod by remember {
        mutableStateOf(
            initial?.paymentMethod?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: PaymentMethod.CASH
        )
    }
    var dateMillis by remember { mutableStateOf(initial?.dateMillis ?: System.currentTimeMillis()) }
    var isEstimate by remember { mutableStateOf(initial?.isEstimate ?: false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = {
            Text(
                text = if (initial == null) {
                    if (type == TransactionType.INCOME) s.addIncome else s.addExpense
                } else {
                    if (isArabic) "تعديل المعاملة" else "Edit Transaction"
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label(isArabic)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text(if (isArabic) "المبلغ ($currency) *" else "Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; errorMsg = null },
                    label = { Text(if (isArabic) "الوصف *" else "Description *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_desc_input"),
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(if (isArabic) "الفئة" else "Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    validCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label(isArabic)) },
                        )
                    }
                }
                Text(if (isArabic) "طريقة الدفع" else "Payment Method", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = paymentMethod == pm,
                            onClick = { paymentMethod = pm },
                            label = { Text(pm.label(isArabic)) },
                        )
                    }
                }
                DatePickerField(
                    label = if (isArabic) "التاريخ" else "Date",
                    selectedMillis = dateMillis,
                    onDateSelected = { dateMillis = it },
                    isArabic = isArabic,
                    tzId = tzId,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEstimate, onCheckedChange = { isEstimate = it })
                    Text(
                        text = if (isArabic) "مبلغ تقديري (غير مؤكد بعد)" else "Estimated amount (not actual yet)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitted,
                onClick = {
                    val amount = amountText.trim().replace(',', '.').replace('٫', '.').toDoubleOrNull()
                    when {
                        amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0.0 -> {
                            errorMsg = if (isArabic) "أدخل مبلغاً صحيحاً أكبر من صفر" else "Enter a valid amount greater than 0"
                        }
                        description.isBlank() -> {
                            errorMsg = if (isArabic) "يرجى كتابة وصف المعاملة" else "Please enter a description"
                        }
                        else -> {
                            submitted = true
                            onSave(
                                (initial ?: TransactionEntity(
                                    type = type.name,
                                    amount = amount,
                                    category = category.name,
                                    description = description.trim(),
                                    dateMillis = dateMillis,
                                )).copy(
                                    type = type.name,
                                    amount = amount,
                                    category = category.name,
                                    description = description.trim(),
                                    paymentMethod = paymentMethod.name,
                                    dateMillis = dateMillis,
                                    isEstimate = isEstimate,
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_tx_button"),
            ) {
                Text(s.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillFormDialog(
    initial: BillEntity? = null,
    isArabic: Boolean,
    tzId: String,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (BillEntity) -> Unit,
) {
    val s = S()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember {
        mutableStateOf(
            initial?.category?.let { runCatching { BillCategory.valueOf(it) }.getOrNull() } ?: BillCategory.ELECTRICITY
        )
    }
    var amountText by remember { mutableStateOf(initial?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var dueMillis by remember { mutableStateOf(initial?.dueDateMillis ?: DateTimeUtils.addDays(System.currentTimeMillis(), 3, tzId)) }
    var isRecurring by remember { mutableStateOf(initial?.isRecurring ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = { Text(if (initial == null) s.addBill else (if (isArabic) "تعديل الفاتورة" else "Edit Bill"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(if (isArabic) "اسم الفاتورة *" else "Bill Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_title_input"),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text(if (isArabic) "القيمة ($currency) *" else "Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(if (isArabic) "النوع" else "Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BillCategory.entries.forEach { bc ->
                        FilterChip(
                            selected = category == bc,
                            onClick = {
                                category = bc
                                if (title.isBlank()) title = if (isArabic) "فاتورة ${bc.label(true)}" else "${bc.label(false)} Bill"
                            },
                            label = { Text(bc.label(isArabic)) },
                        )
                    }
                }
                DatePickerField(
                    label = if (isArabic) "تاريخ الاستحقاق" else "Due Date",
                    selectedMillis = dueMillis,
                    onDateSelected = { dueMillis = it },
                    isArabic = isArabic,
                    tzId = tzId,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (isArabic) "فاتورة شهرية متكررة" else "Monthly recurring bill", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitted,
                onClick = {
                    val amt = amountText.trim().replace(',', '.').replace('٫', '.').toDoubleOrNull()
                    if (title.isBlank() || amt == null || amt.isNaN() || amt.isInfinite() || amt <= 0.0) {
                        errorMsg = if (isArabic) "يرجى إدخال اسم الفاتورة وقيمة أكبر من صفر" else "Enter bill title and valid amount > 0"
                    } else {
                        submitted = true
                        onSave(
                            (initial ?: BillEntity(title = title.trim(), amount = amt, dueDateMillis = dueMillis)).copy(
                                title = title.trim(),
                                category = category.name,
                                amount = amt,
                                dueDateMillis = dueMillis,
                                isRecurring = isRecurring,
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_bill_button"),
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeworkExamFormDialog(
    children: List<ChildEntity>,
    initial: HomeworkExamEntity? = null,
    defaultChildId: Long? = null,
    defaultType: EducationItemType = EducationItemType.HOMEWORK,
    isArabic: Boolean,
    tzId: String,
    onDismiss: () -> Unit,
    onSave: (HomeworkExamEntity) -> Unit,
) {
    val s = S()
    var selectedChildId by remember { mutableStateOf(initial?.childId ?: defaultChildId ?: children.firstOrNull()?.id ?: 0L) }
    var type by remember {
        mutableStateOf(initial?.type?.let { runCatching { EducationItemType.valueOf(it) }.getOrNull() } ?: defaultType)
    }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var dueMillis by remember { mutableStateOf(initial?.dueDateMillis ?: DateTimeUtils.addDays(System.currentTimeMillis(), 1, tzId)) }
    var scoreText by remember { mutableStateOf(initial?.scoreEarned?.toString() ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = {
            Text(
                text = if (type == EducationItemType.EXAM) s.addExam else s.addHomework,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (children.isEmpty()) {
                        Text(
                            if (isArabic) "يرجى إضافة ملف طفل أولاً من شاشة التعليم" else "Please create a child profile first in Education",
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(if (isArabic) "الابن / الابنة" else "Child", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            children.forEach { c ->
                                FilterChip(
                                    selected = selectedChildId == c.id,
                                    onClick = { selectedChildId = c.id },
                                    label = { Text(c.name) },
                                )
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EducationItemType.entries.forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    label = { Text(t.label(isArabic)) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it; errorMsg = null },
                            label = { Text(if (isArabic) "المادة الدراسية *" else "Subject *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("hw_subject_input"),
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; errorMsg = null },
                            label = { Text(if (isArabic) "العنوان *" else "Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("hw_title_input"),
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(if (isArabic) "تفاصيل / صفحات" else "Details / Pages") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = scoreText,
                            onValueChange = { scoreText = it },
                            label = { Text(if (isArabic) "الدرجة من 100 (اختياري)" else "Score out of 100 (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DatePickerField(
                            label = if (isArabic) "الموعد" else "Due / Exam Date",
                            selectedMillis = dueMillis,
                            onDateSelected = { dueMillis = it },
                            isArabic = isArabic,
                            tzId = tzId,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = children.isNotEmpty() && !submitted,
                onClick = {
                    val trimmedScore = scoreText.trim().replace(',', '.').replace('٫', '.')
                    val parsedScore = if (trimmedScore.isEmpty()) null else trimmedScore.toDoubleOrNull()
                    when {
                        selectedChildId == 0L || subject.isBlank() || title.isBlank() -> {
                            errorMsg = if (isArabic) "يرجى اختيار الطفل وإدخال المادة والعنوان" else "Select child and enter subject and title"
                        }
                        trimmedScore.isNotEmpty() && (parsedScore == null || parsedScore.isNaN() || parsedScore < 0.0 || parsedScore > 100.0) -> {
                            errorMsg = if (isArabic) "الدرجة يجب أن تكون رقماً بين 0 و 100" else "Score must be a number between 0 and 100"
                        }
                        else -> {
                            submitted = true
                            onSave(
                                (initial ?: HomeworkExamEntity(
                                    childId = selectedChildId,
                                    type = type.name,
                                    subject = subject.trim(),
                                    title = title.trim(),
                                    dueDateMillis = dueMillis,
                                )).copy(
                                    childId = selectedChildId,
                                    type = type.name,
                                    subject = subject.trim(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    dueDateMillis = dueMillis,
                                    scoreEarned = parsedScore,
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_hw_button"),
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarEventFormDialog(
    initial: CalendarEventEntity? = null,
    defaultDateMillis: Long = System.currentTimeMillis(),
    isArabic: Boolean,
    tzId: String,
    onDismiss: () -> Unit,
    onSave: (CalendarEventEntity) -> Unit,
) {
    val s = S()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var eventType by remember {
        mutableStateOf(
            initial?.eventType?.let { runCatching { CalendarEventType.valueOf(it) }.getOrNull() } ?: CalendarEventType.PERSONAL
        )
    }
    var dateMillis by remember { mutableStateOf(initial?.startMillis ?: defaultDateMillis) }
    var hourText by remember { mutableStateOf("10") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = { Text(if (initial == null) s.addAppointment else (if (isArabic) "تعديل الموعد" else "Edit Event"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(if (isArabic) "عنوان الموعد *" else "Event Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_title_input"),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isArabic) "الملاحظات / المكان" else "Notes / Location") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { hourText = it },
                    label = { Text(if (isArabic) "الساعة (0 - 23)" else "Hour (0 - 23)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(if (isArabic) "النوع" else "Event Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CalendarEventType.entries.forEach { et ->
                        FilterChip(
                            selected = eventType == et,
                            onClick = { eventType = et },
                            label = { Text(et.label(isArabic)) },
                        )
                    }
                }
                DatePickerField(
                    label = if (isArabic) "التاريخ" else "Date",
                    selectedMillis = dateMillis,
                    onDateSelected = { dateMillis = it },
                    isArabic = isArabic,
                    tzId = tzId,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !submitted,
                onClick = {
                    val hr = hourText.trim().toIntOrNull()
                    when {
                        title.isBlank() -> {
                            errorMsg = if (isArabic) "يرجى إدخال عنوان الموعد" else "Enter event title"
                        }
                        hr == null || hr !in 0..23 -> {
                            errorMsg = if (isArabic) "أدخل ساعة صحيحة بين 0 و 23" else "Enter a valid hour between 0 and 23"
                        }
                        else -> {
                            submitted = true
                            val start = DateTimeUtils.setHourMinute(dateMillis, hr, 0, tzId)
                            onSave(
                                (initial ?: CalendarEventEntity(
                                    title = title.trim(),
                                    eventType = eventType.name,
                                    startMillis = start,
                                    endMillis = start + 3_600_000L,
                                )).copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    eventType = eventType.name,
                                    startMillis = start,
                                    endMillis = start + 3_600_000L,
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_event_button"),
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
