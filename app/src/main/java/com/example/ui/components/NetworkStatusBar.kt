package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.ConnectionStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusErrorText
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusInfoText
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusSuccessText

@Composable
fun NetworkStatusBar(
    connectionStatus: ConnectionStatus,
    isSyncing: Boolean,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    val isOnline = connectionStatus == ConnectionStatus.ONLINE

    val containerColor by animateColorAsState(
        targetValue = when {
            isSyncing -> StatusInfoContainer
            isOnline -> StatusSuccessContainer
            else -> StatusErrorContainer
        },
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSyncing -> StatusInfoText
            isOnline -> StatusSuccessText
            else -> StatusErrorText
        },
        label = "textColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("network_status_bar"),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status Indicator Dot / Spinner
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) StatusSuccess else StatusError)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = when {
                        isSyncing -> "Syncing Deliveries..."
                        isOnline -> "Online"
                        else -> "Offline"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = contentColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "•  $statusMessage",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = contentColor.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }

            // Status Icon
            Icon(
                imageVector = when {
                    isSyncing -> Icons.Default.Sync
                    isOnline -> Icons.Default.Wifi
                    else -> Icons.Default.WifiOff
                },
                contentDescription = if (isOnline) "Connected" else "Disconnected",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
