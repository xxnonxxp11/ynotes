package app.uamo.ynotes.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.parseMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.ui.theme.GlassBorder
import androidx.compose.foundation.border

@Composable
fun NoteCard(note: NoteEntity, onClick: (NoteEntity) -> Unit) {
    val cardColor = if (note.color == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(note.color)
    val context = LocalContext.current

    // Load first media thumbnail if available
    val mediaFileNames = remember(note.mediaFiles) {
        note.mediaFiles.split("|").filter { it.isNotBlank() }
    }
    var firstBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(note.id, mediaFileNames.firstOrNull()) {
        if (mediaFileNames.isNotEmpty()) {
            firstBitmap = withContext(Dispatchers.IO) {
                MediaManager.loadMediaBitmap(context, note.id, mediaFileNames.first(), note.isSecret, maxSize = 256)
            }
        }
    }

    val baseModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
        .clickable { onClick(note) }

    val finalModifier = if (note.isPinned) {
        baseModifier.background(AuroraPrimary)
    } else {
        baseModifier
    }

    Card(
        modifier = finalModifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) Color.Transparent else cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Image preview at top of card
            if (firstBitmap != null) {
                Image(
                    bitmap = firstBitmap!!.asImageBitmap(),
                    contentDescription = "Imagen adjunta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (note.title.isNotEmpty()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (note.isPinned) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fijado",
                            modifier = Modifier.size(16.dp).padding(start = 4.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                if (note.body.isNotEmpty() && !note.isBodyHidden) {
                    if (note.title.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                    val bodyTextColor = if (note.isPinned) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        text = parseMarkdown(note.body, bodyTextColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyTextColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (mediaFileNames.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = (if (note.isPinned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = if (note.isSecret) Icons.Default.VpnKey else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = (if (note.isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
