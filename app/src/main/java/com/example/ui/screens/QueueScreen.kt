package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Delivery
import com.example.data.model.DeliveryStatus
import com.example.sync.ConnectionStatus
import com.example.ui.MainViewModel
import com.example.ui.components.DeliveryItemCard
import com.example.ui.components.NetworkStatusBar
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.StatusError

enum class QueueFilterTab(val title: String) {
    ALL("All"),
    QUEUED("Queued"),
    UPLOADING("Uploading"),
    SYNCED("Synced"),
    FAILED("Failed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onDeliveryClick: (Delivery) -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()
    val counts by viewModel.deliveryCounts.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(QueueFilterTab.ALL) }

    val filteredDeliveries = remember(deliveries, selectedTab) {
        when (selectedTab) {
            QueueFilterTab.ALL -> deliveries
            QueueFilterTab.QUEUED -> deliveries.filter { it.status == DeliveryStatus.QUEUED }
            QueueFilterTab.UPLOADING -> deliveries.filter { it.status == DeliveryStatus.UPLOADING }
            QueueFilterTab.SYNCED -> deliveries.filter { it.status == DeliveryStatus.SYNCED }
            QueueFilterTab.FAILED -> deliveries.filter { it.status == DeliveryStatus.FAILED }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Offline Queue",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Navy900
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("queue_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Navy900
                        )
                    }
                },
                actions = {
                    if (counts.failedCount > 0) {
                        OutlinedButton(
                            onClick = { viewModel.retryAll() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("retry_all_header_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry All (${counts.failedCount})", fontSize = 12.sp)
                        }
                    } else if (counts.queuedCount > 0 && connectionStatus == ConnectionStatus.ONLINE) {
                        Button(
                            onClick = { viewModel.retryAll() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("sync_all_header_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now", fontSize = 12.sp)
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Network Banner
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                NetworkStatusBar(
                    connectionStatus = connectionStatus,
                    isSyncing = isSyncing,
                    statusMessage = statusMessage
                )
            }

            // Filter Tabs (Scrollable)
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Navy800,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = Navy800,
                        height = 3.dp
                    )
                }
            ) {
                QueueFilterTab.values().forEach { tab ->
                    val badgeCount = when (tab) {
                        QueueFilterTab.ALL -> deliveries.size
                        QueueFilterTab.QUEUED -> counts.queuedCount
                        QueueFilterTab.UPLOADING -> counts.uploadingCount
                        QueueFilterTab.SYNCED -> counts.syncedCount
                        QueueFilterTab.FAILED -> counts.failedCount
                    }

                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($badgeCount)",
                                    fontSize = 11.sp,
                                    color = if (selectedTab == tab) Navy800 else Slate400
                                )
                            }
                        }
                    )
                }
            }

            // Deliveries List
            if (filteredDeliveries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${selectedTab.title.lowercase()} deliveries",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedTab) {
                                QueueFilterTab.QUEUED -> "All items have been synced or there are no pending deliveries."
                                QueueFilterTab.FAILED -> "No failed uploads! Everything uploaded smoothly."
                                QueueFilterTab.SYNCED -> "No deliveries have synced yet."
                                QueueFilterTab.UPLOADING -> "No uploads currently active."
                                QueueFilterTab.ALL -> "Queue is empty. Tap 'Record Material Delivery' to add one."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDeliveries, key = { it.id }) { delivery ->
                        DeliveryItemCard(
                            delivery = delivery,
                            onClick = { onDeliveryClick(delivery) },
                            onRetryClick = { viewModel.retryDelivery(delivery.id) }
                        )
                    }
                }
            }
        }
    }
}
