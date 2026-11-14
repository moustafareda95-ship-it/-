package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE ownerId = :ownerId ORDER BY dueDateMillis ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TaskEntity): Long

    @Update
    suspend fun update(item: TaskEntity)

    @Delete
    suspend fun delete(item: TaskEntity)

    @Query("DELETE FROM tasks WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM tasks WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE ownerId = :ownerId ORDER BY dateMillis DESC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionEntity): Long

    @Update
    suspend fun update(item: TransactionEntity)

    @Delete
    suspend fun delete(item: TransactionEntity)

    @Query("DELETE FROM transactions WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM transactions WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits WHERE ownerId = :ownerId ORDER BY id DESC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits WHERE ownerId = :ownerId AND category = :category AND monthKey = :monthKey LIMIT 1")
    suspend fun findByCategoryAndMonth(category: String, monthKey: String, ownerId: String = LOCAL_OWNER_ID): BudgetLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BudgetLimitEntity): Long

    @Delete
    suspend fun delete(item: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM budget_limits WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface DebtInstallmentDao {
    @Query("SELECT * FROM debts_installments WHERE ownerId = :ownerId ORDER BY isCompleted ASC, dueDayOfMonth ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<DebtInstallmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DebtInstallmentEntity): Long

    @Update
    suspend fun update(item: DebtInstallmentEntity)

    @Delete
    suspend fun delete(item: DebtInstallmentEntity)

    @Query("DELETE FROM debts_installments WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM debts_installments WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE ownerId = :ownerId ORDER BY createdAt ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FamilyMemberEntity): Long

    @Update
    suspend fun update(item: FamilyMemberEntity)

    @Delete
    suspend fun delete(item: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM family_members WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE ownerId = :ownerId ORDER BY isPurchased ASC, createdAt DESC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM shopping_items WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE ownerId = :ownerId ORDER BY isPaid ASC, dueDateMillis ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<BillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BillEntity): Long

    @Update
    suspend fun update(item: BillEntity)

    @Delete
    suspend fun delete(item: BillEntity)

    @Query("DELETE FROM bills WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM bills WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface HomeEventDao {
    @Query("SELECT * FROM home_events WHERE ownerId = :ownerId ORDER BY dateMillis ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<HomeEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HomeEventEntity): Long

    @Update
    suspend fun update(item: HomeEventEntity)

    @Delete
    suspend fun delete(item: HomeEventEntity)

    @Query("DELETE FROM home_events WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM home_events WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM children WHERE ownerId = :ownerId ORDER BY createdAt ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE id = :childId AND ownerId = :ownerId LIMIT 1")
    suspend fun findById(childId: Long, ownerId: String = LOCAL_OWNER_ID): ChildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChildEntity): Long

    @Update
    suspend fun update(item: ChildEntity)

    @Delete
    suspend fun delete(item: ChildEntity)

    @Query("DELETE FROM children WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM children WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface HomeworkExamDao {
    @Query("SELECT * FROM homework_exams WHERE ownerId = :ownerId ORDER BY dueDateMillis ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<HomeworkExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HomeworkExamEntity): Long

    @Update
    suspend fun update(item: HomeworkExamEntity)

    @Delete
    suspend fun delete(item: HomeworkExamEntity)

    @Query("DELETE FROM homework_exams WHERE childId = :childId AND ownerId = :ownerId")
    suspend fun deleteForChild(childId: Long, ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM homework_exams WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM homework_exams WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE ownerId = :ownerId ORDER BY startMillis ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CalendarEventEntity): Long

    @Update
    suspend fun update(item: CalendarEventEntity)

    @Delete
    suspend fun delete(item: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE ownerId = :ownerId AND isSample = 1")
    suspend fun deleteSamples(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM calendar_events WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<NotificationItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NotificationItemEntity): Long

    @Update
    suspend fun update(item: NotificationItemEntity)

    @Delete
    suspend fun delete(item: NotificationItemEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE ownerId = :ownerId")
    suspend fun markAllRead(ownerId: String = LOCAL_OWNER_ID)

    @Query("DELETE FROM notifications WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE ownerId = :ownerId ORDER BY timestampMillis ASC")
    fun observeAll(ownerId: String = LOCAL_OWNER_ID): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String = LOCAL_OWNER_ID)
}
