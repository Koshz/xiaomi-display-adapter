package com.obabo.xiaomihdmiadapter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

private val AppBackground = Color(0xFF0E1117)
private val Panel = Color(0xFF151A22)
private val PanelAlt = Color(0xFF10151D)
private val Border = Color(0xFF293241)
private val TextPrimary = Color(0xFFF4F7FB)
private val TextMuted = Color(0xFF9AA6B8)
private val Green = Color(0xFF35D07F)
private val Blue = Color(0xFF7AA2FF)
private val Amber = Color(0xFFFFC857)
private val Red = Color(0xFFFF6B81)

@Composable
fun AdapterScreen(
    status: AdapterStatus,
    displayInfo: ExternalDisplayInfo?,
    onPowerClick: () -> Unit
) {
    val accent by animateColorAsState(targetValue = status.accentColor(), label = "accent")
    val active = status.isEngaged()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF111722), AppBackground)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            val landscape = maxWidth > maxHeight
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TopBar(status = status, accent = accent)

                if (landscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ConnectionPanel(
                            status = status,
                            displayInfo = displayInfo,
                            accent = accent,
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight()
                        )
                        PowerPanel(
                            status = status,
                            accent = accent,
                            active = active,
                            onPowerClick = onPowerClick,
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    PowerPanel(
                        status = status,
                        accent = accent,
                        active = active,
                        onPowerClick = onPowerClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ConnectionPanel(
                        status = status,
                        displayInfo = displayInfo,
                        accent = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(status: AdapterStatus, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HDMI Adapter",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Landscape stretch output",
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        StatusPill(text = status.label(), color = accent)
    }
}

@Composable
private fun PowerPanel(
    status: AdapterStatus,
    accent: Color,
    active: Boolean,
    onPowerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = status !is AdapterStatus.PreparingLandscape &&
        status !is AdapterStatus.NeedsCapturePermission

    BoxWithConstraints(
        modifier = modifier
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(Panel, RoundedCornerShape(8.dp))
            .padding(18.dp)
    ) {
        val compact = maxHeight < 330.dp
        val buttonSize = minOf(maxWidth * 0.58f, maxHeight * 0.42f).coerceIn(104.dp, 188.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = status.headline(),
                color = TextPrimary,
                fontSize = if (compact) 17.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
            PowerButton(
                color = accent,
                active = active,
                enabled = enabled,
                onClick = onPowerClick,
                modifier = Modifier.size(buttonSize)
            )
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 16.dp))
            Text(
                text = status.detail(),
                color = TextMuted,
                fontSize = if (compact) 12.sp else 14.sp,
                lineHeight = if (compact) 16.sp else 19.sp,
                textAlign = TextAlign.Center,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConnectionPanel(
    status: AdapterStatus,
    displayInfo: ExternalDisplayInfo?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(PanelAlt, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Signal")
            MetricRow("HDMI", displayInfo?.name ?: "Disconnected", displayInfo != null)
            MetricRow("Output", displayInfo?.resolutionText() ?: "No display", displayInfo != null)
            MetricRow("Capture", status.captureText(), status is AdapterStatus.On)
        }

        Spacer(modifier = Modifier.height(16.dp))

        PipelineStrip(status = status, accent = accent)

        if (status is AdapterStatus.Error) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = status.message,
                color = Color(0xFFFFB3C0),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PowerButton(
    color: Color,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.45f
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent),
        modifier = modifier
            .border(2.dp, color.copy(alpha = alpha), CircleShape)
            .background(color.copy(alpha = if (active) 0.18f else 0.08f), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.46f)) {
            val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = 132f,
                sweepAngle = 276f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.13f),
                size = Size(size.width * 0.84f, size.height * 0.78f),
                style = stroke
            )
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(size.width / 2f, size.height * 0.02f),
                end = Offset(size.width / 2f, size.height * 0.42f),
                strokeWidth = size.minDimension * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PipelineStrip(status: AdapterStatus, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Pipeline")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PipelineStep("HDMI", status.pipelineIndex() >= 1, accent, Modifier.weight(1f))
            PipelineStep("LAND", status.pipelineIndex() >= 2, accent, Modifier.weight(1f))
            PipelineStep("CAP", status.pipelineIndex() >= 3, accent, Modifier.weight(1f))
            PipelineStep("ON", status.pipelineIndex() >= 4, accent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PipelineStep(
    label: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .border(1.dp, if (active) accent else Border, RoundedCornerShape(8.dp))
            .background(
                if (active) accent.copy(alpha = 0.12f) else Color(0xFF121821),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) TextPrimary else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(Color(0xFF121821), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = value,
            color = if (active) TextPrimary else Color(0xFF768196),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

private fun ExternalDisplayInfo.resolutionText(): String {
    val refresh = if (refreshRate > 0f) {
        " ${String.format(Locale.US, "%.0f", refreshRate)} Hz"
    } else {
        ""
    }
    return "$width x $height$refresh"
}

private fun AdapterStatus.accentColor(): Color {
    return when (this) {
        AdapterStatus.Off -> Blue
        AdapterStatus.MissingHdmi -> Amber
        AdapterStatus.PreparingLandscape -> Amber
        AdapterStatus.NeedsCapturePermission -> Amber
        AdapterStatus.Starting -> Blue
        is AdapterStatus.On -> Green
        is AdapterStatus.Error -> Red
    }
}

private fun AdapterStatus.isEngaged(): Boolean {
    return this is AdapterStatus.PreparingLandscape ||
        this is AdapterStatus.NeedsCapturePermission ||
        this is AdapterStatus.Starting ||
        this is AdapterStatus.On
}

private fun AdapterStatus.label(): String {
    return when (this) {
        AdapterStatus.Off -> "READY"
        AdapterStatus.MissingHdmi -> "NO HDMI"
        AdapterStatus.PreparingLandscape -> "ROTATING"
        AdapterStatus.NeedsCapturePermission -> "CONSENT"
        AdapterStatus.Starting -> "STARTING"
        is AdapterStatus.On -> "LIVE"
        is AdapterStatus.Error -> "ERROR"
    }
}

private fun AdapterStatus.headline(): String {
    return when (this) {
        AdapterStatus.Off -> "Adapter off"
        AdapterStatus.MissingHdmi -> "Connect HDMI"
        AdapterStatus.PreparingLandscape -> "Preparing landscape"
        AdapterStatus.NeedsCapturePermission -> "Android permission"
        AdapterStatus.Starting -> "Opening output"
        is AdapterStatus.On -> "Adapter live"
        is AdapterStatus.Error -> "Stopped"
    }
}

private fun AdapterStatus.detail(): String {
    return when (this) {
        AdapterStatus.Off -> "Landscape capture will stretch to the external monitor."
        AdapterStatus.MissingHdmi -> "External display is not available."
        AdapterStatus.PreparingLandscape -> "Waiting for Android to rotate the source."
        AdapterStatus.NeedsCapturePermission -> "Confirm screen capture in the system dialog."
        AdapterStatus.Starting -> "Creating the HDMI presentation."
        is AdapterStatus.On -> "Rendering ${captureWidth} x ${captureHeight} to HDMI."
        is AdapterStatus.Error -> "Fix the issue and start again."
    }
}

private fun AdapterStatus.captureText(): String {
    return when (this) {
        is AdapterStatus.On -> "$captureWidth x $captureHeight"
        AdapterStatus.PreparingLandscape -> "Waiting"
        AdapterStatus.NeedsCapturePermission -> "Pending"
        AdapterStatus.Starting -> "Starting"
        else -> "Inactive"
    }
}

private fun AdapterStatus.pipelineIndex(): Int {
    return when (this) {
        AdapterStatus.Off -> 0
        AdapterStatus.MissingHdmi -> 0
        AdapterStatus.PreparingLandscape -> 2
        AdapterStatus.NeedsCapturePermission -> 2
        AdapterStatus.Starting -> 3
        is AdapterStatus.On -> 4
        is AdapterStatus.Error -> 0
    }
}
