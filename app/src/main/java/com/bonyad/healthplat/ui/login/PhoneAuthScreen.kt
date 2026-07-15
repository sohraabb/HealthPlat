package com.bonyad.healthplat.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.domain.model.AuthState
import com.bonyad.healthplat.R
import kotlinx.coroutines.delay

@Composable
fun PhoneAuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onPhoneSubmitted: (phoneNumber: String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Track if user has tried to submit - only show error after submit attempt
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // Validation state
    val isPhoneComplete = phoneNumber.length == 11
    val isPhoneValid = viewModel.isValidPersianPhoneNumber(phoneNumber)
    val shouldShowError = hasAttemptedSubmit && isPhoneComplete && !isPhoneValid


    // Reset error state when user changes phone number
    LaunchedEffect(phoneNumber) {
        if (hasAttemptedSubmit && isPhoneValid) {
            hasAttemptedSubmit = false
        }
    }

    // Navigate when OTP is sent
    LaunchedEffect(authState) {
        if (authState is AuthState.PhoneSubmitted) {
            keyboardController?.hide()
            delay(100)
            onPhoneSubmitted(phoneNumber)
            viewModel.resetAuthState()
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    }
                    .padding(padding)

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    // Logo/Icon
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(160.dp)
                    )

                    // Brand name
                    Image(
                        painter = painterResource(id = R.drawable.zhivan_text),
                        contentDescription = "Zhivan",
                        modifier = Modifier.width(100.dp)
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Section title - CENTERED
                    Text(
                        text = "ثبت‌نام یا ورود",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFF383838),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Phone number input with error handling
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { viewModel.updatePhoneNumber(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "شماره همراه خود را وارد کنید",
                                    color = Color(0xFF868686),
                                )
                            },
                            label = {
                                Text(text = "شماره همراه", color = Color(0xFF383838))
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    hasAttemptedSubmit = true
                                    if (isPhoneComplete && isPhoneValid) {
                                        viewModel.sendOtp()
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (shouldShowError) Color(0xFFFF3B30) else Color(0xFF5BA3A3),
                                unfocusedBorderColor = if (shouldShowError) Color(0xFFFF3B30) else Color(0xFFE0E0E0),
                                errorBorderColor = Color(0xFFE53935),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Start,
                                color = Color.Black,
                                textDirection = TextDirection.Ltr
                            ),
                            isError = shouldShowError
                        )

                        // Error message
                        if (shouldShowError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "شماره وارد شده نادرست است",
                                color = Color(0xFFFF3B30),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            hasAttemptedSubmit = true

                            if (isPhoneComplete && isPhoneValid) {
                                viewModel.sendOtp()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPhoneComplete && isPhoneValid)
                                Color(0xFF5BA3A3)
                            else
                                Color(0xFFE0E0E0),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        enabled = isPhoneComplete && authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "ارسال کد یکبار مصرف",
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
}