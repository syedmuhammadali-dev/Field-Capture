package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusErrorText
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusInfoText
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusSuccessText
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.StatusWarningText

@Composable
fun DeliveryStatusBadge(
    status: DeliveryStatus,
    modifier: Modifier = Modifier,
    showDetailedLabel: Boolean = false
) {
    val (backgroundColor, textColor, iconColor, label) = when (status) {
        DeliveryStatus.QUEUED -> Quad(
            StatusWarningContainer,
            StatusWarningText,
            StatusWarning,
            if (showDetailedLabel) "Waiting for connection" else "Queued"
        )
        DeliveryStatus.UPLOADING -> Quad(
            StatusInfoContainer,
            StatusInfoText,
            StatusInfo,
            if (showDetailedLabel) "Uploading delivery..." else "Uploading"
        )
        DeliveryStatus.SYNCED -> Quad(
            StatusSuccessContainer,
            StatusSuccessText,
            StatusSuccess,
            if (showDetailedLabel) "Successfully synced" else "Synced"
        )
        DeliveryStatus.FAILED -> Quad(
            StatusErrorContainer,
            StatusErrorText,
            StatusError,
            if (showDetailedLabel) "Sync failed" else "Failed"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                DeliveryStatus.QUEUED -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Queued",
                        tint = iconColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                DeliveryStatus.UPLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        strokeWidth = 1.5.dp,
                        color = iconColor
                    )
                }
                DeliveryStatus.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Synced",
                        tint = iconColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                DeliveryStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Failed",
                        tint = iconColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
