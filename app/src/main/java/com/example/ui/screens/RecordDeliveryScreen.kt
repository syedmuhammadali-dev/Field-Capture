package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sync.ConnectionStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SafetyAmberContainer
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarningText
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDeliveryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onDeliverySaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val connectionStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var supplierName by remember { mutableStateOf("") }
    var poNumber by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedPhotoUriOrPath by remember { mutableStateOf<String?>(null) }

    var supplierError by remember { mutableStateOf<String?>(null) }
    var poError by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }

    // Android Modern Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUriOrPath = uri.toString()
            photoError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Record Material Delivery",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Navy900
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("record_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Navy900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline Notification Banner (Section 5 requirement)
            if (connectionStatus == ConnectionStatus.OFFLINE) {
                Surface(
                    color = SafetyAmberContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("offline_notice_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = SafetyAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "You're offline",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = StatusWarningText
                            )
                            Text(
                                text = "Your delivery will be saved on this device and synced automatically when you're back online.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = StatusWarningText
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Offline-First",
                            tint = Navy800,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saved on this device first. Uploads automatically in background.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Slate700
                        )
                    }
                }
            }

            // DELIVERY TICKET PHOTO SECTION
            Text(
                text = "Delivery Ticket Photo *",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = Navy900
            )

            // Large photo capture area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (photoError != null) 2.dp else 1.dp,
                        color = if (photoError != null) StatusError else Slate200,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("photo_capture_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (selectedPhotoUriOrPath != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val photoModel = if (selectedPhotoUriOrPath!!.startsWith("content://")) {
                            Uri.parse(selectedPhotoUriOrPath)
                        } else {
                            File(selectedPhotoUriOrPath!!)
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected Delivery Ticket",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlay button to replace image
                        Surface(
                            color = Navy900.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Replace Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replace Image",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Ticket",
                            tint = Slate400,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Take or choose a photo of the delivery ticket",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Gallery button
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("choose_gallery_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontSize = 13.sp)
                            }

                            // Generate jobsite ticket (Zero-friction simulation & emulator testing)
                            Button(
                                onClick = {
                                    val generatedPath = viewModel.generateSampleTicket(
                                        supplier = supplierName.ifBlank { "Apex Construction Materials Ltd." },
                                        po = poNumber.ifBlank { "PO-7741-TEXAS" },
                                        notes = note.ifBlank { "Inspected at site entry by Foreman" }
                                    )
                                    selectedPhotoUriOrPath = generatedPath
                                    photoError = null
                                    if (supplierName.isBlank()) supplierName = "Apex Construction Materials Ltd."
                                    if (poNumber.isBlank()) poNumber = "PO-7741-TEXAS"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_ticket_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Capture Ticket", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            if (photoError != null) {
                Text(
                    text = photoError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusError,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // SUPPLIER NAME INPUT
            Column {
                Text(
                    text = "Supplier Name *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = {
                        supplierName = it
                        if (it.isNotBlank()) supplierError = null
                    },
                    placeholder = { Text("Enter supplier name") },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null, tint = Slate400)
                    },
                    isError = supplierError != null,
                    supportingText = {
                        if (supplierError != null) {
                            Text(text = supplierError!!, color = StatusError)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy800,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }

            // PO NUMBER INPUT
            Column {
                Text(
                    text = "PO Number *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = poNumber,
                    onValueChange = {
                        poNumber = it
                        if (it.isNotBlank()) poError = null
                    },
                    placeholder = { Text("Enter PO number") },
                    leadingIcon = {
                        Icon(Icons.Default.Numbers, contentDescription = null, tint = Slate400)
                    },
                    isError = poError != null,
                    supportingText = {
                        if (poError != null) {
                            Text(text = poError!!, color = StatusError)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("po_number_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy800,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }

            // DELIVERY NOTES INPUT
            Column {
                Text(
                    text = "Delivery Notes",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Add delivery notes...") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Slate400)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy800,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    minLines = 3,
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PRIMARY CTA: SAVE DELIVERY
            Button(
                onClick = {
                    focusManager.clearFocus()

                    var hasError = false
                    if (supplierName.isBlank()) {
                        supplierError = "Supplier name is required"
                        hasError = true
                    }
                    if (poNumber.isBlank()) {
                        poError = "PO number is required"
                        hasError = true
                    }
                    if (selectedPhotoUriOrPath == null) {
                        photoError = "Please take or choose a ticket photo"
                        hasError = true
                    }

                    if (!hasError) {
                        viewModel.saveDelivery(
                            supplierName = supplierName,
                            poNumber = poNumber,
                            note = note,
                            photoPathOrUri = selectedPhotoUriOrPath!!,
                            onSuccess = { delivery ->
                                onDeliverySaved()
                            },
                            onError = { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(err)
                                }
                            }
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_delivery_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy800,
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving Locally...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Delivery", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Saved locally on device first • Never requires internet connection",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate400,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
