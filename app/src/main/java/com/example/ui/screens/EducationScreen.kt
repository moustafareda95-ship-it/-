package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ChildEntity
import com.example.data.local.EducationItemType
import com.example.data.local.HomeworkExamEntity
import com.example.domain.ai.LessonTutorResult
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.AiPreviewBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.HomeworkExamFormDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorSoft
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.S

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EducationScreen(
    state: HayatiUiState,
    tutorResult: LessonTutorResult?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId

    var selectedChildId by remember(state.children) {
        mutableStateOf(state.children.firstOrNull()?.id)
    }
    val selectedChild = state.children.firstOrNull { it.id == selectedChildId } ?: state.children.firstOrNull()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showChildDialog by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ChildEntity?>(null) }
    var showHwDialog by remember { mutableStateOf(false) }
    var editingHw by remember { mutableStateOf<HomeworkExamEntity?>(null) }

    // Lesson Tutor state
    var tutorSubject by remember { mutableStateOf("") }
    var tutorContent by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    val selectedQuizAnswers = remember(tutorResult) { mutableStateMapOf<Int, Int>() }
    var quizChecked by remember(tutorResult) { mutableStateOf(false) }

    // Zero-permission Photo Picker (Play Policy compliant)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        attachedImageUri = uri
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedChild != null && selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showHwDialog = true },
                    containerColor = TurquoisePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("edu_add_hw_fab"),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = s.addHomework)
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("edu_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(MaterialTheme.shapes.large),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_education_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NavyDeep.copy(alpha = 0.88f), NavyDeep.copy(alpha = 0.45f))
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "مساعد تعليم الأبناء" else "Children's Education Assistant",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = if (isAr) "متابعة الواجبات، الامتحانات، الجداول، والاختبارات الذكية"
                                else "Homework, exams, study schedules, and smart quizzes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                    }
                }
            }

            // Child Profiles selector
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isAr) "ملفات الأبناء" else "Children Profiles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedButton(
                            onClick = { showChildDialog = true },
                            modifier = Modifier.testTag("add_child_btn"),
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAr) "إضافة طفل" else "Add Child")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (state.children.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Outlined.School,
                            title = if (isAr) "لا يوجد ملفات أبناء بعد" else "No child profiles yet",
                            subtitle = if (isAr) "أضف ملف ابنك بالصف الدراسي والمواد لبدء المتابعة" else "Create a child profile with grade and subjects",
                            actionLabel = if (isAr) "إنشاء ملف طفل" else "Create Child Profile",
                            onAction = { showChildDialog = true },
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.children.forEach { child ->
                                FilterChip(
                                    selected = selectedChild?.id == child.id,
                                    onClick = { selectedChildId = child.id },
                                    label = { Text("${child.name} (${child.gradeStage})") },
                                )
                            }
                        }
                    }
                }
            }

            if (selectedChild != null) {
                val childItems = state.homeworkExams.filter { it.childId == selectedChild.id }

                item {
                    HayatiCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedChild.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${selectedChild.gradeStage} • ${selectedChild.ageYears} " + (if (isAr) "سنوات" else "yrs"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TurquoisePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val subjectsList = selectedChild.subjectsCsv
                                    .split(',', '،')
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                if (subjectsList.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        subjectsList.forEach { subj ->
                                            StatusBadge(
                                                text = subj,
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        }
                                    }
                                }
                                if (selectedChild.weeklySchedule.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = (if (isAr) "جدول المذاكرة: " else "Schedule: ") + selectedChild.weeklySchedule,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { editingChild = selectedChild }) {
                                Icon(Icons.Outlined.Edit, contentDescription = s.edit, tint = TurquoisePrimary)
                            }
                            IconButton(onClick = { viewModel.deleteChild(selectedChild) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError)
                            }
                        }
                    }
                }

                item {
                    val tabs = if (isAr) listOf("الواجبات والامتحانات", "المعلم والاختبار", "تقرير التقدم")
                    else listOf("Homework & Exams", "Tutor & Quiz", "Progress Report")
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { idx, label ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (childItems.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.School,
                                    title = if (isAr) "لا توجد واجبات أو امتحانات" else "No homework or exams",
                                    subtitle = if (isAr) "أضف الواجب أو الامتحان القادم لـ ${selectedChild.name}" else "Add homework or exam for ${selectedChild.name}",
                                    actionLabel = s.addHomework,
                                    onAction = { showHwDialog = true },
                                )
                            }
                        } else {
                            items(childItems, key = { it.id }) { hw ->
                                val isExam = hw.type == EducationItemType.EXAM.name
                                val accentColor = when {
                                    hw.isCompleted -> StatusSuccess
                                    isExam -> StatusError
                                    else -> GoldAccent
                                }
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(42.dp)
                                                .clip(CircleShape)
                                                .background(accentColor),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Checkbox(
                                            checked = hw.isCompleted,
                                            onCheckedChange = { viewModel.toggleHomeworkCompleted(hw) },
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = hw.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (hw.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            )
                                            Text(
                                                text = "${hw.subject} • ${DateTimeUtils.formatDate(hw.dueDateMillis, isAr, tz)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (hw.scoreEarned != null) {
                                                Text(
                                                    text = (if (isAr) "الدرجة: " else "Score: ") + "${hw.scoreEarned.toInt()} / ${hw.maxScore.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StatusSuccess,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                        StatusBadge(
                                            text = if (isExam) (if (isAr) "امتحان" else "Exam") else (if (isAr) "واجب" else "HW"),
                                            containerColor = if (isExam) StatusErrorSoft else StatusWarningSoft,
                                            contentColor = if (isExam) StatusError else StatusWarning,
                                        )
                                        IconButton(onClick = { editingHw = hw }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = s.edit, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteHomeworkExam(hw) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            HayatiCard {
                                AiPreviewBanner()
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (isAr) "اشرح درساً وولّد أسئلة مناسبة لعمر ${selectedChild.name}"
                                    else "Explain a lesson & generate an age-appropriate quiz for ${selectedChild.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = tutorSubject,
                                    onValueChange = { tutorSubject = it },
                                    label = { Text(if (isAr) "المادة (مثال: العلوم)" else "Subject (e.g., Science)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = tutorContent,
                                    onValueChange = { tutorContent = it },
                                    label = { Text(if (isAr) "عنوان الدرس أو ملخصه *" else "Lesson Topic or Text *") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (attachedImageUri != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = TurquoisePrimary.copy(alpha = 0.12f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Outlined.Image, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (isAr) "تم إرفاق صورة الدرس" else "Lesson image attached",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TurquoisePrimary,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            IconButton(
                                                onClick = { attachedImageUri = null },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Close,
                                                    contentDescription = if (isAr) "إزالة الصورة" else "Remove image",
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                    ) {
                                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (attachedImageUri != null) (if (isAr) "تغيير الصورة" else "Change Image")
                                            else (if (isAr) "إرفاق صورة درس" else "Attach Lesson Image")
                                        )
                                    }
                                    Button(
                                        enabled = tutorContent.isNotBlank(),
                                        onClick = {
                                            viewModel.runLessonTutor(
                                                child = selectedChild,
                                                subject = tutorSubject.ifBlank { if (isAr) "مادة عامة" else "General" },
                                                topicOrContent = tutorContent,
                                                hasAttachedImage = attachedImageUri != null,
                                            )
                                        },
                                    ) {
                                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isAr) "شرح + اختبار" else "Explain & Quiz")
                                    }
                                }
                            }
                        }

                        if (tutorResult != null) {
                            item {
                                HayatiCard {
                                    if (tutorResult.imageNotice != null) {
                                        StatusBadge(
                                            text = if (isAr) "تنبيه معالجة الملف" else "File Processing Notice",
                                            containerColor = StatusWarningSoft,
                                            contentColor = StatusWarning,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(tutorResult.imageNotice, style = MaterialTheme.typography.bodySmall, color = StatusWarning)
                                        Spacer(Modifier.height(10.dp))
                                    }
                                    Text(tutorResult.explanation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            item {
                                val correctCount = tutorResult.questions.count { q ->
                                    selectedQuizAnswers[q.id] == q.correctIndex
                                }
                                HayatiCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (isAr) "اختبار قصير (اختيار من متعدد وصح/خطأ)" else "Short Quiz (MCQ & True/False)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (quizChecked) {
                                            val totalQ = tutorResult.questions.size
                                            StatusBadge(
                                                text = if (isAr) "النتيجة: $correctCount / $totalQ" else "Score: $correctCount / $totalQ",
                                                containerColor = if (correctCount * 2 >= totalQ) StatusSuccessSoft else StatusWarningSoft,
                                                contentColor = if (correctCount * 2 >= totalQ) StatusSuccess else StatusWarning,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    tutorResult.questions.forEach { q ->
                                        Text("${q.id}. ${q.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        q.options.forEachIndexed { optIdx, optText ->
                                            val chosen = selectedQuizAnswers[q.id] == optIdx
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedQuizAnswers[q.id] = optIdx },
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RadioButton(selected = chosen, onClick = { selectedQuizAnswers[q.id] = optIdx })
                                                Text(optText, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        if (quizChecked) {
                                            val correct = selectedQuizAnswers[q.id] == q.correctIndex
                                            Text(
                                                text = (if (correct) "✅ " else "❌ ") + q.explanation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (correct) StatusSuccess else StatusError,
                                                modifier = Modifier.padding(bottom = 8.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { quizChecked = true }) {
                                        Text(if (isAr) "تصحيح الإجابات" else "Check Answers")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        val completedCount = childItems.count { it.isCompleted }
                        val ratio = if (childItems.isNotEmpty()) completedCount.toFloat() / childItems.size else 0f
                        val scoredItems = childItems.filter { it.scoreEarned != null }
                        val avgScore = if (scoredItems.isNotEmpty()) scoredItems.mapNotNull { it.scoreEarned }.average() else null
                        item {
                            HayatiCard {
                                Text(
                                    text = if (isAr) "تقرير تقدم ${selectedChild.name}" else "${selectedChild.name}'s Progress Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (isAr) "إنجاز الواجبات والامتحانات: $completedCount من ${childItems.size}"
                                    else "Completed items: $completedCount of ${childItems.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = TurquoisePrimary,
                                )
                                Spacer(Modifier.height(10.dp))
                                if (avgScore != null) {
                                    Text(
                                        text = if (isAr) "متوسط الدرجات المسجلة: ${avgScore.toInt()}%" else "Recorded Average Score: ${avgScore.toInt()}%",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                val bySubject = scoredItems.groupBy { it.subject }
                                bySubject.forEach { (subj, list) ->
                                    val subjAvg = list.mapNotNull { it.scoreEarned }.average().toInt()
                                    val tag = if (subjAvg >= 85) (if (isAr) "نقطة قوة" else "Strength") else (if (isAr) "يحتاج مراجعة" else "Needs Review")
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("$subj ($subjAvg%)", style = MaterialTheme.typography.bodyMedium)
                                        StatusBadge(
                                            text = tag,
                                            containerColor = if (subjAvg >= 85) StatusSuccessSoft else StatusWarningSoft,
                                            contentColor = if (subjAvg >= 85) StatusSuccess else StatusWarning,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showChildDialog || editingChild != null) {
        ChildProfileFormDialog(
            initial = editingChild,
            isArabic = isAr,
            onDismiss = {
                showChildDialog = false
                editingChild = null
            },
            onSave = {
                viewModel.saveChild(it)
                showChildDialog = false
                editingChild = null
            },
        )
    }

    if (showHwDialog) {
        HomeworkExamFormDialog(
            children = state.children,
            defaultChildId = selectedChild?.id,
            isArabic = isAr,
            tzId = tz,
            onDismiss = { showHwDialog = false },
            onSave = {
                viewModel.saveHomeworkExam(it)
                showHwDialog = false
            },
        )
    }

    editingHw?.let { hw ->
        HomeworkExamFormDialog(
            children = state.children,
            initial = hw,
            isArabic = isAr,
            tzId = tz,
            onDismiss = { editingHw = null },
            onSave = {
                viewModel.saveHomeworkExam(it)
                editingHw = null
            },
        )
    }
}

@Composable
private fun ChildProfileFormDialog(
    initial: ChildEntity?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (ChildEntity) -> Unit,
) {
    val s = S()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var grade by remember { mutableStateOf(initial?.gradeStage ?: "") }
    var ageText by remember { mutableStateOf(initial?.ageYears?.toString() ?: "10") }
    var subjects by remember { mutableStateOf(initial?.subjectsCsv ?: "") }
    var schedule by remember { mutableStateOf(initial?.weeklySchedule ?: "") }
    var err by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
        title = {
            Text(
                if (initial == null) (if (isArabic) "إضافة ملف طفل" else "Add Child Profile")
                else (if (isArabic) "تعديل ملف الطفل" else "Edit Child Profile"),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it; err = null }, label = { Text(if (isArabic) "اسم الطفل *" else "Child Name *") }, modifier = Modifier.fillMaxWidth().testTag("child_name_input"))
                    OutlinedTextField(value = grade, onValueChange = { grade = it; err = null }, label = { Text(if (isArabic) "الصف / المرحلة الدراسية *" else "Grade / Stage *") }, modifier = Modifier.fillMaxWidth().testTag("child_grade_input"))
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it; err = null },
                        label = { Text(if (isArabic) "العمر" else "Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("child_age_input"),
                    )
                    OutlinedTextField(value = subjects, onValueChange = { subjects = it }, label = { Text(if (isArabic) "المواد الدراسية (مفصولة بفاصلة)" else "Subjects (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text(if (isArabic) "جدول المذاكرة الأسبوعي" else "Weekly Study Schedule") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitted,
                onClick = {
                    val parsedAge = ageText.trim().toIntOrNull()
                    when {
                        name.isBlank() || grade.isBlank() -> {
                            err = if (isArabic) "أدخل الاسم والصف الدراسي" else "Enter name and grade"
                        }
                        parsedAge == null || parsedAge !in 2..25 -> {
                            err = if (isArabic) "أدخل عمراً صحيحاً بين 2 و 25" else "Enter valid age (2 - 25)"
                        }
                        else -> {
                            submitted = true
                            onSave(
                                (initial ?: ChildEntity(name = name.trim(), gradeStage = grade.trim())).copy(
                                    name = name.trim(),
                                    gradeStage = grade.trim(),
                                    ageYears = parsedAge,
                                    subjectsCsv = subjects.trim(),
                                    weeklySchedule = schedule.trim(),
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_child_btn"),
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
