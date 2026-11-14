package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        TransactionEntity::class,
        BudgetLimitEntity::class,
        DebtInstallmentEntity::class,
        FamilyMemberEntity::class,
        ShoppingItemEntity::class,
        BillEntity::class,
        HomeEventEntity::class,
        ChildEntity::class,
        HomeworkExamEntity::class,
        CalendarEventEntity::class,
        NotificationItemEntity::class,
        ChatMessageEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class HayatiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun debtInstallmentDao(): DebtInstallmentDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun billDao(): BillDao
    abstract fun homeEventDao(): HomeEventDao
    abstract fun childDao(): ChildDao
    abstract fun homeworkExamDao(): HomeworkExamDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun notificationDao(): NotificationDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: HayatiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf(
                    "tasks",
                    "transactions",
                    "budget_limits",
                    "debts_installments",
                    "family_members",
                    "shopping_items",
                    "bills",
                    "home_events",
                    "children",
                    "homework_exams",
                    "calendar_events",
                    "notifications",
                    "chat_messages",
                )
                tables.forEach { table ->
                    db.execSQL(
                        "ALTER TABLE $table ADD COLUMN ownerId TEXT NOT NULL DEFAULT '$LOCAL_OWNER_ID'"
                    )
                }
            }
        }

        fun getInstance(context: Context): HayatiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HayatiDatabase::class.java,
                    "hayati_ai_db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
