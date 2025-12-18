package com.bonyad.healthplat.ui.access


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPrivacyScreen(
    viewModel: TermsPrivacyViewModel = hiltViewModel(),
    onAccept: () -> Unit,
    onBack: (() -> Unit)? = null,

) {
    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val marketingAccepted by viewModel.marketingAccepted.collectAsState()

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            onAccept()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
//            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.fillMaxSize(),
//                    .verticalScroll(scrollState),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image with gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.terms_ring_img),
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFF5F5F5).copy(alpha = 0.3f),
                                        Color(0xFFF5F5F5).copy(alpha = 0.9f),
                                        Color(0xFFF5F5F5)
                                    )
                                )
                            )
                    )

                    if (onBack != null) {
                        IconButton(
                            onClick = { onBack() },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(start = 24.dp, top = 16.dp)
                                .size(48.dp)
                        ) {
                            Icon(
                                // FIX: Using your custom drawable
                                painter = painterResource(id = R.drawable.back_arrow),
                                contentDescription = "بازگشت",
                                tint = Color(0xFF2C2C2C)
                            )
                        }
                    }
                }

                // Content section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = "شرایط استفاده و سیاست حفظ حریم خصوصی",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instruction text
                    Text(
                        text = "لطفاً موارد زیر را مرور کنید تا بتوانید ادامه دهید:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Terms checkbox
                    TermsCheckboxRow(
                        checked = termsAccepted,
                        onCheckedChange = { viewModel.updateTermsAccepted(it) },
                        text = buildAnnotatedString {
                            append("من ")
                            withStyle(style = SpanStyle(color = Color(0xFF5BA3A3), fontWeight = FontWeight.Bold)) {
                                append("شرایط استفاده")
                            }
                            append(" و ")
                            withStyle(style = SpanStyle(color = Color(0xFF5BA3A3), fontWeight = FontWeight.Bold)) {
                                append("سیاست حفظ حریم خصوصی")
                            }
                            append(" تن‌بار را خوانده‌ام و با آن موافقم.")
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Marketing checkbox
                    TermsCheckboxRow(
                        checked = marketingAccepted,
                        onCheckedChange = { viewModel.updateMarketingAccepted(it) },
                        text = buildAnnotatedString {
                            append("برای بهره‌مندی هرچه بیشتر از تجربه تن‌بار، آموزش‌های اختصاصی سلامت، نکات مفید و پیشنهادهای ویژه دریافت کنید.")
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Accept button
                    Button(
                        onClick = { viewModel.acceptTerms() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (termsAccepted)
                                Color(0xFF5BA3A3)
                            else
                                Color(0xFFE0E0E0),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        enabled = termsAccepted
                    ) {
                        Text(
                            text = "ذخیره و ادامه",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun TermsCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: androidx.compose.ui.text.AnnotatedString
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.End
            ),
            color = Color(0xFF2C2C2C),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF5BA3A3),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}

