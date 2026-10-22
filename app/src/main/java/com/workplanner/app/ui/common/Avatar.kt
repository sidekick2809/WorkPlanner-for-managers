package com.workplanner.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workplanner.app.data.PhotoStorage
import com.workplanner.app.ui.theme.avatarColors

fun initialsOf(name: String): String {
    val letters = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    return letters.ifEmpty { "?" }
}

@Composable
fun InitialsAvatar(
    name: String,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val colors = avatarColors(colorIndex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.container),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            color = colors.content,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp,
            maxLines = 1
        )
    }
}

/** Avatar that shows the chosen profile picture, falling back to initials. */
@Composable
fun PersonAvatar(
    name: String,
    colorIndex: Int,
    photoPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bitmap = remember(photoPath) { PhotoStorage.read(photoPath) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        InitialsAvatar(name = name, colorIndex = colorIndex, modifier = modifier, size = size)
    }
}

/** Compact alarm label used inside task rows and detail headers. */
@Composable
fun ReminderLabel(
    reminderAt: Long,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val overdue = isOverdue(reminderAt, isDone)
    val color = tint ?: when {
        isDone -> MaterialTheme.colorScheme.onSurfaceVariant
        overdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Alarm,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = if (overdue) {
                "Gecikti · ${formatReminderFriendly(reminderAt)}"
            } else {
                formatReminderFriendly(reminderAt)
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
