package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BleBlue
import com.example.ui.theme.BleRingInner
import com.example.ui.theme.BleRingMiddle
import com.example.ui.theme.BleRingOuter
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.StatusAbsentRedBg
import com.example.ui.theme.StatusAbsentRedText
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Standard Bluetooth Circular Icon Badge used across Splash, Login, Home, and About screens.
 */
@Composable
fun BleCircleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 36.dp,
    backgroundColor: Color = BleBlue,
    iconColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth",
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Pulsing Concentric Ripple Rings animation for BLE Scanning.
 * Optimized with graphicsLayer lambdas to bypass recomposition on low-end hardware.
 */
@Composable
fun BleScanningAnimation(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blePulse")

    val pulse1 = infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )

    val pulse2 = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    val alphaPulse = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric ring
        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    val scale = if (isScanning) pulse2.value else 1.0f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (isScanning) alphaPulse.value * 0.7f else 0.5f
                }
                .clip(CircleShape)
                .background(BleRingOuter)
        )

        // Middle concentric ring
        Box(
            modifier = Modifier
                .size(170.dp)
                .graphicsLayer {
                    val scale = if (isScanning) pulse1.value else 1.0f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (isScanning) alphaPulse.value else 0.8f
                }
                .clip(CircleShape)
                .background(BleRingMiddle)
        )

        // Inner soft ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(BleRingInner)
        )

        // Core Bluetooth Button / Icon
        BleCircleIcon(
            size = 64.dp,
            iconSize = 36.dp,
            backgroundColor = BleBlue,
            iconColor = Color.White,
            modifier = Modifier.testTag("home_ble_icon")
        )
    }
}

/**
 * Clean Top Bar with Back Arrow and Title
 */
@Composable
fun SmartTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("topbar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )
        }
        actions()
    }
}

/**
 * Status Pill Badge for Present / Absent
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isPresent = status.equals("Present", ignoreCase = true)
    val bgColor = if (isPresent) StatusPresentGreenBg else StatusAbsentRedBg
    val textColor = if (isPresent) StatusPresentGreenText else StatusAbsentRedText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = textColor
        )
    }
}

/**
 * Modern Bottom Navigation Bar matching Screens 4, 6, 7 in Reference Image.
 */
enum class BottomNavTab {
    HOME, HISTORY, PROFILE
}

enum class FacultyBottomNavTab {
    HOME, CLASSES, STUDENTS, HISTORY, PROFILE
}

@Composable
fun FacultyBottomNavigationBar(
    currentTab: FacultyBottomNavTab,
    onTabSelected: (FacultyBottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SurfaceWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartBottomNavItem(
                iconSelected = Icons.Filled.Home,
                iconUnselected = Icons.Outlined.Home,
                label = "Home",
                isSelected = currentTab == FacultyBottomNavTab.HOME,
                onClick = { onTabSelected(FacultyBottomNavTab.HOME) },
                testTag = "faculty_nav_home"
            )
            SmartBottomNavItem(
                iconSelected = Icons.Filled.School,
                iconUnselected = Icons.Outlined.School,
                label = "Classes",
                isSelected = currentTab == FacultyBottomNavTab.CLASSES,
                onClick = { onTabSelected(FacultyBottomNavTab.CLASSES) },
                testTag = "faculty_nav_classes"
            )
            SmartBottomNavItem(
                iconSelected = Icons.Filled.People,
                iconUnselected = Icons.Outlined.People,
                label = "Students",
                isSelected = currentTab == FacultyBottomNavTab.STUDENTS,
                onClick = { onTabSelected(FacultyBottomNavTab.STUDENTS) },
                testTag = "faculty_nav_students"
            )
            SmartBottomNavItem(
                iconSelected = Icons.Filled.History,
                iconUnselected = Icons.Outlined.History,
                label = "History",
                isSelected = currentTab == FacultyBottomNavTab.HISTORY,
                onClick = { onTabSelected(FacultyBottomNavTab.HISTORY) },
                testTag = "faculty_nav_history"
            )
            SmartBottomNavItem(
                iconSelected = Icons.Filled.Person,
                iconUnselected = Icons.Outlined.Person,
                label = "Profile",
                isSelected = currentTab == FacultyBottomNavTab.PROFILE,
                onClick = { onTabSelected(FacultyBottomNavTab.PROFILE) },
                testTag = "faculty_nav_profile"
            )
        }
    }
}

@Composable
fun SmartBottomNavigationBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SurfaceWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartBottomNavItem(
                iconSelected = Icons.Filled.Home,
                iconUnselected = Icons.Outlined.Home,
                label = "Home",
                isSelected = currentTab == BottomNavTab.HOME,
                onClick = { onTabSelected(BottomNavTab.HOME) },
                testTag = "nav_home"
            )
            SmartBottomNavItem(
                iconSelected = Icons.Filled.History,
                iconUnselected = Icons.Outlined.History,
                label = "History",
                isSelected = currentTab == BottomNavTab.HISTORY,
                onClick = { onTabSelected(BottomNavTab.HISTORY) },
                testTag = "nav_history"
            )
            SmartBottomNavItem(
                iconSelected = Icons.Filled.Person,
                iconUnselected = Icons.Outlined.Person,
                label = "Profile",
                isSelected = currentTab == BottomNavTab.PROFILE,
                onClick = { onTabSelected(BottomNavTab.PROFILE) },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
private fun SmartBottomNavItem(
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_item_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) BrandPrimaryBlue else TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_color"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) PaletteLightBlue.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "nav_pill_color"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "nav_pill_width"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(if (pillWidth > 0.dp) pillWidth else 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = contentColor
        )
    }
}

/**
 * Modern animated toggle switch with smooth physics-based spring slide animation.
 */
@Composable
fun AnimatedToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) BrandPrimaryBlue else PaletteGray,
        animationSpec = tween(durationMillis = 220),
        label = "switch_track_color"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switch_thumb_offset"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = thumbOffset.toPx()
                }
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Ultra-smooth physics-based bouncing clickable modifier.
 * Executes on the graphicsLayer to avoid recompositions and layout passes on low-end devices.
 */
@Composable
fun Modifier.bouncingClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncing_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
