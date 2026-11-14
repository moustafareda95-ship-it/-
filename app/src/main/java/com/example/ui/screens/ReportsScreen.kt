package com.example.ui.screens

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.local.AppLanguage
import com.example.data.local.TaskStatus
import com.example.ui.HayatiUiState
import com.example.ui.components.HayatiCard
import com.example.ui.components.SpendingDonutChart
import com.example.ui.theme.EventBillColor
import com.example.ui.theme.EventFamilyColor
import com.example.ui.theme.EventHomeworkColor
import com.example.ui.theme.EventWorkColor
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import java.io.File
import java.io.FileOutputStream

@Composable
fun ReportsScreen(
    state: HayatiUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isAr = state.prefs.language == AppLanguage.ARABIC
    val tz = state.prefs.timeZoneId
    val currency = state.prefs.currency
    val fin = state.financeSummary

    var exportStatus by remember { mutableStateOf<String?>(null) }

    val totalTasks = state.tasks.size
    val completedTasks = state.tasks.count { it.status == TaskStatus.COMPLETED.name }
    val completionPct = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

    val chartColors = listOf(TurquoisePrimary, GoldAccent, EventWorkColor, EventHomeworkColor, EventBillColor, EventFamilyColor)
    val slices = remember(fin.categoryTotals, isAr) {
        fin.categoryTotals.entries.sortedByDescending { it.value }.mapIndexed { idx, (cat, v) ->
            Triple(cat.label(isAr), v, chartColors[idx % chartColors.size])
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("reports_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isAr) "التقارير والتحليلات" else "Reports & Analytics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            exportStatus = exportPdfReport(context, state, isAr, tz, currency)
                        },
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAr) "تصدير PDF" else "Export PDF")
                    }
                }
                if (exportStatus != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = TurquoisePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().testTag("report_export_status"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                exportStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f).testTag("report_export_status_text"),
                            )
                            IconButton(onClick = { exportStatus = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = if (isAr) "إغلاق" else "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Productivity Card
        item {
            HayatiCard {
                Text(
                    text = if (isAr) "معدل إنجاز المهام" else "Task Completion Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(if (isAr) "$completedTasks من $totalTasks مهمة" else "$completedTasks of $totalTasks tasks")
                    Text("$completionPct%", fontWeight = FontWeight.Bold, color = TurquoisePrimary)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = TurquoisePrimary,
                )
            }
        }

        // Financial Performance
        item {
            HayatiCard {
                Text(
                    text = if (isAr) "الأداء المالي الشهري" else "Monthly Financial Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                SummaryRow(if (isAr) "إجمالي الدخل" else "Total Income", DateTimeUtils.formatCurrency(fin.totalIncome, currency, isAr), StatusSuccess)
                SummaryRow(if (isAr) "إجمالي المصروفات" else "Total Expenses", DateTimeUtils.formatCurrency(fin.totalExpenses, currency, isAr), StatusError)
                SummaryRow(
                    if (isAr) "صافي الرصيد" else "Net Balance",
                    DateTimeUtils.formatCurrency(fin.remainingBalance, currency, isAr),
                    if (fin.remainingBalance >= 0) TurquoisePrimary else StatusError,
                )
                Spacer(Modifier.height(12.dp))
                SpendingDonutChart(
                    slices = slices,
                    centerLabel = if (isAr) "المصروفات" else "Spent",
                    centerValue = DateTimeUtils.formatCurrency(fin.totalExpenses, currency, isAr),
                )
                if (slices.isNotEmpty() && fin.totalExpenses > 0.0) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slices.forEach { (label, amount, color) ->
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

        // Children Progress Summary
        item {
            HayatiCard {
                Text(
                    text = if (isAr) "التقدم التعليمي للأبناء" else "Children's Education Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                if (state.children.isEmpty()) {
                    Text(if (isAr) "لا يوجد أبناء مسجلون" else "No children profiles yet", style = MaterialTheme.typography.bodySmall)
                } else {
                    state.children.forEach { c ->
                        val items = state.homeworkExams.filter { it.childId == c.id }
                        val done = items.count { it.isCompleted }
                        SummaryRow("${c.name} (${c.gradeStage})", "$done / ${items.size}", TurquoisePrimary)
                    }
                }
            }
        }

        // Upcoming Obligations
        item {
            val unpaidBills = state.bills.filter { !it.isPaid }
            val activeDebts = state.debts.filter { !it.isCompleted }
            HayatiCard {
                Text(
                    text = if (isAr) "الالتزامات القادمة" else "Upcoming Obligations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                SummaryRow(
                    if (isAr) "فواتير غير مدفوعة (${unpaidBills.size})" else "Unpaid Bills (${unpaidBills.size})",
                    DateTimeUtils.formatCurrency(unpaidBills.sumOf { it.amount }, currency, isAr),
                    GoldAccent,
                )
                SummaryRow(
                    if (isAr) "أقساط وديون متبقية (${activeDebts.size})" else "Remaining Debts (${activeDebts.size})",
                    DateTimeUtils.formatCurrency(activeDebts.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0.0) }, currency, isAr),
                    StatusError,
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

private fun exportPdfReport(
    context: android.content.Context,
    state: HayatiUiState,
    isAr: Boolean,
    tz: String,
    currency: String,
): String {
    val doc = PdfDocument()
    return try {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { textSize = 14f }
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }

        var y = 60f
        canvas.drawText("HAYATI AI - Monthly Summary Report", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("User: ${state.prefs.userName} | Date: ${DateTimeUtils.formatDateNumeric(System.currentTimeMillis(), tz)}", 40f, y, paint)
        y += 30f
        canvas.drawText("Total Income: ${DateTimeUtils.formatCurrency(state.financeSummary.totalIncome, currency, false)}", 40f, y, paint)
        y += 22f
        canvas.drawText("Total Expenses: ${DateTimeUtils.formatCurrency(state.financeSummary.totalExpenses, currency, false)}", 40f, y, paint)
        y += 22f
        canvas.drawText("Remaining Balance: ${DateTimeUtils.formatCurrency(state.financeSummary.remainingBalance, currency, false)}", 40f, y, paint)
        y += 30f
        val done = state.tasks.count { it.status == TaskStatus.COMPLETED.name }
        canvas.drawText("Tasks Completed: $done / ${state.tasks.size}", 40f, y, paint)
        y += 22f
        canvas.drawText("Unpaid Bills: ${state.bills.count { !it.isPaid }}", 40f, y, paint)
        y += 22f
        canvas.drawText("Children Profiles: ${state.children.size}", 40f, y, paint)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(dir, "hayati_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { doc.writeTo(it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, if (isAr) "مشاركة تقرير PDF" else "Share PDF Report").apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching { context.startActivity(chooser) }
        if (isAr) "تم إنشاء التقرير: ${pdfFile.name}" else "PDF generated: ${pdfFile.name}"
    } catch (e: Exception) {
        if (isAr) "تعذر تصدير PDF: ${e.localizedMessage}" else "PDF export failed: ${e.localizedMessage}"
    } finally {
        runCatching { doc.close() }
    }
}
