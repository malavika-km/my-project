package com.resquex.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resquex.app.audio.EmergencySpeechManager
import com.resquex.app.model.*
import com.resquex.app.util.EmergencyMediaUtils
import com.resquex.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

private val DarkBlue = Color(0xFF102A43)
private val EmergencyRed = Color(0xFFD62828)
private val BackgroundColor = Color(0xFFF3F6FA)
private val CardBlue = Color(0xFFE4EEFA)
private val ForestGreen = Color(0xFF2B8A3E)
private val AmberOrange = Color(0xFFD97706)
private val SurfaceWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onNavigateToEmergencies: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val messages by chatViewModel.messages.collectAsState()
    val isGenerating by chatViewModel.isGenerating.collectAsState()
    val chatMode by chatViewModel.chatMode.collectAsState()
    val modelStatus by chatViewModel.modelStatus.collectAsState()
    val isSpeaking by chatViewModel.isSpeaking.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelSettingsDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Multimodal image state
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }

    // Multimodal speech manager
    val speechManager = remember { EmergencySpeechManager(context) }
    val isListening by speechManager.isListening.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            speechManager.shutdown()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedImageBitmap = bitmap
            selectedImageBase64 = EmergencyMediaUtils.bitmapToBase64(bitmap)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = EmergencyMediaUtils.uriToBitmap(context, uri)
            if (bitmap != null) {
                selectedImageBitmap = bitmap
                selectedImageBase64 = EmergencyMediaUtils.bitmapToBase64(bitmap)
            }
        }
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            speechManager.startListening(
                onResult = { text ->
                    inputText = if (inputText.isBlank()) text else "$inputText $text"
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is needed for emergency voice queries", Toast.LENGTH_SHORT).show()
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val handleSendMessage: () -> Unit = {
        if ((inputText.isNotBlank() || selectedImageBase64 != null) && !isGenerating) {
            chatViewModel.sendMessage(
                query = inputText,
                imageBase64 = selectedImageBase64,
                imageUri = null
            )
            inputText = ""
            selectedImageBitmap = null
            selectedImageBase64 = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Emergency Assistant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Mode badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CardBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = chatMode.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBlue
                                )
                            }
                        }
                        // Status indicator
                        val statusText = when (modelStatus) {
                            is ModelStatus.Ready -> "Offline Qwen SLM Ready"
                            is ModelStatus.Downloading -> "Downloading Qwen SLM..."
                            else -> "Offline First-Aid KB Active"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = if (modelStatus is ModelStatus.Ready) ForestGreen else Color.DarkGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    }
                },
                actions = {
                    // Quick Call 112 SOS Button
                    Button(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:112")
                            }
                            context.startActivity(dialIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("SOS 112", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Model Management / Settings
                    IconButton(onClick = { showModelSettingsDialog = true }) {
                        Text("⚙️", fontSize = 18.sp)
                    }

                    // Clear Chat
                    IconButton(onClick = { showClearConfirmationDialog = true }) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        isSpeaking = isSpeaking,
                        onCallEmergency = { number ->
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$number")
                            }
                            context.startActivity(dialIntent)
                        },
                        onRetry = {
                            chatViewModel.retryLastMessage()
                        },
                        onSpeak = { text ->
                            chatViewModel.speak(text)
                        },
                        onStopSpeaking = {
                            chatViewModel.stopSpeaking()
                        }
                    )
                }

                if (isGenerating) {
                    item {
                        GeneratingIndicator(
                            onCancel = { chatViewModel.cancelGeneration() }
                        )
                    }
                }
            }

            // Quick Prompt Suggestions
            QuickPromptChips(
                onSelectPrompt = { prompt ->
                    chatViewModel.sendMessage(prompt)
                }
            )

            // Multimodal Attachment Preview
            if (selectedImageBitmap != null) {
                Surface(
                    color = CardBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, DarkBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = selectedImageBitmap!!.asImageBitmap(),
                                contentDescription = "Injury Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    selectedImageBitmap = null
                                    selectedImageBase64 = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Text("✕", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Injury photo attached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                            Text("Vision AI will assess wound severity", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // Multimodal Input Bar
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera / Photo Attachment Button
                    IconButton(
                        onClick = { showImageSourceDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("📷", fontSize = 20.sp)
                    }

                    // Voice Dictation (Microphone) Button
                    IconButton(
                        onClick = {
                            if (isListening) {
                                speechManager.stopListening()
                            } else {
                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudioPermission) {
                                    speechManager.startListening(
                                        onResult = { text ->
                                            inputText = if (inputText.isBlank()) text else "$inputText $text"
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isListening) EmergencyRed.copy(alpha = 0.2f) else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Text(if (isListening) "🛑" else "🎙️", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isListening) "Listening... speak now" else "Ask first-aid or describe injury...",
                                fontSize = 13.sp,
                                color = if (isListening) EmergencyRed else Color.Gray
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { handleSendMessage() })
                    )

                    // Send Button
                    Button(
                        onClick = handleSendMessage,
                        enabled = (inputText.isNotBlank() || selectedImageBase64 != null) && !isGenerating,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text("➤", fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // Image Source Selection Dialog (Camera vs Gallery)
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Attach Injury Photo", fontWeight = FontWeight.Bold, color = DarkBlue) },
            text = {
                Column {
                    Text("Take a photo of the wound, burn, or bite for multimodal visual analysis:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            cameraLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📷 Take Photo with Camera")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖼️ Select from Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Model Settings & Download Dialog
    if (showModelSettingsDialog) {
        ModelSettingsDialog(
            modelStatus = modelStatus,
            chatMode = chatMode,
            onDismiss = { showModelSettingsDialog = false },
            onSetMode = { chatViewModel.setChatMode(it) },
            onDownload = { chatViewModel.downloadModel() },
            onCancelDownload = { chatViewModel.cancelDownload() },
            onDelete = { chatViewModel.deleteModel() },
            onNavigateToEmergencies = {
                showModelSettingsDialog = false
                onNavigateToEmergencies()
            }
        )
    }

    // Clear Conversation Confirmation Dialog
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("This will remove all messages from the current session.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatViewModel.clearConversation()
                        showClearConfirmationDialog = false
                    }
                ) {
                    Text("Clear", color = EmergencyRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isSpeaking: Boolean,
    onCallEmergency: (String) -> Unit,
    onRetry: () -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Source pill for Assistant messages
        if (!isUser && message.source != MessageSource.SYSTEM_NOTICE) {
            val (badgeText, badgeBg, badgeTextColor) = when (message.source) {
                MessageSource.VERIFIED_EMERGENCY -> Triple("CRITICAL EMERGENCY ACTION", EmergencyRed, Color.White)
                MessageSource.OFFLINE_QWEN -> Triple("OFFLINE QWEN SLM", ForestGreen, Color.White)
                MessageSource.LOCAL_KNOWLEDGE_BASE -> Triple("VERIFIED FIRST-AID KB", Color(0xFF0C8599), Color.White)
                MessageSource.CLOUD_AI -> Triple("CLOUD AI GATEWAY", DarkBlue, Color.White)
                MessageSource.SYSTEM_NOTICE -> Triple("SYSTEM", Color.DarkGray, Color.White)
            }

            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }
        }

        // Message bubble / card
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) DarkBlue else SurfaceWhite,
            shadowElevation = 2.dp,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // If user attached an injury photo, display it inside their bubble
                if (!message.imageBase64.isNullOrBlank()) {
                    val bitmap = remember(message.imageBase64) {
                        EmergencyMediaUtils.base64ToBitmap(message.imageBase64)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Injury Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // If a verified Emergency Action Card is attached, display formatted layout
                if (message.emergencyCard != null) {
                    EmergencyCardView(
                        card = message.emergencyCard,
                        onCallEmergency = onCallEmergency
                    )
                } else {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = if (isUser) Color.White else Color(0xFF1A202C),
                        lineHeight = 22.sp
                    )
                }

                // Citations row
                if (message.citations.isNotEmpty() && message.emergencyCard == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFEDF2F7))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Source: ${message.citations.joinToString("; ")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }

                // Hands-Free Audio Readout (Text-To-Speech) Button for Assistant Guidance
                if (!isUser && message.text.isNotBlank() && message.status == MessageStatus.SUCCESS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardBlue)
                            .clickable {
                                if (isSpeaking) {
                                    onStopSpeaking()
                                } else {
                                    onSpeak(message.text)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isSpeaking) "⏹️ Stop Audio" else "🔊 Read Aloud", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    }
                }

                // Error status & Retry action
                if (message.status == MessageStatus.ERROR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Retry", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyCardView(
    card: EmergencyActionCard,
    onCallEmergency: (String) -> Unit
) {
    Column {
        Text(
            text = card.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EmergencyRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Big Emergency Call Button
        Button(
            onClick = { onCallEmergency(card.emergencyNumber) },
            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🚨 CALL ${card.emergencyNumber} / 911 NOW",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "IMMEDIATE ACTION STEPS:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBlue
        )

        card.immediateSteps.forEachIndexed { index, step ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${index + 1}. $step",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF2D3748)
            )
        }

        if (card.warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "CRITICAL CONTRAINDICATIONS:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmergencyRed
            )
            card.warnings.forEach { warning ->
                Text(
                    text = "• $warning",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = EmergencyRed
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Standard: ${card.sourceCitation}",
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun QuickPromptChips(onSelectPrompt: (String) -> Unit) {
    val samplePrompts = listOf(
        "How to treat a burn?",
        "Choking first aid",
        "Adult CPR steps",
        "Severe bleeding control",
        "Electric shock safety"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(samplePrompts) { prompt ->
            SuggestionChip(
                onClick = { onSelectPrompt(prompt) },
                label = { Text(prompt, fontSize = 12.sp, color = DarkBlue) },
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CardBlue)
            )
        }
    }
}

@Composable
fun GeneratingIndicator(onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = EmergencyRed
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Generating first-aid guidance...",
            fontSize = 13.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(
            onClick = onCancel,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
        ) {
            Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModelSettingsDialog(
    modelStatus: ModelStatus,
    chatMode: ChatMode,
    onDismiss: () -> Unit,
    onSetMode: (ChatMode) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToEmergencies: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency AI Settings", fontWeight = FontWeight.Bold, color = DarkBlue) },
        text = {
            Column {
                Text(text = "Chat Routing Mode:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ChatMode.values().forEach { mode ->
                        FilterChip(
                            selected = chatMode == mode,
                            onClick = { onSetMode(mode) },
                            label = { Text(mode.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Offline Model (Qwen SLM):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                when (modelStatus) {
                    is ModelStatus.Ready -> {
                        Text(
                            text = "✅ Qwen2.5-0.5B-Instruct Installed (${modelStatus.sizeBytes / (1024 * 1024)} MB)",
                            color = ForestGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                        ) {
                            Text("Delete Local Model")
                        }
                    }
                    is ModelStatus.Downloading -> {
                        Text(
                            text = "Downloading model: ${(modelStatus.progress * 100).toInt()}% (${modelStatus.downloadedBytes / (1024 * 1024)} MB / ${modelStatus.totalBytes / (1024 * 1024)} MB)",
                            fontSize = 12.sp,
                            color = DarkBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { modelStatus.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onCancelDownload) {
                            Text("Cancel Download", color = EmergencyRed)
                        }
                    }
                    is ModelStatus.Error -> {
                        Text(
                            text = "❌ Error: ${modelStatus.message}",
                            color = EmergencyRed,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = onDownload) {
                            Text("Retry Download")
                        }
                    }
                    is ModelStatus.NotInstalled -> {
                        Text(
                            text = "Offline Qwen SLM is not yet downloaded. The app uses verified local guidance cards in offline mode until installed.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            Text("Download Qwen SLM (~398 MB)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onNavigateToEmergencies,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Browse Verified Category Cards")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = DarkBlue)
            }
        }
    )
}
