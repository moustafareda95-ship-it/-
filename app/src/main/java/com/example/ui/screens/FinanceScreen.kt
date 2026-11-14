package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppLanguage
import com.example.data.local.BudgetLimitEntity
import com.example.data.local.DebtInstallmentEntity
import com.example.data.local.FinanceCategory
import com.example.data.local.PaymentMethod
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.AiPreviewBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.SpendingDonutChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionFormDialog
import com.example.ui.theme.EventBillColor
import com.example.ui.theme.EventFamilyColor
import com.example.ui.theme.EventHomeworkColor
import com.example.ui.theme.EventPersonalColor
import com.example.ui.theme.EventWorkColor
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorSoft
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessSoft
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningSoft
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.S

private enum class TxFilterMode { ALL, EXPENSES, INCOME, ESTIMATE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceScreen(
    state: HayatiUiState,
    savingTips: List<String>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val currency = state.prefs.currency
    val fin = state.financeSummary

    var selectedTab by remember { mutableIntStateOf(0) }
    var txFilter by remember { mutableStateOf(TxFilterMode.ALL) }
    var showTxDialog by remember { mutableStateOf(false) }
    var defaultTxType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<DebtInstallmentEntity?>(null) }

    val filteredTransactions = remember(state.transactions, txFilter) {
        when (txFilter) {
            TxFilterMode.ALL -> state.transactions
            TxFilterMode.EXPENSES -> state.transactions.filter { it.type == TransactionType.EXPENSE.name }
            TxFilterMode.INCOME -> state.transactions.filter { it.type == TransactionType.INCOME.name }
            TxFilterMode.ESTIMATE -> state.transactions.filter { it.isEstimate }
        }
    }

