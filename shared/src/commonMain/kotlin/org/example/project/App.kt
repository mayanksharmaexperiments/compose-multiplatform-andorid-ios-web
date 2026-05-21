package org.example.project

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.serialization.Serializable
import org.example.project.model.Client
import org.example.project.ui.ClientDetailScreen
import org.example.project.ui.ClientListScreen

@Serializable
sealed interface Screen {
    @Serializable
    data object List : Screen

    @Serializable
    data class Detail(val client: Client) : Screen
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
    MaterialTheme {
        // Keep the backstack as a mutableStateListOf to make it reactive and observable.
        val backStack = remember {
            mutableStateListOf<Screen>(
                Screen.List
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLargeScreen = maxWidth > 720.dp

            if (isLargeScreen) {
                // Wide Screen (Tablet / Web Landscape): Split Layout (40% List, 60% Detail/Empty State)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Extract active detail from backstack if it exists
                    val activeDetail = backStack.filterIsInstance<Screen.Detail>().lastOrNull()
                    val selectedClientId = activeDetail?.client?.id

                    // Left Pane: Client List (width weight = 0.4)
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                        ClientListScreen(
                            onClientClick = { client ->
                                // On wide screens, we don't want to pile up multiple details in the stack.
                                // Instead, we replace/update the current detail if there is one.
                                if (backStack.size > 1 && backStack[1] is Screen.Detail) {
                                    backStack[1] = Screen.Detail(client)
                                } else {
                                    backStack.add(Screen.Detail(client))
                                }
                            },
                            selectedClientId = selectedClientId
                        )
                    }

                    // Divider line with subtle modern shading
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    // Right Pane: Detail view or premium welcoming empty state (width weight = 0.6)
                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                        if (activeDetail != null) {
                            ClientDetailScreen(
                                client = activeDetail.client,
                                showBackButton = false, // Hide back button in split screen
                                onBack = {
                                    backStack.remove(activeDetail)
                                }
                            )
                        } else {
                            // High-fidelity premium Empty State with smooth gradient background & welcoming messaging
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
                // Small Screen (Mobile): Single-pane Navigation 3 style stack
                NavDisplay(backStack = backStack, modifier = Modifier.fillMaxSize()) { screen ->
                    when (screen) {
                        is Screen.List -> {
                            ClientListScreen(
                                onClientClick = { client ->
                                    backStack.add(Screen.Detail(client))
                                }
                            )
                        }
                        is Screen.Detail -> {
                            ClientDetailScreen(
                                client = screen.client,
                                showBackButton = true, // Show back button on mobile details
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