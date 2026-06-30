package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun deleteUserProfile()

    // Bank Account
    @Query("SELECT * FROM bank_account WHERE id = 1 LIMIT 1")
    fun getBankAccount(): Flow<BankAccount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(bank: BankAccount)

    @Query("DELETE FROM bank_account")
    suspend fun deleteBankAccount()

    // Loan Details (Since it's a mock app, we focus on the active loan)
    @Query("SELECT * FROM loan_details ORDER BY id DESC LIMIT 1")
    fun getLatestLoan(): Flow<LoanDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanDetails)

    @Update
    suspend fun updateLoan(loan: LoanDetails)

    @Query("DELETE FROM loan_details")
    suspend fun deleteLoan()

    // Transactions
    @Query("SELECT * FROM transaction_item ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionItem)

    @Query("DELETE FROM transaction_item")
    suspend fun deleteTransactions()

    // Chat Messages
    @Query("SELECT * FROM chat_message ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: ChatMessage)

    @Query("DELETE FROM chat_message")
    suspend fun clearChatMessages()
}
