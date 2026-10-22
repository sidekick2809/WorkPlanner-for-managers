package com.workplanner.app.ui.reminder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workplanner.app.data.Person
import com.workplanner.app.data.Task
import com.workplanner.app.ui.common.PersonAvatar
import com.workplanner.app.ui.common.formatClock
import com.workplanner.app.ui.theme.Olive600
import com.workplanner.app.ui.theme.Olive700
import com.workplanner.app.ui.theme.Olive900
import com.workplanner.app.ui.theme.Paper
import com.workplanner.app.ui.theme.Sage200
import com.workplanner.app.ui.theme.Sage300

/** The screen that greets the user when a reminder fires, lock screen included. */
@Composable
fun ReminderAlarmScreen(
    task: Task,
    person: Person?,
    onOpen: () -> Unit,
    onSnooze: () -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(Olive900, Olive700, Olive600)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.6f))

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = Sage200,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(36.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "HATIRLATICI",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 3.sp,
                color = Sage300
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = task.reminderAt?.let(::formatClock) ?: formatClock(System.currentTimeMillis()),
                style = MaterialTheme.typography.displayMedium,
                color = Paper,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PersonAvatar(
                        name = person?.name ?: "Bilinmeyen kişi",
                        colorIndex = person?.colorIndex ?: 0,
                        photoPath = person?.photoPath,
                        size = 66.dp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = person?.name ?: "Bilinmeyen kişi",
                        style = MaterialTheme.typography.labelLarge,
                        color = Sage300
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Paper,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.summary.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = task.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Paper.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Paper,
                    contentColor = Olive900
                )
            ) {
                Text(
                    text = "İşi aç",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Paper.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Paper)
                ) {
                    Text("10 dk ertele")
                }

                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Paper.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Paper)
                ) {
                    Text("Tamamlandı")
                }
            }

            Spacer(Modifier.height(6.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    text = "Kapat",
                    color = Paper.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
