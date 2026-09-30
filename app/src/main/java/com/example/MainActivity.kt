package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.screens.DeliveryDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.RecordDeliveryScreen
import com.example.ui.theme.FieldCaptureTheme

sealed class Screen {
    object Home : Screen()
    object Record : Screen()
    object Queue : Screen()
    data class Detail(val deliveryId: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FieldCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FieldCaptureApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FieldCaptureApp(viewModel: MainViewModel) {
    var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val currentScreen = navigationStack.lastOrNull() ?: Screen.Home

    fun navigateTo(screen: Screen) {
        navigationStack = navigationStack + screen
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    // Handle system back gesture
    if (navigationStack.size > 1) {
        BackHandler {
            navigateBack()
        }
    }

    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToRecord = { navigateTo(Screen.Record) },
                onNavigateToQueue = { navigateTo(Screen.Queue) },
                onDeliveryClick = { delivery ->
                    navigateTo(Screen.Detail(delivery.id))
                }
            )
        }
        is Screen.Record -> {
            RecordDeliveryScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBack() },
                onDeliverySaved = {
                    // Navigate to Queue screen so the user immediately sees the saved record
                    navigationStack = listOf(Screen.Home, Screen.Queue)
                }
            )
        }
        is Screen.Queue -> {
            QueueScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBack() },
                onDeliveryClick = { delivery ->
                    navigateTo(Screen.Detail(delivery.id))
                }
            )
        }
        is Screen.Detail -> {
            DeliveryDetailScreen(
                deliveryId = currentScreen.deliveryId,
                viewModel = viewModel,
                onNavigateBack = { navigateBack() }
            )
        }
    }
}
