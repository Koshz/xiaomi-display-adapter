package com.obabo.xiaomihdmiadapter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obabo.xiaomihdmiadapter.core.ExternalDisplayInfo
import com.obabo.xiaomihdmiadapter.state.AdapterStatus
import java.util.Locale

@Composable
fun AdapterScreen(
    status: AdapterStatus,
    displayInfo: ExternalDisplayInfo?,
    onPowerClick: () -> Unit
) {
    val isOn = status is AdapterStatus.On ||
        status is AdapterStatus.Starting ||
        status is AdapterStatus.PreparingLandscape ||
        status is AdapterStatus.NeedsCapturePermission
    val buttonColor by animateColorAsState(
        targetValue = when {
            status is AdapterStatus.Error -> Color(0xFFE85D75)
            isOn -> Color(0xFF3DDC84)
            else -> Color(0xFF8DA2FB)
        },
        label = "buttonColor"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF111318)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Header(displayInfo = displayInfo)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerButton(
                    color = buttonColor,
                    isOn = isOn,
                    enabled = status !is AdapterStatus.PreparingLandscape &&
                        status !is AdapterStatus.NeedsCapturePermission,
                    onClick = onPowerClick
                )
                Spacer(modifier = Modifier.height(28.dp))
                StatusText(status = status)
            }

            Footer(status = status, displayInfo = displayInfo)
        }
    }
}

@Composable
private fun Header(displayInfo: ExternalDisplayInfo?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HDMI Adapter",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = displayInfo?.let {
                val refresh = if (it.refreshRate > 0f) {
                    " ${String.format(Locale.US, "%.0f", it.refreshRate)} Hz"
                } else {
                    ""
                }
                "${it.width} x ${it.height}$refresh"
            } ?: "No HDMI display",
            color = if (displayInfo == null) Color(0xFFFFC857) else Color(0xFFBAC2D9),
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PowerButton(
    color: Color,
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .size(212.dp)
            .border(3.dp, color.copy(alpha = if (enabled) 1f else 0.45f), CircleShape)
            .background(color.copy(alpha = if (isOn) 0.16f else 0.08f), CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(92.dp)) {
                val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = color,
                    startAngle = 132f,
                    sweepAngle = 276f,
                    useCenter = false,
                    topLeft = Offset(8.dp.toPx(), 12.dp.toPx()),
                    size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    color = color,
                    start = Offset(size.width / 2f, 4.dp.toPx()),
                    end = Offset(size.width / 2f, 40.dp.toPx()),
                    strokeWidth = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun StatusText(status: AdapterStatus) {
    val text = when (status) {
        AdapterStatus.Off -> "OFF"
        AdapterStatus.MissingHdmi -> "HDMI NOT FOUND"
        AdapterStatus.PreparingLandscape -> "ROTATING"
        AdapterStatus.NeedsCapturePermission -> "WAITING FOR ANDROID"
        AdapterStatus.Starting -> "STARTING"
        is AdapterStatus.On -> "ON"
        is AdapterStatus.Error -> "ERROR"
    }

    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Footer(status: AdapterStatus, displayInfo: ExternalDisplayInfo?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2A3142), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FooterRow(label = "Display", value = displayInfo?.name ?: "Disconnected")
        FooterRow(label = "Mode", value = status.modeValue())
        if (status is AdapterStatus.Error) {
            Text(
                text = status.message,
                color = Color(0xFFFFB3C0),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun FooterRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF8D96AD),
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color(0xFFE3E7F2),
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun AdapterStatus.modeValue(): String {
    return when (this) {
        AdapterStatus.Off -> "Idle"
        AdapterStatus.MissingHdmi -> "Waiting for HDMI"
        AdapterStatus.PreparingLandscape -> "Preparing landscape"
        AdapterStatus.NeedsCapturePermission -> "Capture consent"
        AdapterStatus.Starting -> "Opening HDMI"
        is AdapterStatus.On -> "${captureWidth} x ${captureHeight} stretch"
        is AdapterStatus.Error -> "Stopped"
    }
}
