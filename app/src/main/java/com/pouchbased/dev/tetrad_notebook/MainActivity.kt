package com.pouchbased.dev.tetrad_notebook

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pouchbased.dev.tetrad_notebook.ui.theme.TetradNotebookTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val repository by lazy { NoteRepository(applicationContext) }
    private val pdfRenderer by lazy { PdfRendererManager(applicationContext) }
    
    private val viewModel: NoteViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NoteViewModel(repository, pdfRenderer) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TetradNotebookTheme {
                NoteScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
