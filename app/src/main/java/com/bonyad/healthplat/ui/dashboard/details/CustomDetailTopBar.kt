package com.bonyad.healthplat.ui.dashboard.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bonyad.healthplat.R

@Composable
fun CustomDetailTopBar(
    title: String,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Title - Absolutely centered on screen
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        // Left icons
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painterResource(R.drawable.info_circle),
                    contentDescription = "Info",
                    tint = Color.Gray
                )
            }

            IconButton(
                onClick = onSync,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painterResource(R.drawable.refresh_square),
                    contentDescription = "Sync",
                    tint = Color.Gray
                )
            }
        }

        // Right icon
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                painterResource(R.drawable.close_square),
                contentDescription = "Close",
                tint = Color.Gray
            )
        }
    }
}