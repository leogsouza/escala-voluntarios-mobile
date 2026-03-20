package br.com.leogsouza.escalav.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val forcedLogoutMessage by viewModel.forcedLogoutMessage.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var showEnableBiometricDialog by remember { mutableStateOf(false) }
    // Track whether the last action was a manual password login (to offer biometric enrollment)
    var wasPasswordLogin by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val canUseBiometric = remember(context) {
        BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Helper: show the system biometric prompt
    fun launchBiometricPrompt() {
        activity ?: return
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.loginWithBiometric()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Ignore user-initiated cancels; show all other errors
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        biometricError = errString.toString()
                    }
                }
                override fun onAuthenticationFailed() {
                    biometricError = "Biometria não reconhecida. Tente novamente."
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login com biometria")
            .setSubtitle("Use sua impressão digital para entrar")
            .setNegativeButtonText("Usar senha")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(promptInfo)
    }

    // Auto-trigger biometric on first composition if opt-in is active
    LaunchedEffect(Unit) {
        if (canUseBiometric && viewModel.biometricEnabled && viewModel.hasStoredSession) {
            launchBiometricPrompt()
        }
    }

    // React to auth state changes
    LaunchedEffect(state) {
        when (state) {
            is AuthState.Success -> {
                if (wasPasswordLogin && canUseBiometric && !viewModel.biometricEnabled) {
                    // Offer biometric enrollment after first successful password login
                    showEnableBiometricDialog = true
                } else {
                    onLoginSuccess()
                }
                wasPasswordLogin = false
            }
            else -> { /* nothing */ }
        }
    }

    // ── "Enable biometric?" dialog shown after successful password login ──────
    if (showEnableBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showEnableBiometricDialog = false; onLoginSuccess() },
            title = { Text("Ativar biometria?") },
            text  = { Text("Deseja usar impressão digital para entrar nas próximas vezes?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.enableBiometric()
                    showEnableBiometricDialog = false
                    onLoginSuccess()
                }) { Text("Ativar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEnableBiometricDialog = false
                    onLoginSuccess()
                }) { Text("Não") }
            }
        )
    }

    // ── Login form ────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Escala de Voluntários",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Entrar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Use suas credenciais para acessar sua escala.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário") },
                    placeholder = { Text("Digite seu usuário") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    placeholder = { Text("Digite sua senha") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Error messages
                val errorMsg = biometricError
                    ?: forcedLogoutMessage
                    ?: (state as? AuthState.Error)?.message
                if (errorMsg != null) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Primary login button
                Button(
                    onClick = {
                        wasPasswordLogin = true
                        biometricError = null
                        viewModel.login(username.trim(), password)
                    },
                    enabled = username.isNotBlank() && password.isNotBlank() && state !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Entrar")
                    }
                }

                // Fingerprint button — only when biometric is enrolled and opted-in
                if (canUseBiometric && viewModel.biometricEnabled && viewModel.hasStoredSession) {
                    OutlinedButton(
                        onClick = { biometricError = null; launchBiometricPrompt() },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Entrar com impressão digital")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Em caso de problemas de acesso, procure o administrador da escala.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