    val chartColors = listOf(TurquoisePrimary, GoldAccent, EventWorkColor, EventHomeworkColor, EventBillColor, EventPersonalColor, EventFamilyColor)
    val donutSlices = remember(fin.categoryTotals, isAr) {
        fin.categoryTotals.entries
            .sortedByDescending { it.value }
            .mapIndexed { idx, (cat, amount) ->
                Triple(cat.label(isAr), amount, chartColors[idx % chartColors.size])
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        1 -> showBudgetDialog = true
                        2 -> showDebtDialog = true
                        else -> {
                            defaultTxType = TransactionType.EXPENSE
                            showTxDialog = true
                        }
                    }
                },
                containerColor = TurquoisePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("finance_add_fab"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = s.add)
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val tabs = if (isAr) {
                listOf("المعاملات", "الميزانيات", "الأقساط والديون", "التحليل والادخار")
            } else {
                listOf("Transactions", "Budgets", "Debts", "Analytics & AI")
            }
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = {
                            selectedTab = idx
                            if (idx == 3) viewModel.refreshSavingTips()
                        },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("finance_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Summary Card always visible at top
                item {
                    HayatiCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(s.monthlyIncome, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    DateTimeUtils.formatCurrency(fin.totalIncome, currency, isAr),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess,
                                )
                            }
                            Column {
                                Text(s.monthlyExpenses, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    DateTimeUtils.formatCurrency(fin.totalExpenses, currency, isAr),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(s.remainingBudget, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    DateTimeUtils.formatCurrency(fin.remainingBalance, currency, isAr),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fin.remainingBalance >= 0) TurquoisePrimary else StatusError,
                                )
                            }
                        }
                        if (fin.totalIncome > 0.0) {
                            val expenseRatio = (fin.totalExpenses / fin.totalIncome).toFloat().coerceIn(0f, 1f)
                            val savingsPct = (((fin.totalIncome - fin.totalExpenses) / fin.totalIncome) * 100.0).toInt()
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { expenseRatio },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (fin.remainingBalance < 0) StatusError else TurquoisePrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (isAr) "نسبة الصرف من الدخل: ${(expenseRatio * 100).toInt()}%" else "Spent of income: ${(expenseRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                StatusBadge(
                                    text = if (savingsPct >= 0) {
                                        if (isAr) "ادخار $savingsPct%" else "Savings $savingsPct%"
                                    } else {
                                        if (isAr) "تجاوز الدخل" else "Over Income"
                                    },
                                    containerColor = if (savingsPct >= 0) StatusSuccessSoft else StatusErrorSoft,
                                    contentColor = if (savingsPct >= 0) StatusSuccess else StatusError,
                                )
                            }
                        }
                        if (fin.estimatedExpenses > 0.0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (isAr) {
                                    "تفصيل المصروفات: فعلي (${DateTimeUtils.formatCurrency(fin.actualExpenses, currency, true)}) • تقديري (${DateTimeUtils.formatCurrency(fin.estimatedExpenses, currency, true)})"
                                } else {
                                    "Breakdown: Actual (${DateTimeUtils.formatCurrency(fin.actualExpenses, currency, false)}) • Estimated (${DateTimeUtils.formatCurrency(fin.estimatedExpenses, currency, false)})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        defaultTxType = TransactionType.EXPENSE
                                        showTxDialog = true
                                    },
                                    modifier = Modifier.weight(1f).testTag("add_expense_btn"),
                                ) { Text(s.addExpense) }
                                FilledTonalButton(
                                    onClick = {
                                        defaultTxType = TransactionType.INCOME
                                        showTxDialog = true
                                    },
                                    modifier = Modifier.weight(1f).testTag("add_income_btn"),
                                ) { Text(s.addIncome) }
                            }
                        }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    TxFilterMode.ALL to (if (isAr) "الكل" else "All"),
                                    TxFilterMode.EXPENSES to (if (isAr) "المصروفات" else "Expenses"),
                                    TxFilterMode.INCOME to (if (isAr) "الدخل" else "Income"),
                                    TxFilterMode.ESTIMATE to (if (isAr) "تقديري" else "Estimated"),
                                ).forEach { (mode, label) ->
                                    FilterChip(
                                        selected = txFilter == mode,
                                        onClick = { txFilter = mode },
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                        if (filteredTransactions.isEmpty()) {
                            item {
                                val hasFilter = txFilter != TxFilterMode.ALL
                                EmptyStateCard(
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    title = if (hasFilter) {
                                        if (isAr) "لا توجد معاملات في هذا التصنيف" else "No transactions in this filter"
                                    } else {
                                        s.noRecordsYet
                                    },
                                    subtitle = if (hasFilter) {
                                        if (isAr) "اضغط لعرض جميع المعاملات المسجلة" else "Tap to view all recorded transactions"
                                    } else {
                                        if (isAr) "سجل الدخل والمصروفات لحساب الميزانية بدقة" else "Record income and expenses for accurate tracking"
                                    },
                                    actionLabel = if (hasFilter) (if (isAr) "عرض الكل" else "Show All") else null,
                                    onAction = if (hasFilter) ({ txFilter = TxFilterMode.ALL }) else null,
                                )
                            }
                        } else {
                            items(filteredTransactions, key = { it.id }) { tx ->
                                val isInc = tx.type == TransactionType.INCOME.name
                                val cat = runCatching { FinanceCategory.valueOf(tx.category) }.getOrElse { FinanceCategory.OTHER_EXPENSE }
                                val pm = runCatching { PaymentMethod.valueOf(tx.paymentMethod) }.getOrElse { PaymentMethod.CASH }
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isInc) StatusSuccess else StatusError),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(tx.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (tx.isEstimate) {
                                                    StatusBadge(
                                                        text = if (isAr) "تقديري" else "Estimate",
                                                        containerColor = StatusWarningSoft,
                                                        contentColor = StatusWarning,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${cat.label(isAr)} • ${pm.label(isAr)} • ${DateTimeUtils.formatShortDate(tx.dateMillis, isAr, tz)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = (if (isInc) "+" else "-") + DateTimeUtils.formatCurrency(tx.amount, currency, isAr),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInc) StatusSuccess else StatusError,
                                        )
                                        IconButton(onClick = { editingTx = tx }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = s.edit, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            OutlinedButton(onClick = { showBudgetDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (isAr) "تحديد حد إنفاق شهري لفئة" else "Set Monthly Category Limit")
                            }
                        }
                        if (state.budgetLimits.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    title = if (isAr) "لا توجد حدود ميزانية محددة" else "No category budgets set",
                                    subtitle = if (isAr) "ضع حداً شهرياً لكل فئة لمتابعة الصرف" else "Set spending limits per category",
                                )
                            }
                        } else {
                            items(state.budgetLimits, key = { it.id }) { limit ->
                                val cat = runCatching { FinanceCategory.valueOf(limit.category) }.getOrElse { FinanceCategory.OTHER_EXPENSE }
                                val spent = fin.categoryTotals[cat] ?: 0.0
                                val ratio = if (limit.monthlyLimit > 0) (spent / limit.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
                                val over = spent > limit.monthlyLimit
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat.label(isAr), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${DateTimeUtils.formatCurrency(spent, currency, isAr)} / ${DateTimeUtils.formatCurrency(limit.monthlyLimit, currency, isAr)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (over) StatusError else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        IconButton(onClick = { viewModel.deleteBudgetLimit(limit) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = if (over) StatusError else TurquoisePrimary,
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            OutlinedButton(onClick = { showDebtDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (isAr) "إضافة قسط أو التزام مالي" else "Add Debt or Installment")
                            }
                        }
                        if (state.debts.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    title = if (isAr) "لا توجد أقساط أو ديون" else "No debts or installments",
                                    subtitle = if (isAr) "تابع الأقساط الشهرية والمدفوعات المتبقية" else "Track monthly installments and balances",
                                )
                            }
                        } else {
                            items(state.debts, key = { it.id }) { debt ->
                                val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0.0)
                                val progress = if (debt.totalAmount > 0) (debt.paidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(debt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isAr) {
                                                    "المتبقي: ${DateTimeUtils.formatCurrency(remaining, currency, true)} • قسط شهري: ${DateTimeUtils.formatCurrency(debt.monthlyInstallment, currency, true)} (يوم ${debt.dueDayOfMonth})"
                                                } else {
                                                    "Remaining: ${DateTimeUtils.formatCurrency(remaining, currency, false)} • Monthly: ${DateTimeUtils.formatCurrency(debt.monthlyInstallment, currency, false)}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (!debt.isCompleted) {
                                            FilledTonalButton(onClick = { payingDebt = debt }) {
                                                Text(if (isAr) "سداد دفعة" else "Pay")
                                            }
                                        } else {
                                            StatusBadge(if (isAr) "مسدد" else "Paid", StatusSuccessSoft, StatusSuccess)
                                        }
                                        IconButton(onClick = { viewModel.deleteDebt(debt) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                        color = TurquoisePrimary,
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            HayatiCard {
                                Text(
                                    text = if (isAr) "توزيع المصروفات حسب الفئة" else "Spending by Category",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(12.dp))
                                SpendingDonutChart(
                                    slices = donutSlices,
                                    centerLabel = if (isAr) "الإجمالي" else "Total",
                                    centerValue = DateTimeUtils.formatCurrency(fin.totalExpenses, currency, isAr),
                                )
                                if (donutSlices.isNotEmpty() && fin.totalExpenses > 0.0) {
                                    Spacer(Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                    Spacer(Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        donutSlices.forEach { (label, amount, color) ->
                                            val pct = ((amount / fin.totalExpenses) * 100.0).toInt()
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(9.dp)
                                                                .clip(CircleShape)
                                                                .background(color),
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Text(
                                                        text = "${DateTimeUtils.formatCurrency(amount, currency, isAr)} ($pct%)",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = color,
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { (amount / fin.totalExpenses).toFloat().coerceIn(0f, 1f) },
                                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                                    color = color,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            HayatiCard(borderColor = TurquoisePrimary.copy(alpha = 0.45f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = TurquoisePrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isAr) "اقتراحات الادخار الذكية" else "AI Saving Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { viewModel.refreshSavingTips() }) {
                                        Text(if (isAr) "تحديث" else "Refresh")
                                    }
                                }
                                AiPreviewBanner()
                                Spacer(Modifier.height(8.dp))
                                savingTips.forEach { tip ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(TurquoisePrimary),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(tip, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showTxDialog) {
        TransactionFormDialog(
            defaultType = defaultTxType,
            isArabic = isAr,
            tzId = tz,
            currency = currency,
            onDismiss = { showTxDialog = false },
            onSave = {
                viewModel.saveTransaction(it)
                showTxDialog = false
            },
        )
    }

    editingTx?.let { tx ->
        TransactionFormDialog(
            initial = tx,
            isArabic = isAr,
            tzId = tz,
            currency = currency,
            onDismiss = { editingTx = null },
            onSave = {
                viewModel.saveTransaction(it)
                editingTx = null
            },
        )
    }

    if (showBudgetDialog) {
        var selectedCat by remember { mutableStateOf(FinanceCategory.HOME) }
        var limitText by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
            title = { Text(if (isAr) "حد الميزانية الشهري" else "Monthly Budget Limit", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FinanceCategory.expenseCategories.forEach { c ->
                                FilterChip(selected = selectedCat == c, onClick = { selectedCat = c }, label = { Text(c.label(isAr)) })
                            }
                        }
                        OutlinedTextField(
                            value = limitText,
                            onValueChange = { limitText = it; err = null },
                            label = { Text(if (isAr) "الحد الأقصى ($currency)" else "Limit ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !submitted && !state.isSaving,
                    onClick = {
                        val amt = limitText.trim().replace(',', '.').replace('٫', '.').toDoubleOrNull()
                        if (amt == null || amt.isNaN() || amt.isInfinite() || amt <= 0.0) {
                            err = if (isAr) "أدخل قيمة صحيحة أكبر من صفر" else "Enter valid limit > 0"
                        } else {
                            submitted = true
                            viewModel.saveBudgetLimit(
                                BudgetLimitEntity(
                                    category = selectedCat.name,
                                    monthlyLimit = amt,
                                    monthKey = DateTimeUtils.currentMonthKey(tzId = tz),
                                )
                            )
                            showBudgetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_budget_btn"),
                ) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text(s.cancel) } },
        )
    }

    if (showDebtDialog) {
        var title by remember { mutableStateOf("") }
        var totalText by remember { mutableStateOf("") }
        var installmentText by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDebtDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
            title = { Text(if (isAr) "قسط أو التزام جديد" else "New Debt / Installment", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; err = null },
                            label = { Text(if (isAr) "العنوان *" else "Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("debt_title_input"),
                        )
                        OutlinedTextField(
                            value = totalText,
                            onValueChange = { totalText = it; err = null },
                            label = { Text(if (isAr) "إجمالي المبلغ ($currency) *" else "Total Amount *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("debt_total_input"),
                        )
                        OutlinedTextField(
                            value = installmentText,
                            onValueChange = { installmentText = it; err = null },
                            label = { Text(if (isAr) "القسط الشهري ($currency)" else "Monthly Installment") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("debt_installment_input"),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !submitted && !state.isSaving,
                    onClick = {
                        val total = totalText.trim().replace(',', '.').replace('٫', '.').toDoubleOrNull()
                        val instRaw = installmentText.trim().replace(',', '.').replace('٫', '.')
                        val inst = if (instRaw.isEmpty()) 0.0 else instRaw.toDoubleOrNull()
                        when {
                            title.isBlank() || total == null || total.isNaN() || total.isInfinite() || total <= 0.0 -> {
                                err = if (isAr) "أدخل عنواناً ومبلغاً صحيحاً أكبر من صفر" else "Enter title and valid total > 0"
                            }
                            inst == null || inst.isNaN() || inst.isInfinite() || inst < 0.0 -> {
                                err = if (isAr) "أدخل قيمة قسط شهري صالحة (0 أو أكثر)" else "Enter valid monthly installment"
                            }
                            else -> {
                                submitted = true
                                viewModel.saveDebt(DebtInstallmentEntity(title = title.trim(), totalAmount = total, monthlyInstallment = inst))
                                showDebtDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_debt_btn"),
                ) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { showDebtDialog = false }) { Text(s.cancel) } },
        )
    }

    payingDebt?.let { debt ->
        var payText by remember { mutableStateOf(if (debt.monthlyInstallment > 0) debt.monthlyInstallment.toInt().toString() else "") }
        var err by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { payingDebt = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
            title = { Text(if (isAr) "تسجيل سداد دفعة" else "Record Payment") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = payText,
                        onValueChange = { payText = it; err = null },
                        label = { Text(if (isAr) "المبلغ المدفوع ($currency)" else "Amount Paid") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("debt_pay_input"),
                    )
                    if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    enabled = !submitted && !state.isSaving,
                    onClick = {
                        val amt = payText.trim().replace(',', '.').replace('٫', '.').toDoubleOrNull()
                        if (amt == null || amt.isNaN() || amt.isInfinite() || amt <= 0.0) {
                            err = if (isAr) "أدخل مبلغ سداد صحيحاً أكبر من صفر" else "Enter valid payment > 0"
                        } else {
                            submitted = true
                            viewModel.recordDebtPayment(debt, amt)
                            payingDebt = null
                        }
                    },
                    modifier = Modifier.testTag("save_debt_pay_btn"),
                ) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { payingDebt = null }) { Text(s.cancel) } },
        )
    }
}
