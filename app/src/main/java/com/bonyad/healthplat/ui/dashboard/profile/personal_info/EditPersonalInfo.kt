package com.bonyad.healthplat.ui.dashboard.profile.personal_info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.profile.GenderPickerBottomSheet
import com.bonyad.healthplat.ui.profile.PersianDatePickerBottomSheet
import com.bonyad.healthplat.ui.profile.PersonalInfoUiState
import com.bonyad.healthplat.ui.profile.PersonalInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalInfoScreen(
    onBack: () -> Unit,
    viewModel: PersonalInfoViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val birthDate by viewModel.birthDate.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val email by viewModel.email.collectAsState()
    val nationalCode by viewModel.nationalCode.collectAsState()
    val disease by viewModel.disease.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val isFormValid by viewModel.isEditFormValid.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("اطلاعات کاربری", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.back_arrow), contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF5F5F5))
                )
            },
            bottomBar = {
                Button(
                    onClick = { viewModel.savePersonalInfo() },
                    modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                    enabled = isFormValid && uiState !is PersonalInfoUiState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BA3A3))
                ) {
                    Text("ذخیره", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Field
                CustomEditField(label = "نام و نام خانوادگی", value = name, onValueChange = { viewModel.updateName(it) })

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomEditField(label = "قد", value = height, modifier = Modifier.weight(1f), onValueChange = { viewModel.updateHeight(it) })
                    CustomEditField(label = "تاریخ تولد", value = birthDate, modifier = Modifier.weight(1f), readOnly = true, onClick = { viewModel.onDatePickerClick() })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomEditField(label = "جنسیت", value = gender, modifier = Modifier.weight(1f), readOnly = true, onClick = { viewModel.onGenderPickerClick() })
                    CustomEditField(label = "وزن", value = weight, modifier = Modifier.weight(1f), onValueChange = { viewModel.updateWeight(it) })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomEditField(label = "کد ملی", value = nationalCode, modifier = Modifier.weight(1f), onValueChange = { viewModel.updateNationalCode(it) })
                    // Note: Phone number isn't in your current API snippet, but is in the screenshot
                    CustomEditField(label = "شماره تماس", value = "۰۹۱۰۹۸۵۹۵۰۸", modifier = Modifier.weight(1.5f), readOnly = true)
                }

                CustomEditField(label = "بیماری زمینه ای", value = disease, readOnly = true, trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) })
                CustomEditField(label = "ایمیل", value = email, onValueChange = { viewModel.updateEmail(it) })
            }
        }

        // Reusing your existing pickers
        if (viewModel.showDatePicker.collectAsState().value) {
            PersianDatePickerBottomSheet(onDismiss = { viewModel.onDatePickerDismiss() }, onDateSelected = { y, m, d -> viewModel.onDateSelected(y, m, d) })
        }
    }
}

@Composable
fun CustomEditField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Box(modifier = modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            readOnly = readOnly,
            enabled = !readOnly || onClick != null,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedBorderColor = Color(0xFF5BA3A3)
            ),
            textStyle = TextStyle(textAlign = TextAlign.Start, fontSize = 16.sp, color = Color.Black)
        )
        // Label overlay
        Text(
            text = label,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = (-10).dp).background(Color(0xFFF5F5F5)).padding(horizontal = 4.dp),
            style = TextStyle(color = Color.Gray, fontSize = 12.sp)
        )
    }
}