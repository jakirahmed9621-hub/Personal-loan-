package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phoneNumber: String = "",
    val aadhaarNumber: String = "",
    val panNumber: String = "",
    val creditScore: Int = 0,
    val monthlyIncome: Double = 0.0,
    val isKycVerified: Boolean = false,
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "bank_account")
data class BankAccount(
    @PrimaryKey val id: Int = 1,
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val isVerified: Boolean = false,
    val balance: Double = 0.0
)

@Entity(tableName = "loan_details")
data class LoanDetails(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double = 0.0,
    val tenureMonths: Int = 0,
    val interestRate: Double = 0.0,
    val emiAmount: Double = 0.0,
    val status: String = "NONE", // "NONE", "APPLIED", "APPROVED", "DISBURSED", "REPAID"
    val disbursementTimestamp: Long = 0L,
    val nextDueDate: Long = 0L,
    val remainingAmount: Double = 0.0
)

@Entity(tableName = "transaction_item")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double = 0.0,
    val type: String = "", // "DISBURSEMENT", "EMI_PAYMENT"
    val bankName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS" // "SUCCESS", "FAILED"
)

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String = "", // "user", "assistant"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
