package com.pouchbased.dev.tetrad_notebook

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Handles saving and loading [Note] annotations.
 * Annotations are stored as hidden files next to the source file.
 */
class NoteRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Gets the storage file for a note.
     * If sourceUri is present, it's an annotation file.
     * If sourceUri is null, it's a standalone note file.
     */
    private fun getNoteFile(noteId: String, sourceUri: Uri?): File {
        return if (sourceUri != null) {
            val fileName = getFileName(sourceUri)
            File(context.filesDir, ".$fileName.note")
        } else {
            File(context.filesDir, "note_$noteId.note")
        }
    }

    private fun getFileName(uri: Uri): String {
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
        return result ?: "unknown_document"
    }

    fun saveNote(note: Note) {
        val targetFile = getNoteFile(note.id, note.sourceUri)
        
        try {
            val content = json.encodeToString(note)
            targetFile.writeText(content)
            println("Saved note ${note.id} to ${targetFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNote(sourceUri: Uri): Note? {
        val targetFile = getNoteFile("", sourceUri)
        if (!targetFile.exists()) return null
        
        return try {
            val content = targetFile.readText()
            json.decodeFromString<Note>(content)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listStandaloneNotes(): List<Note> {
        val files = context.filesDir.listFiles { _, name -> 
            name.startsWith("note_") && name.endsWith(".note")
        } ?: emptyArray()
        
        return files.mapNotNull { file ->
            try {
                val content = file.readText()
                json.decodeFromString<Note>(content)
            } catch (e: Exception) {
                null
            }
        }
    }
}
