package com.prayag.pocketscanner.presentation.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prayag.pocketscanner.ui.theme.ScannerGreen
import com.prayag.pocketscanner.ui.theme.SurfaceLight
import com.prayag.pocketscanner.ui.theme.VibrantBlue
import com.prayag.pocketscanner.ui.theme.VibrantOrange
import com.prayag.pocketscanner.ui.theme.VibrantPurple
import com.prayag.pocketscanner.ui.theme.VibrantRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SaveDocumentScreen(
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit,
    onSave: (fileName: String, format: String) -> Unit,
    previewImageUri: Uri? = null
) {
    // State
    var fileName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("pdf") }
    var fileQuality by remember { mutableIntStateOf(80) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var isErrorVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPreviewDetail by remember { mutableStateOf(false) }

    val iconBounce = rememberInfiniteTransition(label = "IconBounce")
    val bounceScale by iconBounce.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bounce"
    )

    val hoverState = remember { mutableStateOf(false) }
    val hoverScale by animateFloatAsState(
        targetValue = if (hoverState.value) 1.05f else 1f,
        label = "HoverScale"
    )

    // Animations
    val headerElevation by animateFloatAsState(
        targetValue = if (showSaveSuccess) 12f else 4f,
        animationSpec = tween(500),
        label = "HeaderElevation"
    )

    val saveButtonScale by animateFloatAsState(
        targetValue = if (showSaveSuccess) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SaveButtonScale"
    )

    val rotationAnimation = rememberInfiniteTransition(label = "SpinnerRotation")
    val rotation by rotationAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val shimmerAnim = remember { Animatable(0f) }


    // Validations
    val isFileNameValid = fileName.isNotBlank() &&
            !fileName.contains(Regex("[\\\\/:*?\"<>|]"))
    val hasError = !isFileNameValid && fileName.isNotBlank()

    LaunchedEffect(hasError) {
        if (hasError) {
            shimmerAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(300)
            )
            shimmerAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(300)
            )
        }
    }

    val shimmerOffset = if (hasError) shimmerAnim.value * 10f else 0f

    // Format options with additional info
    val formatOptions = remember {
        listOf(
            FormatOption("pdf", "PDF Document", "Best for documents with text"),
            FormatOption("jpg", "JPEG Image", "Smaller file size, some quality loss"),
            FormatOption("png", "PNG Image", "Lossless quality, larger file size")
        )
    }

    // UI helpers
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val fileNameFocusRequester = remember { FocusRequester() }

    // Format-specific colors
    val formatColor = when(selectedFormat) {
        "pdf" -> VibrantBlue
        "jpg" -> ScannerGreen
        "png" -> VibrantOrange
        else -> VibrantPurple
    }


    LaunchedEffect(Unit) {
        fileNameFocusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Save Document",
                            fontWeight = FontWeight.Bold,
                            color = SurfaceLight
                        )
                        AnimatedVisibility(
                            visible = selectedFormat.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                "Format: ${selectedFormat.uppercase()}",
                                fontSize = 14.sp,
                                color = SurfaceLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .scale(1.2f)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = formatColor,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = 0f
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = formatColor
                ),
                modifier = Modifier.graphicsLayer {
                    this.shadowElevation = headerElevation
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            formatColor.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                previewImageUri?.let {
                    DocumentPreviewCard(
                        previewImageUri = it,
                        formatColor = formatColor,
                        isExpanded = showPreviewDetail,
                        onExpandToggle = { showPreviewDetail = !showPreviewDetail }
                    )
                }

                // File name section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "File Name",
                        fontWeight = FontWeight.Medium,
                        color = SurfaceLight
                    )

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            if (hasError) {
                                errorMessage = "File name cannot contain: \\ / : * ? \" < > |"
                                isErrorVisible = true
                            } else {
                                isErrorVisible = false
                            }
                        },
                        placeholder = {
                            Text(
                                "Enter document name",
                                color = formatColor.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = formatColor,
                                modifier = Modifier.scale(
                                    animateFloatAsState(
                                        if (fileName.isNotBlank()) 1.2f else 1f,
                                        label = "IconScale"
                                    ).value
                                )
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = hasError || fileName.isNotBlank(),
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                if (hasError) {
                                    Icon(
                                        Icons.Default.Error,
                                        "Error",
                                        tint = VibrantRed,
                                        modifier = Modifier.rotate(
                                            animateFloatAsState(
                                                if (isErrorVisible) 5f else 0f,
                                                spring(stiffness = Spring.StiffnessLow),
                                                label = "ErrorShake"
                                            ).value
                                        )
                                    )
                                } else if (fileName.isNotBlank()) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        "Valid",
                                        tint = formatColor
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        isError = hasError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = formatColor,
                            focusedLabelColor = formatColor,
                            cursorColor = formatColor,
                            errorBorderColor = VibrantRed,
                            errorCursorColor = VibrantRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fileNameFocusRequester)
                            .graphicsLayer {
                                translationX = shimmerOffset
                            }
                    )

                    AnimatedVisibility(
                        visible = isErrorVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            errorMessage,
                            color = VibrantRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                // Format section with animated transition
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "File Format",
                        fontWeight = FontWeight.Medium,
                        color = SurfaceLight
                    )

                    FormatSelector(
                        options = formatOptions,
                        selectedValue = selectedFormat,
                        onFormatSelected = { selectedFormat = it }
                    )
                }

                // Quality slider (only for image formats) with animated entry/exit
                AnimatedVisibility(
                    visible = selectedFormat != "pdf",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Quality",
                                fontWeight = FontWeight.Medium,
                                color = formatColor
                            )

                            Text(
                                "$fileQuality%",
                                fontWeight = FontWeight.SemiBold,
                                color = formatColor,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = 1f + (fileQuality - 50f) / 200f
                                    scaleY = 1f + (fileQuality - 50f) / 200f
                                }
                            )
                        }

                        Slider(
                            value = fileQuality.toFloat(),
                            onValueChange = { fileQuality = it.toInt() },
                            valueRange = 50f..100f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = formatColor,
                                activeTrackColor = formatColor,
                                inactiveTrackColor = formatColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            val pulseAnim = rememberInfiniteTransition(label = "InfoPulse")
                            val pulse by pulseAnim.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Pulse"
                            )

                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = formatColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(pulse)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (fileQuality < 70)
                                    "Lower quality saves storage space"
                                else if (fileQuality > 90)
                                    "Higher quality uses more storage space"
                                else
                                    "Balanced quality and file size",
                                fontSize = 12.sp,
                                color = formatColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Format details card with animated entry
                val currentFormat = formatOptions.find { it.value == selectedFormat }
                currentFormat?.let {
                    FormatInfoCard(format = it, formatColor = formatColor)
                }

                Spacer(modifier = Modifier.height(72.dp))
            }

            // Bottom save button area with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface
                            ),
                            startY = 0f,
                            endY = 300f
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button with animation
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                // Button press animation
                                val scaleDown = Animatable(1f)
                                scaleDown.animateTo(
                                    0.9f,
                                    animationSpec = tween(100)
                                )
                                scaleDown.animateTo(
                                    1f,
                                    animationSpec = tween(100)
                                )
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = formatColor,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    formatColor.copy(alpha = 0.5f),
                                    formatColor
                                )
                            )
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .graphicsLayer {
                                scaleX = hoverScale
                                scaleY = hoverScale
                            }
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Save button with animations
                    Button(
                        onClick = {
                            if (isFileNameValid) {
                                keyboardController?.hide()
                                showSaveSuccess = true
                                scope.launch {
                                    delay(1500)
                                    onSave(fileName, selectedFormat)
                                }
                            } else {
                                errorMessage = "Please enter a valid file name"
                                isErrorVisible = true

                                // Animate error shake
                                scope.launch {
                                    val shake = Animatable(0f)
                                    repeat(3) {
                                        shake.animateTo(
                                            10f,
                                            animationSpec = tween(100)
                                        )
                                        shake.animateTo(
                                            -10f,
                                            animationSpec = tween(100)
                                        )
                                    }
                                    shake.animateTo(0f, animationSpec = tween(100))
                                }
                            }
                        },
                        enabled = isFileNameValid && !showSaveSuccess,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = formatColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(saveButtonScale)
                            .graphicsLayer {
                                shadowElevation = if (showSaveSuccess) 8f else 4f
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (showSaveSuccess) {
                                Box(modifier = Modifier.size(20.dp)) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(rotation)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = if (isFileNameValid) bounceScale else 1f
                                        scaleY = if (isFileNameValid) bounceScale else 1f
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Save",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentPreviewCard(
    previewImageUri: Uri?,
    formatColor: Color,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val context = LocalContext.current

    // Animate height directly in Dp for smoother performance
    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 180.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CardHeight"
    )

    val rotationAnim = rememberInfiniteTransition(label = "IconRotation")
    val slowRotation by rotationAnim.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SlowRotation"
    )

    // Icon scale animation, remember outside conditional
    val iconScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "IconScale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = formatColor.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                shadowElevation = if (isExpanded) 8f else 4f
                // Optional: add smooth shadow elevation animation using animateFloatAsState if needed
            }
            .clickable { onExpandToggle() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            formatColor.copy(alpha = 0.2f),
                            formatColor.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                if (previewImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Scanned Document",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (isExpanded) 160.dp else 100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = formatColor,
                        modifier = Modifier
                            .size(64.dp)
                            .rotate(slowRotation)
                            .scale(iconScale)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Document Preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = formatColor,
                    fontWeight = FontWeight.SemiBold
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(formatColor.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = formatColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "1 page · Captured a moment ago",
                                fontSize = 12.sp,
                                color = formatColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormatSelector(
    options: List<FormatOption>,
    selectedValue: String,
    onFormatSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { format ->
            FormatOption(
                format = format,
                isSelected = format.value == selectedValue,
                onClick = { onFormatSelected(format.value) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FormatOption(
    format: FormatOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatValue = remember(format.value) { format.value.lowercase() }

    val iconForFormat = remember(formatValue) {
        when (formatValue) {
            "pdf" -> Icons.Default.PictureAsPdf
            "jpg" -> Icons.Default.Image
            "png" -> Icons.Default.Image
            else -> Icons.Default.Description
        }
    }

    val formatColor = remember(formatValue) {
        when (formatValue) {
            "pdf" -> VibrantBlue
            "jpg" -> ScannerGreen
            "png" -> VibrantOrange
            else -> VibrantPurple
        }
    }

    val transition = updateTransition(targetState = isSelected, label = "FormatOptionTransition")

    val scale by transition.animateFloat(
        label = "ScaleAnimation",
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) }
    ) { selected -> if (selected) 1.05f else 1f }

    val rotation by transition.animateFloat(
        label = "RotationAnimation",
        transitionSpec = { tween(500) }
    ) { selected -> if (selected) 360f else 0f }

    val backgroundColor = remember(isSelected, formatColor) {
        if (isSelected)
            formatColor.copy(alpha = 0.35f)
        else
            formatColor.copy(alpha = 0.15f)
    }

    val contentColor = remember(isSelected, formatColor) {
        if (isSelected)
            formatColor
        else
            formatColor.copy(alpha = 0.15f)
    }

    val gradientBrush = remember(isSelected, formatColor, backgroundColor) {
        if (isSelected)
            Brush.radialGradient(
                colors = listOf(formatColor.copy(alpha = 0.2f), backgroundColor)
            )
        else
            SolidColor(backgroundColor)
    }

    val labelText = remember(formatValue) { formatValue.uppercase() }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent, // Use brush directly in background
        shadowElevation = 1.dp,
        modifier = modifier
            .height(100.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBrush)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = iconForFormat,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = rotation }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = labelText,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .background(color = contentColor, shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}


@Composable
fun FormatInfoCard(format: FormatOption, formatColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "BorderAnimation")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderPulse"
    )

    val additionalInfo = remember(format.value) {
        when (format.value.lowercase()) {
            "pdf" -> "PDF files preserve formatting across devices and are ideal for documents that might be printed."
            "jpg", "jpeg" -> "JPEG is best for photos and complex images where file size matters more than perfect quality."
            "png" -> "PNG maintains image quality and supports transparency, but creates larger files."
            else -> ""
        }
    }

    val gradientBrush = remember(formatColor) {
        Brush.verticalGradient(
            colors = listOf(
                formatColor.copy(alpha = 0.15f),
                Color.Transparent
            )
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = formatColor.copy(alpha = borderAlpha)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = format.name,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = format.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )

            if (additionalInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = additionalInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}


data class FormatOption(
    val value: String = "",
    val name: String = "asdasd",
    val description: String = "asdasd"
)

@Preview(showBackground = true)
@Composable
fun SaveDocumentScreenPreview() {
    val mockUri = "https://picsum.photos/300/200".toUri()
    MaterialTheme {
        SaveDocumentScreen(
            onDismiss = {},
            onNavigateBack = {},
            onSave = { _, _ -> },
            previewImageUri = mockUri
        )
    }
}