package com.vibeterminal.ui.file

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for File Browser
 */
class FileViewModel : ViewModel() {

    private val _currentPath = MutableStateFlow(getDefaultPath())
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    init {
        loadFiles(_currentPath.value)
    }

    private fun getDefaultPath(): String {
        // Start from Termux home directory if available
        return System.getenv("HOME") ?: Environment.getExternalStorageDirectory().absolutePath
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path

            val items = withContext(Dispatchers.IO) {
                try {
                    val dir = File(path)
                    if (!dir.exists() || !dir.isDirectory) {
                        return@withContext emptyList<FileItem>()
                    }

                    dir.listFiles()
                        ?.filter { _showHiddenFiles.value || !it.isHidden }
                        ?.map { FileItem(it) }
                        ?.sortedWith(getComparator())
                        ?: emptyList()
                } catch (e: SecurityException) {
                    emptyList()
                }
            }

            _files.value = items
            _isLoading.value = false
        }
    }

    fun navigateUp() {
        val currentFile = File(_currentPath.value)
        currentFile.parentFile?.let { parent ->
            loadFiles(parent.absolutePath)
        }
    }

    fun navigateTo(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            loadFiles(fileItem.path)
        }
    }

    fun refresh() {
        loadFiles(_currentPath.value)
    }

    fun toggleHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        refresh()
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
        refresh()
    }

    fun deleteFile(fileItem: FileItem): Boolean {
        return try {
            fileItem.file.deleteRecursively()
            refresh()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun renameFile(fileItem: FileItem, newName: String): Boolean {
        return try {
            val newFile = File(fileItem.file.parent, newName)
            fileItem.file.renameTo(newFile)
            refresh()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(name: String): Boolean {
        return try {
            val newDir = File(_currentPath.value, name)
            newDir.mkdir()
            refresh()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getComparator(): Comparator<FileItem> {
        return when (_sortBy.value) {
            SortBy.NAME -> compareBy({ !it.isDirectory }, { it.name.lowercase() })
            SortBy.SIZE -> compareBy({ !it.isDirectory }, { -it.size })
            SortBy.DATE -> compareBy({ !it.isDirectory }, { -it.lastModified })
            SortBy.TYPE -> compareBy({ !it.isDirectory }, { it.extension }, { it.name.lowercase() })
        }
    }
}

enum class SortBy {
    NAME,
    SIZE,
    DATE,
    TYPE
}
