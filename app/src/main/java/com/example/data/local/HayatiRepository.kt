package com.example.data.local

import androidx.room.withTransaction
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class HayatiValidationException(message: String) : IllegalArgumentException(message)

data class FinanceSummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val actualExpenses: Double,
    val estimatedExpenses: Double,
    val remainingBalance: Double,
    val savingsRatePercent: Int,
    val categoryTotals: Map<FinanceCategory, Double>,
    val monthlyBudgetLimit: Double,
    val budgetUsedPercent: Int,
)

class HayatiRepository(
    private val db: HayatiDatabase,
    val activeOwnerId: String = LOCAL_OWNER_ID,
) {

    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeAll(activeOwnerId)
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll(activeOwnerId)
    val budgetLimits: Flow<List<BudgetLimitEntity>> = db.budgetLimitDao().observeAll(activeOwnerId)
    val debts: Flow<List<DebtInstallmentEntity>> = db.debtInstallmentDao().observeAll(activeOwnerId)
    val familyMembers: Flow<List<FamilyMemberEntity>> = db.familyMemberDao().observeAll(activeOwnerId)
    val shoppingItems: Flow<List<ShoppingItemEntity>> = db.shoppingItemDao().observeAll(activeOwnerId)
    val bills: Flow<List<BillEntity>> = db.billDao().observeAll(activeOwnerId)
    val homeEvents: Flow<List<HomeEventEntity>> = db.homeEventDao().observeAll(activeOwnerId)
    val children: Flow<List<ChildEntity>> = db.childDao().observeAll(activeOwnerId)
    val homeworkExams: Flow<List<HomeworkExamEntity>> = db.homeworkExamDao().observeAll(activeOwnerId)
    val calendarEvents: Flow<List<CalendarEventEntity>> = db.calendarEventDao().observeAll(activeOwnerId)
    val notifications: Flow<List<NotificationItemEntity>> = db.notificationDao().observeAll(activeOwnerId)
    val chatMessages: Flow<List<ChatMessageEntity>> = db.chatMessageDao().observeAll(activeOwnerId)

    // --- Validation Helpers ---
    private fun requireOwnerMatches(recordOwnerId: String) {
        if (recordOwnerId != activeOwnerId) {
            throw HayatiValidationException("لا يمكن تعديل سجل لا يخص المستخدم النشط")
        }
    }

    private fun requireNonBlank(value: String, fieldLabel: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            throw HayatiValidationException("الحقل المطلوب فارغ: $fieldLabel")
        }
        return trimmed
    }

    private fun requirePositiveAmount(value: Double, fieldLabel: String): Double {
        if (value.isNaN() || value.isInfinite() || value <= 0.0) {
            throw HayatiValidationException("يجب إدخال قيمة صالحة أكبر من صفر لـ: $fieldLabel")
        }
        return value
    }

    private fun requireNonNegativeAmount(value: Double, fieldLabel: String): Double {
        if (value.isNaN() || value.isInfinite() || value < 0.0) {
            throw HayatiValidationException("القيمة لا يمكن أن تكون سالبة لـ: $fieldLabel")
        }
        return value
    }

    private fun requireValidTimestamp(millis: Long, fieldLabel: String): Long {
        if (millis <= 0L) {
            throw HayatiValidationException("التاريخ أو الوقت غير صالح لـ: $fieldLabel")
        }
        return millis
    }

    private inline fun <reified T : Enum<T>> requireValidEnum(raw: String, fieldLabel: String): T {
        return runCatching { enumValueOf<T>(raw) }.getOrElse {
            throw HayatiValidationException("تصنيف غير صالح لـ: $fieldLabel")
        }
    }

    // --- Tasks ---
    suspend fun saveTask(task: TaskEntity): Long {
        if (task.id != 0L) requireOwnerMatches(task.ownerId)
        val cleanTitle = requireNonBlank(task.title, "عنوان المهمة")
        requireValidEnum<TaskPriority>(task.priority, "أولوية المهمة")
        requireValidEnum<TaskStatus>(task.status, "حالة المهمة")
        requireValidEnum<TaskCategory>(task.category, "تصنيف المهمة")
        requireValidTimestamp(task.dueDateMillis, "تاريخ المهمة")

        val sanitized = task.copy(
            ownerId = activeOwnerId,
            title = cleanTitle,
            description = task.description.trim(),
            labels = task.labels.trim(),
            meetingNotes = task.meetingNotes.trim(),
            updatedAt = System.currentTimeMillis(),
        )
        return if (sanitized.id == 0L) {
            db.taskDao().insert(sanitized)
        } else {
            db.taskDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        requireOwnerMatches(task.ownerId)
        val nextStatus = if (task.status == TaskStatus.COMPLETED.name) {
            TaskStatus.PENDING.name
        } else {
            TaskStatus.COMPLETED.name
        }
        db.taskDao().update(task.copy(status = nextStatus, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: TaskEntity) {
        requireOwnerMatches(task.ownerId)
        db.taskDao().delete(task)
    }

    // --- Transactions ---
    suspend fun saveTransaction(tx: TransactionEntity): Long {
        if (tx.id != 0L) requireOwnerMatches(tx.ownerId)
        val type = requireValidEnum<TransactionType>(tx.type, "نوع المعاملة")
        val cat = requireValidEnum<FinanceCategory>(tx.category, "فئة المعاملة")
        if (cat.isIncome != (type == TransactionType.INCOME)) {
            throw HayatiValidationException("فئة المعاملة لا تطابق نوعها (دخل/مصروف)")
        }
        requireValidEnum<PaymentMethod>(tx.paymentMethod, "طريقة الدفع")
        val amount = requirePositiveAmount(tx.amount, "المبلغ")
        val desc = requireNonBlank(tx.description, "وصف المعاملة")
        requireValidTimestamp(tx.dateMillis, "تاريخ المعاملة")

        val sanitized = tx.copy(
            ownerId = activeOwnerId,
            amount = amount,
            description = desc,
        )
        return if (sanitized.id == 0L) {
            db.transactionDao().insert(sanitized)
        } else {
            db.transactionDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        requireOwnerMatches(tx.ownerId)
        db.transactionDao().delete(tx)
    }

    // --- Budgets (deduplicated per owner + category + monthKey) ---
    suspend fun saveBudgetLimit(limit: BudgetLimitEntity): Long {
        if (limit.id != 0L) requireOwnerMatches(limit.ownerId)
        val cat = requireValidEnum<FinanceCategory>(limit.category, "فئة الميزانية")
        if (cat.isIncome) {
            throw HayatiValidationException("حد الميزانية يطبق على فئات المصروفات فقط")
        }
        val monthlyLimit = requirePositiveAmount(limit.monthlyLimit, "حد الميزانية")
        val cleanMonth = requireNonBlank(limit.monthKey, "الشهر")
        if (!Regex("""^\d{4}-\d{2}$""").matches(cleanMonth)) {
            throw HayatiValidationException("تنسيق الشهر يجب أن يكون YYYY-MM")
        }

        return db.withTransaction {
            val existing = db.budgetLimitDao().findByCategoryAndMonth(
                category = cat.name,
                monthKey = cleanMonth,
                ownerId = activeOwnerId,
            )
            val resolvedId = if (limit.id == 0L && existing != null) existing.id else limit.id
            db.budgetLimitDao().insert(
                limit.copy(
                    id = resolvedId,
                    ownerId = activeOwnerId,
                    category = cat.name,
                    monthlyLimit = monthlyLimit,
                    monthKey = cleanMonth,
                )
            )
        }
    }

    suspend fun deleteBudgetLimit(limit: BudgetLimitEntity) {
        requireOwnerMatches(limit.ownerId)
        db.budgetLimitDao().delete(limit)
    }

    // --- Debts & Installments ---
    suspend fun saveDebt(debt: DebtInstallmentEntity): Long {
        if (debt.id != 0L) requireOwnerMatches(debt.ownerId)
        val title = requireNonBlank(debt.title, "عنوان القسط أو الدين")
        val total = requirePositiveAmount(debt.totalAmount, "إجمالي المبلغ")
        val paid = requireNonNegativeAmount(debt.paidAmount, "المبلغ المدفوع")
        if (paid > total) {
            throw HayatiValidationException("المبلغ المدفوع لا يمكن أن يتجاوز إجمالي الالتزام")
        }
        val installment = requireNonNegativeAmount(debt.monthlyInstallment, "القسط الشهري")
        if (debt.dueDayOfMonth !in 1..31) {
            throw HayatiValidationException("يوم الاستحقاق يجب أن يكون بين 1 و 31")
        }

        val sanitized = debt.copy(
            ownerId = activeOwnerId,
            title = title,
            totalAmount = total,
            paidAmount = paid,
            monthlyInstallment = installment,
            notes = debt.notes.trim(),
            isCompleted = paid >= total,
        )
        return if (sanitized.id == 0L) {
            db.debtInstallmentDao().insert(sanitized)
        } else {
            db.debtInstallmentDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun recordDebtPayment(debt: DebtInstallmentEntity, payment: Double) {
        requireOwnerMatches(debt.ownerId)
        val validPayment = requirePositiveAmount(payment, "قيمة الدفعة")
        val newPaid = (debt.paidAmount + validPayment).coerceAtMost(debt.totalAmount)
        val completed = newPaid >= debt.totalAmount
        db.debtInstallmentDao().update(debt.copy(paidAmount = newPaid, isCompleted = completed))
    }

    suspend fun deleteDebt(debt: DebtInstallmentEntity) {
        requireOwnerMatches(debt.ownerId)
        db.debtInstallmentDao().delete(debt)
    }

    // --- Family & Home ---
    suspend fun saveFamilyMember(member: FamilyMemberEntity): Long {
        if (member.id != 0L) requireOwnerMatches(member.ownerId)
        val name = requireNonBlank(member.name, "اسم فرد الأسرة")
        val relation = requireNonBlank(member.relation, "صلة القرابة")
        val sanitized = member.copy(
            ownerId = activeOwnerId,
            name = name,
            relation = relation,
            phoneOrNote = member.phoneOrNote.trim(),
        )
        return if (sanitized.id == 0L) {
            db.familyMemberDao().insert(sanitized)
        } else {
            db.familyMemberDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteFamilyMember(member: FamilyMemberEntity) {
        requireOwnerMatches(member.ownerId)
        db.familyMemberDao().delete(member)
    }

    suspend fun saveShoppingItem(item: ShoppingItemEntity): Long {
        if (item.id != 0L) requireOwnerMatches(item.ownerId)
        val name = requireNonBlank(item.name, "اسم الصنف")
        val price = requireNonNegativeAmount(item.estimatedPrice, "السعر التقديري")
        val sanitized = item.copy(
            ownerId = activeOwnerId,
            listName = item.listName.trim().ifBlank { "مقاضي البيت" },
            name = name,
            quantity = item.quantity.trim().ifBlank { "1" },
            estimatedPrice = price,
        )
        return if (sanitized.id == 0L) {
            db.shoppingItemDao().insert(sanitized)
        } else {
            db.shoppingItemDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun toggleShoppingPurchased(item: ShoppingItemEntity) {
        requireOwnerMatches(item.ownerId)
        db.shoppingItemDao().update(item.copy(isPurchased = !item.isPurchased))
    }

    suspend fun deleteShoppingItem(item: ShoppingItemEntity) {
        requireOwnerMatches(item.ownerId)
        db.shoppingItemDao().delete(item)
    }

    suspend fun saveBill(bill: BillEntity): Long {
        if (bill.id != 0L) requireOwnerMatches(bill.ownerId)
        val title = requireNonBlank(bill.title, "اسم الفاتورة")
        requireValidEnum<BillCategory>(bill.category, "فئة الفاتورة")
        val amount = requirePositiveAmount(bill.amount, "قيمة الفاتورة")
        requireValidTimestamp(bill.dueDateMillis, "تاريخ استحقاق الفاتورة")

        val sanitized = bill.copy(
            ownerId = activeOwnerId,
            title = title,
            amount = amount,
            notes = bill.notes.trim(),
        )
        return if (sanitized.id == 0L) {
            db.billDao().insert(sanitized)
        } else {
            db.billDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun toggleBillPaid(bill: BillEntity) {
        requireOwnerMatches(bill.ownerId)
        db.billDao().update(bill.copy(isPaid = !bill.isPaid))
    }

    suspend fun deleteBill(bill: BillEntity) {
        requireOwnerMatches(bill.ownerId)
        db.billDao().delete(bill)
    }

    suspend fun saveHomeEvent(event: HomeEventEntity): Long {
        if (event.id != 0L) requireOwnerMatches(event.ownerId)
        val title = requireNonBlank(event.title, "عنوان المناسبة أو الصيانة")
        requireValidEnum<HomeEventType>(event.type, "نوع الحدث المنزلي")
        requireValidTimestamp(event.dateMillis, "تاريخ الحدث المنزلي")

        val sanitized = event.copy(
            ownerId = activeOwnerId,
            title = title,
            notes = event.notes.trim(),
        )
        return if (sanitized.id == 0L) {
            db.homeEventDao().insert(sanitized)
        } else {
            db.homeEventDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun toggleHomeEventCompleted(event: HomeEventEntity) {
        requireOwnerMatches(event.ownerId)
        db.homeEventDao().update(event.copy(isCompleted = !event.isCompleted))
    }

    suspend fun deleteHomeEvent(event: HomeEventEntity) {
        requireOwnerMatches(event.ownerId)
        db.homeEventDao().delete(event)
    }

    // --- Children & Education ---
    suspend fun saveChild(child: ChildEntity): Long {
        if (child.id != 0L) requireOwnerMatches(child.ownerId)
        val name = requireNonBlank(child.name, "اسم الطفل")
        val grade = requireNonBlank(child.gradeStage, "الصف الدراسي")
        if (child.ageYears !in 2..25) {
            throw HayatiValidationException("العمر يجب أن يكون بين 2 و 25 سنة")
        }

        val sanitized = child.copy(
            ownerId = activeOwnerId,
            name = name,
            gradeStage = grade,
            subjectsCsv = child.subjectsCsv.trim(),
            weeklySchedule = child.weeklySchedule.trim(),
            notes = child.notes.trim(),
        )
        return if (sanitized.id == 0L) {
            db.childDao().insert(sanitized)
        } else {
            db.childDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteChild(child: ChildEntity) {
        requireOwnerMatches(child.ownerId)
        db.withTransaction {
            db.homeworkExamDao().deleteForChild(child.id, activeOwnerId)
            db.childDao().delete(child)
        }
    }

    suspend fun saveHomeworkExam(item: HomeworkExamEntity): Long {
        if (item.id != 0L) requireOwnerMatches(item.ownerId)
        if (item.childId <= 0L) {
            throw HayatiValidationException("يجب اختيار ملف الطفل المرتبط بالواجب أو الامتحان")
        }
        requireValidEnum<EducationItemType>(item.type, "نوع العنصر التعليمي")
        val subject = requireNonBlank(item.subject, "المادة الدراسية")
        val title = requireNonBlank(item.title, "عنوان الواجب أو الامتحان")
        requireValidTimestamp(item.dueDateMillis, "موعد الواجب أو الامتحان")
        val maxScore = requirePositiveAmount(item.maxScore, "الدرجة العظمى")
        item.scoreEarned?.let { score ->
            if (score.isNaN() || score.isInfinite() || score < 0.0 || score > maxScore) {
                throw HayatiValidationException("الدرجة يجب أن تكون بين 0 و ${maxScore.toInt()}")
            }
        }

        // Verify child exists and belongs to active owner
        db.childDao().findById(item.childId, activeOwnerId)
            ?: throw HayatiValidationException("ملف الطفل المحدد غير موجود")

        val sanitized = item.copy(
            ownerId = activeOwnerId,
            subject = subject,
            title = title,
            description = item.description.trim(),
            maxScore = maxScore,
        )
        return if (sanitized.id == 0L) {
            db.homeworkExamDao().insert(sanitized)
        } else {
            db.homeworkExamDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun toggleHomeworkCompleted(item: HomeworkExamEntity) {
        requireOwnerMatches(item.ownerId)
        db.homeworkExamDao().update(item.copy(isCompleted = !item.isCompleted))
    }

    suspend fun deleteHomeworkExam(item: HomeworkExamEntity) {
        requireOwnerMatches(item.ownerId)
        db.homeworkExamDao().delete(item)
    }

    // --- Calendar Events ---
    suspend fun saveCalendarEvent(event: CalendarEventEntity): Long {
        if (event.id != 0L) requireOwnerMatches(event.ownerId)
        val title = requireNonBlank(event.title, "عنوان الموعد")
        requireValidEnum<CalendarEventType>(event.eventType, "نوع الموعد")
        requireValidTimestamp(event.startMillis, "بداية الموعد")
        if (event.endMillis < event.startMillis) {
            throw HayatiValidationException("وقت انتهاء الموعد لا يمكن أن يسبق بدايته")
        }

        val sanitized = event.copy(
            ownerId = activeOwnerId,
            title = title,
            description = event.description.trim(),
        )
        return if (sanitized.id == 0L) {
            db.calendarEventDao().insert(sanitized)
        } else {
            db.calendarEventDao().update(sanitized)
            sanitized.id
        }
    }

    suspend fun deleteCalendarEvent(event: CalendarEventEntity) {
        requireOwnerMatches(event.ownerId)
        db.calendarEventDao().delete(event)
    }

    // --- Notifications & Chat ---
    suspend fun addNotification(item: NotificationItemEntity): Long {
        val title = requireNonBlank(item.title, "عنوان التنبيه")
        val msg = requireNonBlank(item.message, "نص التنبيه")
        requireValidEnum<CalendarEventType>(item.category, "تصنيف التنبيه")
        return db.notificationDao().insert(
            item.copy(ownerId = activeOwnerId, title = title, message = msg)
        )
    }

    suspend fun markAllNotificationsRead() = db.notificationDao().markAllRead(activeOwnerId)

    suspend fun deleteNotification(item: NotificationItemEntity) {
        requireOwnerMatches(item.ownerId)
        db.notificationDao().delete(item)
    }

    suspend fun addChatMessage(msg: ChatMessageEntity): Long {
        val text = requireNonBlank(msg.text, "نص الرسالة")
        return db.chatMessageDao().insert(msg.copy(ownerId = activeOwnerId, text = text))
    }

    suspend fun clearChat() = db.chatMessageDao().deleteAll(activeOwnerId)

    // --- Deterministic financial calculator — all math performed strictly in code ---
    fun computeFinanceSummary(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetLimitEntity>,
        monthKey: String = DateTimeUtils.currentMonthKey(),
        tzId: String = DateTimeUtils.DEFAULT_TIMEZONE,
    ): FinanceSummary {
        val monthTxs = transactions.filter {
            DateTimeUtils.currentMonthKey(it.dateMillis, tzId) == monthKey
        }
        val totalIncome = monthTxs
            .filter { it.type == TransactionType.INCOME.name }
            .sumOf { it.amount }
        val expenseTxs = monthTxs.filter { it.type == TransactionType.EXPENSE.name }
        val totalExpenses = expenseTxs.sumOf { it.amount }
        val actualExpenses = expenseTxs.filter { !it.isEstimate }.sumOf { it.amount }
        val estimatedExpenses = expenseTxs.filter { it.isEstimate }.sumOf { it.amount }
        val remaining = totalIncome - totalExpenses
        val savingsRate = if (totalIncome > 0.0) {
            (((totalIncome - totalExpenses) / totalIncome) * 100.0).roundToInt().coerceIn(-100, 100)
        } else 0

        val categoryTotals = mutableMapOf<FinanceCategory, Double>()
        expenseTxs.forEach { tx ->
            val cat = runCatching { FinanceCategory.valueOf(tx.category) }.getOrElse { FinanceCategory.OTHER_EXPENSE }
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + tx.amount
        }

        val monthBudgets = budgets.filter { it.monthKey == monthKey }
        val totalBudgetLimit = monthBudgets.sumOf { it.monthlyLimit }
        val budgetUsedPercent = if (totalBudgetLimit > 0.0) {
            ((totalExpenses / totalBudgetLimit) * 100.0).roundToInt().coerceAtLeast(0)
        } else 0

        return FinanceSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            actualExpenses = actualExpenses,
            estimatedExpenses = estimatedExpenses,
            remainingBalance = remaining,
            savingsRatePercent = savingsRate,
            categoryTotals = categoryTotals,
            monthlyBudgetLimit = totalBudgetLimit,
            budgetUsedPercent = budgetUsedPercent,
        )
    }

    suspend fun loadSampleData() = db.withTransaction {
        clearSampleDataInternal()
        val now = System.currentTimeMillis()
        val todayStart = DateTimeUtils.startOfDayMillis(now)
        val yesterday = DateTimeUtils.addDays(todayStart, -1)
        val tomorrow = DateTimeUtils.addDays(todayStart, 1)
        val in3Days = DateTimeUtils.addDays(todayStart, 3)
        val in5Days = DateTimeUtils.addDays(todayStart, 5)
        val monthKey = DateTimeUtils.currentMonthKey(now)

        // Sample Work & Home Tasks
        db.taskDao().insert(
            TaskEntity(
                ownerId = activeOwnerId,
                title = "مراجعة عرض المشروع للربع الرابع",
                description = "تجهيز الشرائح النهائية ومراجعة الأرقام مع الفريق",
                priority = TaskPriority.HIGH.name,
                status = TaskStatus.IN_PROGRESS.name,
                category = TaskCategory.WORK.name,
                labels = "عاجل، إدارة",
                dueDateMillis = DateTimeUtils.setHourMinute(todayStart, 14, 0),
                isMeeting = true,
                meetingNotes = "تأكيد حضور مدير المنتج",
                isSample = true,
            )
        )
        db.taskDao().insert(
            TaskEntity(
                ownerId = activeOwnerId,
                title = "إرسال التقرير المالي الشهري",
                description = "مراجعة المصروفات واعتماد التقرير",
                priority = TaskPriority.HIGH.name,
                status = TaskStatus.PENDING.name,
                category = TaskCategory.WORK.name,
                labels = "تقارير",
                dueDateMillis = DateTimeUtils.setHourMinute(yesterday, 17, 0),
                isSample = true,
            )
        )
        db.taskDao().insert(
            TaskEntity(
                ownerId = activeOwnerId,
                title = "تحديث خطة العمل الأسبوعية",
                description = "توزيع المهام على أعضاء الفريق للأسبوع القادم",
                priority = TaskPriority.MEDIUM.name,
                status = TaskStatus.PENDING.name,
                category = TaskCategory.WORK.name,
                labels = "تخطيط",
                dueDateMillis = DateTimeUtils.setHourMinute(tomorrow, 11, 0),
                isSample = true,
            )
        )
        db.taskDao().insert(
            TaskEntity(
                ownerId = activeOwnerId,
                title = "تنظيم خزانة الكتب المدرسية",
                description = "ترتيب كتب الفصل الدراسي الأول",
                priority = TaskPriority.LOW.name,
                status = TaskStatus.COMPLETED.name,
                category = TaskCategory.HOME.name,
                dueDateMillis = DateTimeUtils.setHourMinute(todayStart, 18, 0),
                isSample = true,
            )
        )

        // Sample Transactions
        db.transactionDao().insert(
            TransactionEntity(
                ownerId = activeOwnerId,
                type = TransactionType.INCOME.name,
                amount = 18500.0,
                category = FinanceCategory.SALARY.name,
                description = "الراتب الشهري",
                paymentMethod = PaymentMethod.BANK_TRANSFER.name,
                dateMillis = DateTimeUtils.addDays(todayStart, -5),
                isEstimate = false,
                isSample = true,
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                ownerId = activeOwnerId,
                type = TransactionType.INCOME.name,
                amount = 3500.0,
                category = FinanceCategory.BUSINESS.name,
                description = "استشارة تصميم تطبيق",
                paymentMethod = PaymentMethod.WALLET.name,
                dateMillis = DateTimeUtils.addDays(todayStart, -2),
                isEstimate = false,
                isSample = true,
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                ownerId = activeOwnerId,
                type = TransactionType.EXPENSE.name,
                amount = 2400.0,
                category = FinanceCategory.HOME.name,
                description = "مستلزمات البيت والسوبرماركت",
                paymentMethod = PaymentMethod.CARD.name,
                dateMillis = DateTimeUtils.addDays(todayStart, -3),
                isEstimate = false,
                isSample = true,
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                ownerId = activeOwnerId,
                type = TransactionType.EXPENSE.name,
                amount = 1800.0,
                category = FinanceCategory.EDUCATION.name,
                description = "كتب ومصاريف دروس الأبناء",
                paymentMethod = PaymentMethod.CASH.name,
                dateMillis = DateTimeUtils.addDays(todayStart, -1),
                isEstimate = false,
                isSample = true,
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                ownerId = activeOwnerId,
                type = TransactionType.EXPENSE.name,
                amount = 950.0,
                category = FinanceCategory.FOOD.name,
                description = "غداء عائلي وخضروات",
                paymentMethod = PaymentMethod.CASH.name,
                dateMillis = todayStart,
                isEstimate = false,
                isSample = true,
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                ownerId = activeOwnerId,
                type = TransactionType.EXPENSE.name,
                amount = 650.0,
                category = FinanceCategory.TRANSPORTATION.name,
                description = "بنزين ومواصلات",
                paymentMethod = PaymentMethod.CASH.name,
                dateMillis = todayStart,
                isEstimate = false,
                isSample = true,
            )
        )

        // Sample Budgets
        db.budgetLimitDao().insert(
            BudgetLimitEntity(ownerId = activeOwnerId, category = FinanceCategory.HOME.name, monthlyLimit = 4000.0, monthKey = monthKey, isSample = true)
        )
        db.budgetLimitDao().insert(
            BudgetLimitEntity(ownerId = activeOwnerId, category = FinanceCategory.FOOD.name, monthlyLimit = 3000.0, monthKey = monthKey, isSample = true)
        )
        db.budgetLimitDao().insert(
            BudgetLimitEntity(ownerId = activeOwnerId, category = FinanceCategory.EDUCATION.name, monthlyLimit = 2500.0, monthKey = monthKey, isSample = true)
        )

        // Sample Debt / Installment
        db.debtInstallmentDao().insert(
            DebtInstallmentEntity(
                ownerId = activeOwnerId,
                title = "قسط جهاز التابلت المدرسي",
                totalAmount = 12000.0,
                paidAmount = 4000.0,
                monthlyInstallment = 1000.0,
                dueDayOfMonth = 25,
                isDebtToOthers = true,
                notes = "بدون فوائد",
                isSample = true,
            )
        )

        // Family Members
        db.familyMemberDao().insert(
            FamilyMemberEntity(ownerId = activeOwnerId, name = "سارة علي", relation = "الزوجة", phoneOrNote = "متابعة شؤون المنزل والمدارس", isSample = true)
        )
        db.familyMemberDao().insert(
            FamilyMemberEntity(ownerId = activeOwnerId, name = "عمر", relation = "الابن", phoneOrNote = "الصف الخامس الابتدائي", isSample = true)
        )
        db.familyMemberDao().insert(
            FamilyMemberEntity(ownerId = activeOwnerId, name = "ليلى", relation = "الابنة", phoneOrNote = "الصف الثاني الإعدادي", isSample = true)
        )

        // Shopping List
        db.shoppingItemDao().insert(
            ShoppingItemEntity(ownerId = activeOwnerId, listName = "مقاضي الأسبوع", name = "لبن وأجبان", quantity = "3 عبوات", estimatedPrice = 210.0, isPurchased = false, isSample = true)
        )
        db.shoppingItemDao().insert(
            ShoppingItemEntity(ownerId = activeOwnerId, listName = "مقاضي الأسبوع", name = "فواكه وخضروات طازجة", quantity = "4 كجم", estimatedPrice = 320.0, isPurchased = false, isSample = true)
        )
        db.shoppingItemDao().insert(
            ShoppingItemEntity(ownerId = activeOwnerId, listName = "مقاضي الأسبوع", name = "منظفات منزلية", quantity = "2 قطعة", estimatedPrice = 185.0, isPurchased = true, isSample = true)
        )

        // Bills
        db.billDao().insert(
            BillEntity(ownerId = activeOwnerId, title = "فاتورة الإنترنت المنزلي", category = BillCategory.INTERNET.name, amount = 410.0, dueDateMillis = in3Days, isPaid = false, isRecurring = true, notes = "WE Home Internet", isSample = true)
        )
        db.billDao().insert(
            BillEntity(ownerId = activeOwnerId, title = "فاتورة الكهرباء", category = BillCategory.ELECTRICITY.name, amount = 580.0, dueDateMillis = in5Days, isPaid = false, isRecurring = true, isSample = true)
        )
        db.billDao().insert(
            BillEntity(ownerId = activeOwnerId, title = "فاتورة الغاز الطبيعي", category = BillCategory.GAS.name, amount = 145.0, dueDateMillis = yesterday, isPaid = true, isRecurring = true, isSample = true)
        )

        // Home Events & Maintenance
        db.homeEventDao().insert(
            HomeEventEntity(ownerId = activeOwnerId, type = HomeEventType.MAINTENANCE.name, title = "صيانة فلتر المياه (تغيير الشمعات)", dateMillis = tomorrow, notes = "الموعد الساعة 5 مساءً", isSample = true)
        )
        db.homeEventDao().insert(
            HomeEventEntity(ownerId = activeOwnerId, type = HomeEventType.OCCASION.name, title = "عشاء عائلي عند الجدة", dateMillis = DateTimeUtils.addDays(todayStart, 4), notes = "إحضار حلويات", isSample = true)
        )

        // Children & Education
        val omarId = db.childDao().insert(
            ChildEntity(
                ownerId = activeOwnerId,
                name = "عمر",
                gradeStage = "الصف الخامس الابتدائي",
                ageYears = 11,
                subjectsCsv = "الرياضيات، العلوم، اللغة العربية، الإنجليزية",
                weeklySchedule = "الأحد: رياضيات | الإثنين: علوم | الثلاثاء: عربي | الأربعاء: إنجليزي | الخميس: مراجعة",
                notes = "يحب التجارب العلمية ويحتاج تدريباً إضافياً في الكسور",
                isSample = true,
            )
        )
        val lailaId = db.childDao().insert(
            ChildEntity(
                ownerId = activeOwnerId,
                name = "ليلى",
                gradeStage = "الصف الثاني الإعدادي",
                ageYears = 14,
                subjectsCsv = "الجبر، الهندسة، العلوم، الدراسات، الإنجليزية",
                weeklySchedule = "الأحد: جبر | الإثنين: علوم | الثلاثاء: إنجليزي | الأربعاء: هندسة | الخميس: دراسات",
                notes = "مستوى ممتاز في اللغات",
                isSample = true,
            )
        )

        db.homeworkExamDao().insert(
            HomeworkExamEntity(
                ownerId = activeOwnerId,
                childId = omarId,
                type = EducationItemType.HOMEWORK.name,
                subject = "الرياضيات",
                title = "حل تمارين ضرب الكسور العشرية (ص 42)",
                description = "المسائل من 1 إلى 10",
                dueDateMillis = tomorrow,
                isCompleted = false,
                isSample = true,
            )
        )
        db.homeworkExamDao().insert(
            HomeworkExamEntity(
                ownerId = activeOwnerId,
                childId = omarId,
                type = EducationItemType.EXAM.name,
                subject = "العلوم",
                title = "اختبار وحدة الجهاز التنفسي والدوري",
                description = "مراجعة الرسوم التوضيحية والمصطلحات",
                dueDateMillis = in3Days,
                isCompleted = false,
                scoreEarned = 92.0,
                maxScore = 100.0,
                isSample = true,
            )
        )
        db.homeworkExamDao().insert(
            HomeworkExamEntity(
                ownerId = activeOwnerId,
                childId = lailaId,
                type = EducationItemType.HOMEWORK.name,
                subject = "اللغة الإنجليزية",
                title = "كتابة مقال قصير عن التكنولوجيا الحديثة",
                description = "150 كلمة على الأقل",
                dueDateMillis = todayStart,
                isCompleted = true,
                scoreEarned = 96.0,
                maxScore = 100.0,
                isSample = true,
            )
        )

        // Calendar Event
        db.calendarEventDao().insert(
            CalendarEventEntity(
                ownerId = activeOwnerId,
                title = "اجتماع تخطيط الأسبوع",
                description = "مكالمة فيديو مع فريق التصميم",
                eventType = CalendarEventType.WORK.name,
                startMillis = DateTimeUtils.setHourMinute(todayStart, 11, 0),
                endMillis = DateTimeUtils.setHourMinute(todayStart, 12, 0),
                isSample = true,
            )
        )
    }

    suspend fun clearSampleData() = db.withTransaction {
        clearSampleDataInternal()
    }

    private suspend fun clearSampleDataInternal() {
        db.taskDao().deleteSamples(activeOwnerId)
        db.transactionDao().deleteSamples(activeOwnerId)
        db.budgetLimitDao().deleteSamples(activeOwnerId)
        db.debtInstallmentDao().deleteSamples(activeOwnerId)
        db.familyMemberDao().deleteSamples(activeOwnerId)
        db.shoppingItemDao().deleteSamples(activeOwnerId)
        db.billDao().deleteSamples(activeOwnerId)
        db.homeEventDao().deleteSamples(activeOwnerId)
        db.homeworkExamDao().deleteSamples(activeOwnerId)
        db.childDao().deleteSamples(activeOwnerId)
        db.calendarEventDao().deleteSamples(activeOwnerId)
    }

    suspend fun clearAllData() = db.withTransaction {
        db.taskDao().deleteAll(activeOwnerId)
        db.transactionDao().deleteAll(activeOwnerId)
        db.budgetLimitDao().deleteAll(activeOwnerId)
        db.debtInstallmentDao().deleteAll(activeOwnerId)
        db.familyMemberDao().deleteAll(activeOwnerId)
        db.shoppingItemDao().deleteAll(activeOwnerId)
        db.billDao().deleteAll(activeOwnerId)
        db.homeEventDao().deleteAll(activeOwnerId)
        db.homeworkExamDao().deleteAll(activeOwnerId)
        db.childDao().deleteAll(activeOwnerId)
        db.calendarEventDao().deleteAll(activeOwnerId)
        db.notificationDao().deleteAll(activeOwnerId)
        db.chatMessageDao().deleteAll(activeOwnerId)
    }
}
