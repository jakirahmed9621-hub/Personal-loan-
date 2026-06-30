package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LoanUiState
import com.example.ui.viewmodel.LoanViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: LoanViewModel = viewModel()
                val context = LocalContext.current

                // Observe global UI States
                val uiState by viewModel.uiState.collectAsState()

                // Trigger brief Toast feedback on success or error
                LaunchedEffect(uiState) {
                    when (uiState) {
                        is LoanUiState.Success -> {
                            Toast.makeText(context, (uiState as LoanUiState.Success).message, Toast.LENGTH_LONG).show()
                        }
                        is LoanUiState.Error -> {
                            Toast.makeText(context, (uiState as LoanUiState.Error).message, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main layout
                        MainAppContent(viewModel = viewModel)

                        // Floating dynamic status overlays for loading or alerts
                        when (val state = uiState) {
                            is LoanUiState.Loading -> {
                                GlassLoadingOverlay(message = state.message)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: LoanViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Screen content switcher
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (userProfile == null || !userProfile!!.isLoggedIn) {
            LoginScreen(viewModel = viewModel)
        } else {
            // Main App Header (Rahul profile avatar, welcome)
            AppHeader(userProfile = userProfile!!, onLogoutClick = { viewModel.logout() })

            // Content container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is Screen.Home -> DashboardScreen(viewModel = viewModel)
                        is Screen.Kyc -> KycScreen(viewModel = viewModel)
                        is Screen.BankAccountAdd -> BankAccountAddScreen(viewModel = viewModel)
                        is Screen.ApplyLoan -> LoanCalculatorScreen(viewModel = viewModel)
                        is Screen.Chat -> HelpChatScreen(viewModel = viewModel)
                        is Screen.Transactions -> TransactionsScreen(viewModel = viewModel)
                        is Screen.Cibil -> KycScreen(viewModel = viewModel) // Maps to KycScreen showing scores
                    }
                }
            }

            // Bottom Navigation bar matching design
            AppBottomNavigation(currentScreen = currentScreen, onTabSelected = { screen ->
                viewModel.navigateTo(screen)
            })
        }
    }
}

// --- Glassmorphic UI Components ---

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GradientHeroCard(
    userProfile: UserProfile,
    loan: LoanDetails?,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amountDisplay = when {
        loan == null || loan.status == "NONE" -> "₹50,000"
        else -> "₹${loan.amount.toInt()}"
    }

    val statusText = when (loan?.status) {
        "APPROVED" -> "Approved"
        "DISBURSED" -> "Active Loan"
        "REPAID" -> "Settled"
        else -> "Pre-Approved limit"
    }

    val statusColor = when (loan?.status) {
        "APPROVED" -> Color(0xFF4CAF50)
        "DISBURSED" -> Color(0xFFE91E63)
        "REPAID" -> Color(0xFF00BCD4)
        else -> Color(0xFFFF9800)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6750A4), Color(0xFF9278D4))
                )
            )
            .clickable { onActionClick() }
            .padding(24.dp)
    ) {
        // Subtle background emblem icon simulation
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp),
            tint = Color(0x1AFFFFFF)
        )

        Column {
            // Status Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)), RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (loan?.status == "DISBURSED") "Remaining Loan Amount" else "Max Loan Limit",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (loan?.status == "DISBURSED") "₹${loan.remainingAmount.toInt()}" else amountDisplay,
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = ".00",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "INTEREST RATE",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1.2% / month",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TENURE PERIOD",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${loan?.tenureMonths ?: 12} Months",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// --- Specific Screens ---

// Login Screen
@Composable
fun LoginScreen(viewModel: LoanViewModel) {
    val name by viewModel.loginName.collectAsState()
    val phone by viewModel.loginPhone.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = "App Logo",
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF6750A4)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RupeeSwift",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1D1B20)
        )

        Text(
            text = "Premium instant fake loan disbursement app",
            fontSize = 14.sp,
            color = Color(0xFF49454F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        FrostedGlassCard {
            Text(
                text = "Enter Details to Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.loginName.value = it },
                label = { Text("Customer Name") },
                placeholder = { Text("e.g. Rahul Kumar") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_name_input"),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) viewModel.loginPhone.value = it },
                label = { Text("Mobile Number") },
                placeholder = { Text("10 Digit Mobile Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_phone_input"),
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.login() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4)
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = "Verify OTP & Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Top Application Header
@Composable
fun AppHeader(userProfile: UserProfile, onLogoutClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F2F9))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile Initials Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4))
                    .clickable { onLogoutClick() }, // Log out on click avatar
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F)
                )
                Text(
                    text = userProfile.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            }
        }

        // Active Quick Badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (userProfile.isKycVerified) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F4CAF50))
                        .border(BorderStroke(1.dp, Color(0x334CAF50)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified KYC",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(1.dp, Color(0xFFE7E0EC)), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Main Dashboard
@Composable
fun DashboardScreen(viewModel: LoanViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val bankAccount by viewModel.bankAccount.collectAsState()
    val loanDetails by viewModel.latestLoan.collectAsState()
    val recentTransactions by viewModel.transactions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            userProfile?.let { profile ->
                GradientHeroCard(
                    userProfile = profile,
                    loan = loanDetails,
                    onActionClick = {
                        if (!profile.isKycVerified) {
                            viewModel.navigateTo(Screen.Kyc)
                        } else if (bankAccount == null || !bankAccount!!.isVerified) {
                            viewModel.navigateTo(Screen.BankAccountAdd)
                        } else {
                            viewModel.navigateTo(Screen.ApplyLoan)
                        }
                    }
                )
            }
        }

        // Quick Grid section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bank Account Quick Status Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE7E0EC)), RoundedCornerShape(20.dp))
                        .clickable { viewModel.navigateTo(Screen.BankAccountAdd) }
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEADDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Bank Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    Text(
                        text = bankAccount?.let { "${it.bankName} ••${it.accountNumber.takeLast(4)}" }
                            ?: "Link Bank Account",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F)
                    )
                }

                // Customer Support Chat Quick Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE7E0EC)), RoundedCornerShape(20.dp))
                        .clickable { viewModel.navigateTo(Screen.Chat) }
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFCDDEC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFFC2185B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Customer Care",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    Text(
                        text = "24/7 Smart Support",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
        }

        // KYC Verification strip
        item {
            userProfile?.let { profile ->
                val kycStatusText = if (profile.isKycVerified) "KYC Documents Verified" else "Submit KYC & Get Pre-Approval"
                val kycIcon = if (profile.isKycVerified) Icons.Default.CheckCircle else Icons.Default.ArrowCircleUp
                val kycBgColor = if (profile.isKycVerified) Color(0x224CAF50) else Color(0x1F6750A4)
                val kycBorderColor = if (profile.isKycVerified) Color(0x444CAF50) else Color(0x666750A4)
                val kycTextColor = if (profile.isKycVerified) Color(0xFF1B5E20) else Color(0xFF6750A4)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(kycBgColor)
                        .border(BorderStroke(1.dp, kycBorderColor), RoundedCornerShape(20.dp))
                        .clickable { viewModel.navigateTo(Screen.Kyc) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(kycTextColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = kycStatusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            if (profile.isKycVerified) {
                                Text(
                                    text = "Bureau CIBIL Score: ${profile.creditScore}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = kycIcon,
                        contentDescription = null,
                        tint = kycTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Active Main Action Footer Button inside home
        item {
            userProfile?.let { profile ->
                when {
                    !profile.isKycVerified -> {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Kyc) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("dash_action_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Unlock Credit Line instantly", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.TrendingUp, contentDescription = null)
                        }
                    }
                    bankAccount == null -> {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.BankAccountAdd) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("dash_action_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Connect Bank Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.CreditCard, contentDescription = null)
                        }
                    }
                    loanDetails?.status == "APPROVED" -> {
                        Button(
                            onClick = { viewModel.disburseLoan() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("dash_action_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Disburse ₹${loanDetails?.amount?.toInt()} to Bank", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                        }
                    }
                    loanDetails?.status == "DISBURSED" -> {
                        Button(
                            onClick = { viewModel.payEmi() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("dash_action_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Pay Monthly EMI (₹${loanDetails?.emiAmount?.toInt()})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Payment, contentDescription = null)
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.ApplyLoan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("dash_action_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Configure Upgraded Loan Options", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }

        // Recent Activities List header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activities",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF49454F),
                    letterSpacing = 1.sp
                )

                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.navigateTo(Screen.Transactions) }
                )
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, Color(0x33CAC4D0)), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF49454F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No recent transactions found.",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
        } else {
            items(recentTransactions.take(3)) { tx ->
                TransactionRow(tx = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Transaction Row helper
@Composable
fun TransactionRow(tx: TransactionItem) {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(tx.timestamp))

    val title = when (tx.type) {
        "DISBURSEMENT" -> "Loan Disbursed to Bank"
        "EMI_PAYMENT" -> "EMI Payment Received"
        "PENNY_DROP_CREDIT" -> "Penny-Drop Verification"
        else -> "Transaction Credit"
    }

    val icon = when (tx.type) {
        "DISBURSEMENT" -> Icons.Default.ArrowDownward
        "EMI_PAYMENT" -> Icons.Default.ArrowUpward
        "PENNY_DROP_CREDIT" -> Icons.Default.CardGiftcard
        else -> Icons.Default.AccountBalance
    }

    val color = when (tx.type) {
        "DISBURSEMENT" -> Color(0xFF4CAF50)
        "EMI_PAYMENT" -> Color(0xFF6750A4)
        "PENNY_DROP_CREDIT" -> Color(0xFF00BCD4)
        else -> Color(0xFF49454F)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE7E0EC)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = "$dateString • ${tx.bankName}",
                    fontSize = 10.sp,
                    color = Color(0xFF49454F)
                )
            }
        }

        Text(
            text = (if (tx.type == "DISBURSEMENT" || tx.type == "PENNY_DROP_CREDIT") "+" else "-") + " ₹${tx.amount.toInt()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (tx.type == "DISBURSEMENT" || tx.type == "PENNY_DROP_CREDIT") Color(0xFF2E7D32) else Color(0xFF1D1B20)
        )
    }
}

// KYC check screen
@Composable
fun KycScreen(viewModel: LoanViewModel) {
    val aadhaar by viewModel.kycAadhaar.collectAsState()
    val pan by viewModel.kycPan.collectAsState()
    val income by viewModel.kycIncome.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KYC & Credit Score Check",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Enter details to retrieve verified bureau CIBIL records",
            fontSize = 12.sp,
            color = Color(0xFF49454F),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        FrostedGlassCard {
            Text(
                text = "Identity Credentials",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = aadhaar,
                onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) viewModel.kycAadhaar.value = it },
                label = { Text("Aadhaar Number (12 Digits)") },
                placeholder = { Text("0000 0000 0000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("aadhaar_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pan,
                onValueChange = { if (it.length <= 10) viewModel.kycPan.value = it.uppercase() },
                label = { Text("PAN Card (10 Chars)") },
                placeholder = { Text("ABCDE1234F") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pan_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = income,
                onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.kycIncome.value = it },
                label = { Text("Estimated Monthly Income (₹)") },
                placeholder = { Text("e.g. 45000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.verifyKyc() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("kyc_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Verify Profile Details", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CIBIL Gauge illustration if verified
        userProfile?.let { profile ->
            if (profile.isKycVerified) {
                FrostedGlassCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Verified Bureau Credit Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1B5E20)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${profile.creditScore}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B5E20)
                        )

                        Text(
                            text = when {
                                profile.creditScore >= 750 -> "EXCELLENT"
                                profile.creditScore >= 700 -> "VERY GOOD"
                                else -> "GOOD"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }
        }
    }
}

// Add bank account screen
@Composable
fun BankAccountAddScreen(viewModel: LoanViewModel) {
    val bName by viewModel.bankName.collectAsState()
    val acNo by viewModel.bankAccountNumber.collectAsState()
    val ifsc by viewModel.bankIfsc.collectAsState()
    val linkedBank by viewModel.bankAccount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Link Bank Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20)
        )
        Text(
            text = "Secure instant verification via Simulated Penny Drop",
            fontSize = 12.sp,
            color = Color(0xFF49454F),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FrostedGlassCard {
            Text(
                text = "Beneficiary Bank Details",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = bName,
                onValueChange = { viewModel.bankName.value = it },
                label = { Text("Bank Name") },
                placeholder = { Text("e.g. HDFC Bank") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = acNo,
                onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.bankAccountNumber.value = it },
                label = { Text("Account Number") },
                placeholder = { Text("e.g. 501004829100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_account_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ifsc,
                onValueChange = { if (it.length <= 11) viewModel.bankIfsc.value = it.uppercase() },
                label = { Text("IFSC Code") },
                placeholder = { Text("HDFC0000012") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_ifsc_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.addBankAccount() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("bank_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Verify Bank instantly", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        linkedBank?.let { bank ->
            if (bank.isVerified) {
                Spacer(modifier = Modifier.height(20.dp))

                FrostedGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connected & Active Bank",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "${bank.bankName} - Account ending •••• ${bank.accountNumber.takeLast(4)}",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F)
                            )
                            Text(
                                text = "Virtual Ledger Balance: ₹${bank.balance}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Loan calculator / setups screen
@Composable
fun LoanCalculatorScreen(viewModel: LoanViewModel) {
    val currentAmount by viewModel.loanAmountSlider.collectAsState()
    val currentTenure by viewModel.loanTenureMonths.collectAsState()
    val activeLoan by viewModel.latestLoan.collectAsState()

    val calculatedEmi = (currentAmount.toDouble() * (1 + (viewModel.loanInterestRate / 100) * currentTenure)) / currentTenure

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "EMI Calculator & Application",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20)
        )
        Text(
            text = "Select custom loan amount and repayment period",
            fontSize = 12.sp,
            color = Color(0xFF49454F),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FrostedGlassCard {
            Text(
                text = "Loan Slider",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${currentAmount.toInt()}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = "Interest: 1.2%/mo",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F)
                )
            }

            Slider(
                value = currentAmount,
                onValueChange = { viewModel.loanAmountSlider.value = it },
                valueRange = 5000f..50000f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6750A4),
                    activeTrackColor = Color(0xFF6750A4)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("₹5,000", fontSize = 10.sp, color = Color(0xFF49454F))
                Text("₹50,000", fontSize = 10.sp, color = Color(0xFF49454F))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Repayment Tenure Period",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 6, 9, 12).forEach { months ->
                    val isSelected = currentTenure == months
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF6750A4) else Color.White)
                            .border(BorderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFE7E0EC)), RoundedCornerShape(12.dp))
                            .clickable { viewModel.loanTenureMonths.value = months }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$months M",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF1D1B20),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Divider(color = Color(0xFFE7E0EC))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Calculated Monthly EMI", fontSize = 11.sp, color = Color(0xFF49454F))
                    Text(
                        text = "₹${calculatedEmi.toInt()} / month",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Payment Due", fontSize = 11.sp, color = Color(0xFF49454F))
                    Text(
                        text = "₹${(calculatedEmi * currentTenure).toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.applyOrUpdateLoanParameters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_loan_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Confirm Loan Configuration", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Support Help chatbot
@Composable
fun HelpChatScreen(viewModel: LoanViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val scrollState = rememberScrollState()
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .padding(16.dp)
    ) {
        Text(
            text = "24/7 Support Assistant",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20)
        )
        Text(
            text = "Instant advice on KYC, Bank verification, and Loan transfers",
            fontSize = 12.sp,
            color = Color(0xFF49454F),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Chat List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE7E0EC)), RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                messages.forEach { msg ->
                    val isBot = msg.role == "assistant"
                    val bubbleColor = if (isBot) Color(0xFFF2EDF7) else Color(0xFF6750A4)
                    val textColor = if (isBot) Color(0xFF1D1B20) else Color.White
                    val alignment = if (isBot) Alignment.Start else Alignment.End

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = alignment
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isBot) 4.dp else 16.dp,
                                        bottomEnd = if (isBot) 16.dp else 4.dp
                                    )
                                )
                                .background(bubbleColor)
                                .padding(12.dp)
                                .widthIn(max = 260.dp)
                        ) {
                            Text(
                                text = msg.message,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Suggestions strips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Verify KYC details",
                "Penny Drop Status",
                "EMI Due details",
                "What is interest rate?"
            ).forEach { hint ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE7E0EC)), RoundedCornerShape(100.dp))
                        .clickable { viewModel.sendChatMessage(hint) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = hint, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                }
            }
        }

        // Input send layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask SwiftBot anything...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6750A4)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                        scope.launch {
                            delay(200)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF6750A4), CircleShape)
                    .testTag("send_chat_btn")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// Transactions Screen
@Composable
fun TransactionsScreen(viewModel: LoanViewModel) {
    val txList by viewModel.transactions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2F9))
            .padding(16.dp)
    ) {
        Text(
            text = "Passbook & Logs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20)
        )
        Text(
            text = "Full statement of virtual credits and EMI transfers",
            fontSize = 12.sp,
            color = Color(0xFF49454F),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (txList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFFCAC4D0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your digital passbook is clean.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(txList) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }
}

// --- App Overlay & Bottom Bars ---

@Composable
fun GlassLoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Dim scrim backing
            .clickable(enabled = false) {}, // Intercept clicks
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentScreen: Screen, onTabSelected: (Screen) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(64.dp)
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Home,
            onClick = { onTabSelected(Screen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6750A4),
                unselectedIconColor = Color(0xFF49454F)
            )
        )

        NavigationBarItem(
            selected = currentScreen is Screen.ApplyLoan,
            onClick = { onTabSelected(Screen.ApplyLoan) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Loans") },
            label = { Text("Loans", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6750A4),
                unselectedIconColor = Color(0xFF49454F)
            )
        )

        NavigationBarItem(
            selected = currentScreen is Screen.Chat,
            onClick = { onTabSelected(Screen.Chat) },
            icon = { Icon(Icons.Default.SupportAgent, contentDescription = "Support") },
            label = { Text("Support", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6750A4),
                unselectedIconColor = Color(0xFF49454F)
            )
        )

        NavigationBarItem(
            selected = currentScreen is Screen.Transactions,
            onClick = { onTabSelected(Screen.Transactions) },
            icon = { Icon(Icons.Default.Receipt, contentDescription = "History") },
            label = { Text("History", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6750A4),
                unselectedIconColor = Color(0xFF49454F)
            )
        )
    }
}
