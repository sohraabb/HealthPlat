package com.bonyad.healthplat.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bonyad.healthplat.domain.model.AuthState
import com.bonyad.healthplat.R
import com.yourpackage.healthplat.ui.auth.AuthViewModel

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    viewModel: AuthViewModel = hiltViewModel(),
    onVerified: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val otp by viewModel.otp.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Auto-submit when 5 digits entered
    LaunchedEffect(otp) {
        if (otp.length == 5) {
            viewModel.verifyOtp()
        }
    }

    // Navigate when verified
    LaunchedEffect(authState) {
        if (authState is AuthState.OtpVerified) {
            onVerified()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            snackbarHostState.showSnackbar((authState as AuthState.Error).message)
            viewModel.resetError()
        }
    }

    // Request focus on mount
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Logo/Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "تن‌بار",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF2C2C2C)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "ثبت‌نام یا ورود",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF2C2C2C)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "کد پیامک شده",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Input boxes
                OtpInputField(
                    otp = otp,
                    onOtpChange = { viewModel.updateOtp(it) },
                    focusRequester = focusRequester
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone number and resend text
                Text(
                    text = "کد به شماره ۰۹۹۱۸۴۷۸۱۰۰ ارسال شد. تغییر شماره همراه",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                // Verify button
                Button(
                    onClick = { viewModel.verifyOtp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (otp.length == 5)
                            Color(0xFF5BA3A3)
                        else
                            Color(0xFFE0E0E0)
                    ),
                    enabled = otp.length == 5 && authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "ورود",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        // Hidden text field for input
        BasicTextField(
            value = otp,
            onValueChange = onOtpChange,
            modifier = Modifier
                .size(0.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // 5 boxes for OTP display
        repeat(5) { index ->
            OtpBox(
                digit = otp.getOrNull(index)?.toString() ?: "",
                isFilled = index < otp.length
            )
        }
    }
}

@Composable
fun OtpBox(
    digit: String,
    isFilled: Boolean
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isFilled) Color(0xFF5BA3A3) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color(0xFF2C2C2C)
        )
    }
}