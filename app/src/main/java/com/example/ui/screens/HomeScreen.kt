package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Delivery
import com.example.sync.ConnectionStatus
import com.example.sync.NetworkOverrideMode
import com.example.ui.MainViewModel
import com.example.ui.components.DeliveryItemCard
import com.example.ui.components.NetworkStatusBar
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToRecord: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onDeliveryClick: (Delivery) -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val counts by viewModel.deliveryCounts.collectAsStateWithLifecycle()
    val deliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()
    val overrideMode by viewModel.overrideMode.collectAsStateWithLifecycle()

    var showTestingTools by remember { mutableStateOf(false) }
    var recoveryFeedbackMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafetyAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "FieldCapture Logo",
                                tint = Navy900,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FieldCapture",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Navy900
                            )
                            Text(
                                text = "Material Delivery System",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate700
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTestingTools = !showTestingTools },
                        modifier = Modifier.testTag("toggle_dev_tools_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Demo Testing Tools",
                            tint = if (showTestingTools) SafetyAmber else Slate700
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Network Status Component (Section 17)
                NetworkStatusBar(
                    connectionStatus = connectionStatus,
                    isSyncing = isSyncing,
                    statusMessage = statusMessage
                )
            }

            // Developer Testing Panel (Section 22 & 26 for quick offline/online/recovery testing)
            if (showTestingTools) {
                item {
                    TestingControlsCard(
                        currentMode = overrideMode,
                        onModeSelected = { viewModel.setNetworkOverrideMode(it) },
                        onSimulateCrashRecovery = {
                            viewModel.simulateAppKillRecovery { count ->
                                recoveryFeedbackMessage = if (count > 0) {
                                    "Recovered $count deliveries from UPLOADING back to QUEUED!"
                                } else {
                                    "No interrupted uploads to recover."
                                }
                            }
                        },
                        onManualSync = { viewModel.retryAll() },
                        feedbackMessage = recoveryFeedbackMessage
                    )
                }
            }

            // Delivery Counts Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CountCard(
                        title = "Queued",
                        count = counts.queuedCount,
                        subtitle = "Pending Sync",
                        icon = Icons.Default.CloudQueue,
                        color = SafetyAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("queued_count_card")
                    )
                    CountCard(
                        title = "Synced",
                        count = counts.syncedCount,
                        subtitle = "Safe in DB",
                        icon = Icons.Default.CheckCircle,
                        color = StatusSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("synced_count_card")
                    )
                    if (counts.failedCount > 0) {
                        CountCard(
                            title = "Failed",
                            count = counts.failedCount,
                            subtitle = "Needs Retry",
                            icon = Icons.Default.ErrorOutline,
                            color = StatusError,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("failed_count_card")
                        )
                    }
                }
            }

            // Main Actions Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Action: Record Material Delivery
                    Button(
                        onClick = onNavigateToRecord,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("record_material_delivery_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy800,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Record Material Delivery",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    // Secondary Action: View Queue
                    OutlinedButton(
                        onClick = onNavigateToQueue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("view_queue_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Navy800
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Queue (${counts.total} Deliveries)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }

            // Recent Deliveries Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Navy900
                    )

                    if (deliveries.isNotEmpty()) {
                        Text(
                            text = "${deliveries.size} total entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }

            // Recent Deliveries List
            if (deliveries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No deliveries recorded yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Take photos of delivery tickets even when offline. They are saved safely on device and synced when connected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(deliveries.take(4)) { delivery ->
                    DeliveryItemCard(
                        delivery = delivery,
                        onClick = { onDeliveryClick(delivery) },
                        onRetryClick = { viewModel.retryDelivery(delivery.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CountCard(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Slate700
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                ),
                color = Navy900
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate400
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestingControlsCard(
    currentMode: NetworkOverrideMode,
    onModeSelected: (NetworkOverrideMode) -> Unit,
    onSimulateCrashRecovery: () -> Unit,
    onManualSync: () -> Unit,
    feedbackMessage: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Demo & Assessment Test Controls",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Navy900
                )
                Text(
                    text = "Section 22",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate700
                )
            }

            Text(
                text = "Simulate real-world field scenarios with zero physical network adjustments:",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Slate700
            )

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = currentMode == NetworkOverrideMode.SYSTEM_REAL,
                    onClick = { onModeSelected(NetworkOverrideMode.SYSTEM_REAL) },
                    label = { Text("Auto (Real)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = currentMode == NetworkOverrideMode.SIMULATE_OFFLINE,
                    onClick = { onModeSelected(NetworkOverrideMode.SIMULATE_OFFLINE) },
                    label = { Text("Force Offline", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                )
                FilterChip(
                    selected = currentMode == NetworkOverrideMode.SIMULATE_ONLINE,
                    onClick = { onModeSelected(NetworkOverrideMode.SIMULATE_ONLINE) },
                    label = { Text("Force Online", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentMode == NetworkOverrideMode.SIMULATE_SERVER_500,
                    onClick = { onModeSelected(NetworkOverrideMode.SIMULATE_SERVER_500) },
                    label = { Text("Simulate 500 Error (Test Retry)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateCrashRecovery,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Crash Recovery", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onManualSync,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Queue Now", fontSize = 11.sp)
                }
            }

            if (feedbackMessage != null) {
                Text(
                    text = feedbackMessage,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = SafetyAmber
                )
            }
        }
    }
}
