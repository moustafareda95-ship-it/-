package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BillCategory
import com.example.data.local.BillEntity
import com.example.data.local.BudgetLimitEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CalendarEventType
import com.example.data.local.ChildEntity
import com.example.data.local.EducationItemType
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.FinanceCategory
import com.example.data.local.HayatiDatabase
import com.example.data.local.HayatiRepository
import com.example.data.local.HayatiValidationException
import com.example.data.local.HomeworkExamEntity
import com.example.data.local.LOCAL_OWNER_ID
import com.example.data.local.ShoppingItemEntity
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferencesRepository
import com.example.domain.ai.LocalPreviewAiRepository
import com.example.ui.MainViewModel
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HayatiStorageSecurityTest {

    private lateinit var context: Context
    private lateinit var inMemoryDb: HayatiDatabase
    private val diskDbName = "hayati_restart_persistence_test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(diskDbName)
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, HayatiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        inMemoryDb.close()
        context.deleteDatabase(diskDbName)
    }

    @Test
    fun `file backed database persists all 9 record types across close and reopen`() = runBlocking {
        val now = System.currentTimeMillis()
        val tomorrow = DateTimeUtils.addDays(now, 1)

        // Session 1: Create and save all 9 user record types to disk SQLite DB
        val db1 = Room.databaseBuilder(context, HayatiDatabase::class.java, diskDbName)
            .allowMainThreadQueries()
            .addMigrations(HayatiDatabase.MIGRATION_1_2)
            .build()
        val repo1 = HayatiRepository(db1)

        repo1.saveTask(
            TaskEntity(
                title = "مراجعة العقد السنوي",
                priority = TaskPriority.HIGH.name,
                category = TaskCategory.WORK.name,
                dueDateMillis = tomorrow,
            )
        )
        repo1.saveTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 425.5,
                category = FinanceCategory.FOOD.name,
                description = "مقاضي المنزل",
                dateMillis = now,
            )
        )
        repo1.saveTransaction(
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = 15000.0,
                category = FinanceCategory.SALARY.name,
                description = "راتب الشهر",
                dateMillis = now,
            )
        )
        repo1.saveBill(
            BillEntity(
                title = "فاتورة الكهرباء",
                category = BillCategory.ELECTRICITY.name,
                amount = 540.0,
                dueDateMillis = tomorrow,
            )
        )
        repo1.saveFamilyMember(
            FamilyMemberEntity(
                name = "سارة",
                relation = "الزوجة",
                phoneOrNote = "01000000000",
            )
        )
        val childId = repo1.saveChild(
            ChildEntity(
                name = "عمر",
                gradeStage = "الصف الخامس",
                ageYears = 11,
                subjectsCsv = "رياضيات، علوم",
            )
        )
        repo1.saveHomeworkExam(
            HomeworkExamEntity(
                childId = childId,
                type = EducationItemType.HOMEWORK.name,
                subject = "الرياضيات",
                title = "حل تمارين الكسور",
                dueDateMillis = tomorrow,
            )
        )
        repo1.saveHomeworkExam(
            HomeworkExamEntity(
                childId = childId,
                type = EducationItemType.EXAM.name,
                subject = "العلوم",
                title = "اختبار الشهر الأول",
                dueDateMillis = DateTimeUtils.addDays(now, 3),
                scoreEarned = 94.0,
            )
        )
        repo1.saveCalendarEvent(
            CalendarEventEntity(
                title = "موعد طبيب الأسنان",
                eventType = CalendarEventType.FAMILY.name,
                startMillis = tomorrow,
                endMillis = tomorrow + 3_600_000L,
            )
        )

        // Simulate app process termination / DB close
        db1.close()

        // Session 2: Reopen DB from disk and verify every record survived restart
        val db2 = Room.databaseBuilder(context, HayatiDatabase::class.java, diskDbName)
            .allowMainThreadQueries()
            .addMigrations(HayatiDatabase.MIGRATION_1_2)
            .build()
        try {
            val repo2 = HayatiRepository(db2)

            val tasks = repo2.tasks.first()
            assertEquals(1, tasks.size)
            assertEquals("مراجعة العقد السنوي", tasks.first().title)
            assertEquals(LOCAL_OWNER_ID, tasks.first().ownerId)

            val txs = repo2.transactions.first()
            assertEquals(2, txs.size)
            assertEquals(1, txs.count { it.type == TransactionType.EXPENSE.name && it.amount == 425.5 })
            assertEquals(1, txs.count { it.type == TransactionType.INCOME.name && it.amount == 15000.0 })

            val bills = repo2.bills.first()
            assertEquals(1, bills.size)
            assertEquals("فاتورة الكهرباء", bills.first().title)

            val members = repo2.familyMembers.first()
            assertEquals(1, members.size)
            assertEquals("سارة", members.first().name)

            val children = repo2.children.first()
            assertEquals(1, children.size)
            assertEquals("عمر", children.first().name)

            val eduItems = repo2.homeworkExams.first()
            assertEquals(2, eduItems.size)
            assertEquals(1, eduItems.count { it.type == EducationItemType.HOMEWORK.name })
            assertEquals(1, eduItems.count { it.type == EducationItemType.EXAM.name && it.scoreEarned == 94.0 })

            val events = repo2.calendarEvents.first()
            assertEquals(1, events.size)
            assertEquals("موعد طبيب الأسنان", events.first().title)
        } finally {
            db2.close()
        }
    }

    @Test
    fun `owner isolation prevents cross user reading updating or deleting private records`() = runBlocking {
        val now = System.currentTimeMillis()
        val repoA = HayatiRepository(inMemoryDb, activeOwnerId = "user_a")
        val repoB = HayatiRepository(inMemoryDb, activeOwnerId = "user_b")

        val taskId = repoA.saveTask(TaskEntity(title = "مهمة خاصة أ", dueDateMillis = now))
        val childAId = repoA.saveChild(ChildEntity(name = "طفل أ", gradeStage = "الأول"))
        repoA.saveTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = 200.0,
                category = FinanceCategory.HOME.name,
                description = "مصروف خاص أ",
                dateMillis = now,
            )
        )
        repoA.saveBill(BillEntity(title = "فاتورة أ", amount = 100.0, dueDateMillis = now))

        // User B must see 0 records from User A
        assertTrue(repoB.tasks.first().isEmpty())
        assertTrue(repoB.transactions.first().isEmpty())
        assertTrue(repoB.bills.first().isEmpty())
        assertTrue(repoB.children.first().isEmpty())

        val userATask = repoA.tasks.first().first { it.id == taskId }

        // User B cannot update, toggle, delete, or attach homework to User A's records
        assertValidationFails { repoB.deleteTask(userATask) }
        assertValidationFails { repoB.saveTask(userATask.copy(title = "تعديل غير مصرح")) }
        assertValidationFails {
            repoB.saveHomeworkExam(
                HomeworkExamEntity(
                    childId = childAId,
                    subject = "رياضيات",
                    title = "واجب غير مصرح",
                    dueDateMillis = now,
                )
            )
        }

        // User A's record remains intact
        assertEquals("مهمة خاصة أ", repoA.tasks.first().single().title)
    }

    @Test
    fun `repository validation rejects invalid inputs and deduplicates monthly category budgets`() = runBlocking {
        val now = System.currentTimeMillis()
        val repo = HayatiRepository(inMemoryDb)

        // Blank task title
        assertValidationFails {
            repo.saveTask(TaskEntity(title = "   ", dueDateMillis = now))
        }
        // Zero, negative, NaN, Infinite transaction amounts
        assertValidationFails {
            repo.saveTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE.name,
                    amount = 0.0,
                    category = FinanceCategory.FOOD.name,
                    description = "صفر",
                    dateMillis = now,
                )
            )
        }
        assertValidationFails {
            repo.saveTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE.name,
                    amount = -15.0,
                    category = FinanceCategory.FOOD.name,
                    description = "سالب",
                    dateMillis = now,
                )
            )
        }
        assertValidationFails {
            repo.saveTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE.name,
                    amount = Double.NaN,
                    category = FinanceCategory.FOOD.name,
                    description = "غير رقمي",
                    dateMillis = now,
                )
            )
        }
        // Mismatched expense/income category
        assertValidationFails {
            repo.saveTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE.name,
                    amount = 100.0,
                    category = FinanceCategory.SALARY.name,
                    description = "خطأ فئة",
                    dateMillis = now,
                )
            )
        }
        // Invalid bill amount
        assertValidationFails {
            repo.saveBill(BillEntity(title = "فاتورة", amount = 0.0, dueDateMillis = now))
        }
        // Orphan homework (missing child)
        assertValidationFails {
            repo.saveHomeworkExam(
                HomeworkExamEntity(childId = 999L, subject = "علوم", title = "واجب", dueDateMillis = now)
            )
        }
        // Invalid exam score > 100
        val childId = repo.saveChild(ChildEntity(name = "ليلى", gradeStage = "الثاني الإعدادي"))
        assertValidationFails {
            repo.saveHomeworkExam(
                HomeworkExamEntity(childId = childId, subject = "علوم", title = "امتحان", dueDateMillis = now, scoreEarned = 120.0)
            )
        }
        // Negative shopping price
        assertValidationFails {
            repo.saveShoppingItem(ShoppingItemEntity(name = "خبز", estimatedPrice = -10.0))
        }
        // Calendar event end before start
        assertValidationFails {
            repo.saveCalendarEvent(CalendarEventEntity(title = "موعد", startMillis = now, endMillis = now - 1000L))
        }

        // Budget deduplication: saving same category + month twice updates instead of duplicating
        val monthKey = DateTimeUtils.currentMonthKey(now)
        repo.saveBudgetLimit(BudgetLimitEntity(category = FinanceCategory.FOOD.name, monthlyLimit = 2000.0, monthKey = monthKey))
        repo.saveBudgetLimit(BudgetLimitEntity(category = FinanceCategory.FOOD.name, monthlyLimit = 3500.0, monthKey = monthKey))
        val budgets = repo.budgetLimits.first()
        assertEquals(1, budgets.size)
        assertEquals(3500.0, budgets.first().monthlyLimit, 0.01)
    }

    @Test
    fun `viewmodel exposes loading error states and prevents duplicate records from rapid clicks`() = runBlocking {
        val repo = HayatiRepository(inMemoryDb)
        val viewModel = MainViewModel(
            repo = repo,
            prefsRepo = UserPreferencesRepository(context),
            aiRepo = LocalPreviewAiRepository(),
        )
        // Initial state before collection starts should indicate loading
        assertTrue(viewModel.uiState.value.isLoading)

        val collectJob = launch { viewModel.uiState.collect {} }
        awaitCondition { !viewModel.uiState.value.isLoading }
        assertFalse(viewModel.uiState.value.isLoading)

        // Rapid repeated clicks on saveTask with identical task must only persist 1 record
        val due = System.currentTimeMillis() + 3_600_000L
        val task = TaskEntity(title = "مهمة متكررة النقر", dueDateMillis = due)
        viewModel.saveTask(task)
        viewModel.saveTask(task)
        viewModel.saveTask(task)
        awaitCondition { repo.tasks.first().size == 1 && !viewModel.uiState.value.isSaving }
        assertEquals(1, repo.tasks.first().size)
        assertNull(viewModel.uiState.value.lastError)

        // Invalid save must expose lastError instead of false success
        viewModel.saveTask(TaskEntity(title = "   ", dueDateMillis = due))
        awaitCondition { viewModel.uiState.value.lastError != null && !viewModel.uiState.value.isSaving }
        assertNotNull(viewModel.uiState.value.lastError)
        assertEquals(1, repo.tasks.first().size)

        viewModel.clearLastError()
        awaitCondition { viewModel.uiState.value.lastError == null }
        assertNull(viewModel.uiState.value.lastError)

        // Rapid repeated AI confirmation must save once and clear pending action
        viewModel.sendAssistantMessage("سجل مصروف 150 جنيه مشتريات للبيت")
        awaitCondition { viewModel.pendingAiAction.value != null && !viewModel.aiBusy.value }
        assertNotNull(viewModel.pendingAiAction.value)
        viewModel.confirmPendingAiAction()
        viewModel.confirmPendingAiAction()
        awaitCondition { repo.transactions.first().size == 1 && viewModel.pendingAiAction.value == null && !viewModel.uiState.value.isSaving }
        assertEquals(1, repo.transactions.first().size)
        assertNull(viewModel.pendingAiAction.value)

        collectJob.cancel()
    }

    @Test
    fun `migration 1 to 2 preserves existing v1 records with local ownerId`() {
        val migDbName = "hayati_v1_migration_test.db"
        context.deleteDatabase(migDbName)

        // Create a v1 schema manually without ownerId column and insert a row
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(migDbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `category` TEXT NOT NULL, `labels` TEXT NOT NULL, `dueDateMillis` INTEGER NOT NULL, `isMeeting` INTEGER NOT NULL, `meetingNotes` TEXT NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, `isEstimate` INTEGER NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `budget_limits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, `monthKey` TEXT NOT NULL, `isSample` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `debts_installments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `paidAmount` REAL NOT NULL, `monthlyInstallment` REAL NOT NULL, `dueDayOfMonth` INTEGER NOT NULL, `isDebtToOthers` INTEGER NOT NULL, `notes` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `family_members` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `relation` TEXT NOT NULL, `phoneOrNote` TEXT NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `shopping_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `listName` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` TEXT NOT NULL, `estimatedPrice` REAL NOT NULL, `isPurchased` INTEGER NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `dueDateMillis` INTEGER NOT NULL, `isPaid` INTEGER NOT NULL, `isRecurring` INTEGER NOT NULL, `notes` TEXT NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `home_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, `notes` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `children` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `gradeStage` TEXT NOT NULL, `ageYears` INTEGER NOT NULL, `subjectsCsv` TEXT NOT NULL, `weeklySchedule` TEXT NOT NULL, `notes` TEXT NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `homework_exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `childId` INTEGER NOT NULL, `type` TEXT NOT NULL, `subject` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `dueDateMillis` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `scoreEarned` REAL, `maxScore` REAL NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `eventType` TEXT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL, `isSample` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `category` TEXT NOT NULL, `triggerMillis` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isUser` INTEGER NOT NULL, `text` TEXT NOT NULL, `actionTag` TEXT NOT NULL, `timestampMillis` INTEGER NOT NULL)")
                    db.execSQL("INSERT INTO tasks (title, description, priority, status, category, labels, dueDateMillis, isMeeting, meetingNotes, isSample, createdAt, updatedAt) VALUES ('مهمة من الإصدار الأول', '', 'HIGH', 'PENDING', 'WORK', '', 1700000000000, 0, '', 0, 1700000000000, 1700000000000)")
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()
        helper.close()

        // Open with Room v2 + MIGRATION_1_2 and confirm old row migrated cleanly
        val migratedDb = Room.databaseBuilder(context, HayatiDatabase::class.java, migDbName)
            .allowMainThreadQueries()
            .addMigrations(HayatiDatabase.MIGRATION_1_2)
            .build()
        try {
            runBlocking {
                val repo = HayatiRepository(migratedDb)
                val tasks = repo.tasks.first()
                assertEquals(1, tasks.size)
                assertEquals("مهمة من الإصدار الأول", tasks.first().title)
                assertEquals(LOCAL_OWNER_ID, tasks.first().ownerId)
            }
        } finally {
            migratedDb.close()
            context.deleteDatabase(migDbName)
        }
    }

    private suspend fun awaitCondition(timeoutMs: Long = 3_000L, predicate: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            delay(25)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private suspend fun assertValidationFails(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected HayatiValidationException to be thrown")
        } catch (_: HayatiValidationException) {
            // Expected
        }
    }
}
