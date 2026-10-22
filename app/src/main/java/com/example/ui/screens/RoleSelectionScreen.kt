package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RoleSelectionScreen(
    onSelectRole: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("role_selection_screen")
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Select Your Role",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose how you want to use the app",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Student Role Card
        AnimatedRoleCard(
            title = "Student",
            subtitle = "Mark your attendance via BLE & Face verification",
            icon = Icons.Default.School,
            onClick = { onSelectRole(UserRole.STUDENT) },
            testTag = "role_card_student"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Faculty Role Card
        AnimatedRoleCard(
            title = "Faculty",
            subtitle = "Broadcast sessions & monitor attendance",
            icon = Icons.Default.CoPresent,
            onClick = { onSelectRole(UserRole.FACULTY) },
            testTag = "role_card_faculty"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Admin Role Card
        AnimatedRoleCard(
            title = "Admin",
            subtitle = "Manage faculty, classes & ESP32 devices",
            icon = Icons.Default.AdminPanelSettings,
            onClick = { onSelectRole(UserRole.ADMIN) },
            testTag = "role_card_admin"
        )
    }
}

@Composable
private fun AnimatedRoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "role_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) BrandPrimaryBlue else SurfaceBorder,
        animationSpec = tween(durationMillis = 200),
        label = "role_border"
    )

    val iconBgColor by animateColorAsState(
        targetValue = if (isPressed) BrandPrimaryBlue else PaletteLightBlue,
        animationSpec = tween(durationMillis = 200),
        label = "role_icon_bg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isPressed) Color.White else BrandPrimaryBlue,
        animationSpec = tween(durationMillis = 200),
        label = "role_icon_tint"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Select",
                tint = if (isPressed) BrandPrimaryBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
