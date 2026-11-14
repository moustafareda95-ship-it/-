package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppLanguage
import com.example.data.local.BillEntity
import com.example.data.local.BudgetLimitEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CalendarEventType
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChildEntity
import com.example.data.local.DebtInstallmentEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.FinanceSummary
import com.example.data.local.HayatiRepository
import com.example.data.local.HomeEventEntity
import com.example.data.local.HomeworkExamEntity
import com.example.data.local.LOCAL_OWNER_ID
import com.example.data.local.NotificationItemEntity
import com.example.data.local.ShoppingItemEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskStatus
import com.example.data.local.ThemeMode
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.domain.ai.AiRepository
import com.example.domain.ai.AppContextSnapshot
import com.example.domain.ai.DailyPlanSlot
import com.example.domain.ai.LessonTutorResult
import com.example.domain.ai.PendingAiAction
import com.example.util.DateTimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class HayatiUiState(
    val prefs: UserPreferences = UserPreferences(),
    val tasks: List<TaskEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgetLimits: List<BudgetLimitEntity> = emptyList(),
    val debts: List<DebtInstallmentEntity> = emptyList(),
    val familyMembers: List<FamilyMemberEntity> = emptyList(),
    val shoppingItems: List<ShoppingItemEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val homeEvents: List<HomeEventEntity> = emptyList(),
    val children: List<ChildEntity> = emptyList(),
    val homeworkExams: List<HomeworkExamEntity> = emptyList(),
    val calendarEvents: List<CalendarEventEntity> = emptyList(),
    val notifications: List<NotificationItemEntity> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val financeSummary: FinanceSummary = FinanceSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0, emptyMap(), 0.0, 0),
    val hasAnyRecords: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val lastError: String? = null,
    val ownerId: String = LOCAL_OWNER_ID,
)

