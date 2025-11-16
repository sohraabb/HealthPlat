package com.bonyad.healthplat.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@Composable
inline fun <reified VM : ViewModel> safeSharedViewModel(
    navController: NavController,
    parentRoute: String,
    backStackEntry: NavBackStackEntry
): VM? {
    val parentEntry = remember(backStackEntry) {
        try {
            navController.getBackStackEntry(parentRoute)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    return parentEntry?.let { hiltViewModel(it) }
}