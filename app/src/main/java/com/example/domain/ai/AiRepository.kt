package com.example.domain.ai

import com.example.data.local.BillCategory
import com.example.data.local.BillEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CalendarEventType
import com.example.data.local.ChildEntity
import com.example.data.local.EducationItemType
import com.example.data.local.FinanceCategory
import com.example.data.local.FinanceSummary
import com.example.data.local.HomeworkExamEntity
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.Locale

data class AppContextSnapshot(
    val tasks: List<TaskEntity>,
    val transactions: List<TransactionEntity>,
    val financeSummary: FinanceSummary,
    val bills: List<BillEntity>,
    val children: List<ChildEntity>,
    val homeworkExams: List<HomeworkExamEntity>,
    val calendarEvents: List<CalendarEventEntity>,
    val currency: String,
    val timeZoneId: String,
    val isArabic: Boolean,
)

sealed interface PendingAiAction {
    val confirmationSummary: String

    data class AddExpense(
        val amount: Double,
        val category: FinanceCategory,
        val description: String,
        val dateMillis: Long,
        override val confirmationSummary: String,
    ) : PendingAiAction

    data class AddIncome(
        val amount: Double,
        val category: FinanceCategory,
        val description: String,
        val dateMillis: Long,
        override val confirmationSummary: String,
    ) : PendingAiAction

    data class AddBill(
        val title: String,
        val category: BillCategory,
        val amount: Double,
        val dueDateMillis: Long,
        override val confirmationSummary: String,
    ) : PendingAiAction

    data class AddTask(
        val title: String,
        val priority: TaskPriority,
        val category: TaskCategory,
        val dueDateMillis: Long,
        override val confirmationSummary: String,
    ) : PendingAiAction

    data class AddExamOrHomework(
        val childId: Long,
        val childName: String,
        val type: EducationItemType,
        val subject: String,
        val title: String,
        val dueDateMillis: Long,
        override val confirmationSummary: String,
    ) : PendingAiAction
}

data class AiAssistantReply(
    val message: String,
    val pendingAction: PendingAiAction? = null,
    val isSamplePreview: Boolean = true,
)

data class DailyPlanSlot(
    val id: String,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val title: String,
    val sourceLabel: String,
    val isUrgentOrOverdue: Boolean = false,
    val isLockedEvent: Boolean = false,
)

