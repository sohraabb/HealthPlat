package com.bonyad.healthplat.domain.model

/**
 * Represents a wallet transaction
 */
data class WalletTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val amount: Long,
    val type: TransactionType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.COMPLETED
)

/**
 * Transaction types
 */
enum class TransactionType {
    DEPOSIT,    // افزایش موجودی
    WITHDRAWAL, // برداشت
    PURCHASE,   // خرید
    REFUND      // بازگشت وجه
}

/**
 * Transaction status
 */
enum class TransactionStatus {
    PENDING,    // در انتظار
    COMPLETED,  // تکمیل شده
    FAILED,     // ناموفق
    CANCELLED   // لغو شده
}

/**
 * Wallet information
 */
data class WalletInfo(
    val balance: Long = 0, // Balance in Rials
    val transactions: List<WalletTransaction> = emptyList()
)

/**
 * Preset amount options for top-up (in Rials)
 */
object PresetAmounts {
    val amounts = listOf(
        500_000L,      // ۵۰۰,۰۰۰ ریال
        1_000_000L,    // ۱,۰۰۰,۰۰۰ ریال
        5_000_000L,    // ۵,۰۰۰,۰۰۰ ریال
        10_000_000L    // ۱۰,۰۰۰,۰۰۰ ریال
    )
}

/**
 * UI State for wallet screen
 */
sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val walletInfo: WalletInfo) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

/**
 * Top-up result state
 */
sealed class TopUpState {
    object Idle : TopUpState()
    object Loading : TopUpState()
    data class Success(val newBalance: Long) : TopUpState()
    data class Error(val message: String) : TopUpState()
}