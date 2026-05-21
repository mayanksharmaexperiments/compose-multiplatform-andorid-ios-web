package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.model.Client
import org.example.project.repository.ClientRepository

class ClientListViewModel : ViewModel() {
    private val repository = ClientRepository()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        fetchClients()
    }

    fun fetchClients() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val clients = repository.getClients()
                if (clients.isEmpty()) {
                    _uiState.value = UiState.Empty
                } else {
                    _uiState.value = UiState.Success(clients)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class UiState {
    data object Loading : UiState()
    data object Empty : UiState()
    data class Success(val clients: List<Client>) : UiState()
    data class Error(val message: String) : UiState()
}

@Composable
fun ClientListScreen(
    onClientClick: (Client) -> Unit,
    selectedClientId: Long? = null,
    viewModel: ClientListViewModel = viewModel { ClientListViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    ClientListContent(
        uiState = uiState,
        onRetry = { viewModel.fetchClients() },
        onClientClick = onClientClick,
        selectedClientId = selectedClientId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListContent(
    uiState: UiState,
    onRetry: () -> Unit,
    onClientClick: (Client) -> Unit,
    selectedClientId: Long?
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F9FA))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "My Clients",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search for name...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFFBDBDBD)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("All Clients", true)
                    FilterChip("Active", false)
                    FilterChip("Pending", false)
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Empty -> {
                    Text("No clients found.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${uiState.message}", color = Color.Red, modifier = Modifier.padding(16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
                is UiState.Success -> {
                    val filteredClients = uiState.clients.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredClients) { client ->
                            ClientCard(
                                client = client,
                                isSelected = client.id == selectedClientId,
                                onClick = { onClientClick(client) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) Color(0xFFE0E0E0) else Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.Gray,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ClientCard(
    client: Client,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF1976D2) else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val backgroundColor = if (isSelected) Color(0xFFF0F7FF) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (client.name.isNotEmpty()) client.name.take(1).uppercase() else "?"
                    Text(
                        text = initials,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF212121)
                    )
                    if (client.createdAt != null) {
                        Text(
                            text = "Joined: ${client.createdAt.take(10)}", 
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Active", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!client.phoneNo.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = "Phone", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(client.phoneNo, color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
                if (!client.address.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Address", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(client.address, color = Color.DarkGray, fontSize = 14.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ClientListScreenPreview() {
    val sampleClients = listOf(
        Client(id = 1, name = "John Doe", phoneNo = "+1 234 567 890", address = "123 Main St, New York", createdAt = "2023-10-27T10:00:00Z"),
        Client(id = 2, name = "Jane Smith", phoneNo = "+1 987 654 321", address = "456 Oak Ave, Los Angeles", createdAt = "2023-11-15T14:30:00Z"),
        Client(id = 3, name = "Alice Johnson", phoneNo = "+1 555 012 3456", address = "789 Pine Rd, Chicago", createdAt = "2024-01-05T09:15:00Z")
    )
    MaterialTheme {
        ClientListContent(
            uiState = UiState.Success(sampleClients),
            onRetry = {},
            onClientClick = {},
            selectedClientId = null
        )
    }
}

@Preview
@Composable
fun ClientListScreenLoadingPreview() {
    MaterialTheme {
        ClientListContent(
            uiState = UiState.Loading,
            onRetry = {},
            onClientClick = {},
            selectedClientId = null
        )
    }
}

@Preview
@Composable
fun ClientListScreenEmptyPreview() {
    MaterialTheme {
        ClientListContent(
            uiState = UiState.Empty,
            onRetry = {},
            onClientClick = {},
            selectedClientId = null
        )
    }
}