data class QuizQuestion(
    val id: Int,
    val isTrueFalse: Boolean,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class LessonTutorResult(
    val explanation: String,
    val questions: List<QuizQuestion>,
    val imageNotice: String? = null,
    val isSamplePreview: Boolean = true,
)

interface AiRepository {
    suspend fun processAssistantMessage(userText: String, snapshot: AppContextSnapshot): AiAssistantReply
    suspend fun generateDailyPlan(snapshot: AppContextSnapshot, targetDayMillis: Long): List<DailyPlanSlot>
    suspend fun summarizeWorkTasks(tasks: List<TaskEntity>, isArabic: Boolean, tzId: String): String
    suspend fun generateSavingSuggestions(
        transactions: List<TransactionEntity>,
        summary: FinanceSummary,
        currency: String,
        isArabic: Boolean,
    ): List<String>

    suspend fun explainLessonAndGenerateQuiz(
        child: ChildEntity,
        subject: String,
        lessonTopicOrContent: String,
        hasAttachedImage: Boolean,
        isArabic: Boolean,
    ): LessonTutorResult
}

/**
 * Local preview implementation used when Firebase AI Logic is not yet enabled.
 * Parses Egyptian Arabic, MSA, and English commands against the real Room data store,
 * never invents financial numbers or unseen document contents, and marks outputs clearly as preview.
 */
class LocalPreviewAiRepository : AiRepository {

    override suspend fun processAssistantMessage(
        userText: String,
        snapshot: AppContextSnapshot,
    ): AiAssistantReply {
        val text = userText.trim()
        val lower = text.lowercase(Locale.getDefault())
        val isAr = snapshot.isArabic
        val now = System.currentTimeMillis()
        val tz = snapshot.timeZoneId

        // 1. Query: Overdue tasks
        if (lower.contains("متأخر") || lower.contains("overdue")) {
            val overdue = snapshot.tasks.filter {
                DateTimeUtils.isOverdue(it.dueDateMillis, it.status == TaskStatus.COMPLETED.name, now, tz)
            }
            val msg = if (overdue.isEmpty()) {
                if (isAr) "ممتاز! لا توجد لديك أي مهام متأخرة حالياً." else "Great! You have no overdue tasks right now."
            } else {
                val list = overdue.joinToString("\n") {
                    "• ${it.title} (${DateTimeUtils.formatShortDate(it.dueDateMillis, isAr, tz)})"
                }
                if (isAr) "لديك ${overdue.size} مهام متأخرة من سجلاتك الفعلية:\n$list"
                else "You have ${overdue.size} overdue task(s) from your records:\n$list"
            }
            return AiAssistantReply(msg)
        }

        // 2. Query: Spending this month
        if (lower.contains("كم صرفت") || lower.contains("صرفت كام") || lower.contains("how much did i spend")) {
            val categoryMatch = detectExpenseCategory(lower)
            val msg = if (lower.contains("البيت") || lower.contains("home")) {
                val homeAmt = snapshot.financeSummary.categoryTotals[FinanceCategory.HOME] ?: 0.0
                val shoppingAmt = snapshot.financeSummary.categoryTotals[FinanceCategory.SHOPPING] ?: 0.0
                val formattedHome = DateTimeUtils.formatCurrency(homeAmt, snapshot.currency, isAr)
                val formattedShop = DateTimeUtils.formatCurrency(shoppingAmt, snapshot.currency, isAr)
                if (isAr) {
                    "من سجلاتك الفعلية هذا الشهر:\n• بند البيت: $formattedHome\n• بند التسوق: $formattedShop\n• إجمالي المصروفات: ${DateTimeUtils.formatCurrency(snapshot.financeSummary.totalExpenses, snapshot.currency, true)}"
                } else {
                    "From your actual records this month:\n• Home: $formattedHome\n• Shopping: $formattedShop\n• Total expenses: ${DateTimeUtils.formatCurrency(snapshot.financeSummary.totalExpenses, snapshot.currency, false)}"
                }
            } else if (categoryMatch != FinanceCategory.OTHER_EXPENSE) {
                val amt = snapshot.financeSummary.categoryTotals[categoryMatch] ?: 0.0
                val formatted = DateTimeUtils.formatCurrency(amt, snapshot.currency, isAr)
                if (isAr) "إجمالي مصروفات بند (${categoryMatch.label(true)}) هذا الشهر هو: $formatted"
                else "Total ${categoryMatch.label(false)} spending this month: $formatted"
            } else {
                val totalExp = DateTimeUtils.formatCurrency(snapshot.financeSummary.totalExpenses, snapshot.currency, isAr)
                val rem = DateTimeUtils.formatCurrency(snapshot.financeSummary.remainingBalance, snapshot.currency, isAr)
                if (isAr) "إجمالي مصروفاتك المسجلة هذا الشهر: $totalExp، والرصيد المتبقي: $rem."
                else "Total recorded expenses this month: $totalExp. Remaining balance: $rem."
            }
            return AiAssistantReply(msg)
        }

        // 3. Command: Plan tomorrow / today
        if (lower.contains("خطط") || lower.contains("رتب يوم") || lower.contains("plan")) {
            val targetDay = if (lower.contains("غد") || lower.contains("بكرة") || lower.contains("tomorrow")) {
                DateTimeUtils.addDays(DateTimeUtils.startOfDayMillis(now, tz), 1, tz)
            } else {
                DateTimeUtils.startOfDayMillis(now, tz)
            }
            val slots = generateDailyPlan(snapshot, targetDay)
            val lines = slots.joinToString("\n") {
                val timeStr = String.format(Locale.US, "%02d:%02d", it.startHour, it.startMinute)
                "• $timeStr — ${it.title} (${it.sourceLabel})"
            }
            val header = if (isAr) {
                "إليك خطة مقترحة ليوم ${DateTimeUtils.formatShortDate(targetDay, true, tz)} بناءً على مهامك ومواعيدك الفعلية (دون اختلاق مواعيد):\n"
            } else {
                "Suggested plan for ${DateTimeUtils.formatShortDate(targetDay, false, tz)} based on your real commitments:\n"
            }
            return AiAssistantReply(header + lines + if (isAr) "\n\nيمكنك تعديل الخطة أو اعتمادها من زر \"خطط لي يومي\" بالرئيسية." else "\n\nYou can edit or apply this plan from the Dashboard planner button.")
        }

        // 4. Command: Study schedule for children
        if (lower.contains("جدول مذاكرة") || lower.contains("study schedule")) {
            if (snapshot.children.isEmpty()) {
                return AiAssistantReply(
                    if (isAr) "لا يوجد ملفات أبناء مسجلة حالياً. أضف ملف ابنك أولاً في قسم التعليم أو أخبرني باسمه وصفه الدراسي."
                    else "No child profiles found yet. Please add a child profile in the Education section first."
                )
            }
            val sb = StringBuilder()
            sb.append(if (isAr) "جدول مذاكرة أسبوعي مقترح للأبناء:\n" else "Suggested weekly study schedule:\n")
            snapshot.children.forEach { child ->
                val childItems = snapshot.homeworkExams.filter { it.childId == child.id && !it.isCompleted }
                val subjects = child.subjectsCsv.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() }
                sb.append("\n👤 ${child.name} (${child.gradeStage}):\n")
                if (childItems.isNotEmpty()) {
                    sb.append(if (isAr) "  أولوية عاجلة:\n" else "  Priority items:\n")
                    childItems.take(3).forEach { hw ->
                        sb.append("  • ${hw.subject}: ${hw.title} (${DateTimeUtils.formatShortDate(hw.dueDateMillis, isAr, tz)})\n")
                    }
                }
                if (subjects.isNotEmpty()) {
                    val days = if (isAr) listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
                    else listOf("Sun", "Mon", "Tue", "Wed", "Thu")
                    days.forEachIndexed { idx, day ->
                        val subj = subjects[idx % subjects.size]
                        sb.append("  - $day: $subj (45 دقيقة)\n")
                    }
                }
            }
            return AiAssistantReply(sb.toString().trim())
        }

        // 5. Command: Record Expense
        if (lower.contains("مصروف") || lower.contains("صرفت") || lower.contains("دفعت") || lower.contains("expense")) {
            val amount = extractNumber(text)
            if (amount == null || amount <= 0.0) {
                return AiAssistantReply(
                    if (isAr) "كم قيمة المصروف التي تريد تسجيلها؟ (مثال: سجل مصروف 150 جنيه مشتريات للبيت)"
                    else "What is the expense amount you want to record? (e.g., Add expense 150 EGP groceries)"
                )
            }
            val category = detectExpenseCategory(lower)
            val desc = text
                .replace(Regex("[0-9]+(\\.[0-9]+)?"), "")
                .replace(Regex("(سجل|أضف|اضف|مصروف|صرفت|دفعت|جنيه|جنيهاً|egp)", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { category.label(isAr) }
            val formattedAmt = DateTimeUtils.formatCurrency(amount, snapshot.currency, isAr)
            val summary = if (isAr) {
                "تسجيل مصروف: $formattedAmt\nالفئة: ${category.label(true)}\nالوصف: $desc"
            } else {
                "Record Expense: $formattedAmt\nCategory: ${category.label(false)}\nDescription: $desc"
            }
            return AiAssistantReply(
                message = if (isAr) "جهزت لك المصروف التالي. يرجى التأكيد قبل الحفظ:" else "Prepared this expense. Please confirm before saving:",
                pendingAction = PendingAiAction.AddExpense(amount, category, desc, now, summary),
            )
        }

        // 6. Command: Record Income
        if (lower.contains("دخل") || lower.contains("راتب") || lower.contains("مرتب") || lower.contains("income")) {
            val amount = extractNumber(text)
            if (amount == null || amount <= 0.0) {
                return AiAssistantReply(
                    if (isAr) "ما قيمة الدخل المطلوب تسجيله؟" else "What is the income amount to record?"
                )
            }
            val cat = when {
                lower.contains("راتب") || lower.contains("مرتب") || lower.contains("salary") -> FinanceCategory.SALARY
                lower.contains("تجارة") || lower.contains("عمل") || lower.contains("business") -> FinanceCategory.BUSINESS
                lower.contains("استثمار") || lower.contains("investment") -> FinanceCategory.INVESTMENT
                else -> FinanceCategory.OTHER_INCOME
            }
            val desc = text.ifBlank { cat.label(isAr) }
            val formattedAmt = DateTimeUtils.formatCurrency(amount, snapshot.currency, isAr)
            val summary = if (isAr) "تسجيل دخل: $formattedAmt (${cat.label(true)})" else "Record Income: $formattedAmt (${cat.label(false)})"
            return AiAssistantReply(
                message = if (isAr) "هل أؤكد حفظ الدخل التالي؟" else "Confirm saving this income?",
                pendingAction = PendingAiAction.AddIncome(amount, cat, desc, now, summary),
            )
        }

        // 7. Command: Bill Reminder ("فكرني بدفع فاتورة الإنترنت يوم 20")
        if (lower.contains("فاتورة") || lower.contains("فكرني") || lower.contains("تذكير") || lower.contains("bill")) {
            val billCat = when {
                lower.contains("إنترنت") || lower.contains("انترنت") || lower.contains("نت") || lower.contains("internet") -> BillCategory.INTERNET
                lower.contains("كهرباء") || lower.contains("electricity") -> BillCategory.ELECTRICITY
                lower.contains("مياه") || lower.contains("ماء") || lower.contains("water") -> BillCategory.WATER
                lower.contains("غاز") || lower.contains("gas") -> BillCategory.GAS
                lower.contains("هاتف") || lower.contains("موبايل") || lower.contains("تليفون") || lower.contains("phone") -> BillCategory.PHONE
                else -> BillCategory.OTHER
            }
            val dayOfMonth = extractDayOfMonth(lower)
            val amount = extractNumber(lower.replace(Regex("يوم\\s*\\d+"), "")) ?: 0.0
            val cal = Calendar.getInstance(DateTimeUtils.timeZone(tz))
            if (dayOfMonth != null && dayOfMonth in 1..31) {
                if (dayOfMonth < cal.get(Calendar.DAY_OF_MONTH)) {
                    cal.add(Calendar.MONTH, 1)
                }
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            } else {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val dueMillis = DateTimeUtils.startOfDayMillis(cal.timeInMillis, tz)
            val title = if (isAr) "فاتورة ${billCat.label(true)}" else "${billCat.label(false)} Bill"
            val summary = if (isAr) {
                "إضافة فاتورة وتذكير: $title\nالموعد: ${DateTimeUtils.formatShortDate(dueMillis, true, tz)}" +
                    if (amount > 0) "\nالقيمة: ${DateTimeUtils.formatCurrency(amount, snapshot.currency, true)}" else "\n(يمكنك تعديل القيمة لاحقاً)"
            } else {
                "Add Bill & Reminder: $title\nDue: ${DateTimeUtils.formatShortDate(dueMillis, false, tz)}"
            }
            return AiAssistantReply(
                message = if (isAr) "تم إعداد الفاتورة والتذكير. هل تؤكد الحفظ؟" else "Bill and reminder prepared. Confirm to save?",
                pendingAction = PendingAiAction.AddBill(title, billCat, amount, dueMillis, summary),
            )
        }

        // 8. Command: Add Exam or Homework ("أضف امتحان علوم يوم الأحد")
        if (lower.contains("امتحان") || lower.contains("اختبار") || lower.contains("واجب") || lower.contains("exam") || lower.contains("homework")) {
            if (snapshot.children.isEmpty()) {
                return AiAssistantReply(
                    if (isAr) "يرجى إضافة ملف الطفل أولاً من قسم التعليم لكي أربط الامتحان أو الواجب به."
                    else "Please create a child profile in the Education section first so I can attach this item."
                )
            }
            val matchedChild = snapshot.children.firstOrNull { lower.contains(it.name.lowercase(Locale.getDefault())) }
                ?: if (snapshot.children.size == 1) snapshot.children.first() else null
            if (matchedChild == null) {
                val names = snapshot.children.joinToString("، ") { it.name }
                return AiAssistantReply(
                    if (isAr) "لأي من أبنائك تريد إضافة هذا الموعد؟ ($names)"
                    else "Which child is this for? ($names)"
                )
            }
            val isExam = lower.contains("امتحان") || lower.contains("اختبار") || lower.contains("exam")
            val itemType = if (isExam) EducationItemType.EXAM else EducationItemType.HOMEWORK
            val subject = detectSubject(lower, isAr)
            val dueMillis = parseRelativeDayMillis(lower, now, tz)
            val title = if (isAr) "${itemType.label(true)} $subject" else "$subject ${itemType.label(false)}"
            val summary = if (isAr) {
                "إضافة ${itemType.label(true)} لـ ${matchedChild.name}\nالمادة: $subject\nالموعد: ${DateTimeUtils.formatShortDate(dueMillis, true, tz)}"
            } else {
                "Add ${itemType.label(false)} for ${matchedChild.name}\nSubject: $subject\nDate: ${DateTimeUtils.formatShortDate(dueMillis, false, tz)}"
            }
            return AiAssistantReply(
                message = if (isAr) "يرجى تأكيد إضافة سجل التعليم التالي:" else "Confirm saving this education record:",
                pendingAction = PendingAiAction.AddExamOrHomework(
                    childId = matchedChild.id,
                    childName = matchedChild.name,
                    type = itemType,
                    subject = subject,
                    title = title,
                    dueDateMillis = dueMillis,
                    confirmationSummary = summary,
                ),
            )
        }

        // 9. Command: Add Task
        if (lower.contains("مهمة") || lower.contains("task")) {
            val cleanTitle = text
                .replace(Regex("(أضف|اضف|سجل|مهمة|جديدة|عاجلة|task|add)", RegexOption.IGNORE_CASE), "")
                .trim()
            if (cleanTitle.isBlank()) {
                return AiAssistantReply(
                    if (isAr) "ما عنوان المهمة التي تريد إضافتها؟"
                    else "What is the title of the task you want to add?"
                )
            }
            val priority = if (lower.contains("عاجل") || lower.contains("مهم") || lower.contains("urgent")) TaskPriority.HIGH else TaskPriority.MEDIUM
            val dueMillis = parseRelativeDayMillis(lower, now, tz)
            val summary = if (isAr) {
                "إضافة مهمة: $cleanTitle\nالأولوية: ${priority.label(true)}\nالموعد: ${DateTimeUtils.formatShortDate(dueMillis, true, tz)}"
            } else {
                "Add Task: $cleanTitle\nPriority: ${priority.label(false)}\nDue: ${DateTimeUtils.formatShortDate(dueMillis, false, tz)}"
            }
            return AiAssistantReply(
                message = if (isAr) "هل أؤكد إضافة هذه المهمة؟" else "Confirm adding this task?",
                pendingAction = PendingAiAction.AddTask(cleanTitle, priority, TaskCategory.WORK, dueMillis, summary),
            )
        }

        // Fallback helpful response using real counts
        val pendingCount = snapshot.tasks.count { it.status != TaskStatus.COMPLETED.name }
        val unpaidBills = snapshot.bills.count { !it.isPaid }
        return AiAssistantReply(
            if (isAr) {
                "أهلاً بك في مساعد حياتي AI! لديك حالياً $pendingCount مهام غير مكتملة و$unpaidBills فواتير غير مدفوعة.\n" +
                    "جرّب أن تقول لي:\n• \"سجل مصروف 150 جنيه مشتريات للبيت\"\n• \"فكرني بدفع فاتورة الإنترنت يوم 20\"\n• \"كم صرفت على البيت هذا الشهر؟\"\n• \"ما المهام المتأخرة عندي؟\"\n• \"خطط لي يوم الغد\""
            } else {
                "Welcome to HAYATI AI! You currently have $pendingCount open tasks and $unpaidBills unpaid bills.\n" +
                    "Try commands like: \"Add expense 150 EGP groceries\", \"What are my overdue tasks?\", or \"Plan tomorrow\"."
            }
        )
    }

    override suspend fun generateDailyPlan(
        snapshot: AppContextSnapshot,
        targetDayMillis: Long,
    ): List<DailyPlanSlot> {
        val isAr = snapshot.isArabic
        val tz = snapshot.timeZoneId
        val slots = mutableListOf<DailyPlanSlot>()
        val occupiedHours = mutableSetOf<Int>()

        // 1. Lock existing confirmed calendar events on targetDayMillis (never double-book)
        snapshot.calendarEvents
            .filter { DateTimeUtils.isSameDay(it.startMillis, targetDayMillis, tz) }
            .forEach { ev ->
                val cal = Calendar.getInstance(DateTimeUtils.timeZone(tz)).apply { timeInMillis = ev.startMillis }
                val hr = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                occupiedHours.add(hr)
                slots.add(
                    DailyPlanSlot(
                        id = "cal_${ev.id}",
                        startHour = hr,
                        startMinute = min,
                        durationMinutes = 60,
                        title = ev.title,
                        sourceLabel = if (isAr) "موعد مؤكد" else "Confirmed Event",
                        isLockedEvent = true,
                    )
                )
            }

        fun nextFreeHour(preferredStart: Int): Int {
            var h = preferredStart
            while (occupiedHours.contains(h) && h < 22) h++
            occupiedHours.add(h)
            return h
        }

        // 2. Overdue & Today tasks sorted by priority
        val now = System.currentTimeMillis()
        val pendingTasks = snapshot.tasks
            .filter { it.status != TaskStatus.COMPLETED.name }
            .filter {
                DateTimeUtils.isOverdue(it.dueDateMillis, false, now, tz) ||
                    DateTimeUtils.isSameDay(it.dueDateMillis, targetDayMillis, tz)
            }
            .sortedBy {
                val overdueBoost = if (DateTimeUtils.isOverdue(it.dueDateMillis, false, now, tz)) 0 else 10
                val pWeight = runCatching { TaskPriority.valueOf(it.priority).sortWeight }.getOrElse { 1 }
                overdueBoost + pWeight
            }

        pendingTasks.take(5).forEach { task ->
            val isOverdue = DateTimeUtils.isOverdue(task.dueDateMillis, false, now, tz)
            val hr = nextFreeHour(9)
            slots.add(
                DailyPlanSlot(
                    id = "task_${task.id}",
                    startHour = hr,
                    startMinute = 0,
                    durationMinutes = 60,
                    title = task.title,
                    sourceLabel = if (isOverdue) {
                        if (isAr) "مهمة متأخرة • أولوية" else "Overdue Task"
                    } else {
                        if (isAr) "مهمة اليوم" else "Today's Task"
                    },
                    isUrgentOrOverdue = isOverdue || task.priority == TaskPriority.HIGH.name,
                )
            )
        }

        // 3. Due bills on target day
        snapshot.bills
            .filter { !it.isPaid && DateTimeUtils.isSameDay(it.dueDateMillis, targetDayMillis, tz) }
            .forEach { bill ->
                val hr = nextFreeHour(13)
                slots.add(
                    DailyPlanSlot(
                        id = "bill_${bill.id}",
                        startHour = hr,
                        startMinute = 0,
                        durationMinutes = 30,
                        title = if (isAr) "سداد ${bill.title}" else "Pay ${bill.title}",
                        sourceLabel = if (isAr) "فاتورة مستحقة" else "Bill Due",
                        isUrgentOrOverdue = true,
                    )
                )
            }

        // 4. Children's homework & exams due today or tomorrow
        snapshot.homeworkExams
            .filter { !it.isCompleted && DateTimeUtils.isSameDay(it.dueDateMillis, targetDayMillis, tz) }
            .forEach { hw ->
                val childName = snapshot.children.firstOrNull { it.id == hw.childId }?.name ?: ""
                val hr = nextFreeHour(17)
                slots.add(
                    DailyPlanSlot(
                        id = "hw_${hw.id}",
                        startHour = hr,
                        startMinute = 0,
                        durationMinutes = 45,
                        title = "${hw.subject}: ${hw.title}" + if (childName.isNotBlank()) " ($childName)" else "",
                        sourceLabel = if (hw.type == EducationItemType.EXAM.name) {
                            if (isAr) "مراجعة امتحان" else "Exam Prep"
                        } else {
                            if (isAr) "متابعة واجب" else "Homework"
                        },
                        isUrgentOrOverdue = hw.type == EducationItemType.EXAM.name,
                    )
                )
            }

        if (slots.isEmpty()) {
            slots.add(
                DailyPlanSlot(
                    id = "empty_tip",
                    startHour = 9,
                    startMinute = 0,
                    durationMinutes = 30,
                    title = if (isAr) "لا توجد التزامات مسجلة لهذا اليوم — أضف مهامك أو مواعيدك أولاً"
                    else "No commitments recorded for this day — add tasks or events first",
                    sourceLabel = if (isAr) "تنبيه التخطيط" else "Planner Note",
                )
            )
        }

        return slots.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    override suspend fun summarizeWorkTasks(
        tasks: List<TaskEntity>,
        isArabic: Boolean,
        tzId: String,
    ): String {
        val now = System.currentTimeMillis()
        val open = tasks.filter { it.status != TaskStatus.COMPLETED.name }
        val overdue = open.filter { DateTimeUtils.isOverdue(it.dueDateMillis, false, now, tzId) }
        val high = open.filter { it.priority == TaskPriority.HIGH.name }
        if (open.isEmpty()) {
            return if (isArabic) "أحسنت! جميع المهام مكتملة ولا توجد أعمال معلقة." else "All tasks are completed!"
        }
        val topOrdered = open.sortedWith(
            compareBy<TaskEntity>(
                { if (DateTimeUtils.isOverdue(it.dueDateMillis, false, now, tzId)) 0 else 1 },
                { runCatching { TaskPriority.valueOf(it.priority).sortWeight }.getOrElse { 1 } },
                { it.dueDateMillis },
            )
        ).take(3)

        return if (isArabic) {
            "ملخص المهام المعلقة (${open.size} مهمة):\n" +
                "• متأخرة: ${overdue.size} | أولوية عالية: ${high.size}\n\n" +
                "الترتيب المقترح للبدء:\n" +
                topOrdered.mapIndexed { idx, t -> "${idx + 1}. ${t.title}" }.joinToString("\n")
        } else {
            "Pending Work Summary (${open.size} open):\n" +
                "• Overdue: ${overdue.size} | High priority: ${high.size}\n\n" +
                "Suggested priority order:\n" +
                topOrdered.mapIndexed { idx, t -> "${idx + 1}. ${t.title}" }.joinToString("\n")
        }
    }

    override suspend fun generateSavingSuggestions(
        transactions: List<TransactionEntity>,
        summary: FinanceSummary,
        currency: String,
        isArabic: Boolean,
    ): List<String> {
        if (transactions.isEmpty()) {
            return listOf(
                if (isArabic) "أضف معاملاتك الفعلية (دخل ومصروفات) لتوليد نصائح ادخار مبنية على بياناتك."
                else "Record actual income and expenses to generate personalized saving insights."
            )
        }
        val tips = mutableListOf<String>()
        val topCat = summary.categoryTotals.maxByOrNull { it.value }
        if (topCat != null && summary.totalExpenses > 0.0) {
            val pct = ((topCat.value / summary.totalExpenses) * 100).toInt()
            val formatted = DateTimeUtils.formatCurrency(topCat.value, currency, isArabic)
            tips.add(
                if (isArabic) "أعلى بند إنفاق هذا الشهر هو (${topCat.key.label(true)}) بمبلغ $formatted ($pct% من إجمالي مصروفاتك). خفضه بنسبة 10% يوفر ${DateTimeUtils.formatCurrency(topCat.value * 0.1, currency, true)}."
                else "Top spending category is ${topCat.key.label(false)} at $formatted ($pct% of expenses). Reducing it by 10% saves ${DateTimeUtils.formatCurrency(topCat.value * 0.1, currency, false)}."
            )
        }
        if (summary.estimatedExpenses > 0.0) {
            tips.add(
                if (isArabic) "لديك مصروفات تقديرية بقيمة ${DateTimeUtils.formatCurrency(summary.estimatedExpenses, currency, true)}؛ تأكد من تحديثها بالقيم الفعلية فور الدفع."
                else "You have ${DateTimeUtils.formatCurrency(summary.estimatedExpenses, currency, false)} in estimated expenses; update them with actual receipts."
            )
        }
        if (summary.remainingBalance < 0) {
            tips.add(
                if (isArabic) "تنبيه: المصروفات تجاوزت الدخل المسجل بمقدار ${DateTimeUtils.formatCurrency(-summary.remainingBalance, currency, true)}."
                else "Alert: Expenses exceed recorded income by ${DateTimeUtils.formatCurrency(-summary.remainingBalance, currency, false)}."
            )
        } else if (summary.totalIncome > 0) {
            tips.add(
                if (isArabic) "نسبة الادخار الحالية من سجلاتك: ${summary.savingsRatePercent}% (${DateTimeUtils.formatCurrency(summary.remainingBalance, currency, true)} متبقية)."
                else "Current savings rate from recorded entries: ${summary.savingsRatePercent}% (${DateTimeUtils.formatCurrency(summary.remainingBalance, currency, false)} remaining)."
            )
        }
        return tips
    }

    override suspend fun explainLessonAndGenerateQuiz(
        child: ChildEntity,
        subject: String,
        lessonTopicOrContent: String,
        hasAttachedImage: Boolean,
        isArabic: Boolean,
    ): LessonTutorResult {
        val imageWarning = if (hasAttachedImage) {
            if (isArabic) {
                "ملاحظة أمانة تعليمية: تم إرفاق صورة للدرس، ولكن في وضع المعاينة المحلية لا يتم قراءة النصوص داخل الصور تلقائياً (OCR). يعتمد الشرح أدناه فقط على النص الذي أدخلته دون افتراض أو اختلاق محتوى غير مقروء من الصورة."
            } else {
                "Transparency Notice: An image was attached, but local preview mode does not parse image pixels/OCR. The explanation below uses only the text you typed and does not invent unseen image content."
            }
        } else null

        val cleanTopic = lessonTopicOrContent.trim()
        val isYoung = child.ageYears <= 11
        val explanation = if (isArabic) {
            val tone = if (isYoung) {
                "مرحباً يا ${child.name}! هيا نفهم درس ($cleanTopic) في مادة $subject بطريقة بسيطة وممتعة تناسب ${child.gradeStage}:"
            } else {
                "ملخص منظم لدرس ($cleanTopic) في مادة $subject لمرحلة ${child.gradeStage}:"
            }
            "$tone\n\n" +
                "1. الفكرة الأساسية: يركز الدرس على فهم مفهوم ($cleanTopic) وربطه بأمثلة من حياتنا اليومية.\n" +
                "2. خطوات الفهم:\n" +
                "   • اقرأ القاعدة أو التعريف الرئيسي للدرس بصوت واضح.\n" +
                "   • استخرج الكلمات المفتاحية واربط كل كلمة بمثال عملي.\n" +
                "   • حل 3 تدريبات متدرجة من السهل إلى الأصعب.\n" +
                "3. نصيحة المذاكرة: خصص 15 دقيقة لمراجعة هذا الجزء قبل النوم لضمان تثبيت المعلومة."
        } else {
            "Structured study guide for \"$cleanTopic\" in $subject (${child.gradeStage}):\n\n" +
                "1. Core Idea: Understand the main concept of \"$cleanTopic\" with everyday examples.\n" +
                "2. Key Steps: Identify key terms, solve 3 practice exercises from easy to medium, and summarize in your own words."
        }

        val questions = if (isArabic) {
            listOf(
                QuizQuestion(
                    id = 1,
                    isTrueFalse = false,
                    question = "ما أفضل خطوة أولى عند مذاكرة درس \"$cleanTopic\" في $subject؟",
                    options = listOf(
                        "فهم الفكرة الأساسية وربطها بمثال عملي",
                        "حفظ الكلمات دون فهم معناها",
                        "تجاهل التدريبات العملية تماماً",
                    ),
                    correctIndex = 0,
                    explanation = "الفهم أولاً مع أمثلة عملية يجعل التذكر أسهل وأطول.",
                ),
                QuizQuestion(
                    id = 2,
                    isTrueFalse = true,
                    question = "صح أم خطأ: حل تدريبات متدرجة بعد قراءة درس \"$cleanTopic\" يساعد على تثبيت المعلومة.",
                    options = listOf("صح", "خطأ"),
                    correctIndex = 0,
                    explanation = "التطبيق العملي بالأسئلة يثبت المعلومة ويكشف النقاط التي تحتاج مراجعة.",
                ),
                QuizQuestion(
                    id = 3,
                    isTrueFalse = false,
                    question = "عند مواجهة مسألة صعبة في درس \"$cleanTopic\"، ماذا تفعل؟",
                    options = listOf(
                        "تقسيم المسألة إلى خطوات صغيرة ومراجعة القاعدة",
                        "ترك المادة نهائياً",
                        "التخمين العشوائي دون تفكير",
                    ),
                    correctIndex = 0,
                    explanation = "تقسيم السؤال لخطوات صغيرة يسهل الوصول للحل الصحيح.",
                ),
            )
        } else {
            listOf(
                QuizQuestion(
                    id = 1,
                    isTrueFalse = false,
                    question = "What is the best first step when studying \"$cleanTopic\" in $subject?",
                    options = listOf(
                        "Understand the core concept with a practical example",
                        "Memorize without understanding",
                        "Skip all practice questions",
                    ),
                    correctIndex = 0,
                    explanation = "Understanding concepts before practicing leads to strong retention.",
                ),
                QuizQuestion(
                    id = 2,
                    isTrueFalse = true,
                    question = "True or False: Practicing step-by-step exercises strengthens understanding of \"$cleanTopic\".",
                    options = listOf("True", "False"),
                    correctIndex = 0,
                    explanation = "Active practice reinforces memory and highlights weak spots.",
                ),
            )
        }

        return LessonTutorResult(
            explanation = explanation,
            questions = questions,
            imageNotice = imageWarning,
            isSamplePreview = true,
        )
    }

    private fun extractNumber(text: String): Double? {
        val normalized = text
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
            .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
            .replace('٨', '8').replace('٩', '9')
        val match = Regex("\\d+(?:\\.\\d+)?").find(normalized) ?: return null
        return match.value.toDoubleOrNull()
    }

    private fun extractDayOfMonth(text: String): Int? {
        val normalized = text
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
            .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
            .replace('٨', '8').replace('٩', '9')
        val match = Regex("يوم\\s*(\\d{1,2})").find(normalized)
            ?: Regex("day\\s*(\\d{1,2})").find(normalized)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun detectExpenseCategory(lower: String): FinanceCategory = when {
        lower.contains("بيت") || lower.contains("منزل") || lower.contains("مشتريات") || lower.contains("home") -> FinanceCategory.HOME
        lower.contains("طعام") || lower.contains("أكل") || lower.contains("اكل") || lower.contains("سوبرماركت") || lower.contains("food") -> FinanceCategory.FOOD
        lower.contains("تعليم") || lower.contains("مدرسة") || lower.contains("درس") || lower.contains("كتب") || lower.contains("education") -> FinanceCategory.EDUCATION
        lower.contains("مواصلات") || lower.contains("بنزين") || lower.contains("أوبر") || lower.contains("transport") -> FinanceCategory.TRANSPORTATION
        lower.contains("صحة") || lower.contains("دواء") || lower.contains("طبيب") || lower.contains("health") -> FinanceCategory.HEALTH
        lower.contains("فاتورة") || lower.contains("كهرباء") || lower.contains("نت") || lower.contains("bill") -> FinanceCategory.BILLS
        lower.contains("تسوق") || lower.contains("ملابس") || lower.contains("shopping") -> FinanceCategory.SHOPPING
        lower.contains("ترفيه") || lower.contains("خروج") || lower.contains("entertainment") -> FinanceCategory.ENTERTAINMENT
        else -> FinanceCategory.OTHER_EXPENSE
    }

    private fun detectSubject(lower: String, isAr: Boolean): String = when {
        lower.contains("علوم") || lower.contains("science") -> if (isAr) "العلوم" else "Science"
        lower.contains("رياضيات") || lower.contains("حساب") || lower.contains("math") -> if (isAr) "الرياضيات" else "Math"
        lower.contains("عربي") || lower.contains("arabic") -> if (isAr) "اللغة العربية" else "Arabic"
        lower.contains("إنجليزي") || lower.contains("انجليزي") || lower.contains("english") -> if (isAr) "اللغة الإنجليزية" else "English"
        lower.contains("دراسات") || lower.contains("تاريخ") -> if (isAr) "الدراسات الاجتماعية" else "Social Studies"
        else -> if (isAr) "مادة عامة" else "General Subject"
    }

    private fun parseRelativeDayMillis(lower: String, now: Long, tz: String): Long {
        val today = DateTimeUtils.startOfDayMillis(now, tz)
        if (lower.contains("غد") || lower.contains("بكرة") || lower.contains("tomorrow")) {
            return DateTimeUtils.addDays(today, 1, tz)
        }
        val dayMap = mapOf(
            "الأحد" to Calendar.SUNDAY, "الاحد" to Calendar.SUNDAY, "sunday" to Calendar.SUNDAY,
            "الإثنين" to Calendar.MONDAY, "الاثنين" to Calendar.MONDAY, "monday" to Calendar.MONDAY,
            "الثلاثاء" to Calendar.TUESDAY, "tuesday" to Calendar.TUESDAY,
            "الأربعاء" to Calendar.WEDNESDAY, "الاربعاء" to Calendar.WEDNESDAY, "wednesday" to Calendar.WEDNESDAY,
            "الخميس" to Calendar.THURSDAY, "thursday" to Calendar.THURSDAY,
            "الجمعة" to Calendar.FRIDAY, "friday" to Calendar.FRIDAY,
            "السبت" to Calendar.SATURDAY, "saturday" to Calendar.SATURDAY,
        )
        val matchedDow = dayMap.entries.firstOrNull { lower.contains(it.key) }?.value
        if (matchedDow != null) {
            val cal = Calendar.getInstance(DateTimeUtils.timeZone(tz)).apply { timeInMillis = today }
            for (i in 1..7) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                if (cal.get(Calendar.DAY_OF_WEEK) == matchedDow) return cal.timeInMillis
            }
        }
        return today
    }
}
