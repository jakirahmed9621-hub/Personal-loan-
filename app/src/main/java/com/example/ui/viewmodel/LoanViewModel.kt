package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.LoanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LoanRepository
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient()

    // Database flow streams
    val userProfile: StateFlow<UserProfile?>
    val bankAccount: StateFlow<BankAccount?>
    val latestLoan: StateFlow<LoanDetails?>
    val transactions: StateFlow<List<TransactionItem>>
    val chatMessages: StateFlow<List<ChatMessage>>

    // UI Interactive States
    private val _uiState = MutableStateFlow<LoanUiState>(LoanUiState.Idle)
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Form Temporary Inputs
    val loginName = MutableStateFlow("")
    val loginPhone = MutableStateFlow("")

    val kycAadhaar = MutableStateFlow("")
    val kycPan = MutableStateFlow("")
    val kycIncome = MutableStateFlow("")

    val bankName = MutableStateFlow("")
    val bankAccountNumber = MutableStateFlow("")
    val bankIfsc = MutableStateFlow("")

    // Loan Selection Temporary Inputs
    val loanAmountSlider = MutableStateFlow(25000f) // default mid-way
    val loanTenureMonths = MutableStateFlow(12) // default 12 months
    val loanInterestRate = 1.2 // 1.2% per month (from Frosted Glass theme HTML)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = LoanRepository(db.userDao())

        // Collect DB flows as StateFlows
        userProfile = repository.userProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        bankAccount = repository.bankAccount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        latestLoan = repository.latestLoan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        transactions = repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        chatMessages = repository.chatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Insert initial chat message if empty
        viewModelScope.launch {
            repository.chatMessages.firstOrNull()?.let {
                if (it.isEmpty()) {
                    repository.addChatMessage(
                        ChatMessage(
                            role = "assistant",
                            message = "Namaste! Welcome to RupeeSwift customer care. I can help you with your loan options, KYC verification, or bank transfers. How can I help you today?"
                        )
                    )
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Core Operations ---

    fun login() {
        val name = loginName.value.trim()
        val phone = loginPhone.value.trim()

        if (name.isEmpty() || phone.length < 10) {
            _uiState.value = LoanUiState.Error("Please enter a valid name and phone number (min 10 digits)")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoanUiState.Loading("Setting up your secure profile...")
            val profile = UserProfile(
                name = name,
                phoneNumber = phone,
                isLoggedIn = true,
                creditScore = 0,
                isKycVerified = false
            )
            repository.saveUserProfile(profile)
            _uiState.value = LoanUiState.Success("Welcome $name!")
            delay(500)
            _uiState.value = LoanUiState.Idle
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUserProfile()
            repository.deleteBankAccount()
            repository.deleteLoan()
            repository.deleteTransactions()
            repository.clearChat()

            // Reset forms
            loginName.value = ""
            loginPhone.value = ""
            kycAadhaar.value = ""
            kycPan.value = ""
            kycIncome.value = ""
            bankName.value = ""
            bankAccountNumber.value = ""
            bankIfsc.value = ""
            loanAmountSlider.value = 25000f

            _currentScreen.value = Screen.Home
            repository.addChatMessage(
                ChatMessage(
                    role = "assistant",
                    message = "Namaste! Welcome to RupeeSwift customer care. I can help you with your loan options, KYC verification, or bank transfers. How can I help you today?"
                )
            )
        }
    }

    fun verifyKyc() {
        val aadhaar = kycAadhaar.value.trim().replace(" ", "")
        val pan = kycPan.value.trim().uppercase()
        val incomeStr = kycIncome.value.trim()

        if (aadhaar.length != 12 || !aadhaar.all { it.isDigit() }) {
            _uiState.value = LoanUiState.Error("Aadhaar Number must be exactly 12 digits")
            return
        }
        if (pan.length != 10) {
            _uiState.value = LoanUiState.Error("PAN Card must be exactly 10 characters")
            return
        }
        val income = incomeStr.toDoubleOrNull() ?: 0.0
        if (income <= 5000) {
            _uiState.value = LoanUiState.Error("Monthly income must be greater than ₹5,000")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoanUiState.Loading("Analyzing credit bureau and verifying KYC documents...")
            delay(2000) // Realistic delay

            // Generate a solid CIBIL score based on input income
            val generatedScore = when {
                income >= 50000 -> (760..840).random()
                income >= 25000 -> (700..780).random()
                else -> (650..720).random()
            }

            val currentProfile = userProfile.value ?: UserProfile()
            val updatedProfile = currentProfile.copy(
                aadhaarNumber = aadhaar,
                panNumber = pan,
                monthlyIncome = income,
                creditScore = generatedScore,
                isKycVerified = true
            )

            repository.saveUserProfile(updatedProfile)

            // Setup a pre-approved fake loan limit
            val preApprovedLoan = LoanDetails(
                amount = loanAmountSlider.value.toDouble(),
                tenureMonths = loanTenureMonths.value,
                interestRate = loanInterestRate,
                emiAmount = (loanAmountSlider.value.toDouble() * (1 + (loanInterestRate / 100) * loanTenureMonths.value)) / loanTenureMonths.value,
                status = "APPROVED"
            )
            repository.saveLoan(preApprovedLoan)

            _uiState.value = LoanUiState.Success("KYC Verified Successfully! Credit Score: $generatedScore")
            delay(1000)
            _uiState.value = LoanUiState.Idle
            _currentScreen.value = Screen.Home
        }
    }

    fun addBankAccount() {
        val name = bankName.value.trim()
        val account = bankAccountNumber.value.trim()
        val ifsc = bankIfsc.value.trim().uppercase()

        if (name.isEmpty() || account.length < 8 || ifsc.length != 11) {
            _uiState.value = LoanUiState.Error("Please fill valid Account details & IFSC (11 chars)")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoanUiState.Loading("Initiating penny-drop verification with $name...")
            delay(2000) // Wait for verification

            // Virtual Bank Account object
            val bank = BankAccount(
                bankName = name,
                accountNumber = account,
                ifscCode = ifsc,
                isVerified = true,
                balance = 1.0 // Penny-drop deposited!
            )
            repository.saveBankAccount(bank)

            // Log Penny drop transaction
            val pennyTx = TransactionItem(
                amount = 1.0,
                type = "PENNY_DROP_CREDIT",
                bankName = name,
                status = "SUCCESS"
            )
            repository.addTransaction(pennyTx)

            _uiState.value = LoanUiState.Success("Penny Drop Successful! Deposited ₹1.00 for account verification.")
            delay(1200)
            _uiState.value = LoanUiState.Idle
            _currentScreen.value = Screen.Home
        }
    }

    fun applyOrUpdateLoanParameters() {
        val loanAmount = loanAmountSlider.value.toDouble()
        val tenure = loanTenureMonths.value
        val emi = (loanAmount * (1 + (loanInterestRate / 100) * tenure)) / tenure

        viewModelScope.launch(Dispatchers.IO) {
            val currentLoan = latestLoan.value
            val newLoan = (currentLoan ?: LoanDetails()).copy(
                amount = loanAmount,
                tenureMonths = tenure,
                interestRate = loanInterestRate,
                emiAmount = emi,
                status = "APPROVED" // Automatically approved in RupeeSwift
            )
            repository.saveLoan(newLoan)
            _uiState.value = LoanUiState.Success("Loan setup configured for ₹${loanAmount.toInt()}")
            delay(500)
            _uiState.value = LoanUiState.Idle
        }
    }

    fun disburseLoan() {
        val bank = bankAccount.value
        val loan = latestLoan.value

        if (bank == null || !bank.isVerified) {
            _uiState.value = LoanUiState.Error("Please add and verify your Bank Account first")
            return
        }
        if (loan == null || loan.status != "APPROVED") {
            _uiState.value = LoanUiState.Error("No approved loan available for disbursement")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoanUiState.Loading("Disbursing ₹${loan.amount.toInt()} securely to ${bank.bankName}...")
            delay(2500) // Disbursement takes time

            // Update bank balance
            val updatedBank = bank.copy(balance = bank.balance + loan.amount)
            repository.saveBankAccount(updatedBank)

            // Update loan details
            val updatedLoan = loan.copy(
                status = "DISBURSED",
                disbursementTimestamp = System.currentTimeMillis(),
                nextDueDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // In 30 days
                remainingAmount = loan.amount * (1 + (loan.interestRate / 100) * loan.tenureMonths)
            )
            repository.saveLoan(updatedLoan)

            // Add transaction log
            val tx = TransactionItem(
                amount = loan.amount,
                type = "DISBURSEMENT",
                bankName = bank.bankName,
                status = "SUCCESS"
            )
            repository.addTransaction(tx)

            _uiState.value = LoanUiState.Success("₹${loan.amount.toInt()} disbursed successfully!")
            delay(1000)
            _uiState.value = LoanUiState.Idle
        }
    }

    fun payEmi() {
        val bank = bankAccount.value
        val loan = latestLoan.value

        if (bank == null || loan == null || loan.status != "DISBURSED") {
            _uiState.value = LoanUiState.Error("No active loan to pay EMI for")
            return
        }

        val paymentAmount = loan.emiAmount
        if (bank.balance < paymentAmount) {
            _uiState.value = LoanUiState.Error("Insufficient bank account balance to pay EMI of ₹${paymentAmount.toInt()}")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoanUiState.Loading("Processing EMI payment of ₹${paymentAmount.toInt()}...")
            delay(1500)

            // Deduct from bank balance
            val updatedBank = bank.copy(balance = bank.balance - paymentAmount)
            repository.saveBankAccount(updatedBank)

            // Update remaining loan amount
            val newRemaining = (loan.remainingAmount - paymentAmount).coerceAtLeast(0.0)
            val updatedLoan = if (newRemaining <= 0.1) {
                loan.copy(
                    status = "REPAID",
                    remainingAmount = 0.0
                )
            } else {
                loan.copy(
                    remainingAmount = newRemaining,
                    nextDueDate = loan.nextDueDate + (30L * 24 * 60 * 60 * 1000) // Extend by 30 days
                )
            }
            repository.saveLoan(updatedLoan)

            // Transaction log
            val tx = TransactionItem(
                amount = paymentAmount,
                type = "EMI_PAYMENT",
                bankName = bank.bankName,
                status = "SUCCESS"
            )
            repository.addTransaction(tx)

            val successMsg = if (newRemaining <= 0.1) "Loan Fully Repaid! Thank you." else "EMI Paid Successfully!"
            _uiState.value = LoanUiState.Success(successMsg)
            delay(1000)
            _uiState.value = LoanUiState.Idle
        }
    }

    // --- Interactive Customer Care Chatbot (with optional actual Gemini API backend) ---

    fun sendChatMessage(userText: String) {
        if (userText.trim().isEmpty()) return

        val userMsg = ChatMessage(role = "user", message = userText)
        viewModelScope.launch {
            repository.addChatMessage(userMsg)

            // Add typing placeholder
            val typingMessage = ChatMessage(role = "assistant", message = "Thinking...")
            repository.addChatMessage(typingMessage)

            // Query response (either Gemini API or Local context-aware responses)
            val response = fetchBotResponse(userText)

            // Remove typing/Thinking and replace with actual response
            repository.clearChat() // we can delete last or replace it simply by fetching flow
            // Actually, userDao retrieves ordered list, we can rewrite properly.
            // Let's create an elegant rewrite by clearing chat and reloading all except the placeholder
            // Or we can just filter the list when rendering. Let's do a cleaner replacement logic:
            val currentMessages = chatMessages.value.filter { it.message != "Thinking..." }
            repository.clearChat()
            currentMessages.forEach { repository.addChatMessage(it) }

            // Insert real response
            repository.addChatMessage(ChatMessage(role = "assistant", message = response))
        }
    }

    private suspend fun fetchBotResponse(query: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasGemini = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (hasGemini) {
            try {
                return callGeminiApi(query)
            } catch (e: Exception) {
                Log.e("LoanViewModel", "Gemini call failed, falling back", e)
            }
        }

        // Context-aware Smart local support assistant responses in Hindi/English
        val lowercaseQuery = query.lowercase()
        val user = userProfile.value
        val bank = bankAccount.value
        val loan = latestLoan.value

        return when {
            lowercaseQuery.contains("hello") || lowercaseQuery.contains("hi") || lowercaseQuery.contains("namaste") || lowercaseQuery.contains("hey") -> {
                "Hello ${user?.name ?: "Customer"}! Welcome to RupeeSwift Helpdesk. How can I assist you with your instant ₹50,000 loan today?"
            }
            lowercaseQuery.contains("status") || lowercaseQuery.contains("loan") || lowercaseQuery.contains("paisae") -> {
                when {
                    loan == null || loan.status == "NONE" -> "You haven't applied for a loan yet. Please complete your KYC verification to get a pre-approved limit up to ₹50,000."
                    loan.status == "APPROVED" -> "Aapka ₹${loan.amount.toInt()} ka loan pre-approve ho gaya hai! Please click 'Disburse to Bank' to transfer the funds to your added bank account."
                    loan.status == "DISBURSED" -> "Congratulations! Your loan of ₹${loan.amount.toInt()} has been successfully disbursed to your bank account. Your next EMI is ₹${loan.emiAmount.toInt()} due on ${formatDate(loan.nextDueDate)}."
                    loan.status == "REPAID" -> "Wonderful! You have successfully repaid your loan. You are now eligible for an upgraded loan limit up to ₹1,00,000!"
                    else -> "Your application is under review. Please ensure your PAN and Aadhaar KYC details are correct."
                }
            }
            lowercaseQuery.contains("bank") || lowercaseQuery.contains("khata") || lowercaseQuery.contains("account") -> {
                if (bank != null && bank.isVerified) {
                    "Your bank account (${bank.bankName} - Account ending in •••• ${bank.accountNumber.takeLast(4)}) is successfully linked and verified with penny-drop."
                } else {
                    "Bank Account connect karna bohot simple hai. Dashboard pe 'Bank Account' select karein, details aur IFSC code fill karein aur verified click karein."
                }
            }
            lowercaseQuery.contains("kyc") || lowercaseQuery.contains("aadhaar") || lowercaseQuery.contains("pan") || lowercaseQuery.contains("civil") || lowercaseQuery.contains("score") -> {
                if (user != null && user.isKycVerified) {
                    "Aapki KYC verified hai. Aapka bureau score (CIBIL) **${user.creditScore}** hai, jo ki bohot badhiya hai! Aap instantly loan withdraw kar sakte hain."
                } else {
                    "KYC submit karne ke liye aapko digital PAN aur Aadhaar card scan enter karna hota hai. Isse aapka accurate credit score fetch hota hai."
                }
            }
            lowercaseQuery.contains("interest") || lowercaseQuery.contains("byaj") || lowercaseQuery.contains("dar") -> {
                "RupeeSwift offers an incredibly affordable interest rate of only 1.2% per month with easy tenure options of up to 12 months."
            }
            lowercaseQuery.contains("number") || lowercaseQuery.contains("contact") || lowercaseQuery.contains("care") || lowercaseQuery.contains("call") -> {
                "Our customer care number is 1800-300-SWIFT (toll-free) and mail is support@rupeeswift-mock.in. We are here to support you 24/7!"
            }
            else -> {
                "I understand your query. RupeeSwift offers hassle-free loans up to ₹50,000 instantly. " +
                        "Aap KYC verify karke custom EMI calculate kar sakte hain aur immediate disbursement le sakte hain. Please let me know how I can guide you further!"
            }
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val user = userProfile.value
        val bank = bankAccount.value
        val loan = latestLoan.value

        // Contextual system prompt to make the AI extremely context-aware
        val systemPrompt = "You are 'SwiftBot', the friendly AI assistant of RupeeSwift, a premium Instant Personal Loan app. " +
                "You must help the user politely in a mix of Hindi and English (Hinglish/English). " +
                "Current user details: Name: ${user?.name ?: "Guest"}, Phone: ${user?.phoneNumber ?: "N/A"}, KYC Status: ${if (user?.isKycVerified == true) "Verified (Score: ${user.creditScore})" else "Not Verified"}, " +
                "Linked Bank: ${bank?.bankName ?: "None"}, Loan Status: ${loan?.status ?: "None"}, Amount: ${loan?.amount ?: 0.0}, EMI: ${loan?.emiAmount ?: 0.0}. " +
                "Do not mention you are a language model. Keep answers short, punchy and highly relevant."

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser Question: $prompt")
                        })
                    })
                })
            })
        }

        val body = jsonRequest.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected response code: ${response.code}")
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

// Sealed screen definitions
sealed class Screen {
    object Home : Screen()
    object ApplyLoan : Screen()
    object Kyc : Screen()
    object BankAccountAdd : Screen()
    object Chat : Screen()
    object Transactions : Screen()
    object Cibil : Screen()
}

// UI State notifications
sealed class LoanUiState {
    object Idle : LoanUiState()
    data class Loading(val message: String) : LoanUiState()
    data class Success(val message: String) : LoanUiState()
    data class Error(val message: String) : LoanUiState()
}
