package com.example.data.repository

import com.example.data.db.UserDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class LoanRepository(private val userDao: UserDao) {

    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()
    val bankAccount: Flow<BankAccount?> = userDao.getBankAccount()
    val latestLoan: Flow<LoanDetails?> = userDao.getLatestLoan()
    val transactions: Flow<List<TransactionItem>> = userDao.getAllTransactions()
    val chatMessages: Flow<List<ChatMessage>> = userDao.getChatMessages()

    suspend fun saveUserProfile(user: UserProfile) {
        userDao.insertUserProfile(user)
    }

    suspend fun deleteUserProfile() {
        userDao.deleteUserProfile()
    }

    suspend fun saveBankAccount(bank: BankAccount) {
        userDao.insertBankAccount(bank)
    }

    suspend fun deleteBankAccount() {
        userDao.deleteBankAccount()
    }

    suspend fun saveLoan(loan: LoanDetails) {
        userDao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: LoanDetails) {
        userDao.updateLoan(loan)
    }

    suspend fun deleteLoan() {
        userDao.deleteLoan()
    }

    suspend fun addTransaction(tx: TransactionItem) {
        userDao.insertTransaction(tx)
    }

    suspend fun deleteTransactions() {
        userDao.deleteTransactions()
    }

    suspend fun addChatMessage(msg: ChatMessage) {
        userDao.insertChatMessage(msg)
    }

    suspend fun clearChat() {
        userDao.clearChatMessages()
    }
}
