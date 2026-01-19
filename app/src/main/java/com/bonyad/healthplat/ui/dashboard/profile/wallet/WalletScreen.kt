package com.bonyad.healthplat.ui.dashboard.profile.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.PresetAmounts
import com.bonyad.healthplat.domain.model.TopUpState
import com.bonyad.healthplat.domain.model.TransactionType
import com.bonyad.healthplat.domain.model.WalletTransaction
import com.bonyad.healthplat.domain.model.WalletUiState
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import java.text.SimpleDateFormat
import java.util.*

// Color constants
private val TealColor = Color(0xFF5BA3A3)
private val GreenColor = Color(0xFF4CAF50)
private val BackgroundColor = Color(0xFFF5F5F5)
private val CardColor = Color.White
private val TextPrimaryColor = Color(0xFF2C2C2C)
private val TextSecondaryColor = Color(0xFF666666)
private val TextTertiaryColor = Color(0xFF999999)
private val BorderColor = Color(0xFFE8E8E8)
private val PatternColor = Color(0xFF4A9090)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val walletInfo by viewModel.walletInfo.collectAsState()
    val showTopUpSheet by viewModel.showTopUpSheet.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                WalletTopBar(onBack = onBack)
            },
            bottomBar = {
                TopUpButton(onClick = { viewModel.onTopUpClick() })
            },
            containerColor = BackgroundColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState) {
                    is WalletUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = TealColor
                        )
                    }
                    is WalletUiState.Success -> {
                        WalletContent(
                            balance = walletInfo.balance,
                            transactions = walletInfo.transactions
                        )
                    }
                    is WalletUiState.Error -> {
                        ErrorState(
                            message = (uiState as WalletUiState.Error).message,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // Top-up Bottom Sheet
        if (showTopUpSheet) {
            TopUpBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissTopUpSheet() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "کیف پول",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimaryColor
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.back_arrow),
                    contentDescription = "بازگشت",
                    tint = TextPrimaryColor
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Sync action */ }) {
                Icon(
                    painter = painterResource(R.drawable.sync_icon),
                    contentDescription = "همگام‌سازی",
                    tint = TealColor
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundColor
        )
    )
}

