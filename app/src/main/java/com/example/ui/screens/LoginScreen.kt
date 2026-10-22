package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BleCircleIcon
import com.example.ui.theme.BleBlue
import com.example.ui.theme.BrandPrimaryBlue
import com.example.ui.theme.PaletteGray
import com.example.ui.theme.PaletteLightBlue
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (faceRegistered: Boolean) -> Unit,
    onSignUpSuccess: () -> Unit = { onLoginSuccess(false) },
    onBackToRoleSelection: (() -> Unit)? = null,
    onPerformLogin: (email: String, pass: String, (Boolean, Boolean, String?) -> Unit) -> Unit,
    onPerformSignUp: (name: String, email: String, enrollment: String, dept: String, pass: String, (Boolean, String?) -> Unit) -> Unit
) {
    var emailOrEnrollment by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showSignUpDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional back button
            if (onBackToRoleSelection != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBackToRoleSelection,
                        modifier = Modifier.testTag("student_login_back_btn")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Blue Bluetooth Icon Badge
            BleCircleIcon(
                size = 64.dp,
                iconSize = 36.dp,
                backgroundColor = BleBlue,
                iconColor = Color.White,
                modifier = Modifier.testTag("login_ble_icon")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // App Title
            Text(
                text = "Smart Attendance",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Student Login",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(42.dp))

            // Email / Enrollment No. input
            OutlinedTextField(
                value = emailOrEnrollment,
                onValueChange = { emailOrEnrollment = it },
                label = { Text("Email / Enrollment No.") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "User Icon",
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_email_or_enrollment"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = PaletteGray,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Lock Icon",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = PaletteGray,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Forgot Password link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = BrandPrimaryBlue
                    ),
                    modifier = Modifier
                        .clickable { showForgotPasswordDialog = true }
                        .padding(4.dp)
                        .testTag("btn_forgot_password")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login button
            Button(
                onClick = {
                    if (emailOrEnrollment.isBlank() || password.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please fill in email/enrollment and password") }
                        return@Button
                    }
                    isLoading = true
                    onPerformLogin(emailOrEnrollment, password) { success, faceRegistered, errorMsg ->
                        isLoading = false
                        if (success) {
                            onLoginSuccess(faceRegistered)
                        } else {
                            val friendlyMsg = errorMsg ?: "Login failed. Please check credentials or sign up."
                            scope.launch { snackbarHostState.showSnackbar(friendlyMsg) }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_login"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Don't have an account? Sign Up
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandPrimaryBlue
                    ),
                    modifier = Modifier
                        .clickable { showSignUpDialog = true }
                        .padding(4.dp)
                        .testTag("btn_signup_navigate")
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Sign Up Dialog
    if (showSignUpDialog) {
        var suName by remember { mutableStateOf("") }
        var suEmail by remember { mutableStateOf("") }
        var suEnrollment by remember { mutableStateOf("") }
        var suDept by remember { mutableStateOf("Computer Science") }
        var suPassword by remember { mutableStateOf("") }
        var suConfirmPassword by remember { mutableStateOf("") }
        var isSuPasswordVisible by remember { mutableStateOf(false) }
        var isSuConfirmPasswordVisible by remember { mutableStateOf(false) }
        var regError by remember { mutableStateOf<String?>(null) }
        var isSubmittingReg by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingReg) showSignUpDialog = false },
            title = {
                Text(
                    text = "Student Registration",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (regError != null) {
                        Text(
                            text = regError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = suName,
                        onValueChange = {
                            suName = it
                            regError = null
                        },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. John Doe") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suEmail,
                        onValueChange = {
                            suEmail = it
                            regError = null
                        },
                        label = { Text("College Email") },
                        placeholder = { Text("e.g. john@college.edu") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suEnrollment,
                        onValueChange = {
                            suEnrollment = it
                            regError = null
                        },
                        label = { Text("Enrollment Number") },
                        placeholder = { Text("e.g. 22BCS001") },
                        leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suDept,
                        onValueChange = {
                            suDept = it
                            regError = null
                        },
                        label = { Text("Course / Department") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suPassword,
                        onValueChange = {
                            suPassword = it
                            regError = null
                        },
                        label = { Text("Create Password") },
                        placeholder = { Text("At least 6 characters") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isSuPasswordVisible = !isSuPasswordVisible }) {
                                Icon(
                                    imageVector = if (isSuPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isSuPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suConfirmPassword,
                        onValueChange = {
                            suConfirmPassword = it
                            regError = null
                        },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Re-enter password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isSuConfirmPasswordVisible = !isSuConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isSuConfirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isSuConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = suName.trim()
                        val cleanEmail = suEmail.trim()
                        val cleanEnroll = suEnrollment.trim()
                        val cleanDept = suDept.trim()
                        val cleanPass = suPassword.trim()
                        val cleanConfirm = suConfirmPassword.trim()

                        if (cleanName.isBlank()) {
                            regError = "Please enter your full name."
                            return@Button
                        }
                        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                            regError = "Please enter a valid college email address."
                            return@Button
                        }
                        if (cleanEnroll.isBlank()) {
                            regError = "Please enter your enrollment number."
                            return@Button
                        }
                        if (cleanPass.length < 6) {
                            regError = "Password must be at least 6 characters."
                            return@Button
                        }
                        if (cleanPass != cleanConfirm) {
                            regError = "Passwords do not match."
                            return@Button
                        }

                        isSubmittingReg = true
                        onPerformSignUp(cleanName, cleanEmail, cleanEnroll, cleanDept, cleanPass) { success, msg ->
                            isSubmittingReg = false
                            if (success) {
                                showSignUpDialog = false
                                onSignUpSuccess()
                            } else {
                                regError = msg ?: "Error registering student"
                            }
                        }
                    },
                    enabled = !isSubmittingReg,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                ) {
                    if (isSubmittingReg) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Register & Enter")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignUpDialog = false },
                    enabled = !isSubmittingReg
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(emailOrEnrollment) }
        var resetSent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    if (resetSent) {
                        Text("Password reset instructions have been sent to $resetEmail.")
                    } else {
                        Text("Enter your college email address to receive password reset link:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!resetSent) {
                            resetSent = true
                        } else {
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                ) {
                    Text(if (resetSent) "Done" else "Send Reset Link")
                }
            },
            dismissButton = {
                if (!resetSent) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
