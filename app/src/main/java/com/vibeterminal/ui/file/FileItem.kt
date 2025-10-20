package com.vibeterminal.ui.file

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

/**
 * Represents a file or directory
 */
data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isFile) file.length() else 0,
    val lastModified: Long = file.lastModified(),
    val isHidden: Boolean = file.isHidden
) {
    val icon: ImageVector
        get() = when {
            isDirectory -> Icons.Default.Folder
            name.endsWith(".kt") -> Icons.Default.Code
            name.endsWith(".java") -> Icons.Default.Code
            name.endsWith(".xml") -> Icons.Default.Code
            name.endsWith(".json") -> Icons.Default.Code
            name.endsWith(".txt") -> Icons.Default.TextSnippet
            name.endsWith(".md") -> Icons.Default.Description
            name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") -> Icons.Default.Image
            name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".gz") -> Icons.Default.Archive
            name.endsWith(".pdf") -> Icons.Default.PictureAsPdf
            else -> Icons.Default.InsertDriveFile
        }

    val extension: String
        get() = name.substringAfterLast('.', "")

    fun isTextFile(): Boolean {
        return extension in listOf("txt", "kt", "java", "xml", "json", "md", "sh", "py", "js", "ts", "html", "css", "gradle")
    }

    fun isImageFile(): Boolean {
        return extension in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
    }
}
