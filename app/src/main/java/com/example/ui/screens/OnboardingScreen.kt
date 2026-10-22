package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.StatusPresentGreenBg
import com.example.ui.theme.StatusPresentGreenText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val stepIndex: Int,
    val totalSteps: Int,
    val stepLabel: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val imageResId: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val accentGradient: List<Color>,
    val highlights: List<String>
)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            stepIndex = 1,
            totalSteps = 4,
            stepLabel = "STEP 1 OF 4 • ZERO-TOUCH BLE",
            title = "Automatic BLE\nBeacon Detection",
            subtitle = "Smart Proximity Attendance",
            description = "Your smartphone discovers classroom ESP32 beacon transmitters seamlessly in under 3 seconds with cryptographically rolling tokens.",
            imageResId = R.drawable.onboarding_ble_beacon,
            icon = Icons.Default.BluetoothSearching,
            iconColor = BrandPrimaryBlue,
            accentGradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            highlights = listOf(
                "Detects nearby classroom beacon in < 3 seconds",
                "Cryptographically signed rolling session tokens",
                "Ultra low-power background Bluetooth operation"
            )
        ),
        OnboardingPageData(
            stepIndex = 2,
            totalSteps = 4,
            stepLabel = "STEP 2 OF 4 • ANTI-PROXY BIOMETRICS",
            title = "AI Face & Liveness\nVerification",
            subtitle = "Guaranteed Identity Security",
            description = "Triple-layer biometric authentication combining BLE beacon proximity, on-device facial recognition, and real-time blink detection.",
            imageResId = R.drawable.img_onboarding_face,
            icon = Icons.Default.Face,
            iconColor = Color(0xFF059669),
            accentGradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
            highlights = listOf(
                "192-dimensional on-device MobileFaceNet neural embeddings",
                "Active blink & motion liveness anti-spoof checks",
                "Completely eliminates buddy-punching & proxy marking"
            )
        ),
        OnboardingPageData(
            stepIndex = 3,
            totalSteps = 4,
            stepLabel = "STEP 3 OF 4 • CLOUD INTELLIGENCE",
            title = "Real-Time Sync\n& Analytics",
            subtitle = "Instant Records & Export",
            description = "Instant bidirectional synchronization with live Firestore, automated attendance percentages, and one-tap professional PDF/CSV reports.",
            imageResId = R.drawable.img_onboarding_reports,
            icon = Icons.Default.Assessment,
            iconColor = Color(0xFF7C3AED),
            accentGradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
            highlights = listOf(
                "Live lecture rosters & dynamic percentage calculation",
                "One-tap official PDF & CSV attendance export",
                "Seamless offline mode with automatic sync recovery"
            )
        ),
        OnboardingPageData(
            stepIndex = 4,
            totalSteps = 4,
            stepLabel = "STEP 4 OF 4 • UNIFIED PLATFORM",
            title = "Connected Campus\nCollaboration",
            subtitle = "Students, Faculty & Admin Hub",
            description = "A unified ecosystem connecting students to check attendance records, professors to launch lecture sessions, and admins to configure institutions.",
            imageResId = R.drawable.img_onboarding_collaboration,
            icon = Icons.Default.Hub,
            iconColor = Color(0xFFD97706),
            accentGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            highlights = listOf(
                "Tailored dashboards for Students, Faculty, and Admin",
                "Full classroom, student roster & faculty assignment tools",
                "Real-time live session broadcasting & instant verification"
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("onboarding_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: App Branding & Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PaletteLightBlue, Color(0xFFE0F2FE))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = BrandPrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Smart Attendance",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "Next-Gen BLE & Face Verification",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = !isLastPage,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                TextButton(
                    onClick = onGetStarted,
                    modifier = Modifier.testTag("btn_skip_onboarding")
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BrandPrimaryBlue
                        )
                    )
                }
            }
        }

        // Carousel Horizontal Pager with Parallax Transitions
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("onboarding_pager")
        ) { pageIndex ->
            val page = pages[pageIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        val scale = (1f - (pageOffset.absoluteValue * 0.10f)).coerceIn(0.90f, 1f)
                        val alpha = (1f - (pageOffset.absoluteValue * 0.35f)).coerceIn(0.50f, 1f)
                        val translation = pageOffset * 40f

                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.translationX = translation
                    }
            ) {
                OnboardingPageContent(
                    page = page,
                    currentPage = pagerState.currentPage,
                    pageIndex = pageIndex
                )
            }
        }

        // Bottom Controls: Page Indicator & Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive Animated Pill Step Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "dot_width"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) pages[pagerState.currentPage].iconColor else PaletteLightBlue,
                        animationSpec = tween(durationMillis = 300),
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }

            // Next / Previous / Get Started Action Buttons
            if (isLastPage) {
                val buttonInteraction = remember { MutableInteractionSource() }
                val isButtonPressed by buttonInteraction.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonPressed) 0.97f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "btn_get_started_scale"
                )

                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .testTag("onboarding_get_started_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimaryBlue,
                        contentColor = Color.White
                    ),
                    interactionSource = buttonInteraction
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Get Started",
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .weight(0.38f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_next_onboarding"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Next Step (${pagerState.currentPage + 1}/${pages.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPageData,
    currentPage: Int,
    pageIndex: Int
) {
    val isCurrent = currentPage == pageIndex

    val infiniteTransition = rememberInfiniteTransition(label = "pulseBadge")
    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgePulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step Badge & Headings
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(page.iconColor.copy(alpha = 0.12f))
                        .border(1.dp, page.iconColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = page.stepLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = page.iconColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            if (isCurrent) {
                                scaleX = badgePulse
                                scaleY = badgePulse
                            }
                        }
                        .clip(CircleShape)
                        .background(page.iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = page.iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    lineHeight = 31.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Central Feature Hero Illustration with Soft Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PaletteLightBlue.copy(alpha = 0.45f),
                            PaletteLightBlue.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(1.dp, SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = page.imageResId),
                contentDescription = page.title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Highlights Card with Bullet Points
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            page.highlights.forEach { highlight ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(StatusPresentGreenBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusPresentGreenText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            fontSize = 12.8.sp,
                            lineHeight = 17.sp
                        )
                    )
                }
            }
        }
    }
}
