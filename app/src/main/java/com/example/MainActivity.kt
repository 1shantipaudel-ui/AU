package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.ConversationList
import com.example.ui.components.DeviceQuickDashboard
import com.example.ui.components.HelpDialog
import com.example.ui.components.LanguageSelector
import com.example.ui.components.NotesBottomSheet
import com.example.ui.components.QuickSuggestionChips
import com.example.ui.components.VoiceOrb
import com.example.ui.components.WakeWordDialog
import com.example.ui.theme.CleanAccentPurple
import com.example.ui.theme.CleanAmber
import com.example.ui.theme.CleanBg
import com.example.ui.theme.CleanCardBorder
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VoiceAssistantApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VoiceAssistantApp(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startHandsFreeListeningIfEnabled()
        }
    }

    val messages by viewModel.messages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val wakeWord by viewModel.wakeWord.collectAsState()
    val isHandsFreeWakeEnabled by viewModel.isHandsFreeWakeEnabled.collectAsState()
    val showWakeWordDialog by viewModel.showWakeWordDialog.collectAsState()
    val showNotesSheet by viewModel.showNotesSheet.collectAsState()
    val showHelpDialog by viewModel.showHelpDialog.collectAsState()
    val statusNotification by viewModel.statusNotification.collectAsState()
    val savedNotes by viewModel.savedNotes.collectAsState()

    // Auto-listen for hands-free wake word when permission is granted
    LaunchedEffect(hasAudioPermission, isHandsFreeWakeEnabled) {
        if (hasAudioPermission && isHandsFreeWakeEnabled) {
            viewModel.startHandsFreeListeningIfEnabled()
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show status snackbar messages
    LaunchedEffect(statusNotification) {
        statusNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        containerColor = CleanBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // 1. Top Bar / Status Dashboard
            DeviceQuickDashboard(
                isFlashlightOn = isFlashlightOn,
                isCloudEnabled = viewModel.isGeminiCloudEnabled,
                notesCount = savedNotes.size,
                wakeWord = wakeWord,
                onFlashlightToggle = { viewModel.toggleFlashlightDirect() },
                onOpenNotes = { viewModel.openNotesSheet() },
                onOpenHelp = { viewModel.openHelpDialog() },
                onOpenWakeWordSettings = { viewModel.openWakeWordDialog() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Language Selector Bar (Auto / Hindi / Nepali / English)
            LanguageSelector(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { viewModel.setLanguageMode(it) }
            )

            // Permission Warning if not granted
            if (!hasAudioPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CleanAmber.copy(alpha = 0.1f))
                        .border(1.dp, CleanAmber.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("permission_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Mic Permission",
                                tint = CleanAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mic permission needed for voice input",
                                fontSize = 12.sp,
                                color = CleanAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Grant",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Conversation Message History
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ConversationList(
                    messages = messages,
                    listState = listState,
                    onReplayAudio = { text, lang -> viewModel.replayAudio(text, lang) },
                    onActionClick = { action -> viewModel.executeQuickAction(action) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Quick Suggestion Chips in Detected/Chosen Language
            QuickSuggestionChips(
                selectedLanguage = selectedLanguage,
                onSuggestionClick = { prompt ->
                    viewModel.processUserPrompt(prompt)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Central Clean Voice Controller & Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                VoiceOrb(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isProcessing = isProcessing,
                    audioRms = audioRms,
                    onClick = {
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.toggleListening()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Voice status indicator text
                Text(
                    text = when {
                        isListening -> "Listening... Say \"$wakeWord\" or your command"
                        isProcessing -> "Processing command..."
                        isSpeaking -> "Speaking response (Tap orb to stop)"
                        else -> "Say \"$wakeWord\" to wake up, or tap orb"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isListening -> CleanPrimary
                        isSpeaking -> CleanAmber
                        isProcessing -> CleanAccentPurple
                        else -> TextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Text Query Input Field & Direct Send Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CleanSurface)
                    .border(1.dp, CleanCardBorder, RoundedCornerShape(26.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = when (selectedLanguage) {
                                com.example.data.model.LanguageMode.NEPALI -> "आदेश लेख्नुहोस् (उदा: फ्ल्यासलाइट बाल)..."
                                com.example.data.model.LanguageMode.HINDI -> "कमांड टाइप करें (उदा: अलार्म लगाओ)..."
                                else -> "Type command (e.g. Set alarm, open YouTube)..."
                            },
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("user_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CleanPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                val query = textInput.trim()
                                textInput = ""
                                keyboardController?.hide()
                                viewModel.processUserPrompt(query)
                            }
                        }
                    )
                )

                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = { textInput = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CleanPrimary)
                        .clickable {
                            if (textInput.isNotBlank()) {
                                val query = textInput.trim()
                                textInput = ""
                                keyboardController?.hide()
                                viewModel.processUserPrompt(query)
                            } else {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.toggleListening()
                                }
                            }
                        }
                        .testTag("send_query_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (textInput.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                        contentDescription = "Submit",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Bottom Sheets & Dialogs
        if (showNotesSheet) {
            NotesBottomSheet(
                notes = savedNotes,
                onDismiss = { viewModel.closeNotesSheet() },
                onDeleteNote = { viewModel.deleteVoiceNote(it) },
                onSaveManualNote = { title, content -> viewModel.saveManualNote(title, content) }
            )
        }

        if (showHelpDialog) {
            HelpDialog(
                onDismiss = { viewModel.closeHelpDialog() },
                onSampleCommandClick = { sample ->
                    viewModel.processUserPrompt(sample)
                }
            )
        }

        if (showWakeWordDialog) {
            WakeWordDialog(
                currentWakeWord = wakeWord,
                isHandsFreeEnabled = isHandsFreeWakeEnabled,
                onSaveWakeWord = { newWord ->
                    viewModel.setCustomWakeWord(newWord)
                },
                onToggleHandsFree = { enabled ->
                    viewModel.setHandsFreeMode(enabled)
                },
                onTestWakeWord = {
                    viewModel.testWakeWord()
                },
                onDismiss = { viewModel.closeWakeWordDialog() }
            )
        }
    }
}

