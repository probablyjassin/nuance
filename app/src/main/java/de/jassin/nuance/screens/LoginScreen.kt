package de.jassin.nuance.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import de.jassin.nuance.UserPreferences
import de.jassin.nuance.data.models.AuthViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

fun Modifier.onTripleTap(onTripleTap: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        var tapCount = 0
        var lastTapTime = 0L
        val tripleTapTimeout = 300L // Milliseconds window to complete the next tap

        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val downChanged = event.changes.firstOrNull()?.changedToDown() ?: false

                if (downChanged) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < tripleTapTimeout) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = currentTime

                    if (tapCount == 3) {
                        onTripleTap()
                        tapCount = 0 // Reset
                    }
                }
            }
        }
    }

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onBack: () -> Unit,
    userPrefs: UserPreferences,
    httpClient: HttpClient,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val savedName by userPrefs.server.userName.collectAsState(initial = null)
    val savedServerURL by userPrefs.server.serverURL.collectAsState(initial = null)

    val savedSecureHostnames by userPrefs.server.secureHostnames.collectAsState(initial = true)

    val savedToken by userPrefs.auth.token.collectAsState(initial = null)
    val savedSubSonicToken by userPrefs.auth.subsonicToken.collectAsState(initial = null)

    var tempName by remember { mutableStateOf("") }
    var tempServerURL by remember { mutableStateOf("") }
    var tempPassword by remember { mutableStateOf("") }

    LaunchedEffect(savedName, savedServerURL) {
        if (savedName != null) {
            tempName = savedName!!
        }
        if (savedServerURL != null) {
            tempServerURL = savedServerURL!!
        }
    }
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel =
        viewModel(
            key = "auth_$savedSecureHostnames",
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                            return AuthViewModel(httpClient) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                },
        )
    val authResult by authViewModel.result.collectAsState()
    val requestText by authViewModel.request.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val onFinishAction = {
        keyboardController?.hide()
        scope.launch {
            val loginData = authViewModel.login(tempServerURL, tempName, tempPassword)
            if (loginData != null) {
                userPrefs.server.saveUsername(tempName)
                userPrefs.server.saveServerURL(tempServerURL)
                userPrefs.server.savePassword(tempPassword)
                userPrefs.auth.saveToken(loginData.token)
                userPrefs.auth.saveSubsonicToken(loginData.subsonicToken)
                loginData.subsonicSalt?.let { userPrefs.auth.saveSubsonicSalt(it) }
                Toast
                    .makeText(
                        context,
                        "Login details saved",
                        Toast.LENGTH_LONG,
                    ).show()
                onLogin()
            }
        }
    }

    // responsible for the themed bg color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        // prevents overlap with the status bar
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Sign into your Navidrome server",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                )

                OutlinedTextField(
                    value = tempServerURL,
                    onValueChange = { tempServerURL = it },
                    // This is the text that floats to the top corner
                    label = { Text("Server URL") },
                    // This only appears once you click and the label moves up
                    placeholder = { Text("https://navidrome.int") },
                    modifier =
                        Modifier.fillMaxWidth().onTripleTap {
                            tempServerURL = "https://navidrome.int"
                        },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Dns, contentDescription = null)
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Next, // move cursor to next text field
                        ),
                )

                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Username") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentType = ContentType.Username
                            },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email, // for password managers / autofill
                            imeAction = ImeAction.Next, // move cursor to next text field
                        ),
                )
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = { tempPassword = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(), // for password security
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentType = ContentType.Password
                            },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password, // for password managers / autofill
                            imeAction = ImeAction.Done, // close keyboard when finished
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                onFinishAction()
                            },
                        ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { onFinishAction() }) {
                        Text("Login")
                    }

                    Button(onClick = { activity?.finish() ?: onBack() }) {
                        // Trigger the back action. If we're on the login screen, finishing
                        // the activity closes the app instead of navigating back to a
                        // previous screen and potentially being routed back to login.
                        Text("Go Back")
                    }
                }
                // Intercept system back presses on the login screen and close the app.
                BackHandler(enabled = true) {
                    activity?.finish()
                }
                // Collapsible "Advanced" section for debug / request information
                var advancedExpanded by remember { mutableStateOf(false) }
                val chevronRotation by animateFloatAsState(targetValue = if (advancedExpanded) 180f else 0f)

                TextButton(onClick = { advancedExpanded = !advancedExpanded }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Advanced", color = MaterialTheme.colorScheme.onBackground)
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(chevronRotation),
                        )
                    }
                }

                AnimatedVisibility(visible = advancedExpanded) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val secureHostnamesChecked by userPrefs.server.secureHostnames.collectAsState(initial = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Use secure hostname checking")
                            Switch(
                                checked = secureHostnamesChecked == true,
                                onCheckedChange = { newValue: Boolean ->
                                    scope.launch {
                                        userPrefs.server.saveSecureHostnames(newValue)
                                    }
                                }
                            )
                        }
                        Text(
                            text = "Saved subsonicToken: ${savedSubSonicToken ?: "—"}",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Saved token: ${savedToken ?: "—"}",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (authResult != null) {
                            Text(
                                text = authResult ?: "",
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (requestText != null) {
                            Text(
                                text = "Request:\n$requestText",
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
