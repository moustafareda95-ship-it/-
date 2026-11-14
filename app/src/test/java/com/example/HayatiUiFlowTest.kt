package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HayatiDatabase
import com.example.data.local.HayatiRepository
import com.example.data.local.UserPreferencesRepository
import com.example.domain.ai.LocalPreviewAiRepository
import com.example.ui.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HayatiUiFlowTest {

    private lateinit var db: HayatiDatabase

    private val dbRule = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db = Room.inMemoryDatabaseBuilder(context, HayatiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        override fun after() {
            db.close()
        }
    }

    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(dbRule).around(composeRule)

    @Test
    fun dashboard_navigation_and_language_toggle_work() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(
            repo = HayatiRepository(db),
            prefsRepo = UserPreferencesRepository(context),
            aiRepo = LocalPreviewAiRepository(),
        )

        composeRule.setContent {
            HayatiAppRoot(viewModel = viewModel)
        }

        // Dashboard renders Arabic default & key quick actions
        composeRule.onNodeWithTag("plan_my_day_button").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_task").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_expense").assertIsDisplayed()

        // Wait for initial DB load and load sample data
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("تحميل بيانات تجريبية للمعاينة").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("load_sample_data_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("بيانات تجريبية").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("بيانات تجريبية").assertExists()
        composeRule.onNodeWithTag("load_sample_data_btn").assertDoesNotExist()

        // Navigate across primary tabs and top bar destinations
        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.WORK}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.FINANCE}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.FAMILY}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.EDUCATION}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("topbar_calendar_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("topbar_ai_btn").performClick()
        composeRule.waitForIdle()

        // Return to Dashboard and toggle language to English
        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.DASHBOARD}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("topbar_lang_toggle").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Plan My Day with AI").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Plan My Day with AI").assertExists()
        composeRule.onNodeWithText("AR").assertIsDisplayed()
    }

    @Test
    fun theme_toggle_calendar_grid_and_work_filter_reset_work() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(
            repo = HayatiRepository(db),
            prefsRepo = UserPreferencesRepository(context),
            aiRepo = LocalPreviewAiRepository(),
        )

        composeRule.setContent {
            HayatiAppRoot(viewModel = viewModel)
        }

        // Toggle dark/light theme from TopAppBar
        composeRule.onNodeWithTag("topbar_theme_toggle").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Open Calendar -> Monthly tab and verify aligned weekday headers
        composeRule.onNodeWithTag("topbar_calendar_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("شهري").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("س").assertIsDisplayed()
        composeRule.onNodeWithText("ح").assertIsDisplayed()

        // Navigate to Work -> enter unmatched query -> verify filter reset button restores normal state
        composeRule.onNodeWithTag("bottom_nav_${HayatiRoutes.WORK}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("work_search_input").performTextInput("zzz_unmatched")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("إعادة ضبط الفلاتر").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("إعادة ضبط الفلاتر").assertDoesNotExist()
    }
}
