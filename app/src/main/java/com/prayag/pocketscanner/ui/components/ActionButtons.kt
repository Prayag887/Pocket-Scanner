package com.prayag.pocketscanner.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import java.io.File

@Composable
fun ActionButtons(
    pagerState: PagerState,
    document: Document,
    onEdit: (Document, Int) -> Unit,
    onShare: (Document) -> Unit = {}, // You can optionally remove this
    context: Context,
    viewModel: DocumentViewModel,
    navigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(Icons.Default.Edit, "Edit") {
            onEdit(document, pagerState.currentPage)
        }

        ActionButton(Icons.Default.Share, "Share") {
            val imageUri = document.pages.getOrNull(pagerState.currentPage)?.imageUri
            if (imageUri != null) {
                val file = File(Uri.parse(imageUri).path ?: return@ActionButton)
                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider", // make sure to match manifest
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Document Page"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No app found to share", Toast.LENGTH_SHORT).show()
                }
            }
        }

        ActionButton(
            icon = Icons.Default.Delete,
            contentDesc = "Delete",
            tint = MaterialTheme.colorScheme.error
        ) {
            val fileUri = document.pages[pagerState.currentPage].imageUri
            val fileName = File(fileUri.substringBefore("#")).name

            viewModel.deleteDocument(context, fileName) { success ->
                Toast.makeText(
                    context,
                    if (success) "Deleted" else "Failed to delete",
                    Toast.LENGTH_SHORT
                ).show()

                if (success) navigateBack()
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDesc: String,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    FilledTonalIconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDesc, tint = tint)
    }
}
