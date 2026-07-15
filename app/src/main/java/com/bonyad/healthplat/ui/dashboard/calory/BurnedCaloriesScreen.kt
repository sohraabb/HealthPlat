package com.bonyad.healthplat.ui.dashboard.calory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.ActivityFact
import com.bonyad.healthplat.domain.model.UserActivity
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

// ============ Burned Calories Detail Screen ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnedCaloriesScreen(
    viewModel: CaloryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val burnedCalories by viewModel.burnedCalories.collectAsState()
    val loggedActivities by viewModel.loggedActivities.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showAddActivitySheet by remember { mutableStateOf(false) }

    val currentDate = remember(selectedDate) { PersianDateUtils.getFormattedPersianDate(selectedDate) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "کالری سوخته",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(
                                painter = painterResource(R.drawable.notification),
                                contentDescription = "Notifications",
                                tint = TextGray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.back_arrow),
                                contentDescription = "Back",
                                tint = TextDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BackgroundColor
                    )
                )
            },
            containerColor = BackgroundColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                BurnedCalorieGaugeCard(
                    title = "کالری سوخته",
                    date = currentDate,
                    calories = burnedCalories,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ActivityGridWithAddButton(
                    activities = loggedActivities,
                    onAddClick = { showAddActivitySheet = true }
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddActivitySheet) {
        AddActivityBottomSheet(
            viewModel = viewModel,
            onDismiss = {
                viewModel.clearActivityFact()
                showAddActivitySheet = false
            }
        )
    }
}

// ============ Burned Calorie Gauge Card ============

@Composable
fun BurnedCalorieGaugeCard(
    title: String,
    date: String,
    calories: Int,
    modifier: Modifier = Modifier
) {
    val maxCalories = 500
    val progress = (calories.toFloat() / maxCalories).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF6B6B6B)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6B6B)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    drawArc(
                        color = Color(0xFFE8E8E8),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = TealPrimary,
                        startAngle = 180f,
                        sweepAngle = 180f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fire_4),
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = calories.toString().toFarsiDigits(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = TextDark
                    )
                    Text(
                        text = "کیلو کالری",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B)
                    )
                }
            }
        }
    }
}

// ============ Activity Grid with Add Button ============

@Composable
fun ActivityGridWithAddButton(
    activities: List<UserActivity>,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // null sentinel represents the "Add" card
        val itemsWithAdd: List<UserActivity?> = activities + null

        itemsWithAdd.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    if (item != null) {
                        ActivityCard(activity = item, modifier = Modifier.weight(1f))
                    } else {
                        AddActivityCard(onClick = onAddClick, modifier = Modifier.weight(1f))
                    }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ============ Activity Card ============

@Composable
fun ActivityCard(
    activity: UserActivity,
    modifier: Modifier = Modifier
) {
    // Convert hours to a readable duration string
    val totalMinutes = (activity.duration * 60).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val durationText = when {
        hours > 0 && minutes > 0 -> "${hours.toString().toFarsiDigits()} ساعت و ${minutes.toString().toFarsiDigits()} دقیقه"
        hours > 0 -> "${hours.toString().toFarsiDigits()} ساعت"
        else -> "${minutes.toString().toFarsiDigits()} دقیقه"
    }

    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.activityName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF6B6B6B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.walk),
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = durationText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کالری از بین رفته:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6B6B)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fire_4),
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = activity.activityCal.toInt().toString().toFarsiDigits(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "kc",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B)
                    )
                }
            }
        }
    }
}

// ============ Add Activity Card ============

@Composable
fun AddActivityCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "اضافه کردن فعالیت جدید",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, TealPrimary, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Add Activity",
                    tint = TealPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ============ Add Activity Bottom Sheet ============

private enum class AddActivityPhase { SEARCH, DURATION }

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AddActivityBottomSheet(
    viewModel: CaloryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchResults by viewModel.activitySearchResults.collectAsState()
    val isSearchLoading by viewModel.isActivitySearchLoading.collectAsState()
    val hasMorePages by viewModel.hasMoreActivityPages.collectAsState()
    val selectedFact by viewModel.selectedActivityFact.collectAsState()

    // Phase switches to DURATION once the user picks an activity
    val phase = if (selectedFact != null) AddActivityPhase.DURATION else AddActivityPhase.SEARCH

    var searchQuery by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }

    // Debounce search so we don't fire on every keystroke
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(400)
            .collect { query -> viewModel.searchActivities(query) }
    }

    val listState = rememberLazyListState()

    // Trigger load-more when user scrolls near the end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                if (lastVisible != null &&
                    lastVisible >= searchResults.size - 4 &&
                    hasMorePages
                ) {
                    viewModel.loadMoreActivities()
                }
            }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (phase == AddActivityPhase.SEARCH) "جستجوی فعالیت" else "مدت زمان",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextDark
                    )
                    if (phase == AddActivityPhase.DURATION) {
                        IconButton(
                            onClick = { viewModel.clearActivityFact() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت به جستجو",
                                tint = TextGray
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(1.dp, Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (phase) {

                    // ── Phase 1: Search ──────────────────────────────────────
                    AddActivityPhase.SEARCH -> {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("دویدن، پیاده‌روی، شنا...".rtl(), color = Color(0xFF868686))
                            },
                            label = { Text("جستجو", color = Color(0xFF383838)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isSearchLoading && searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TealPrimary)
                            }
                        } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "نتیجه‌ای یافت نشد",
                                    color = TextGray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.height(360.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items(searchResults) { fact ->
                                    ActivityFactRow(
                                        fact = fact,
                                        onClick = { viewModel.selectActivityFact(fact) }
                                    )
                                    HorizontalDivider(color = Color(0xFFF0F0F0))
                                }
                                if (isSearchLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = TealPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Phase 2: Duration picker ─────────────────────────────
                    AddActivityPhase.DURATION -> {
                        val fact = selectedFact!!

                        // Selected activity info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4F4))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = fact.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextDark
                                )
                                Text(
                                    text = fact.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                                Text(
                                    text = "MET ${fact.cal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "چه مدت این فعالیت را انجام دادید؟",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = hours,
                                onValueChange = { if (it.length <= 2) hours = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("ساعت", color = Color(0xFF383838)) },
                                placeholder = { Text("۰", color = Color(0xFF868686)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            )
                            OutlinedTextField(
                                value = minutes,
                                onValueChange = { v ->
                                    val n = v.toIntOrNull()
                                    if (v.isEmpty() || (n != null && n < 60)) minutes = v
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("دقیقه", color = Color(0xFF383838)) },
                                placeholder = { Text("۰", color = Color(0xFF868686)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        val durationHours = (hours.toIntOrNull() ?: 0) +
                                (minutes.toIntOrNull() ?: 0) / 60.0
                        val isValid = durationHours > 0.0

                        Button(
                            onClick = {
                                if (isValid) {
                                    viewModel.logActivity(durationHours)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            enabled = isValid
                        ) {
                            Text(
                                text = "ثبت فعالیت",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Activity Fact Row (search result item) ───────────────────────────────────

@Composable
private fun ActivityFactRow(
    fact: ActivityFact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fact.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = fact.category,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "MET ${fact.cal}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TealPrimary
        )
    }
}
