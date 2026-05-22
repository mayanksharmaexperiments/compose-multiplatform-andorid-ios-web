package org.example.project

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.example.project.di.diModule
import org.example.project.di.presentationModule
import org.example.project.domain.model.AuthStatus
import org.example.project.domain.model.Client
import org.example.project.domain.repository.AuthRepository
import org.example.project.presentation.auth.forgotpassword.ForgotPasswordScreen
import org.example.project.presentation.auth.login.LoginScreen
import org.example.project.presentation.auth.signup.SignUpScreen
import org.example.project.presentation.client.ClientDetailScreen
import org.example.project.presentation.client.ClientListScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

sealed interface Screen {
    data object List : Screen
    data class Detail(val client: Client) : Screen
}

sealed interface AuthScreen {
    data object Login : AuthScreen
    data object SignUp : AuthScreen
    data object ForgotPassword : AuthScreen
}

@Composable
fun <T : Any> NavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val activeKey = backStack.lastOrNull()
    if (activeKey != null) {
        Crossfade(targetState = activeKey, modifier = modifier) { key ->
            content(key)
        }
    }
}

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(diModule, presentationModule)
    }) {
        MaterialTheme {
            val authRepository = koinInject<AuthRepository>()
            val authStatus by authRepository.sessionState.collectAsStateWithLifecycle()
            val coroutineScope = rememberCoroutineScope()

            when (val currentStatus = authStatus) {
                is AuthStatus.Checking -> {
                    // Full-screen Loading State with Premium Visual Design
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF3F51B5), Color(0xFF673AB7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(50.dp))
                            Text(
                                text = "Securing connection...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                is AuthStatus.Unauthenticated -> {
                    val authScreen = remember { mutableStateOf<AuthScreen>(AuthScreen.Login) }
                    Crossfade(targetState = authScreen.value) { screen ->
                        when (screen) {
                            AuthScreen.Login -> LoginScreen(
                                onNavigateToSignUp = { authScreen.value = AuthScreen.SignUp },
                                onNavigateToForgotPassword = { authScreen.value = AuthScreen.ForgotPassword }
                            )
                            AuthScreen.SignUp -> SignUpScreen(
                                onNavigateToLogin = { authScreen.value = AuthScreen.Login }
                            )
                            AuthScreen.ForgotPassword -> ForgotPasswordScreen(
                                onNavigateToLogin = { authScreen.value = AuthScreen.Login }
                            )
                        }
                    }
                }
                is AuthStatus.Authenticated -> {
                    // Main Authenticated Application Dashboard
                    val backStack = remember {
                        mutableStateListOf<Screen>(Screen.List)
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isLargeScreen = maxWidth > 720.dp

                        if (isLargeScreen) {
                            // Wide Screen (Tablet / Web Landscape): Split Layout
                            Row(modifier = Modifier.fillMaxSize()) {
                                val activeDetail = backStack.filterIsInstance<Screen.Detail>().lastOrNull()
                                val selectedClientId = activeDetail?.client?.id

                                // Left Pane: Client List
                                Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                                    ClientListScreen(
                                        onClientClick = { client ->
                                            if (backStack.size > 1 && backStack[1] is Screen.Detail) {
                                                backStack[1] = Screen.Detail(client)
                                            } else {
                                                backStack.add(Screen.Detail(client))
                                            }
                                        },
                                        onLogoutClick = {
                                            coroutineScope.launch {
                                                authRepository.logout()
                                            }
                                        },
                                        selectedClientId = selectedClientId
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(Color(0xFFE0E0E0))
                                )

                                // Right Pane: Detail View or Welcoming Empty State
                                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                                    if (activeDetail != null) {
                                        ClientDetailScreen(
                                            client = activeDetail.client,
                                            showBackButton = false,
                                            onBack = {
                                                backStack.remove(activeDetail)
                                            }
                                        )
                                    } else {
                                        // Empty State Layout
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF5F5F7))
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(Color(0xFFE3F2FD), Color(0xFFEDE7F6))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "CRM Profile Placeholder",
                                                    tint = Color(0xFF673AB7),
                                                    modifier = Modifier.size(56.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text(
                                                text = "Select a Client",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF212121)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Choose a client from the list to view their detailed information, contact phone, location address, and active status history.",
                                                fontSize = 14.sp,
                                                color = Color.Gray,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.width(320.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Small Screen (Mobile): Navigation Stack
                            NavDisplay(backStack = backStack, modifier = Modifier.fillMaxSize()) { screen ->
                                when (screen) {
                                    is Screen.List -> {
                                        ClientListScreen(
                                            onClientClick = { client ->
                                                backStack.add(Screen.Detail(client))
                                            },
                                            onLogoutClick = {
                                                coroutineScope.launch {
                                                    authRepository.logout()
                                                }
                                            }
                                        )
                                    }
                                    is Screen.Detail -> {
                                        ClientDetailScreen(
                                            client = screen.client,
                                            showBackButton = true,
                                            onBack = {
                                                backStack.remove(screen)
                                            }
                                        )
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