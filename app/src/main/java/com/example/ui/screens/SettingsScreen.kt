package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.ui.HayatiUiState
import com.example.ui.MainViewModel
import com.example.ui.components.CloudArchitectureStatusCard
import com.example.ui.components.HayatiCard
import com.example.ui.theme.StatusError
import com.example.util.S

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: HayatiUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val s = S()
    val isAr = state.prefs.language == AppLanguage.ARABIC

    var nameInput by remember(state.prefs.userName) { mutableStateOf(state.prefs.userName) }
    var emailInput by remember(state.prefs.userEmail) { mutableStateOf(state.prefs.userEmail) }
    var showConfirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = s.navSettings,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // User Profile
        item {
            HayatiCard {
                Text(if (isAr) "الملف الشخصي" else "User Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (isAr) "الاسم" else "Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(if (isAr) "البريد الإلكتروني (اختياري)" else "Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = !state.isSaving,
                    onClick = { viewModel.updateUserProfile(nameInput, emailInput) },
                ) {
                    Text(if (isAr) "حفظ الملف الشخصي" else "Save Profile")
                }
            }
        }

        // Language & Theme
        item {
            HayatiCard {
                Text(if (isAr) "اللغة والمظهر" else "Language & Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(if (isAr) "لغة التطبيق (RTL / LTR)" else "App Language", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = state.prefs.language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = { Text(lang.label(isAr)) },
                            modifier = Modifier.testTag("lang_${lang.code}"),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(if (isAr) "وضع الألوان" else "Theme Mode", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.prefs.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.label(isAr)) },
                        )
                    }
                }
            }
        }

        // Currency & Timezone
        item {
            HayatiCard {
                Text(if (isAr) "العملة والمنطقة الزمنية" else "Currency & Time Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(if (isAr) "العملة الافتراضية" else "Currency", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EGP", "SAR", "USD").forEach { c ->
                        FilterChip(
                            selected = state.prefs.currency == c,
                            onClick = { viewModel.setCurrency(c) },
                            label = { Text(c) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(if (isAr) "المنطقة الزمنية" else "Time Zone", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Africa/Cairo", "Asia/Riyadh", "UTC").forEach { tz ->
                        FilterChip(
                            selected = state.prefs.timeZoneId == tz,
                            onClick = { viewModel.setTimeZone(tz) },
                            label = { Text(tz) },
                        )
                    }
                }
            }
        }

        // Notifications Prefs
        item {
            HayatiCard {
                Text(if (isAr) "تفضيلات التذكير" else "Notification Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "ملخص صباحي يومي" else "Morning Daily Summary")
                    Switch(
                        checked = state.prefs.morningSummaryEnabled,
                        onCheckedChange = { viewModel.setNotificationPrefs(it, state.prefs.eveningSummaryEnabled, state.prefs.notificationsEnabled) },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "ملخص مسائي يومي" else "Evening Daily Summary")
                    Switch(
                        checked = state.prefs.eveningSummaryEnabled,
                        onCheckedChange = { viewModel.setNotificationPrefs(state.prefs.morningSummaryEnabled, it, state.prefs.notificationsEnabled) },
                    )
                }
            }
        }

        // Data Management & Cloud Status
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                CloudArchitectureStatusCard(ownerId = state.ownerId, isArabic = isAr)
                Spacer(Modifier.height(10.dp))
                HayatiCard {
                    Text(if (isAr) "إدارة البيانات المحلية" else "Local Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !state.isSaving,
                            onClick = { viewModel.loadSampleData() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (isAr) "تحميل بيانات تجريبية" else "Load Sample Data")
                        }
                        OutlinedButton(
                            enabled = !state.isSaving,
                            onClick = { showConfirmClear = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (isAr) "مسح جميع السجلات" else "Clear All Data")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text(if (isAr) "تأكيد مسح البيانات" else "Confirm Clear Data") },
            text = { Text(if (isAr) "هل تريد حذف جميع السجلات المحفوظة محلياً؟ لا يمكن التراجع عن هذا الإجراء." else "Delete all local records? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                ) { Text(s.delete) }
            },
            dismissButton = { TextButton(onClick = { showConfirmClear = false }) { Text(s.cancel) } },
        )
    }
}
