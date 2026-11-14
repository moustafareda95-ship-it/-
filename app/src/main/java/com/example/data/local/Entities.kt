package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

const val LOCAL_OWNER_ID = "local_device"

enum class AppLanguage(val code: String, val labelAr: String, val labelEn: String) {
    ARABIC("ar", "العربية", "Arabic"),
    ENGLISH("en", "English", "English");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class ThemeMode(val labelAr: String, val labelEn: String) {
    SYSTEM("تلقائي", "System"),
    LIGHT("فاتح", "Light"),
    DARK("داكن", "Dark");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class TaskPriority(val labelAr: String, val labelEn: String, val sortWeight: Int) {
    HIGH("عالية", "High", 0),
    MEDIUM("متوسطة", "Medium", 1),
    LOW("منخفضة", "Low", 2);

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class TaskStatus(val labelAr: String, val labelEn: String) {
    PENDING("قيد الانتظار", "Pending"),
    IN_PROGRESS("جاري التنفيذ", "In Progress"),
    COMPLETED("مكتملة", "Completed");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class TaskCategory(val labelAr: String, val labelEn: String) {
    WORK("العمل", "Work"),
    HOME("المنزل", "Home"),
    PERSONAL("شخصي", "Personal");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class TransactionType(val labelAr: String, val labelEn: String) {
    INCOME("دخل", "Income"),
    EXPENSE("مصروف", "Expense");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class FinanceCategory(val labelAr: String, val labelEn: String, val isIncome: Boolean) {
    // Expense categories
    HOME("البيت", "Home", false),
    FOOD("طعام ومقاضي", "Food", false),
    EDUCATION("التعليم", "Education", false),
    TRANSPORTATION("المواصلات", "Transportation", false),
    HEALTH("الصحة", "Health", false),
    BILLS("الفواتير", "Bills", false),
    SHOPPING("التسوق", "Shopping", false),
    ENTERTAINMENT("الترفيه", "Entertainment", false),
    OTHER_EXPENSE("أخرى", "Other", false),
    // Income categories
    SALARY("الراتب", "Salary", true),
    BUSINESS("عمل حر / تجارة", "Business", true),
    INVESTMENT("استثمار", "Investment", true),
    OTHER_INCOME("دخل آخر", "Other", true);

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn

    companion object {
        val expenseCategories = entries.filter { !it.isIncome }
        val incomeCategories = entries.filter { it.isIncome }
    }
}

enum class PaymentMethod(val labelAr: String, val labelEn: String) {
    CASH("نقدي", "Cash"),
    CARD("بطاقة بنكية", "Card"),
    WALLET("محفظة إلكترونية", "Mobile Wallet"),
    BANK_TRANSFER("تحويل بنكي", "Bank Transfer");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class BillCategory(val labelAr: String, val labelEn: String) {
    ELECTRICITY("الكهرباء", "Electricity"),
    WATER("المياه", "Water"),
    GAS("الغاز", "Gas"),
    INTERNET("الإنترنت", "Internet"),
    PHONE("الهاتف", "Phone"),
    OTHER("أخرى", "Other");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class HomeEventType(val labelAr: String, val labelEn: String) {
    OCCASION("مناسبة عائلية", "Family Occasion"),
    MAINTENANCE("صيانة منزلية", "Home Maintenance");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class EducationItemType(val labelAr: String, val labelEn: String) {
    HOMEWORK("واجب منزلي", "Homework"),
    EXAM("امتحان", "Exam");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

enum class CalendarEventType(val labelAr: String, val labelEn: String) {
    WORK("عمل", "Work"),
    FAMILY("عائلة", "Family"),
    BILL("فاتورة", "Bill"),
    HOMEWORK("واجب", "Homework"),
    EXAM("امتحان", "Exam"),
    PERSONAL("شخصي", "Personal"),
    REMINDER("تذكير", "Reminder");

    fun label(isArabic: Boolean) = if (isArabic) labelAr else labelEn
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val title: String,
    val description: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.PENDING.name,
    val category: String = TaskCategory.WORK.name,
    val labels: String = "",
    val dueDateMillis: Long,
    val isMeeting: Boolean = false,
    val meetingNotes: String = "",
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val type: String, // INCOME or EXPENSE
    val amount: Double,
    val category: String,
    val description: String,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val dateMillis: Long,
    val isEstimate: Boolean = false,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val category: String,
    val monthlyLimit: Double,
    val monthKey: String,
    val isSample: Boolean = false,
)

@Entity(tableName = "debts_installments")
data class DebtInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val title: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val monthlyInstallment: Double = 0.0,
    val dueDayOfMonth: Int = 1,
    val isDebtToOthers: Boolean = true,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val name: String,
    val relation: String,
    val phoneOrNote: String = "",
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val listName: String = "مقاضي البيت",
    val name: String,
    val quantity: String = "1",
    val estimatedPrice: Double = 0.0,
    val isPurchased: Boolean = false,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val title: String,
    val category: String = BillCategory.OTHER.name,
    val amount: Double,
    val dueDateMillis: Long,
    val isPaid: Boolean = false,
    val isRecurring: Boolean = true,
    val notes: String = "",
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "home_events")
data class HomeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val type: String = HomeEventType.OCCASION.name,
    val title: String,
    val dateMillis: Long,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val name: String,
    val gradeStage: String,
    val ageYears: Int = 10,
    val subjectsCsv: String = "",
    val weeklySchedule: String = "",
    val notes: String = "",
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "homework_exams")
data class HomeworkExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val childId: Long,
    val type: String = EducationItemType.HOMEWORK.name,
    val subject: String,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long,
    val isCompleted: Boolean = false,
    val scoreEarned: Double? = null,
    val maxScore: Double = 100.0,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val title: String,
    val description: String = "",
    val eventType: String = CalendarEventType.PERSONAL.name,
    val startMillis: Long,
    val endMillis: Long,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notifications")
data class NotificationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val title: String,
    val message: String,
    val category: String = CalendarEventType.REMINDER.name,
    val triggerMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = LOCAL_OWNER_ID,
    val isUser: Boolean,
    val text: String,
    val actionTag: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
)