class MainViewModel(
    private val repo: HayatiRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val aiRepo: AiRepository,
) : ViewModel() {

    private val writeMutex = Mutex()
    private val recentActionTimestamps = mutableMapOf<String, Long>()

    private val _isSaving = MutableStateFlow(false)
    private val _lastError = MutableStateFlow<String?>(null)

    private val baseFlow = combine(
        prefsRepo.preferencesFlow,
        repo.tasks,
        repo.transactions,
        repo.budgetLimits,
        repo.debts,
    ) { prefs, tasks, transactions, budgets, debts ->
        Quint(prefs, tasks, transactions, budgets, debts)
    }

    private val familyFlow = combine(
        repo.familyMembers,
        repo.shoppingItems,
        repo.bills,
        repo.homeEvents,
        repo.children,
    ) { members, shopping, bills, homeEvents, children ->
        Quint(members, shopping, bills, homeEvents, children)
    }

    private val metaFlow = combine(
        repo.homeworkExams,
        repo.calendarEvents,
        repo.notifications,
        repo.chatMessages,
    ) { hw, cal, notif, chat ->
        Quad(hw, cal, notif, chat)
    }

    val uiState: StateFlow<HayatiUiState> = combine(
        baseFlow,
        familyFlow,
        metaFlow,
        _isSaving,
        _lastError,
    ) { b, f, m, saving, err ->
        val monthKey = DateTimeUtils.currentMonthKey(tzId = b.first.timeZoneId)
        val summary = repo.computeFinanceSummary(b.third, b.fourth, monthKey, b.first.timeZoneId)
        val hasAny = b.second.isNotEmpty() || b.third.isNotEmpty() || f.first.isNotEmpty() ||
            f.second.isNotEmpty() || f.third.isNotEmpty() || f.fourth.isNotEmpty() ||
            f.fifth.isNotEmpty() || m.first.isNotEmpty() || m.second.isNotEmpty()
        HayatiUiState(
            prefs = b.first,
            tasks = b.second,
            transactions = b.third,
            budgetLimits = b.fourth,
            debts = b.fifth,
            familyMembers = f.first,
            shoppingItems = f.second,
            bills = f.third,
            homeEvents = f.fourth,
            children = f.fifth,
            homeworkExams = m.first,
            calendarEvents = m.second,
            notifications = m.third,
            chatMessages = m.fourth,
            financeSummary = summary,
            hasAnyRecords = hasAny,
            isLoading = false,
            isSaving = saving,
            lastError = err,
            ownerId = repo.activeOwnerId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HayatiUiState(isLoading = true, ownerId = repo.activeOwnerId),
    )

    // AI Assistant state
    private val _pendingAiAction = MutableStateFlow<PendingAiAction?>(null)
    val pendingAiAction: StateFlow<PendingAiAction?> = _pendingAiAction.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    // Daily Planner state
    private val _dailyPlanSlots = MutableStateFlow<List<DailyPlanSlot>>(emptyList())
    val dailyPlanSlots: StateFlow<List<DailyPlanSlot>> = _dailyPlanSlots.asStateFlow()

    // Work AI insight
    private val _workAiInsight = MutableStateFlow<String?>(null)
    val workAiInsight: StateFlow<String?> = _workAiInsight.asStateFlow()

    // Finance saving tips
    private val _savingTips = MutableStateFlow<List<String>>(emptyList())
    val savingTips: StateFlow<List<String>> = _savingTips.asStateFlow()

    // Education Tutor state
    private val _tutorResult = MutableStateFlow<LessonTutorResult?>(null)
    val tutorResult: StateFlow<LessonTutorResult?> = _tutorResult.asStateFlow()

    // Snack/feedback event
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearLastError() {
        _lastError.value = null
    }

    private fun runGuardedWrite(
        dedupeKey: String? = null,
        dedupeWindowMs: Long = 800L,
        successAr: String? = null,
        successEn: String? = null,
        block: suspend () -> Unit,
    ) = viewModelScope.launch {
        if (dedupeKey != null) {
            if (!writeMutex.tryLock()) return@launch
        } else {
            writeMutex.lock()
        }
        try {
            val now = System.currentTimeMillis()
            if (dedupeKey != null) {
                val lastRun = recentActionTimestamps[dedupeKey]
                if (lastRun != null && (now - lastRun) < dedupeWindowMs) {
                    return@launch
                }
            }
            _isSaving.value = true
            _lastError.value = null
            block()
            if (dedupeKey != null) {
                recentActionTimestamps[dedupeKey] = System.currentTimeMillis()
                if (recentActionTimestamps.size > 40) {
                    val cutoff = System.currentTimeMillis() - 5_000L
                    recentActionTimestamps.entries.removeAll { it.value < cutoff }
                }
            }
            val isAr = uiState.value.prefs.language == AppLanguage.ARABIC
            val msg = if (isAr) successAr else successEn
            if (!msg.isNullOrBlank()) {
                _statusMessage.value = msg
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val isAr = uiState.value.prefs.language == AppLanguage.ARABIC
            val errText = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: if (isAr) "تعذر حفظ البيانات، يرجى التحقق من المدخلات" else "Could not save record, please check your input"
            _lastError.value = errText
            _statusMessage.value = errText
        } finally {
            _isSaving.value = false
            writeMutex.unlock()
        }
    }

    private fun snapshot(): AppContextSnapshot {
        val st = uiState.value
        return AppContextSnapshot(
            tasks = st.tasks,
            transactions = st.transactions,
            financeSummary = st.financeSummary,
            bills = st.bills,
            children = st.children,
            homeworkExams = st.homeworkExams,
            calendarEvents = st.calendarEvents,
            currency = st.prefs.currency,
            timeZoneId = st.prefs.timeZoneId,
            isArabic = st.prefs.language == AppLanguage.ARABIC,
        )
    }

    // --- Sample / Reset ---
    fun loadSampleData() = runGuardedWrite(
        dedupeKey = "load_sample_data",
        dedupeWindowMs = 1500L,
        successAr = "تم تحميل بيانات تجريبية للمعاينة",
        successEn = "Sample preview data loaded",
    ) {
        repo.loadSampleData()
    }

    fun clearSampleData() = runGuardedWrite(
        dedupeKey = "clear_sample_data",
        successAr = "تم حذف البيانات التجريبية",
        successEn = "Sample data cleared",
    ) {
        repo.clearSampleData()
    }

    fun clearAllData() = runGuardedWrite(
        dedupeKey = "clear_all_data",
        successAr = "تم مسح جميع السجلات المحلية",
        successEn = "All local records cleared",
    ) {
        repo.clearAllData()
    }

    // --- Tasks ---
    fun saveTask(task: TaskEntity) = runGuardedWrite(
        dedupeKey = "save_task:${task.id}:${task.title.trim()}:${task.dueDateMillis}",
        successAr = if (task.id == 0L) "تم حفظ المهمة" else "تم تحديث المهمة",
        successEn = if (task.id == 0L) "Task saved" else "Task updated",
    ) {
        repo.saveTask(task)
    }

    fun toggleTaskCompleted(task: TaskEntity) = runGuardedWrite(
        dedupeKey = "toggle_task:${task.id}:${task.status}",
    ) {
        repo.toggleTaskCompleted(task)
    }

    fun deleteTask(task: TaskEntity) = runGuardedWrite(
        dedupeKey = "delete_task:${task.id}",
        successAr = "تم حذف المهمة",
        successEn = "Task deleted",
    ) {
        repo.deleteTask(task)
    }

    fun generateWorkInsight() = viewModelScope.launch {
        val st = uiState.value
        _workAiInsight.value = aiRepo.summarizeWorkTasks(
            st.tasks,
            st.prefs.language == AppLanguage.ARABIC,
            st.prefs.timeZoneId,
        )
    }

    fun dismissWorkInsight() {
        _workAiInsight.value = null
    }

    // --- Finance ---
    fun saveTransaction(tx: TransactionEntity) = runGuardedWrite(
        dedupeKey = "save_tx:${tx.id}:${tx.type}:${tx.amount}:${tx.description.trim()}:${tx.dateMillis}",
        successAr = if (tx.id == 0L) "تم حفظ المعاملة المالية" else "تم تحديث المعاملة المالية",
        successEn = if (tx.id == 0L) "Transaction saved" else "Transaction updated",
    ) {
        repo.saveTransaction(tx)
    }

    fun deleteTransaction(tx: TransactionEntity) = runGuardedWrite(
        dedupeKey = "delete_tx:${tx.id}",
        successAr = "تم حذف المعاملة",
        successEn = "Transaction deleted",
    ) {
        repo.deleteTransaction(tx)
    }

    fun saveBudgetLimit(limit: BudgetLimitEntity) = runGuardedWrite(
        dedupeKey = "save_budget:${limit.category}:${limit.monthKey}:${limit.monthlyLimit}",
        successAr = "تم حفظ حد الميزانية",
        successEn = "Budget limit saved",
    ) {
        repo.saveBudgetLimit(limit)
    }

    fun deleteBudgetLimit(limit: BudgetLimitEntity) = runGuardedWrite(
        dedupeKey = "delete_budget:${limit.id}",
    ) {
        repo.deleteBudgetLimit(limit)
    }

    fun saveDebt(debt: DebtInstallmentEntity) = runGuardedWrite(
        dedupeKey = "save_debt:${debt.id}:${debt.title.trim()}:${debt.totalAmount}",
        successAr = "تم حفظ الالتزام المالي",
        successEn = "Debt record saved",
    ) {
        repo.saveDebt(debt)
    }

    fun recordDebtPayment(debt: DebtInstallmentEntity, amount: Double) = runGuardedWrite(
        dedupeKey = "pay_debt:${debt.id}:$amount:${debt.paidAmount}",
        successAr = "تم تسجيل الدفعة بنجاح",
        successEn = "Payment recorded",
    ) {
        repo.recordDebtPayment(debt, amount)
    }

    fun deleteDebt(debt: DebtInstallmentEntity) = runGuardedWrite(
        dedupeKey = "delete_debt:${debt.id}",
    ) {
        repo.deleteDebt(debt)
    }

    fun refreshSavingTips() = viewModelScope.launch {
        val st = uiState.value
        _savingTips.value = aiRepo.generateSavingSuggestions(
            st.transactions,
            st.financeSummary,
            st.prefs.currency,
            st.prefs.language == AppLanguage.ARABIC,
        )
    }

    // --- Family & Home ---
    fun saveFamilyMember(member: FamilyMemberEntity) = runGuardedWrite(
        dedupeKey = "save_member:${member.id}:${member.name.trim()}:${member.relation.trim()}",
        successAr = "تم حفظ بيانات فرد الأسرة",
        successEn = "Family member saved",
    ) {
        repo.saveFamilyMember(member)
    }

    fun deleteFamilyMember(member: FamilyMemberEntity) = runGuardedWrite(
        dedupeKey = "delete_member:${member.id}",
    ) {
        repo.deleteFamilyMember(member)
    }

    fun saveShoppingItem(item: ShoppingItemEntity) = runGuardedWrite(
        dedupeKey = "save_shopping:${item.id}:${item.name.trim()}:${item.estimatedPrice}",
        successAr = "تمت إضافة الصنف لقائمة التسوق",
        successEn = "Shopping item saved",
    ) {
        repo.saveShoppingItem(item)
    }

    fun toggleShoppingPurchased(item: ShoppingItemEntity) = runGuardedWrite(
        dedupeKey = "toggle_shopping:${item.id}:${item.isPurchased}",
    ) {
        repo.toggleShoppingPurchased(item)
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) = runGuardedWrite(
        dedupeKey = "delete_shopping:${item.id}",
    ) {
        repo.deleteShoppingItem(item)
    }

    fun saveBill(bill: BillEntity) = runGuardedWrite(
        dedupeKey = "save_bill:${bill.id}:${bill.title.trim()}:${bill.amount}:${bill.dueDateMillis}",
        successAr = if (bill.id == 0L) "تم حفظ الفاتورة" else "تم تحديث الفاتورة",
        successEn = if (bill.id == 0L) "Bill saved" else "Bill updated",
    ) {
        repo.saveBill(bill)
    }

    fun toggleBillPaid(bill: BillEntity) = runGuardedWrite(
        dedupeKey = "toggle_bill:${bill.id}:${bill.isPaid}",
    ) {
        repo.toggleBillPaid(bill)
    }

    fun deleteBill(bill: BillEntity) = runGuardedWrite(
        dedupeKey = "delete_bill:${bill.id}",
    ) {
        repo.deleteBill(bill)
    }

    fun saveHomeEvent(event: HomeEventEntity) = runGuardedWrite(
        dedupeKey = "save_home_event:${event.id}:${event.title.trim()}:${event.dateMillis}",
        successAr = "تم حفظ السجل المنزلي",
        successEn = "Home event saved",
    ) {
        repo.saveHomeEvent(event)
    }

    fun toggleHomeEventCompleted(event: HomeEventEntity) = runGuardedWrite(
        dedupeKey = "toggle_home_event:${event.id}:${event.isCompleted}",
    ) {
        repo.toggleHomeEventCompleted(event)
    }

    fun deleteHomeEvent(event: HomeEventEntity) = runGuardedWrite(
        dedupeKey = "delete_home_event:${event.id}",
    ) {
        repo.deleteHomeEvent(event)
    }

    // --- Children & Education ---
    fun saveChild(child: ChildEntity) = runGuardedWrite(
        dedupeKey = "save_child:${child.id}:${child.name.trim()}:${child.gradeStage.trim()}",
        successAr = if (child.id == 0L) "تم حفظ ملف الطفل" else "تم تحديث ملف الطفل",
        successEn = if (child.id == 0L) "Child profile saved" else "Child profile updated",
    ) {
        repo.saveChild(child)
    }

    fun deleteChild(child: ChildEntity) = runGuardedWrite(
        dedupeKey = "delete_child:${child.id}",
        successAr = "تم حذف ملف الطفل",
        successEn = "Child profile deleted",
    ) {
        repo.deleteChild(child)
    }

    fun saveHomeworkExam(item: HomeworkExamEntity) = runGuardedWrite(
        dedupeKey = "save_hw:${item.id}:${item.childId}:${item.title.trim()}:${item.dueDateMillis}",
        successAr = if (item.id == 0L) "تم حفظ السجل الدراسي" else "تم تحديث السجل الدراسي",
        successEn = if (item.id == 0L) "Homework/Exam saved" else "Homework/Exam updated",
    ) {
        repo.saveHomeworkExam(item)
    }

    fun toggleHomeworkCompleted(item: HomeworkExamEntity) = runGuardedWrite(
        dedupeKey = "toggle_hw:${item.id}:${item.isCompleted}",
    ) {
        repo.toggleHomeworkCompleted(item)
    }

    fun deleteHomeworkExam(item: HomeworkExamEntity) = runGuardedWrite(
        dedupeKey = "delete_hw:${item.id}",
    ) {
        repo.deleteHomeworkExam(item)
    }

    fun runLessonTutor(
        child: ChildEntity,
        subject: String,
        topicOrContent: String,
        hasAttachedImage: Boolean,
    ) = viewModelScope.launch {
        _tutorResult.value = aiRepo.explainLessonAndGenerateQuiz(
            child = child,
            subject = subject,
            lessonTopicOrContent = topicOrContent,
            hasAttachedImage = hasAttachedImage,
            isArabic = uiState.value.prefs.language == AppLanguage.ARABIC,
        )
    }

    fun clearTutorResult() {
        _tutorResult.value = null
    }

    // --- Calendar ---
    fun saveCalendarEvent(event: CalendarEventEntity) = runGuardedWrite(
        dedupeKey = "save_cal:${event.id}:${event.title.trim()}:${event.startMillis}",
        successAr = if (event.id == 0L) "تم حفظ الموعد في التقويم" else "تم تحديث الموعد",
        successEn = if (event.id == 0L) "Calendar event saved" else "Calendar event updated",
    ) {
        repo.saveCalendarEvent(event)
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) = runGuardedWrite(
        dedupeKey = "delete_cal:${event.id}",
    ) {
        repo.deleteCalendarEvent(event)
    }

    // --- Notifications ---
    fun markAllNotificationsRead() = runGuardedWrite(
        dedupeKey = "mark_all_notif_read",
    ) {
        repo.markAllNotificationsRead()
    }

    fun addReminderNotification(title: String, message: String) = runGuardedWrite(
        dedupeKey = "add_notif:${title.trim()}:${message.trim()}",
    ) {
        repo.addNotification(NotificationItemEntity(title = title, message = message))
    }

    // --- AI Chat Assistant ---
    fun sendAssistantMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            if (_aiBusy.value) return@launch
            _aiBusy.value = true
            try {
                repo.addChatMessage(ChatMessageEntity(isUser = true, text = clean))
                val reply = aiRepo.processAssistantMessage(clean, snapshot())
                repo.addChatMessage(ChatMessageEntity(isUser = false, text = reply.message))
                _pendingAiAction.value = reply.pendingAction
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _lastError.value = e.localizedMessage
            } finally {
                _aiBusy.value = false
            }
        }
    }

    fun confirmPendingAiAction() = viewModelScope.launch {
        if (!writeMutex.tryLock()) return@launch
        try {
            val action = _pendingAiAction.value ?: return@launch
            val isAr = uiState.value.prefs.language == AppLanguage.ARABIC
            _isSaving.value = true
            _lastError.value = null
            when (action) {
                is PendingAiAction.AddExpense -> repo.saveTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE.name,
                        amount = action.amount,
                        category = action.category.name,
                        description = action.description,
                        dateMillis = action.dateMillis,
                    )
                )
                is PendingAiAction.AddIncome -> repo.saveTransaction(
                    TransactionEntity(
                        type = TransactionType.INCOME.name,
                        amount = action.amount,
                        category = action.category.name,
                        description = action.description,
                        dateMillis = action.dateMillis,
                    )
                )
                is PendingAiAction.AddBill -> {
                    repo.saveBill(
                        BillEntity(
                            title = action.title,
                            category = action.category.name,
                            amount = action.amount,
                            dueDateMillis = action.dueDateMillis,
                        )
                    )
                    repo.addNotification(
                        NotificationItemEntity(
                            title = action.title,
                            message = if (isAr) "تم ضبط تذكير لدفع ${action.title}" else "Reminder set for ${action.title}",
                            category = CalendarEventType.BILL.name,
                            triggerMillis = action.dueDateMillis,
                        )
                    )
                }
                is PendingAiAction.AddTask -> repo.saveTask(
                    TaskEntity(
                        title = action.title,
                        priority = action.priority.name,
                        status = TaskStatus.PENDING.name,
                        category = action.category.name,
                        dueDateMillis = action.dueDateMillis,
                    )
                )
                is PendingAiAction.AddExamOrHomework -> repo.saveHomeworkExam(
                    HomeworkExamEntity(
                        childId = action.childId,
                        type = action.type.name,
                        subject = action.subject,
                        title = action.title,
                        dueDateMillis = action.dueDateMillis,
                    )
                )
            }
            _pendingAiAction.value = null
            repo.addChatMessage(
                ChatMessageEntity(
                    isUser = false,
                    text = if (isAr) "✅ تم الحفظ بنجاح في قاعدة البيانات المحلية:\n${action.confirmationSummary}"
                    else "✅ Saved successfully to local database:\n${action.confirmationSummary}",
                    actionTag = "SAVED",
                )
            )
            _statusMessage.value = if (isAr) "تم حفظ السجل بنجاح" else "Record saved successfully"
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val isAr = uiState.value.prefs.language == AppLanguage.ARABIC
            val errMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: if (isAr) "تعذر تنفيذ العملية" else "Operation failed"
            _lastError.value = errMsg
            _statusMessage.value = errMsg
            runCatching {
                repo.addChatMessage(
                    ChatMessageEntity(
                        isUser = false,
                        text = if (isAr) "❌ تعذر الحفظ: $errMsg" else "❌ Save failed: $errMsg",
                    )
                )
            }
        } finally {
            _isSaving.value = false
            writeMutex.unlock()
        }
    }

    fun dismissPendingAiAction() {
        val isAr = uiState.value.prefs.language == AppLanguage.ARABIC
        _pendingAiAction.value = null
        viewModelScope.launch {
            repo.addChatMessage(
                ChatMessageEntity(
                    isUser = false,
                    text = if (isAr) "تم إلغاء الإجراء ولم يتم حفظ أي بيانات." else "Action cancelled; nothing was saved.",
                )
            )
        }
    }

    fun clearChatHistory() = runGuardedWrite(dedupeKey = "clear_chat") {
        _pendingAiAction.value = null
        repo.clearChat()
    }

    // --- Smart Daily Planner ---
    fun buildDailyPlan(targetDayMillis: Long = System.currentTimeMillis()) = viewModelScope.launch {
        _dailyPlanSlots.value = aiRepo.generateDailyPlan(snapshot(), targetDayMillis)
    }

    fun removePlanSlot(slotId: String) {
        _dailyPlanSlots.value = _dailyPlanSlots.value.filterNot { it.id == slotId }
    }

    fun shiftPlanSlotHour(slotId: String, deltaHours: Int) {
        _dailyPlanSlots.value = _dailyPlanSlots.value.map { slot ->
            if (slot.id == slotId && !slot.isLockedEvent) {
                slot.copy(startHour = (slot.startHour + deltaHours).coerceIn(6, 23))
            } else slot
        }.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    fun acceptDailyPlan(targetDayMillis: Long) = runGuardedWrite(
        dedupeKey = "accept_plan:$targetDayMillis:${_dailyPlanSlots.value.size}",
        dedupeWindowMs = 1500L,
        successAr = "تم اعتماد الخطة اليومية وإضافتها إلى التقويم الموحد",
        successEn = "Daily plan accepted and added to Unified Calendar",
    ) {
        val tz = uiState.value.prefs.timeZoneId
        val slotsToSave = _dailyPlanSlots.value.filter { !it.isLockedEvent && it.id != "empty_tip" }
        slotsToSave.forEach { slot ->
            val start = DateTimeUtils.setHourMinute(targetDayMillis, slot.startHour, slot.startMinute, tz)
            val end = start + slot.durationMinutes * 60_000L
            repo.saveCalendarEvent(
                CalendarEventEntity(
                    title = slot.title,
                    description = slot.sourceLabel,
                    eventType = CalendarEventType.PERSONAL.name,
                    startMillis = start,
                    endMillis = end,
                )
            )
        }
    }

    // --- Settings / Preferences ---
    fun updateUserProfile(name: String, email: String) = runGuardedWrite(
        dedupeKey = "save_profile:${name.trim()}:${email.trim()}",
        successAr = "تم حفظ الملف الشخصي",
        successEn = "Profile saved",
    ) {
        if (name.isBlank()) {
            val isAr = uiState.value.prefs.language == AppLanguage.ARABIC
            throw IllegalArgumentException(if (isAr) "يرجى إدخال الاسم" else "Please enter your name")
        }
        prefsRepo.updateUserProfile(name, email)
    }

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { prefsRepo.setLanguage(lang) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setCurrency(currency: String) = viewModelScope.launch { prefsRepo.setCurrency(currency) }
    fun setTimeZone(tz: String) = viewModelScope.launch { prefsRepo.setTimeZone(tz) }
    fun setNotificationPrefs(morning: Boolean, evening: Boolean, enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setNotificationSettings(morning, evening, enabled)
    }

    class Factory(
        private val repo: HayatiRepository,
        private val prefsRepo: UserPreferencesRepository,
        private val aiRepo: AiRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repo, prefsRepo, aiRepo) as T
    }
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