@Composable
private fun TopUpButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenColor)
        ) {
            Text(
                text = "افزایش موجودی",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun WalletContent(
    balance: Long,
    transactions: List<WalletTransaction>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Balance Card
        BalanceCard(balance = balance)

        Spacer(modifier = Modifier.height(24.dp))

        // Transaction History
        Text(
            text = "تاریخچه تراکنش",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimaryColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction List or Empty State
        if (transactions.isEmpty()) {
            EmptyTransactionState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Patterned Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(TealColor)
            ) {
                // Draw zigzag pattern
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val patternSize = 20.dp.toPx()
                    val rows = (size.height / patternSize).toInt() + 1
                    val cols = (size.width / patternSize).toInt() + 1

                    for (row in 0..rows) {
                        for (col in 0..cols) {
                            val x = col * patternSize
                            val y = row * patternSize

                            // Draw diagonal lines for pattern effect
                            drawLine(
                                color = PatternColor.copy(alpha = 0.3f),
                                start = Offset(x, y),
                                end = Offset(x + patternSize / 2, y + patternSize / 2),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = PatternColor.copy(alpha = 0.3f),
                                start = Offset(x + patternSize / 2, y),
                                end = Offset(x, y + patternSize / 2),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = "کیف پول تن بار",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            // Balance Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "موجودی",
                        fontSize = 14.sp,
                        color = TextSecondaryColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${formatAmount(balance)} تومان".toFarsiDigits(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Illustration placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Wallet illustration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Wallet icon placeholder
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(TealColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.wallet),
                            contentDescription = null,
                            tint = TealColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Person icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset(y = (-10).dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.profile),
                            contentDescription = null,
                            tint = TextTertiaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "تا کنون تراکنشی با کیف پول تن یار انجام نداده اید",
                fontSize = 14.sp,
                color = TextSecondaryColor,
                textAlign = TextAlign.Center
            )

            // Navigation arrow on the left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painter = painterResource(R.drawable.back_arrow),
                    contentDescription = null,
                    tint = TextTertiaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: WalletTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amount
            Text(
                text = "${if (transaction.type == TransactionType.DEPOSIT) "+" else "-"}${formatAmount(transaction.amount)} تومان".toFarsiDigits(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (transaction.type == TransactionType.DEPOSIT) GreenColor else Color.Red
            )

            // Description and date
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = transaction.description,
                    fontSize = 14.sp,
                    color = TextPrimaryColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDate(transaction.timestamp),
                    fontSize = 12.sp,
                    color = TextTertiaryColor
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.Red,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== Top-Up Bottom Sheet ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopUpBottomSheet(
    viewModel: WalletViewModel,
    onDismiss: () -> Unit
) {
    val selectedPresetAmount by viewModel.selectedPresetAmount.collectAsState()
    val customAmount by viewModel.customAmount.collectAsState()
    val topUpState by viewModel.topUpState.collectAsState()

    val amountToPay = viewModel.getAmountToPay()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, BorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = TextSecondaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "انتقال وجه به کیف پول",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "مبلغ مورد نظر را انتخاب کنید.",
                fontSize = 14.sp,
                color = TextSecondaryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Preset amounts grid (2x2)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PresetAmountButton(
                        amount = PresetAmounts.amounts[0],
                        isSelected = selectedPresetAmount == PresetAmounts.amounts[0],
                        onClick = { viewModel.selectPresetAmount(PresetAmounts.amounts[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetAmountButton(
                        amount = PresetAmounts.amounts[1],
                        isSelected = selectedPresetAmount == PresetAmounts.amounts[1],
                        onClick = { viewModel.selectPresetAmount(PresetAmounts.amounts[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PresetAmountButton(
                        amount = PresetAmounts.amounts[2],
                        isSelected = selectedPresetAmount == PresetAmounts.amounts[2],
                        onClick = { viewModel.selectPresetAmount(PresetAmounts.amounts[2]) },
                        modifier = Modifier.weight(1f)
                    )
                    PresetAmountButton(
                        amount = PresetAmounts.amounts[3],
                        isSelected = selectedPresetAmount == PresetAmounts.amounts[3],
                        onClick = { viewModel.selectPresetAmount(PresetAmounts.amounts[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom amount input
            Text(
                text = "مبلغ دلخواه",
                fontSize = 12.sp,
                color = TextTertiaryColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customAmount.toFarsiDigits(),
                onValueChange = { newValue ->
                    // Convert Persian digits to English for storage
                    val englishValue = newValue
                        .replace('۰', '0').replace('۱', '1').replace('۲', '2')
                        .replace('۳', '3').replace('۴', '4').replace('۵', '5')
                        .replace('۶', '6').replace('۷', '7').replace('۸', '8')
                        .replace('۹', '9')
                        .filter { it.isDigit() }
                    viewModel.updateCustomAmount(englishValue)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ریال",
                            color = TextTertiaryColor,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "مبلغ",
                            color = TextTertiaryColor,
                            fontSize = 14.sp
                        )
                    }
                },
                leadingIcon = {
                    Text(
                        text = "ریال",
                        color = TextTertiaryColor,
                        fontSize = 14.sp
                    )
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.End,
                    fontSize = 16.sp,
                    color = TextPrimaryColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealColor,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Amount to pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatAmount(amountToPay)} ریال".toFarsiDigits(),
                    fontSize = 16.sp,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "مبلغ قابل پرداخت:",
                    fontSize = 14.sp,
                    color = TextSecondaryColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pay button
            Button(
                onClick = { viewModel.processPayment() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amountToPay > 0 && topUpState !is TopUpState.Loading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenColor,
                    disabledContainerColor = GreenColor.copy(alpha = 0.5f)
                )
            ) {
                when (topUpState) {
                    is TopUpState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                    is TopUpState.Success -> {
                        Text(
                            text = "✓ موفق",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    else -> {
                        Text(
                            text = "پرداخت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Error message
            if (topUpState is TopUpState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (topUpState as TopUpState.Error).message,
                    fontSize = 12.sp,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PresetAmountButton(
    amount: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TealColor else BorderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) TealColor.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Text(
            text = "${formatAmountWithCommas(amount)} ریال".toFarsiDigits(),
            fontSize = 14.sp,
            color = if (isSelected) TealColor else TextPrimaryColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==================== Utility Functions ====================

private fun formatAmount(amount: Long): String {
    // Convert Rials to Toman (divide by 10) and format with commas
    val toman = amount / 10
    return formatAmountWithCommas(toman)
}

private fun formatAmountWithCommas(amount: Long): String {
    return String.format("%,d", amount)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}