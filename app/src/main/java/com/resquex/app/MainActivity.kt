package com.resquex.app

import com.resquex.app.ui.ChatScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resquex.app.viewmodel.EmergencyViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resquex.app.ui.theme.RESQUEXTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RESQUEXTheme {
                ResqueXHomeScreen()
            }
        }
    }
}

private val DarkBlue = Color(0xFF102A43)
private val EmergencyRed = Color(0xFFD62828)
private val BackgroundColor = Color(0xFFF3F6FA)
private val CardBlue = Color(0xFFE4EEFA)

@Composable
fun ResqueXHomeScreen() {
    var showEmergencyScreen by remember {
        mutableStateOf(false)
    }
    var showFirstAidScreen by remember {
        mutableStateOf(false)
    }
    var showChatScreen by remember {
        mutableStateOf(false)
    }

    if (showChatScreen) {
        ChatScreen(
            onBack = {
                showChatScreen = false
            },
            onNavigateToEmergencies = {
                showChatScreen = false
                showEmergencyScreen = true
            }
        )
    } else if (showEmergencyScreen) {
        EmergencyScreen(
            onBack = {
                showEmergencyScreen = false
            },
            onOpenChat = {
                showEmergencyScreen = false
                showChatScreen = true
            }
        )
    } else if (showFirstAidScreen) {
        FirstAidScreen(
            onBack = {
                showFirstAidScreen = false
            },
            onOpenChat = {
                showFirstAidScreen = false
                showChatScreen = true
            }
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundColor
        ) {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "RESQUE-X",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Text(
                text = "Emergency Response Assistant",
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EmergencyRed
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "EMERGENCY ASSISTANT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Get first-aid guidance when you need help.",
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showEmergencyScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = EmergencyRed
                            )
                        ) {
                            Text(
                                text = "Get Emergency Help",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                showChatScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "AI Chatbot",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "QUICK ACCESS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        showFirstAidScreen = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardBlue,
                        contentColor = DarkBlue
                    )
                ) {
                    Text(
                        text = "First Aid — Open Guide",
                        fontWeight = FontWeight.Bold
                    )
                }

            FeatureCard(
                title = "Emergency AI Chatbot",
                description = "Online & Offline Qwen SLM First-Aid Guidance",
                backgroundColor = Color(0xFFE3F2FD),
                onClick = {
                    showChatScreen = true
                }
            )

            FeatureCard(
                title = "CPR Assistant",
                description = "CPR guidance and practice support",
                backgroundColor = Color(0xFFE4F4E8)
            )

            FeatureCard(
                title = "Pulse Estimation",
                description = "Experimental camera-based pulse feature",
                backgroundColor = Color(0xFFFFF0D9)
            )

            FeatureCard(
                title = "Emergency Navigation",
                description = "Help locate nearby emergency facilities",
                backgroundColor = Color(0xFFF0E6FA)
            )

            Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Designed for online and offline emergency support",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkBlue
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "OPEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
            }
        }
    }
}


@Composable
fun EmergencyScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit = {}
) {

    val emergencyViewModel: EmergencyViewModel = viewModel()
    val emergencies = emergencyViewModel.emergencies.collectAsState()
    val isLoading = emergencyViewModel.isLoading.collectAsState()
    val errorMessage = emergencyViewModel.errorMessage.collectAsState()

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        emergencyViewModel.loadEmergencies()
    }

    if (selectedCategory != null) {

        val emergency = emergencies.value.find {
            it.category == selectedCategory
        }

        if (isLoading.value) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

        } else if (emergency != null && errorMessage.value == null) {

            EmergencyDetailScreen(
                category = selectedCategory!!,
                instructions = emergency.instructions,
                onBack = {
                    selectedCategory = null
                }
            )

        } else {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage.value
                            ?: "Emergency guidance is unavailable.",
                        color = Color.DarkGray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            emergencyViewModel.loadEmergencies()
                        }
                    ) {
                        Text("Try Again")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            selectedCategory = null
                        }
                    ) {
                        Text("Back to Categories")
                    }
                }
            }
        }

    } else {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Emergency Assistant",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select an emergency category.",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onBack) {
                        Text("Back to Home")
                    }
                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text("💬 Ask Emergency AI")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Available emergency categories:")

                Spacer(modifier = Modifier.height(12.dp))

                EmergencyCategoryButton("Severe Bleeding") {
                    selectedCategory = "Severe Bleeding"
                }

                EmergencyCategoryButton("Burns") {
                    selectedCategory = "Burns"
                }

                EmergencyCategoryButton("Choking") {
                    selectedCategory = "Choking"
                }

                EmergencyCategoryButton("Cardiac Arrest") {
                    selectedCategory = "Cardiac Arrest"
                }

                EmergencyCategoryButton("Electric Shock") {
                    selectedCategory = "Electric Shock"
                }

                EmergencyCategoryButton("Unconscious Person") {
                    selectedCategory = "Unconscious Person"
                }
            }
        }
    }
}




@Composable
fun EmergencyCategoryButton(
    title: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(text = title)
    }
}

@Composable
fun EmergencyDetailScreen(
    category: String,
    instructions: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = category,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Emergency guidance",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = instructions,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onBack) {
                Text("Back to Categories")
            }
        }
    }
}


@Composable
fun FirstAidScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit = {}
) {

    val emergencyViewModel: EmergencyViewModel = viewModel()

    val emergencies = emergencyViewModel.emergencies.collectAsState()
    val isLoading = emergencyViewModel.isLoading.collectAsState()
    val errorMessage = emergencyViewModel.errorMessage.collectAsState()

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        emergencyViewModel.loadEmergencies()
    }

    if (selectedCategory != null) {

        val emergency = emergencies.value.find {
            it.category == selectedCategory
        }

        if (isLoading.value) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

        } else if (errorMessage.value != null) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage.value
                            ?: "Unable to load first-aid guidance.",
                        color = Color.DarkGray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            emergencyViewModel.loadEmergencies()
                        }
                    ) {
                        Text("Try Again")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            selectedCategory = null
                        }
                    ) {
                        Text("Back to Categories")
                    }
                }
            }

        } else if (emergency != null) {

            EmergencyDetailScreen(
                category = selectedCategory!!,
                instructions = emergency.instructions,
                onBack = {
                    selectedCategory = null
                }
            )

        } else {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "First-aid guidance is unavailable.",
                        color = Color.DarkGray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            emergencyViewModel.loadEmergencies()
                        }
                    ) {
                        Text("Try Again")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            selectedCategory = null
                        }
                    ) {
                        Text("Back to Categories")
                    }
                }
            }
        }

    } else {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onBack) {
                        Text("Back to Home")
                    }
                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text("💬 Ask Emergency AI")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "FIRST AID GUIDE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select an emergency topic",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                EmergencyCategoryButton("Severe Bleeding") {
                    selectedCategory = "Severe Bleeding"
                }

                EmergencyCategoryButton("Burns") {
                    selectedCategory = "Burns"
                }

                EmergencyCategoryButton("Choking") {
                    selectedCategory = "Choking"
                }

                EmergencyCategoryButton("Cardiac Arrest") {
                    selectedCategory = "Cardiac Arrest"
                }

                EmergencyCategoryButton("Electric Shock") {
                    selectedCategory = "Electric Shock"
                }

                EmergencyCategoryButton("Unconscious Person") {
                    selectedCategory = "Unconscious Person"
                }
            }
        }
    }
}

