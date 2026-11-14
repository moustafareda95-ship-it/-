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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppLanguage
import com.example.data.local.BillCategory
import com.example.data.local.BillEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.HomeEventEntity
import com.example.data.local.HomeEventType
import com.example.data.local.ShoppingItemEntity
import com.example.data.local.TaskCategory
import com.example.data.local.TaskStatus
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.BillFormDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HayatiCard
import com.example.ui.components.StatusBadge
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
fun FamilyHomeScreen(
    state: HayatiUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val currency = state.prefs.currency
    val now = System.currentTimeMillis()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBillDialog by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var showShoppingDialog by remember { mutableStateOf(false) }
    var showHomeEventDialog by remember { mutableStateOf(false) }
    var showMemberDialog by remember { mutableStateOf(false) }

    val homeTasks = remember(state.tasks) {
        state.tasks.filter { it.category == TaskCategory.HOME.name }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showBillDialog = true
                        1 -> showShoppingDialog = true
                        2 -> showHomeEventDialog = true
                        3 -> showMemberDialog = true
                    }
                },
                containerColor = TurquoisePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("family_add_fab"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = s.add)
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val tabs = if (isAr) {
                listOf("الفواتير", "التسوق", "المنزل والمناسبات", "أفراد الأسرة")
            } else {
                listOf("Bills", "Shopping", "Home & Events", "Family")
            }
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("family_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    // Bills
                    0 -> {
                        val unpaidSum = state.bills.filter { !it.isPaid }.sumOf { it.amount }
                        val unpaidCount = state.bills.count { !it.isPaid }
                        item {
                            HayatiCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            if (isAr) "إجمالي الفواتير غير المدفوعة" else "Unpaid Bills Total",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            if (isAr) "$unpaidCount فاتورة معلّقة" else "$unpaidCount pending bill(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = DateTimeUtils.formatCurrency(unpaidSum, currency, isAr),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unpaidSum > 0) StatusWarning else StatusSuccess,
                                    )
                                }
                            }
                        }
                        if (state.bills.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    title = s.noRecordsYet,
                                    subtitle = if (isAr) "أضف فواتير الكهرباء والمياه والإنترنت والهاتف" else "Add electricity, water, gas, internet, or phone bills",
                                    actionLabel = s.addBill,
                                    onAction = { showBillDialog = true },
                                )
                            }
                        } else {
                            items(state.bills, key = { it.id }) { bill ->
                                val cat = runCatching { BillCategory.valueOf(bill.category) }.getOrElse { BillCategory.OTHER }
                                val isOverdueBill = DateTimeUtils.isOverdue(bill.dueDateMillis, bill.isPaid, now, tz)
                                val accent = when {
                                    bill.isPaid -> StatusSuccess
                                    isOverdueBill -> StatusError
                                    else -> StatusWarning
                                }
                                HayatiCard(
                                    borderColor = if (isOverdueBill) StatusError.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(44.dp)
                                                .clip(CircleShape)
                                                .background(accent),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(bill.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                if (isOverdueBill) {
                                                    StatusBadge(
                                                        text = if (isAr) "متأخرة" else "Overdue",
                                                        containerColor = StatusErrorSoft,
                                                        contentColor = StatusError,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${cat.label(isAr)} • ${DateTimeUtils.formatShortDate(bill.dueDateMillis, isAr, tz)} • ${DateTimeUtils.formatCurrency(bill.amount, currency, isAr)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isOverdueBill) StatusError else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = { viewModel.toggleBillPaid(bill) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                if (bill.isPaid) (if (isAr) "مدفوعة" else "Paid")
                                                else (if (isAr) "دفع" else "Pay"),
                                                color = if (bill.isPaid) StatusSuccess else TurquoisePrimary,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                        IconButton(onClick = { editingBill = bill }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = s.edit, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteBill(bill) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shopping List
                    1 -> {
                        val remainingEst = state.shoppingItems.filter { !it.isPurchased }.sumOf { it.estimatedPrice }
                        val purchasedCount = state.shoppingItems.count { it.isPurchased }
                        item {
                            HayatiCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(if (isAr) "التكلفة التقديرية المتبقية" else "Estimated Remaining Cost", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (state.shoppingItems.isNotEmpty()) {
                                            Text(
                                                text = if (isAr) "تم شراء $purchasedCount من ${state.shoppingItems.size}" else "$purchasedCount of ${state.shoppingItems.size} purchased",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Text(
                                        text = DateTimeUtils.formatCurrency(remainingEst, currency, isAr),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TurquoisePrimary,
                                    )
                                }
                            }
                        }
                        if (state.shoppingItems.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.ShoppingCart,
                                    title = s.noRecordsYet,
                                    subtitle = if (isAr) "أضف مشتريات البيت مع الكمية والسعر التقديري" else "Add shopping items with quantity and estimated price",
                                    actionLabel = if (isAr) "إضافة صنف" else "Add Item",
                                    onAction = { showShoppingDialog = true },
                                )
                            }
                        } else {
                            items(state.shoppingItems, key = { it.id }) { item ->
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = item.isPurchased,
                                            onCheckedChange = { viewModel.toggleShoppingPurchased(item) },
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                                            )
                                            Text(
                                                text = "${item.quantity} • ${DateTimeUtils.formatCurrency(item.estimatedPrice, currency, isAr)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteShoppingItem(item) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Home Events, Maintenance, and Home Tasks
                    2 -> {
                        if (homeTasks.isNotEmpty()) {
                            item {
                                Text(if (isAr) "مهام المنزل" else "Household Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(homeTasks, key = { "ht_${it.id}" }) { t ->
                                val done = t.status == TaskStatus.COMPLETED.name
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = done, onCheckedChange = { viewModel.toggleTaskCompleted(t) })
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(t.title, fontWeight = FontWeight.SemiBold, textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
                                            Text(DateTimeUtils.formatShortDate(t.dueDateMillis, isAr, tz), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Text(if (isAr) "المناسبات والصيانة المنزلية" else "Family Occasions & Maintenance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (state.homeEvents.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.HomeRepairService,
                                    title = s.noRecordsYet,
                                    subtitle = if (isAr) "أضف مواعيد الصيانة أو المناسبات العائلية" else "Add maintenance reminders or family occasions",
                                    actionLabel = if (isAr) "إضافة سجل" else "Add Record",
                                    onAction = { showHomeEventDialog = true },
                                )
                            }
                        } else {
                            items(state.homeEvents, key = { it.id }) { ev ->
                                val evType = runCatching { HomeEventType.valueOf(ev.type) }.getOrElse { HomeEventType.OCCASION }
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = ev.isCompleted, onCheckedChange = { viewModel.toggleHomeEventCompleted(ev) })
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ev.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${evType.label(isAr)} • ${DateTimeUtils.formatShortDate(ev.dateMillis, isAr, tz)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (ev.notes.isNotBlank()) {
                                                Text(ev.notes, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteHomeEvent(ev) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Family Members
                    3 -> {
                        if (state.familyMembers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.FamilyRestroom,
                                    title = s.noRecordsYet,
                                    subtitle = if (isAr) "أضف ملفات أفراد الأسرة" else "Add family member profiles",
                                    actionLabel = if (isAr) "إضافة فرد" else "Add Member",
                                    onAction = { showMemberDialog = true },
                                )
                            }
                        } else {
                            items(state.familyMembers, key = { it.id }) { member ->
                                HayatiCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(TurquoisePrimary.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Outlined.FamilyRestroom, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = member.relation + if (member.phoneOrNote.isNotBlank()) " • ${member.phoneOrNote}" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteFamilyMember(member) }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = s.delete, tint = StatusError, modifier = Modifier.size(18.dp))
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
    }

    if (showBillDialog) {
        BillFormDialog(
            isArabic = isAr,
            tzId = tz,
            currency = currency,
            onDismiss = { showBillDialog = false },
            onSave = {
                viewModel.saveBill(it)
                showBillDialog = false
            },
        )
    }

    editingBill?.let { bill ->
        BillFormDialog(
            initial = bill,
            isArabic = isAr,
            tzId = tz,
            currency = currency,
            onDismiss = { editingBill = null },
            onSave = {
                viewModel.saveBill(it)
                editingBill = null
            },
        )
    }

    if (showShoppingDialog) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var priceText by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showShoppingDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
            title = { Text(if (isAr) "إضافة صنف للتسوق" else "Add Shopping Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; err = null },
                            label = { Text(if (isAr) "اسم الصنف *" else "Item Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shopping_name_input"),
                        )
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text(if (isAr) "الكمية" else "Quantity") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shopping_qty_input"),
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it; err = null },
                            label = { Text(if (isAr) "السعر التقديري ($currency)" else "Est. Price ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("shopping_price_input"),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !submitted && !state.isSaving,
                    onClick = {
                        val rawPrice = priceText.trim().replace(',', '.').replace('٫', '.')
                        val price = if (rawPrice.isEmpty()) 0.0 else rawPrice.toDoubleOrNull()
                        when {
                            name.isBlank() -> err = if (isAr) "أدخل اسم الصنف" else "Enter item name"
                            price == null || price.isNaN() || price.isInfinite() || price < 0.0 -> {
                                err = if (isAr) "أدخل سعراً تقديرياً صحيحاً (0 أو أكثر)" else "Enter valid price (>= 0)"
                            }
                            else -> {
                                submitted = true
                                viewModel.saveShoppingItem(
                                    ShoppingItemEntity(
                                        name = name.trim(),
                                        quantity = qty.trim().ifBlank { "1" },
                                        estimatedPrice = price,
                                    )
                                )
                                showShoppingDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_shopping_btn"),
                ) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { showShoppingDialog = false }) { Text(s.cancel) } },
        )
    }

    if (showHomeEventDialog) {
        var evType by remember { mutableStateOf(HomeEventType.OCCASION) }
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        var err by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showHomeEventDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
            title = { Text(if (isAr) "مناسبة أو صيانة" else "Occasion / Maintenance", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeEventType.entries.forEach { t ->
                                FilterChip(selected = evType == t, onClick = { evType = t }, label = { Text(t.label(isAr)) })
                            }
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; err = null },
                            label = { Text(if (isAr) "العنوان *" else "Title *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("home_event_title_input"),
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isAr) "ملاحظات" else "Notes") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DatePickerField(label = if (isAr) "التاريخ" else "Date", selectedMillis = dateMillis, onDateSelected = { dateMillis = it }, isArabic = isAr, tzId = tz)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !submitted && !state.isSaving,
                    onClick = {
                        if (title.isBlank()) {
                            err = if (isAr) "أدخل العنوان" else "Enter title"
                        } else {
                            submitted = true
                            viewModel.saveHomeEvent(HomeEventEntity(type = evType.name, title = title.trim(), dateMillis = dateMillis, notes = notes.trim()))
                            showHomeEventDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_home_event_btn"),
                ) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { showHomeEventDialog = false }) { Text(s.cancel) } },
        )
    }

    if (showMemberDialog) {
        var name by remember { mutableStateOf("") }
        var relation by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showMemberDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp),
            title = { Text(if (isAr) "إضافة فرد للأسرة" else "Add Family Member", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (err != null) Text(err!!, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; err = null },
                            label = { Text(if (isAr) "الاسم *" else "Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("member_name_input"),
                        )
                        OutlinedTextField(
                            value = relation,
                            onValueChange = { relation = it; err = null },
                            label = { Text(if (isAr) "صلة القرابة *" else "Relation *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("member_relation_input"),
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(if (isAr) "ملاحظة / هاتف" else "Note / Phone") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !submitted && !state.isSaving,
                    onClick = {
                        if (name.isBlank() || relation.isBlank()) {
                            err = if (isAr) "أدخل الاسم وصلة القرابة" else "Enter name and relation"
                        } else {
                            submitted = true
                            viewModel.saveFamilyMember(FamilyMemberEntity(name = name.trim(), relation = relation.trim(), phoneOrNote = note.trim()))
                            showMemberDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_member_btn"),
                ) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { showMemberDialog = false }) { Text(s.cancel) } },
        )
    }
}
