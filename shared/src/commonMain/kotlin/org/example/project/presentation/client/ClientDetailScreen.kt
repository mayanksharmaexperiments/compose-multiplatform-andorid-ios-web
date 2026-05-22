package org.example.project.presentation.client

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.model.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    client: Client,
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            if (showBackButton) {
                TopAppBar(
                    title = { Text("Client Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF8F9FA)
                    )
                )
            }
        },
        containerColor = Color(0xFFF8F9FA),
        modifier = modifier
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card with Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3F51B5), Color(0xFF673AB7)) // Royal Purple to Deep Blue
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Styled Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (client.name.isNotEmpty()) client.name.take(1).uppercase() else "?"
                            Text(
                                text = initials,
                                color = Color(0xFF673AB7),
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = client.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Active Status Chip
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9).copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Client",
                                color = Color(0xFF2E7D32),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Client Profile Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // Phone Details Card
            DetailCard(
                icon = Icons.Default.Phone,
                label = "Phone Number",
                value = client.phoneNo ?: "No phone number available",
                tintColor = Color(0xFF1976D2)
            )

            // Address Details Card
            DetailCard(
                icon = Icons.Default.LocationOn,
                label = "Address Location",
                value = client.address ?: "No address available",
                tintColor = Color(0xFFD32F2F)
            )

            // Date Joined Card
            DetailCard(
                icon = Icons.Default.DateRange,
                label = "Joined Date",
                value = formatJoinedDate(client.createdAt),
                tintColor = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* Action Callback */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF673AB7)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Client", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { /* Action Callback */ },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF673AB7)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Message")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tintColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tintColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = Color(0xFF212121),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatJoinedDate(isoString: String?): String {
    if (isoString == null) return "Unknown"
    return try {
        val datePart = isoString.substringBefore('T') // "2023-10-27"
        val parts = datePart.split('-')
        if (parts.size == 3) {
            val year = parts[0]
            val month = when (parts[1]) {
                "01" -> "January"
                "02" -> "February"
                "03" -> "March"
                "04" -> "April"
                "05" -> "May"
                "06" -> "June"
                "07" -> "July"
                "08" -> "August"
                "09" -> "September"
                "10" -> "October"
                "11" -> "November"
                "12" -> "December"
                else -> parts[1]
            }
            val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
            "$month $day, $year"
        } else {
            isoString.take(10)
        }
    } catch (e: Exception) {
        isoString.take(10)
    }
}
