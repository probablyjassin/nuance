package com.jassin.customdrome

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jassin.customdrome.ui.theme.CustomDromeTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onBack: () -> Unit,
    userPrefs: UserPreferences
) {
    CustomDromeTheme {
        val context = LocalContext.current

        val savedName by userPrefs.userName.collectAsState(initial = null)
        val savedServerURL by userPrefs.serverURL.collectAsState(initial = null)

        var tempName by remember { mutableStateOf("") }
        var tempServerURL by remember { mutableStateOf("") }

        LaunchedEffect(savedName, savedServerURL) {
            if (savedName != null) {
                tempName = savedName!!
            }
            if (savedServerURL != null) {
                tempServerURL = savedServerURL!!
            }
        }
        val scope = rememberCoroutineScope()

        val keyboardController = LocalSoftwareKeyboardController.current
        val onFinishAction = {
            keyboardController?.hide()
            scope.launch {
                userPrefs.saveUsername(tempName)
                userPrefs.saveServerURL((tempServerURL))
                android.widget.Toast.makeText(
                    context,
                    "Login details saved",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            onLogin()
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
                    // var instanceUrl by remember { mutableStateOf("") }
                    // var username by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }

                    Text(
                        text = "Login to CustomDrome",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    OutlinedTextField(
                        value = tempServerURL,
                        onValueChange = { tempServerURL = it },
                        // This is the text that floats to the top corner
                        label = { Text("Server URL") },
                        // This only appears once you click and the label moves up
                        placeholder = { Text("http://navidrome.int") },
                        modifier = Modifier.fillMaxWidth(),
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
                        value = password,
                        onValueChange = { password = it },
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

                        Button(onClick = { onBack() }) {
                            // Trigger the back action
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}
