package com.bonyad.healthplat.ui.dashboard.profile.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.domain.model.PresetAmounts
import com.bonyad.healthplat.domain.model.TopUpState
import com.bonyad.healthplat.domain.model.TransactionType
import com.bonyad.healthplat.domain.model.WalletInfo
import com.bonyad.healthplat.domain.model.WalletTransaction
import com.bonyad.healthplat.domain.model.WalletUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    // TODO: Inject WalletRepository when available
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _walletInfo = MutableStateFlow(WalletInfo())
    val walletInfo: StateFlow<WalletInfo> = _walletInfo.asStateFlow()

    // Top-up bottom sheet states
    private val _showTopUpSheet = MutableStateFlow(false)
    val showTopUpSheet: StateFlow<Boolean> = _showTopUpSheet.asStateFlow()

    private val _selectedPresetAmount = MutableStateFlow<Long?>(null)
    val selectedPresetAmount: StateFlow<Long?> = _selectedPresetAmount.asStateFlow()

    private val _customAmount = MutableStateFlow("")
    val customAmount: StateFlow<String> = _customAmount.asStateFlow()

    private val _topUpState = MutableStateFlow<TopUpState>(TopUpState.Idle)
    val topUpState: StateFlow<TopUpState> = _topUpState.asStateFlow()

    // Computed amount to pay
    val amountToPay: StateFlow<Long>
        get() = MutableStateFlow(calculateAmountToPay()).asStateFlow()

    init {
        loadWalletInfo()
    }

    private fun loadWalletInfo() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            try {
                // TODO: Load from repository
                // For now, using mock data
                delay(500) // Simulate network delay

                val wallet = WalletInfo(
                    balance = 0, // Starting with 0 balance
                    transactions = emptyList()
                )

                _walletInfo.value = wallet
                _uiState.value = WalletUiState.Success(wallet)

                Timber.i("Wallet info loaded: balance = ${wallet.balance}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load wallet info")
                _uiState.value = WalletUiState.Error("خطا در بارگذاری اطلاعات کیف پول")
            }
        }
    }

    // ==================== Top-Up Sheet Actions ====================

    fun onTopUpClick() {
        _selectedPresetAmount.value = null
        _customAmount.value = ""
        _topUpState.value = TopUpState.Idle
        _showTopUpSheet.value = true
    }

    fun dismissTopUpSheet() {
        _showTopUpSheet.value = false
        _selectedPresetAmount.value = null
        _customAmount.value = ""
        _topUpState.value = TopUpState.Idle
    }

    // ==================== Amount Selection ====================

    fun selectPresetAmount(amount: Long) {
        _selectedPresetAmount.value = if (_selectedPresetAmount.value == amount) null else amount
        _customAmount.value = "" // Clear custom amount when preset is selected
    }

    fun updateCustomAmount(value: String) {
        // Only allow digits
        val cleaned = value.filter { it.isDigit() }
        _customAmount.value = cleaned
        _selectedPresetAmount.value = null // Clear preset when custom is entered
    }

    private fun calculateAmountToPay(): Long {
        return _selectedPresetAmount.value
            ?: _customAmount.value.toLongOrNull()
            ?: 0L
    }

    fun getAmountToPay(): Long {
        return calculateAmountToPay()
    }

    // ==================== Payment ====================

    fun processPayment() {
        val amount = calculateAmountToPay()
        if (amount <= 0) {
            _topUpState.value = TopUpState.Error("لطفا مبلغ را وارد کنید")
            return
        }

        viewModelScope.launch {
            _topUpState.value = TopUpState.Loading

            try {
                // TODO: Call payment gateway API
                delay(1500) // Simulate payment processing

                // Create transaction
                val transaction = WalletTransaction(
                    amount = amount,
                    type = TransactionType.DEPOSIT,
                    description = "افزایش موجودی کیف پول"
                )

                // Update wallet
                _walletInfo.update { current ->
                    current.copy(
                        balance = current.balance + amount,
                        transactions = listOf(transaction) + current.transactions
                    )
                }

                _uiState.value = WalletUiState.Success(_walletInfo.value)
                _topUpState.value = TopUpState.Success(_walletInfo.value.balance)

                Timber.i("Payment successful: $amount Rials, new balance: ${_walletInfo.value.balance}")

                // Auto dismiss after success
                delay(1500)
                dismissTopUpSheet()

            } catch (e: Exception) {
                Timber.e(e, "Payment failed")
                _topUpState.value = TopUpState.Error("خطا در پرداخت. لطفا دوباره تلاش کنید")
            }
        }
    }

    fun resetTopUpState() {
        _topUpState.value = TopUpState.Idle
    }

    // ==================== Utility ====================

    fun refreshWallet() {
        loadWalletInfo()
    }
}