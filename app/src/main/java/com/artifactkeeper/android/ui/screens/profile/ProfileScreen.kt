package com.artifactkeeper.android.ui.screens.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.ChangePasswordRequest
import com.artifactkeeper.android.data.models.TotpCodeRequest
import com.artifactkeeper.android.data.models.TotpDisableRequest
import com.artifactkeeper.android.data.models.TotpSetupResponse
import com.artifactkeeper.android.data.models.UserInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.launch

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val writer = MultiFormatWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserInfo,
    onDismiss: () -> Unit,
    onUserUpdated: (UserInfo) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Change password state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordLoading by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf(false) }

    // TOTP setup state
    var totpEnabled by remember { mutableStateOf(user.totpEnabled) }
    var showSetupFlow by remember { mutableStateOf(false) }
    var setupResponse by remember { mutableStateOf<TotpSetupResponse?>(null) }
    var setupLoading by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }
    var verifyCode by remember { mutableStateOf("") }
    var verifyLoading by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }
    var backupCodes by remember { mutableStateOf<List<String>?>(null) }

    // TOTP disable state
    var showDisableFlow by remember { mutableStateOf(false) }
    var disablePassword by remember { mutableStateOf("") }
    var disableCode by remember { mutableStateOf("") }
    var disablePasswordVisible by remember { mutableStateOf(false) }
    var disableLoading by remember { mutableStateOf(false) }
    var disableError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Account Card ---
            item {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (user.isAdmin) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Admin", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                        user.email?.let { email ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        user.displayName?.let { name ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // --- Change Password Card ---
            item {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (passwordSuccess) {
                            Text(
                                text = "Password changed successfully.",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                passwordError = null
                                passwordSuccess = false
                            },
                            label = { Text("Current Password") },
                            singleLine = true,
                            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (currentPasswordVisible) "Hide password" else "Show password",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                passwordError = null
                                passwordSuccess = false
                            },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                    )
                                }
                            },
                            isError = newPassword.isNotEmpty() && newPassword.length < 8,
                            supportingText = {
                                if (newPassword.isNotEmpty() && newPassword.length < 8) {
                                    Text("Must be at least 8 characters")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                passwordError = null
                                passwordSuccess = false
                            },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    )
                                }
                            },
                            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                            supportingText = {
                                if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                                    Text("Passwords do not match")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        passwordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Button(
                            onClick = {
                                passwordError = null
                                passwordSuccess = false
                                if (currentPassword.isBlank()) {
                                    passwordError = "Current password is required"
                                    return@Button
                                }
                                if (newPassword.length < 8) {
                                    passwordError = "New password must be at least 8 characters"
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    passwordError = "Passwords do not match"
                                    return@Button
                                }
                                coroutineScope.launch {
                                    passwordLoading = true
                                    try {
                                        ApiClient.usersApi.changePassword(
                                            user.id,
                                            ChangePasswordRequest(currentPassword, newPassword),
                                        ).unwrap()
                                        passwordSuccess = true
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                    } catch (e: Exception) {
                                        passwordError = e.message ?: "Failed to change password"
                                    } finally {
                                        passwordLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !passwordLoading,
                        ) {
                            if (passwordLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Change Password")
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // --- Two-Factor Authentication Card ---
            item {
                Text(
                    text = "Two-Factor Authentication",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (totpEnabled && backupCodes == null) {
                            // TOTP is enabled
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Enabled") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }

                            if (!showDisableFlow) {
                                OutlinedButton(
                                    onClick = { showDisableFlow = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("Disable 2FA")
                                }
                            } else {
                                // Disable flow: password + code
                                OutlinedTextField(
                                    value = disablePassword,
                                    onValueChange = {
                                        disablePassword = it
                                        disableError = null
                                    },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = if (disablePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { disablePasswordVisible = !disablePasswordVisible }) {
                                            Icon(
                                                imageVector = if (disablePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (disablePasswordVisible) "Hide password" else "Show password",
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                OutlinedTextField(
                                    value = disableCode,
                                    onValueChange = {
                                        disableCode = it.filter { c -> c.isDigit() }.take(6)
                                        disableError = null
                                    },
                                    label = { Text("TOTP Code") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                disableError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            showDisableFlow = false
                                            disablePassword = ""
                                            disableCode = ""
                                            disableError = null
                                        },
                                    ) {
                                        Text("Cancel")
                                    }

                                    Button(
                                        onClick = {
                                            disableError = null
                                            if (disablePassword.isBlank()) {
                                                disableError = "Password is required"
                                                return@Button
                                            }
                                            if (disableCode.length != 6) {
                                                disableError = "Enter a 6-digit code"
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                disableLoading = true
                                                try {
                                                    ApiClient.authApi.disableTotp(
                                                        TotpDisableRequest(disablePassword, disableCode)
                                                    ).unwrap()
                                                    totpEnabled = false
                                                    showDisableFlow = false
                                                    disablePassword = ""
                                                    disableCode = ""
                                                    // Update user info
                                                    try {
                                                        val updatedUser = ApiClient.authApi.getCurrentUser().unwrap()
                                                        onUserUpdated(updatedUser)
                                                    } catch (_: Exception) { }
                                                    snackbarHostState.showSnackbar("Two-factor authentication disabled")
                                                } catch (e: Exception) {
                                                    disableError = e.message ?: "Failed to disable 2FA"
                                                } finally {
                                                    disableLoading = false
                                                }
                                            }
                                        },
                                        enabled = !disableLoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) {
                                        if (disableLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onError,
                                            )
                                        } else {
                                            Text("Disable 2FA")
                                        }
                                    }
                                }
                            }
                        } else if (backupCodes != null) {
                            // Show backup codes after successful enable
                            Text(
                                text = "Two-factor authentication enabled!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Save these backup codes in a secure place. Each code can only be used once.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    backupCodes!!.forEach { code ->
                                        Text(
                                            text = code,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val codesText = backupCodes!!.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(codesText))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Backup codes copied to clipboard")
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Backup Codes")
                            }

                            Button(
                                onClick = {
                                    backupCodes = null
                                },
                            ) {
                                Text("Done")
                            }
                        } else if (!showSetupFlow) {
                            // TOTP not enabled, show enable button
                            Text(
                                text = "Add an extra layer of security to your account using a time-based one-time password (TOTP).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Button(
                                onClick = {
                                    showSetupFlow = true
                                    setupError = null
                                    coroutineScope.launch {
                                        setupLoading = true
                                        try {
                                            setupResponse = ApiClient.authApi.setupTotp().unwrap()
                                        } catch (e: Exception) {
                                            setupError = e.message ?: "Failed to start 2FA setup"
                                        } finally {
                                            setupLoading = false
                                        }
                                    }
                                },
                            ) {
                                Text("Enable 2FA")
                            }
                        } else {
                            // Setup flow
                            if (setupLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text("Setting up...")
                                }
                            } else if (setupError != null) {
                                Text(
                                    text = setupError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                OutlinedButton(
                                    onClick = {
                                        showSetupFlow = false
                                        setupError = null
                                    },
                                ) {
                                    Text("Cancel")
                                }
                            } else if (setupResponse != null) {
                                val setup = setupResponse!!

                                Text(
                                    text = "Scan the QR code with your authenticator app, or enter the secret key manually.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                // QR Code
                                val qrBitmap = remember(setup.qrCodeUrl) {
                                    try {
                                        generateQrBitmap(setup.qrCodeUrl)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                qrBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "QR Code for TOTP setup",
                                        modifier = Modifier.size(200.dp),
                                    )
                                }

                                // Manual secret
                                Text(
                                    text = "Secret Key",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = setup.secret,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(setup.secret))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Secret copied to clipboard")
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy secret",
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }

                                HorizontalDivider()

                                // Verification code input
                                Text(
                                    text = "Enter the 6-digit code from your authenticator app to verify:",
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                OutlinedTextField(
                                    value = verifyCode,
                                    onValueChange = {
                                        verifyCode = it.filter { c -> c.isDigit() }.take(6)
                                        verifyError = null
                                    },
                                    label = { Text("Verification Code") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                verifyError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            showSetupFlow = false
                                            setupResponse = null
                                            verifyCode = ""
                                            verifyError = null
                                        },
                                    ) {
                                        Text("Cancel")
                                    }

                                    Button(
                                        onClick = {
                                            verifyError = null
                                            if (verifyCode.length != 6) {
                                                verifyError = "Enter a 6-digit code"
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                verifyLoading = true
                                                try {
                                                    val response = ApiClient.authApi.enableTotp(
                                                        TotpCodeRequest(verifyCode)
                                                    ).unwrap()
                                                    backupCodes = response.backupCodes
                                                    totpEnabled = true
                                                    showSetupFlow = false
                                                    setupResponse = null
                                                    verifyCode = ""
                                                    // Update user info
                                                    try {
                                                        val updatedUser = ApiClient.authApi.getCurrentUser().unwrap()
                                                        onUserUpdated(updatedUser)
                                                    } catch (_: Exception) { }
                                                } catch (e: Exception) {
                                                    verifyError = e.message ?: "Verification failed"
                                                } finally {
                                                    verifyLoading = false
                                                }
                                            }
                                        },
                                        enabled = !verifyLoading,
                                    ) {
                                        if (verifyLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text("Verify & Enable")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
