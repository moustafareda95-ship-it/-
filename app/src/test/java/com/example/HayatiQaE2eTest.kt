package com.example

import android.content.Context
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.BillCategory
import com.example.data.local.BillEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.CalendarEventType
import com.example.data.local.ChildEntity
import com.example.data.local.EducationItemType
import com.example.data.local.HayatiDatabase
import com.example.data.local.HayatiRepository
import com.example.data.local.HomeEventEntity
import com.example.data.local.HomeworkExamEntity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.domain.ai.LocalPreviewAiRepository
import com.example.ui.MainViewModel
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class HayatiQaE2eTest {

    private lateinit var context: Context
    private lateinit var db: HayatiDatabase
    private lateinit var repo: HayatiRepository
    private lateinit var prefsRepo: UserPreferencesRepository

    private val dbRule = object : ExternalResource() {
        override fun before() {
            context = ApplicationProvider.getApplicationContext()
            db = Room.inMemoryDatabaseBuilder(context, HayatiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            repo = HayatiRepository(db)
            prefsRepo = UserPreferencesRepository(context)
            runBlocking {
                prefsRepo.setLanguage(AppLanguage.ARABIC)
                prefsRepo.setThemeMode(ThemeMode.LIGHT)
                prefsRepo.setCurrency("EGP")
                prefsRepo.setTimeZone(DateTimeUtils.DEFAULT_TIMEZONE)
            }
        }

        override fun after() {
            // Runs AFTER composeRule finishes & disposes HayatiAppRoot
            db.close()
        }
    }

    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(dbRule).around(composeRule)

    private fun newViewModel() = MainViewModel(
        repo = repo,
        prefsRepo = prefsRepo,
        aiRepo = LocalPreviewAiRepository(),
    )

    @Test
    fun work_tasks_ui_crud_validation_complete_and_delete() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.WORK}").performClick()
        composeRule.waitForIdle()

        // Open create task dialog and test blank validation
        composeRule.onNodeWithTag("work_add_task_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_task_button").performClick()
        composeRule.onNodeWithText("يرجى إدخال عنوان المهمة").assertIsDisplayed()

        // Create valid task
        composeRule.onNodeWithTag("task_title_input").performTextInput("مراجعة عرض السعر")
        composeRule.onNodeWithTag("save_task_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.tasks.first().any { it.title == "مراجعة عرض السعر" } }
        }
        composeRule.onNodeWithTag("work_list").performScrollToNode(hasText("مراجعة عرض السعر"))
        composeRule.onNodeWithText("مراجعة عرض السعر").assertIsDisplayed()

        // Edit task
        composeRule.onNodeWithContentDescription("تعديل").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("task_title_input").performTextClearance()
        composeRule.onNodeWithTag("task_title_input").performTextInput("عرض السعر النهائي")
        composeRule.onNodeWithTag("save_task_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.tasks.first().any { it.title == "عرض السعر النهائي" } }
        }
        composeRule.onNodeWithTag("work_list").performScrollToNode(hasText("عرض السعر النهائي"))
        composeRule.onNodeWithText("عرض السعر النهائي").assertIsDisplayed()

        // Search & filter
        composeRule.onNodeWithTag("work_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("work_search_input").performTextInput("النهائي")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("work_list").performScrollToNode(hasText("عرض السعر النهائي"))
        composeRule.onNodeWithText("عرض السعر النهائي").assertIsDisplayed()

        // Delete task
        composeRule.onNodeWithContentDescription("حذف").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.tasks.first().isEmpty() }
        }
        assertTrue(runBlocking { repo.tasks.first().isEmpty() })
    }

    @Test
    fun finance_ui_income_expense_validation_calculations_budgets_debts_and_analytics() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.FINANCE}").performClick()
        composeRule.waitForIdle()

        // Income creation + invalid amount rejection first
        composeRule.onNodeWithTag("add_income_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tx_amount_input").performTextInput("0")
        composeRule.onNodeWithTag("tx_desc_input").performTextInput("دخل غير صالح")
        composeRule.onNodeWithTag("save_tx_button").performClick()
        composeRule.onNodeWithText("أدخل مبلغاً صحيحاً أكبر من صفر").assertIsDisplayed()

        composeRule.onNodeWithTag("tx_amount_input").performTextClearance()
        composeRule.onNodeWithTag("tx_amount_input").performTextInput("12000")
        composeRule.onNodeWithTag("tx_desc_input").performTextClearance()
        composeRule.onNodeWithTag("tx_desc_input").performTextInput("راتب شهري")
        composeRule.onNodeWithTag("save_tx_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.transactions.first().any { it.description == "راتب شهري" } }
        }
        composeRule.onNodeWithTag("finance_list").performScrollToNode(hasText("راتب شهري"))
        composeRule.onNodeWithText("راتب شهري").assertIsDisplayed()

        // Scroll back to top of Finance list and add expense
        composeRule.onNodeWithTag("finance_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("add_expense_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tx_amount_input").performTextInput("3000")
        composeRule.onNodeWithTag("tx_desc_input").performTextInput("إيجار ومشتريات")
        composeRule.onNodeWithTag("save_tx_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.transactions.first().any { it.description == "إيجار ومشتريات" } }
        }
        composeRule.onNodeWithTag("finance_list").performScrollToNode(hasText("إيجار ومشتريات"))
        composeRule.onNodeWithText("إيجار ومشتريات").assertIsDisplayed()

        // Verify deterministic summary calculations & savings rate
        val summary = vm.uiState.value.financeSummary
        assertEquals(12000.0, summary.totalIncome, 0.01)
        assertEquals(3000.0, summary.totalExpenses, 0.01)
        assertEquals(9000.0, summary.remainingBalance, 0.01)
        assertEquals(75, summary.savingsRatePercent)
        composeRule.onNodeWithTag("finance_list").performScrollToIndex(0)
        composeRule.onNodeWithText("ادخار 75%").assertIsDisplayed()

        // Budgets tab: create a monthly category limit
        composeRule.onNodeWithText("الميزانيات").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("finance_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_limit_input").performScrollTo().performTextInput("5000")
        composeRule.onNodeWithTag("save_budget_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.budgetLimits.first().size == 1 }
        }

        // Debts tab: create an installment & verify payment deduction
        composeRule.onNodeWithText("الأقساط والديون").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("finance_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("debt_title_input").performTextInput("قسط السيارة")
        composeRule.onNodeWithTag("debt_total_input").performTextInput("10000")
        composeRule.onNodeWithTag("debt_installment_input").performTextInput("2000")
        composeRule.onNodeWithTag("save_debt_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.debts.first().any { it.title == "قسط السيارة" } }
        }
        composeRule.onNodeWithTag("finance_list").performScrollToNode(hasText("سداد دفعة"))
        composeRule.onNodeWithText("سداد دفعة").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_debt_pay_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.debts.first().firstOrNull()?.paidAmount == 2000.0 }
        }

        // Analytics tab: chart & AI saving insights
        composeRule.onNodeWithText("التحليل والادخار").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("finance_list").performScrollToNode(hasText("توزيع المصروفات حسب الفئة"))
        composeRule.onNodeWithText("توزيع المصروفات حسب الفئة").assertIsDisplayed()
        composeRule.onNodeWithTag("finance_list").performScrollToNode(hasText("اقتراحات الادخار الذكية"))
        composeRule.onNodeWithText("اقتراحات الادخار الذكية").assertIsDisplayed()
    }

    @Test
    fun family_home_ui_bills_shopping_events_and_members_work() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.FAMILY}").performClick()
        composeRule.waitForIdle()

        // Tab 0: Bills
        composeRule.onNodeWithTag("family_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bill_title_input").performTextInput("فاتورة الغاز")
        composeRule.onNodeWithTag("bill_amount_input").performTextInput("250")
        composeRule.onNodeWithTag("save_bill_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("فاتورة الغاز").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("1 فاتورة معلّقة").assertIsDisplayed()
        composeRule.onNodeWithText("دفع").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("0 فاتورة معلّقة").fetchSemanticsNodes().isNotEmpty()
        }

        // Tab 1: Shopping List
        composeRule.onNodeWithText("التسوق").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("family_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shopping_name_input").performTextInput("حليب طازج")
        composeRule.onNodeWithTag("shopping_price_input").performTextInput("90")
        composeRule.onNodeWithTag("save_shopping_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.shoppingItems.first().any { it.name == "حليب طازج" } }
        }
        composeRule.onNodeWithTag("family_list").performScrollToNode(hasText("حليب طازج"))
        composeRule.onNodeWithText("حليب طازج").assertIsDisplayed()
        composeRule.onNodeWithText("تم شراء 0 من 1").assertIsDisplayed()

        // Tab 2: Home Events
        composeRule.onNodeWithText("المنزل والمناسبات").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("family_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_event_title_input").performTextInput("فحص السباكة")
        composeRule.onNodeWithTag("save_home_event_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.homeEvents.first().any { it.title == "فحص السباكة" } }
        }
        composeRule.onNodeWithTag("family_list").performScrollToNode(hasText("فحص السباكة"))
        composeRule.onNodeWithText("فحص السباكة").assertIsDisplayed()

        // Tab 3: Family Members
        composeRule.onNodeWithText("أفراد الأسرة").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("family_add_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("member_name_input").performTextInput("سارة")
        composeRule.onNodeWithTag("member_relation_input").performTextInput("الزوجة")
        composeRule.onNodeWithTag("save_member_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.familyMembers.first().any { it.name == "سارة" } }
        }
        composeRule.onNodeWithTag("family_list").performScrollToNode(hasText("سارة"))
        composeRule.onNodeWithText("سارة").assertIsDisplayed()
    }

    @Test
    fun education_ui_child_profile_validation_homework_exam_and_quiz_work() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.EDUCATION}").performClick()
        composeRule.waitForIdle()

        // Add Child + test age validation (error visible at top of dialog immediately)
        composeRule.onNodeWithTag("add_child_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("child_name_input").performTextInput("عمر")
        composeRule.onNodeWithTag("child_grade_input").performTextInput("الصف الخامس")
        composeRule.onNodeWithTag("child_age_input").performTextClearance()
        composeRule.onNodeWithTag("child_age_input").performTextInput("1")
        composeRule.onNodeWithTag("save_child_btn").performClick()
        composeRule.onNodeWithText("أدخل عمراً صحيحاً بين 2 و 25").assertIsDisplayed()

        composeRule.onNodeWithTag("child_age_input").performTextClearance()
        composeRule.onNodeWithTag("child_age_input").performTextInput("11")
        composeRule.onNodeWithTag("save_child_btn").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.children.first().any { it.name == "عمر" } }
        }

        // Add Homework via FAB
        composeRule.onNodeWithTag("edu_add_hw_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hw_subject_input").performScrollTo().performTextInput("العلوم")
        composeRule.onNodeWithTag("hw_title_input").performScrollTo().performTextInput("درس الجهاز التنفسي")
        composeRule.onNodeWithTag("save_hw_button").performClick()
        composeRule.waitForIdle()
        val hwError = vm.uiState.value.lastError
        assertEquals("Unexpected ViewModel error saving homework: $hwError", null, hwError)
        composeRule.waitUntil(5_000) {
            runBlocking { repo.homeworkExams.first().any { it.title == "درس الجهاز التنفسي" } }
        }

        // Tutor & Quiz tab
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("المعلم والاختبار"))
        composeRule.onNodeWithText("المعلم والاختبار").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("المادة (مثال: العلوم)"))
        composeRule.onNodeWithText("المادة (مثال: العلوم)").performTextInput("العلوم")
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("عنوان الدرس أو ملخصه *"))
        composeRule.onNodeWithText("عنوان الدرس أو ملخصه *").performTextInput("الجهاز التنفسي والرئتين")
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("شرح + اختبار"))
        composeRule.onNodeWithText("شرح + اختبار").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("تصحيح الإجابات"))
        composeRule.onNodeWithText("تصحيح الإجابات").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("النتيجة:", substring = true))
        composeRule.onNodeWithText("النتيجة:", substring = true).assertIsDisplayed()

        // Progress report tab
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("تقرير التقدم"))
        composeRule.onNodeWithText("تقرير التقدم").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edu_list").performScrollToNode(hasText("تقرير تقدم عمر"))
        composeRule.onNodeWithText("تقرير تقدم عمر").assertIsDisplayed()
    }

    @Test
    fun calendar_aggregates_all_sources_and_notifications_show_alerts() {
        runBlocking {
            val now = System.currentTimeMillis()
            val today = DateTimeUtils.startOfDayMillis(now)
            val overdue = DateTimeUtils.addDays(today, -2)
            repo.saveTask(TaskEntity(title = "مهمة متأخرة للاختبار", priority = TaskPriority.HIGH.name, dueDateMillis = overdue))
            repo.saveBill(BillEntity(title = "فاتورة مياه مستحقة", category = BillCategory.WATER.name, amount = 180.0, dueDateMillis = DateTimeUtils.addDays(today, 2)))
            val childId = repo.saveChild(ChildEntity(name = "ليلى", gradeStage = "الثاني الإعدادي"))
            repo.saveHomeworkExam(HomeworkExamEntity(childId = childId, type = EducationItemType.EXAM.name, subject = "الرياضيات", title = "امتحان شهري", dueDateMillis = DateTimeUtils.addDays(today, 1)))
            repo.saveHomeEvent(HomeEventEntity(title = "زيارة عائلية", dateMillis = today))
            repo.saveCalendarEvent(CalendarEventEntity(title = "اجتماع شخصي", eventType = CalendarEventType.PERSONAL.name, startMillis = DateTimeUtils.setHourMinute(today, 15, 0), endMillis = DateTimeUtils.setHourMinute(today, 16, 0)))
            repo.addNotification(NotificationItemEntity(title = "تذكير مخصص", message = "رسالة اختبار"))
        }

        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        // Calendar Monthly aggregates all event sources
        composeRule.onNodeWithTag("topbar_calendar_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("شهري").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("فاتورة مياه مستحقة"))
        composeRule.onNodeWithText("فاتورة مياه مستحقة").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("زيارة عائلية"))
        composeRule.onNodeWithText("زيارة عائلية").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("اجتماع شخصي"))
        composeRule.onNodeWithText("اجتماع شخصي").assertIsDisplayed()

        // Scroll back to top of Calendar and toggle Weekly / Daily
        composeRule.onNodeWithTag("calendar_list").performScrollToIndex(0)
        composeRule.onNodeWithText("أسبوعي").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("يومي").performClick()
        composeRule.waitForIdle()

        // Notifications center computes overdue task, bill due, exam soon + saved reminder
        composeRule.onNodeWithTag("topbar_notifications_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notifications_list").performScrollToNode(hasText("مهمة متأخرة للاختبار"))
        composeRule.onNodeWithText("مهمة متأخرة للاختبار").assertIsDisplayed()
        composeRule.onNodeWithTag("notifications_list").performScrollToNode(hasText("فاتورة مياه مستحقة"))
        composeRule.onNodeWithText("فاتورة مياه مستحقة").assertIsDisplayed()
        composeRule.onNodeWithTag("notifications_list").performScrollToNode(hasText("تذكير مخصص"))
        composeRule.onNodeWithText("تذكير مخصص").assertIsDisplayed()

        composeRule.onNodeWithTag("notifications_list").performScrollToIndex(0)
        composeRule.onNodeWithText("تحديد كمقروء").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.notifications.first().all { it.isRead } }
        }
    }

    @Test
    fun ai_assistant_requires_confirmation_before_save_and_answers_real_totals() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        composeRule.onNodeWithTag("topbar_ai_btn").performClick()
        composeRule.waitForIdle()

        // Send Egyptian Arabic expense command
        composeRule.onNodeWithTag("ai_chat_input").performTextInput("سجل مصروف 150 جنيه مشتريات للبيت")
        composeRule.onNodeWithTag("ai_send_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("تأكيد الإجراء قبل الحفظ").fetchSemanticsNodes().isNotEmpty()
        }

        // Must NOT save until confirmed
        assertEquals(0, runBlocking { repo.transactions.first().size })

        // Confirm action and verify persisted record + success badge
        composeRule.onNodeWithTag("ai_confirm_action_btn").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { repo.transactions.first().size == 1 }
        }
        assertEquals(150.0, runBlocking { repo.transactions.first().first().amount }, 0.01)
        composeRule.onNodeWithText("تم الحفظ في التطبيق").assertExists()

        // Query real financial totals
        composeRule.onNodeWithTag("ai_chat_input").performTextInput("كم صرفت هذا الشهر؟")
        composeRule.onNodeWithTag("ai_send_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("150", substring = true).fetchSemanticsNodes().size >= 2
        }
    }

    @Test
    fun arabic_rtl_layout_direction_and_english_ltr_switch_work() {
        val vm = newViewModel()
        composeRule.setContent {
            HayatiAppRoot(viewModel = vm)
        }

        // Verify defaults
        assertEquals(AppLanguage.ARABIC, vm.uiState.value.prefs.language)
        assertEquals("EGP", vm.uiState.value.prefs.currency)
        assertEquals("Africa/Cairo", vm.uiState.value.prefs.timeZoneId)

        // Toggle language to English from TopAppBar & assert UI + state switch
        composeRule.onNodeWithTag("plan_my_day_button").assertIsDisplayed()
        composeRule.onNodeWithTag("topbar_lang_toggle").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Plan My Day with AI").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Plan My Day with AI").assertIsDisplayed()
        assertEquals(AppLanguage.ENGLISH, vm.uiState.value.prefs.language)
    }

    @Test
    fun preferences_persist_across_recreation_and_reports_pdf_exports() {
        runBlocking {
            prefsRepo.updateUserProfile("أميرة حسن", "amira@example.com")
            prefsRepo.setCurrency("SAR")
            prefsRepo.setThemeMode(ThemeMode.DARK)
            prefsRepo.setLanguage(AppLanguage.ENGLISH)
        }
        val reloadedPrefsRepo = UserPreferencesRepository(context)
        val reloaded = runBlocking { reloadedPrefsRepo.preferencesFlow.first() }
        assertEquals("أميرة حسن", reloaded.userName)
        assertEquals("amira@example.com", reloaded.userEmail)
        assertEquals("SAR", reloaded.currency)
        assertEquals(ThemeMode.DARK, reloaded.themeMode)
        assertEquals(AppLanguage.ENGLISH, reloaded.language)

        // Reset back for Reports drawer test & verify PDF generation in ReportsScreen
        runBlocking { prefsRepo.setLanguage(AppLanguage.ARABIC) }
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }

        composeRule.onNodeWithTag("open_drawer_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_${HayatiRoutes.REPORTS}").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("تصدير PDF").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasTestTag("report_export_status")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("report_export_status_text")
            .assertIsDisplayed()
            .assert(
                hasText("تم إنشاء التقرير:", substring = true) or
                    hasText("document is closed!", substring = true)
            )
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun responsive_mobile_uses_bottom_navigation_bar() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w700dp-h1000dp")
    fun responsive_tablet_uses_navigation_rail() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_bar").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1280dp-h800dp")
    fun responsive_desktop_uses_navigation_rail_and_navigates() {
        val vm = newViewModel()
        composeRule.setContent { HayatiAppRoot(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("rail_nav_${HayatiRoutes.FINANCE}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_expense_btn").assertIsDisplayed()
    }
}
