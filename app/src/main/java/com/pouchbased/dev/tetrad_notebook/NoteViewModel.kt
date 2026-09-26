package com.pouchbased.dev.tetrad_notebook

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class Tool {
    PEN, ERASER
}

/**
 * Holds the state of the current note-taking session.
 */
data class NoteUiState(
    val note: Note? = null,
    val currentPageIndex: Int = 0,
    val activeLayer: InteractionLayer = InteractionLayer.ANNOTATION,
    val currentTool: Tool = Tool.PEN,
    val currentPenColor: Int = 0xFF000000.toInt(),
    val currentPenThickness: Float = 5f,
    val currentStroke: Stroke? = null,
    val isReadOnly: Boolean = false,
    val currentPageBitmap: Bitmap? = null,
    val currentPageLinks: List<PdfLink> = emptyList()
)


class NoteViewModel(
    private val repository: NoteRepository,
    private val pdfRenderer: PdfRendererManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    fun loadNote(note: Note) {
        _uiState.update { it.copy(note = note, currentPageIndex = 0) }
        loadCurrentPageBitmap()
    }

    fun openDocument(uri: Uri, context: Context) {
        viewModelScope.launch {
            // Try to load existing note
            var note = repository.loadNote(uri)
            
            if (note == null) {
                val fileName = getFileName(context, uri) ?: "Untitled"
                var mimeType = context.contentResolver.getType(uri)
                if (mimeType == null && fileName.endsWith(".pdf", ignoreCase = true)) {
                    mimeType = "application/pdf"
                }
                
                if (mimeType?.startsWith("application/pdf") == true) {
                    val pageCount = pdfRenderer.getPageCount(uri)
                    val finalPageCount = if (pageCount > 0) pageCount else 1
                    val pages = (0 until finalPageCount).map { i ->
                        Page(index = i, background = Background.Pdf(uri, i + 1))
                    }
                    note = Note(
                        id = UUID.randomUUID().toString(),
                        name = fileName,
                        sourceUri = uri,
                        pages = pages
                    )
                } else {
                    val background = if (mimeType?.startsWith("image/") == true || 
                        fileName.endsWith(".jpg", ignoreCase = true) || 
                        fileName.endsWith(".jpeg", ignoreCase = true) || 
                        fileName.endsWith(".png", ignoreCase = true)) {
                        Background.Image(uri)
                    } else {
                        Background.None
                    }
                    note = Note(
                        id = UUID.randomUUID().toString(),
                        name = fileName,
                        sourceUri = uri,
                        pages = listOf(Page(index = 0, background = background))
                    )
                }
                repository.saveNote(note)
            }
            
            _uiState.update { it.copy(note = note, currentPageIndex = 0) }
            loadCurrentPageBitmap()
        }
    }

    private fun loadCurrentPageBitmap() {
        val state = _uiState.value
        val note = state.note ?: return
        val page = note.pages.getOrNull(state.currentPageIndex) ?: return
        val background = page.background

        if (background is Background.Pdf) {
            viewModelScope.launch {
                val bitmap = pdfRenderer.renderPage(background.uri, background.pageNumber)
                val links = pdfRenderer.getLinks(background.uri, background.pageNumber)
                _uiState.update { it.copy(currentPageBitmap = bitmap, currentPageLinks = links) }
            }
        } else {
            _uiState.update { it.copy(currentPageBitmap = null, currentPageLinks = emptyList()) }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun setInteractionLayer(layer: InteractionLayer) {
        _uiState.update { it.copy(activeLayer = layer) }
    }

    fun setTool(tool: Tool) {
        _uiState.update { it.copy(currentTool = tool) }
    }

    fun setPenColor(color: Int) {
        _uiState.update { it.copy(currentPenColor = color) }
    }

    fun setPenThickness(thickness: Float) {
        _uiState.update { it.copy(currentPenThickness = thickness) }
    }

    fun goToPage(index: Int) {
        _uiState.update { state ->
            val note = state.note ?: return@update state
            if (index in note.pages.indices) {
                state.copy(currentPageIndex = index)
            } else {
                state
            }
        }
        loadCurrentPageBitmap()
    }

    fun addPage() {
        _uiState.update { state ->
            val currentNote = state.note ?: return@update state
            val nextIndex = currentNote.pages.size
            val newPage = Page(index = nextIndex, background = Background.None)
            val updatedNote = currentNote.copy(pages = currentNote.pages + newPage)
            
            repository.saveNote(updatedNote)
            state.copy(note = updatedNote, currentPageIndex = nextIndex)
        }
        loadCurrentPageBitmap()
    }

    fun saveCurrentNote() {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            repository.saveNote(note)
        }
    }

    fun startStroke(x: Float, y: Float, pressure: Float, isEraserOverride: Boolean = false) {
        val activeTool = if (isEraserOverride) Tool.ERASER else _uiState.value.currentTool
        if (activeTool == Tool.ERASER) {
            eraseAt(x, y)
            return
        }
        val newPoint = Point(x, y, pressure)
        val currentState = _uiState.value
        _uiState.update { it.copy(currentStroke = Stroke(listOf(newPoint), currentState.currentPenColor, currentState.currentPenThickness)) }
    }

    fun addPointToStroke(x: Float, y: Float, pressure: Float, isEraserOverride: Boolean = false) {
        val activeTool = if (isEraserOverride) Tool.ERASER else _uiState.value.currentTool
        if (activeTool == Tool.ERASER) {
            eraseAt(x, y)
            return
        }
        _uiState.update { state ->
            val stroke = state.currentStroke ?: return@update state
            state.copy(currentStroke = stroke.copy(points = stroke.points + Point(x, y, pressure)))
        }
    }

    fun completeStroke() {
        _uiState.update { state ->
            val stroke = state.currentStroke
            val currentNote = state.note ?: return@update state
            
            val updatedNote = if (stroke != null) {
                val updatedPages = currentNote.pages.mapIndexed { index, page ->
                    if (index == state.currentPageIndex) {
                        page.copy(strokes = page.strokes + stroke)
                    } else {
                        page
                    }
                }
                currentNote.copy(pages = updatedPages)
            } else {
                currentNote
            }
            
            saveNoteAsync(updatedNote)
            
            state.copy(
                note = updatedNote,
                currentStroke = null
            )
        }
    }

    private fun eraseAt(x: Float, y: Float) {
        val radius = 30f // Eraser size
        _uiState.update { state ->
            val currentNote = state.note ?: return@update state
            val updatedPages = currentNote.pages.mapIndexed { index, page ->
                if (index == state.currentPageIndex) {
                    val remainingStrokes = page.strokes.filterNot { stroke ->
                        stroke.points.any { pt ->
                            val dx = pt.x - x
                            val dy = pt.y - y
                            (dx * dx + dy * dy) < (radius * radius)
                        }
                    }
                    page.copy(strokes = remainingStrokes)
                } else {
                    page
                }
            }
            val updatedNote = currentNote.copy(pages = updatedPages)
            if (updatedNote != currentNote) {
                saveNoteAsync(updatedNote)
            }
            state.copy(note = updatedNote)
        }
    }

    fun createNewNote() {
        val note = Note(
            id = UUID.randomUUID().toString(),
            name = "Note ${System.currentTimeMillis() % 10000}",
            sourceUri = null,
            pages = listOf(Page(index = 0, background = Background.None))
        )
        repository.saveNote(note)
        _uiState.update { it.copy(note = note, currentPageIndex = 0) }
        loadCurrentPageBitmap()
    }

    private fun saveNoteAsync(note: Note) {
        viewModelScope.launch {
            repository.saveNote(note)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRenderer.close()
    }
}
