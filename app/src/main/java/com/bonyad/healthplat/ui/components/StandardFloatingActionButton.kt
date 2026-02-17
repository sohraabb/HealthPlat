package com.bonyad.healthplat.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bonyad.healthplat.ui.dashboard.calory.TealPrimary

/**
 * Standardized Floating Action Button for consistent appearance across the app
 *
 * @param onClick Action to perform when the FAB is clicked
 * @param icon Resource ID of the icon drawable to display
 * @param contentDescription Accessibility description for the icon
 * @param modifier Optional modifier for the FAB
 */
@Composable
fun StandardFloatingActionButton(
    onClick: () -> Unit,
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.White,
        contentColor = TealPrimary,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        modifier = modifier
            .border(1.dp, TealPrimary, RoundedCornerShape(16.dp))
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}