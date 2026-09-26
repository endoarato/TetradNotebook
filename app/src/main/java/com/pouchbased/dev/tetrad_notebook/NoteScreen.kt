package com.pouchbased.dev.tetrad_notebook

import androidx.compose.foundation.gestures.detectTapGestures
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun NoteScreen(viewModel: NoteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentNote = uiState.note
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.openDocument(it, context) }
        }
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentNote != null) {
                    Text(
                        text = currentNote.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Button(onClick = { viewModel.saveCurrentNote() }) {
                        Text("Save")
                    }
                    Spacer(Modifier.width(8.dp))
                } else {
                    Text(
                        text = "Tetrad Notebook",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(onClick = { viewModel.createNewNote() }) {
                    Text("New")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    launcher.launch(arrayOf("application/pdf", "image/*"))
                }) {
                    Text("Open")
                }
            }
        },
        bottomBar = {
            if (currentNote != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Interaction/Layer toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.setInteractionLayer(InteractionLayer.ANNOTATION) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.activeLayer == InteractionLayer.ANNOTATION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Draw")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.setInteractionLayer(InteractionLayer.SOURCE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.activeLayer == InteractionLayer.SOURCE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Interact")
                        }

                        if (uiState.activeLayer == InteractionLayer.ANNOTATION) {
                            Spacer(Modifier.width(16.dp))
                            Button(
                                onClick = { viewModel.setTool(Tool.PEN) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.currentTool == Tool.PEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Pen")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.setTool(Tool.ERASER) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.currentTool == Tool.ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Eraser")
                            }
                        }
                    }

                    // Pen Customization
                    if (uiState.activeLayer == InteractionLayer.ANNOTATION && uiState.currentTool == Tool.PEN) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Gray)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                items(colors) { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable { viewModel.setPenColor(color.value.toInt()) }
                                            .then(
                                                if (uiState.currentPenColor == color.value.toInt()) {
                                                    Modifier.background(color.copy(alpha = 0.5f))
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Slider(
                                value = uiState.currentPenThickness,
                                onValueChange = { viewModel.setPenThickness(it) },
                                valueRange = 1f..50f,
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }

                    // Page Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.goToPage(uiState.currentPageIndex - 1) },
                            enabled = uiState.currentPageIndex > 0
                        ) {
                            Text("<")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Page ${uiState.currentPageIndex + 1} of ${currentNote.pages.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.goToPage(uiState.currentPageIndex + 1) },
                            enabled = uiState.currentPageIndex < currentNote.pages.size - 1
                        ) {
                            Text(">")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.addPage() }) {
                            Text("+")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (currentNote != null) {
                val page = currentNote.pages.getOrNull(uiState.currentPageIndex) ?: return@Box

                BackgroundLayer(
                    background = page.background,
                    pdfBitmap = uiState.currentPageBitmap,
                    links = uiState.currentPageLinks,
                    interactive = uiState.activeLayer == InteractionLayer.SOURCE,
                    onLinkClick = { targetPage ->
                        viewModel.goToPage(targetPage)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AnnotationLayer(
                    strokes = page.strokes,
                    currentStroke = uiState.currentStroke,
                    interactive = uiState.activeLayer == InteractionLayer.ANNOTATION,
                    onStrokeStart = { x, y, p, isEraser -> viewModel.startStroke(x, y, p, isEraser) },
                    onStrokeMove = { x, y, p, isEraser -> viewModel.addPointToStroke(x, y, p, isEraser) },
                    onStrokeEnd = { viewModel.completeStroke() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("No document loaded. Tap 'Open' or 'New' to start.", Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun BackgroundLayer(
    background: Background,
    pdfBitmap: android.graphics.Bitmap?,
    links: List<PdfLink>,
    interactive: Boolean,
    onLinkClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var size by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .background(Color.White)
            .onGloballyPositioned { size = it.size }
            .then(if (interactive && background is Background.Pdf && pdfBitmap != null) {
                Modifier.pointerInput(links, size) {
                    detectTapGestures { offset ->
                        val (pdfPointX, pdfPointY) = mapScreenToPdf(offset, size, pdfBitmap)

                        links.forEach { link ->
                            if (link.bounds.contains(pdfPointX, pdfPointY)) {
                                link.destPage?.let { onLinkClick(it - 1) }
                                link.uri?.let { uriHandler.openUri(it.toString()) }
                            }
                        }
                    }
                }
            } else Modifier)
    ) {
        when (background) {
            is Background.Pdf -> {
                if (pdfBitmap != null) {
                    Image(
                        bitmap = pdfBitmap.asImageBitmap(),
                        contentDescription = "PDF Page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Link highlighting
                    if (interactive && links.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bitmapWidth = pdfBitmap.width.toFloat()
                            val bitmapHeight = pdfBitmap.height.toFloat()
                            val screenWidth = this.size.width
                            val screenHeight = this.size.height
                            val scale = minOf(screenWidth / bitmapWidth, screenHeight / bitmapHeight)
                            val dx = (screenWidth - bitmapWidth * scale) / 2
                            val dy = (screenHeight - bitmapHeight * scale) / 2

                            links.forEach { link ->
                                // link.bounds are in PDF points (1/72"). 
                                // Our bitmap is 2x that.
                                val left = link.bounds.left * 2 * scale + dx
                                val top = link.bounds.top * 2 * scale + dy
                                val right = link.bounds.right * 2 * scale + dx
                                val bottom = link.bounds.bottom * 2 * scale + dy
                                
                                drawRect(
                                    color = Color.Blue.copy(alpha = 0.2f),
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top)
                                )
                            }
                        }
                    }
                } else {
                    Text("Loading PDF Page...", Modifier.align(Alignment.Center))
                }
            }
            is Background.Image -> {
                AsyncImage(
                    model = background.uri,
                    contentDescription = "Background Image",
                    modifier = Modifier.fillMaxSize()
                )
            }
            is Background.Color -> Box(Modifier.fillMaxSize().background(Color(background.color)))
            Background.None -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun AnnotationLayer(
    strokes: List<Stroke>,
    currentStroke: Stroke?,
    interactive: Boolean,
    onStrokeStart: (Float, Float, Float, Boolean) -> Unit,
    onStrokeMove: (Float, Float, Float, Boolean) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .then(if (interactive) Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    val down = event.changes.first()

                    if (down.type == PointerType.Touch) {
                        return@awaitEachGesture
                    }

                    val isEraser = isEraserActive(down, event)
                    onStrokeStart(down.position.x, down.position.y, down.pressure, isEraser)

                    var pointer = down
                    while (pointer.pressed) {
                        val moveEvent = awaitPointerEvent()
                        pointer = moveEvent.changes.firstOrNull() ?: break
                        if (pointer.pressed) {
                            val movingIsEraser = isEraserActive(pointer, moveEvent)
                            onStrokeMove(pointer.position.x, pointer.position.y, pointer.pressure, movingIsEraser)
                            pointer.consume()
                        }
                    }
                    onStrokeEnd()
                }
            } else Modifier)
    ) {
        strokes.forEach { drawStroke(it) }
        currentStroke?.let { drawStroke(it) }
    }
}

private fun isEraserActive(change: PointerInputChange, event: androidx.compose.ui.input.pointer.PointerEvent): Boolean {
    if (change.type == PointerType.Eraser) return true
    try {
        val fields = event::class.java.declaredFields
        for (field in fields) {
            if (field.type == android.view.MotionEvent::class.java) {
                field.isAccessible = true
                val motionEvent = field.get(event) as? android.view.MotionEvent
                if (motionEvent != null) {
                    val buttonState = motionEvent.buttonState
                    if ((buttonState and android.view.MotionEvent.BUTTON_STYLUS_PRIMARY) != 0 || 
                        (buttonState and android.view.MotionEvent.BUTTON_STYLUS_SECONDARY) != 0) {
                        return true
                    }
                    for (i in 0 until motionEvent.pointerCount) {
                        if (motionEvent.getToolType(i) == android.view.MotionEvent.TOOL_TYPE_ERASER) {
                            return true
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {}
    return false
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: Stroke) {
    if (stroke.points.isEmpty()) return
    val color = Color(stroke.color)
    if (stroke.points.size > 1) {
        for (i in 0 until stroke.points.size - 1) {
            val p1 = stroke.points[i]
            val p2 = stroke.points[i + 1]
            val avgPressure = (p1.pressure + p2.pressure) / 2f
            val width = stroke.width * avgPressure
            drawLine(
                color = color,
                start = Offset(p1.x, p1.y),
                end = Offset(p2.x, p2.y),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    } else {
        val p = stroke.points[0]
        drawCircle(
            color = color,
            center = Offset(p.x, p.y),
            radius = (stroke.width * p.pressure) / 2f
        )
    }
}

private fun mapScreenToPdf(
    offset: Offset,
    screenSize: IntSize,
    pdfBitmap: android.graphics.Bitmap
): Pair<Float, Float> {
    val bitmapWidth = pdfBitmap.width.toFloat()
    val bitmapHeight = pdfBitmap.height.toFloat()
    
    val screenWidth = screenSize.width.toFloat()
    val screenHeight = screenSize.height.toFloat()

    val scale = minOf(screenWidth / bitmapWidth, screenHeight / bitmapHeight)
    val dx = (screenWidth - bitmapWidth * scale) / 2
    val dy = (screenHeight - bitmapHeight * scale) / 2

    val pdfX = (offset.x - dx) / scale
    val pdfY = (offset.y - dy) / scale

    // PdfRenderer coordinates are usually in points (1/72 inch). 
    // Our bitmap is 2x that.
    return Pair(pdfX / 2f, pdfY / 2f)
}
