package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BillCategory
import com.example.data.local.BillEntity
import com.example.data.local.BudgetLimitEntity
import com.example.data.local.ChildEntity
import com.example.data.local.EducationItemType
import com.example.data.local.FinanceCategory
import com.example.data.local.HayatiDatabase
import com.example.data.local.HayatiRepository
import com.example.data.local.HomeworkExamEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.ai.AppContextSnapshot
import com.example.domain.ai.LocalPreviewAiRepository
import com.example.domain.ai.PendingAiAction
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HayatiAppFlowTest {

    private lateinit var db: HayatiDatabase
    private lateinit var repo: HayatiRepository
    private val aiRepo = LocalPreviewAiRepository()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HayatiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = HayatiRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `task CRUD and overdue detection work accurately`() = runBlocking {
        val now = System.currentTimeMillis()
        val yesterday = DateTimeUtils.addDays(now, -2)
        val tomorrow = DateTimeUtils.addDays(now, 1)

        val overdueId = repo.saveTask(
            TaskEntity(
                title = "تقرير متأخر",
                priority = TaskPriority.HIGH.name,
                dueDateMillis = yesterday,
            )
        )
        repo.saveTask(
            TaskEntity(
                title = "مهمة الغد",
                priority = TaskPriority.LOW.name,
                dueDateMillis = tomorrow,
            )
        )

        val tasks = repo.tasks.first()
        assertEquals(2, tasks.size)

        val overdueTask = tasks.first { it.id == overdueId }
        assertTrue(DateTimeUtils.isOverdue(overdueTask.dueDateMillis, false, now))

        repo.toggleTaskCompleted(overdueTask)
        val updated = repo.tasks.first().first { it.id == overdueId }
        assertEquals(TaskStatus.COMPLETED.name, updated.status)
        assertFalse(DateTimeUtils.isOverdue(updated.dueDateMillis, true, now))

        repo.deleteTask(updated)
        assertEquals(1, repo.tasks.first().size)
    }

    @Test
    fun `financial calculations compute income expenses balance and budgets accurately`() = runBlocking {
        val now = System.currentTimeMillis()
        val monthKey = DateTimeUtils.currentMonthKey(now)

        repo.saveTransaction(
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 10000.0,
                category = FinanceCategory.SALARY.name,
                description = "الراتب",
                dateMillis = now,
                isEstimate = false,
            )
        )
        repo.saveTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 1500.0,
                category = FinanceCategory.HOME.name,
                description = "مشتريات البيت",
                dateMillis = now,
                isEstimate = false,
            )
        )
        repo.saveTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 500.0,
                category = FinanceCategory.FOOD.name,
                description = "طعام تقديري",
                dateMillis = now,
                isEstimate = true,
            )
        )
        repo.saveBudgetLimit(
            BudgetLimitEntity(
                category = FinanceCategory.HOME.name,
                monthlyLimit = 4000.0,
                monthKey = monthKey,
            )
        )

        val txs = repo.transactions.first()
        val budgets = repo.budgetLimits.first()
        val summary = repo.computeFinanceSummary(txs, budgets, monthKey)

        assertEquals(10000.0, summary.totalIncome, 0.01)
        assertEquals(2000.0, summary.totalExpenses, 0.01)
        assertEquals(1500.0, summary.actualExpenses, 0.01)
        assertEquals(500.0, summary.estimatedExpenses, 0.01)
        assertEquals(8000.0, summary.remainingBalance, 0.01)
        assertEquals(80, summary.savingsRatePercent)
        assertEquals(50, summary.budgetUsedPercent)
    }

    @Test
    fun `bills and children homework exams persist and update properly`() = runBlocking {
        val now = System.currentTimeMillis()
        val billId = repo.saveBill(
            BillEntity(
                title = "فاتورة الإنترنت",
                category = BillCategory.INTERNET.name,
                amount = 410.0,
                dueDateMillis = DateTimeUtils.addDays(now, 3),
            )
        )
        val childId = repo.saveChild(
            ChildEntity(name = "عمر", gradeStage = "الصف الخامس", ageYears = 11, subjectsCsv = "علوم، رياضيات")
        )
        repo.saveHomeworkExam(
            HomeworkExamEntity(
                childId = childId,
                type = EducationItemType.EXAM.name,
                subject = "العلوم",
                title = "اختبار الشهر",
                dueDateMillis = DateTimeUtils.addDays(now, 2),
            )
        )

        val bill = repo.bills.first().first { it.id == billId }
        assertFalse(bill.isPaid)
        repo.toggleBillPaid(bill)
        assertTrue(repo.bills.first().first { it.id == billId }.isPaid)

        assertEquals(1, repo.children.first().size)
        assertEquals(1, repo.homeworkExams.first().size)
    }

    @Test
    fun `arabic AI assistant parses expense bill and overdue queries accurately`() = runBlocking {
        val now = System.currentTimeMillis()
        val childId = repo.saveChild(ChildEntity(name = "عمر", gradeStage = "الصف الخامس"))
        repo.saveTask(TaskEntity(title = "تقديم التقرير", dueDateMillis = DateTimeUtils.addDays(now, -2)))

        val snapshot = AppContextSnapshot(
            tasks = repo.tasks.first(),
            transactions = emptyList(),
            financeSummary = repo.computeFinanceSummary(emptyList(), emptyList()),
            bills = emptyList(),
            children = listOf(ChildEntity(id = childId, name = "عمر", gradeStage = "الصف الخامس")),
            homeworkExams = emptyList(),
            calendarEvents = emptyList(),
            currency = "EGP",
            timeZoneId = DateTimeUtils.DEFAULT_TIMEZONE,
            isArabic = true,
        )

        val expenseReply = aiRepo.processAssistantMessage("سجل مصروف 150 جنيه مشتريات للبيت", snapshot)
        assertTrue(expenseReply.pendingAction is PendingAiAction.AddExpense)
        val expAction = expenseReply.pendingAction as PendingAiAction.AddExpense
        assertEquals(150.0, expAction.amount, 0.01)

        val billReply = aiRepo.processAssistantMessage("فكرني بدفع فاتورة الإنترنت يوم 20", snapshot)
        assertTrue(billReply.pendingAction is PendingAiAction.AddBill)

        val overdueReply = aiRepo.processAssistantMessage("ما المهام المتأخرة عندي؟", snapshot)
        assertTrue(overdueReply.message.contains("تقديم التقرير"))

        val lessonResult = aiRepo.explainLessonAndGenerateQuiz(
            child = snapshot.children.first(),
            subject = "العلوم",
            lessonTopicOrContent = "الجهاز الدوري",
            hasAttachedImage = true,
            isArabic = true,
        )
        assertNotNull(lessonResult.imageNotice)
        assertTrue(lessonResult.questions.size >= 2)
    }
}
