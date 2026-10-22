package com.workplanner.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workplanner.app.R
import com.workplanner.app.data.WorkSnapshot
import com.workplanner.app.ui.common.formatReminderFriendly
import com.workplanner.app.ui.theme.Leaf400
import com.workplanner.app.ui.theme.Olive500
import com.workplanner.app.ui.theme.Olive600
import com.workplanner.app.ui.theme.Olive800
import com.workplanner.app.ui.theme.Paper
import com.workplanner.app.ui.theme.Sage300
import kotlin.math.roundToInt

/** Dark olive summary banner at the top of the board. */
@Composable
fun OverviewCard(
    snapshot: WorkSnapshot,
    modifier: Modifier = Modifier
) {
    val total = snapshot.tasks.size
    val done = snapshot.doneCount
    val pending = snapshot.pendingCount
    val overdue = snapshot.tasks.count {
        !it.isDone && it.reminderAt != null && it.reminderAt < System.currentTimeMillis()
    }
    val progress = if (total == 0) 0f else done.toFloat() / total
    val nextReminder = snapshot.tasks
        .filter { !it.isDone && (it.reminderAt ?: 0L) > System.currentTimeMillis() }
        .minByOrNull { it.reminderAt ?: Long.MAX_VALUE }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Olive800, Olive600)
                )
            )
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            alpha = 0.13f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-14).dp)
                .size(132.dp)
        )

        Column(modifier = Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "GENEL BAKIŞ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage300,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "%${(progress * 100).roundToInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Paper
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    total == 0 -> "Tahta seni bekliyor"
                    pending == 0 -> "Her şey tamam, harika!"
                    overdue > 0 -> "$overdue işin gecikti"
                    else -> "$pending iş bekliyor"
                },
                style = MaterialTheme.typography.titleLarge,
                color = Paper
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = Leaf400,
                trackColor = Color.White.copy(alpha = 0.18f)
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OverviewStat(label = "Bekleyen", value = pending, modifier = Modifier.weight(1f))
                StatDivider()
                OverviewStat(label = "Tamamlanan", value = done, modifier = Modifier.weight(1f))
                StatDivider()
                OverviewStat(
                    label = "Geciken",
                    value = overdue,
                    modifier = Modifier.weight(1f),
                    highlight = overdue > 0
                )
            }

            if (nextReminder != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = Sage300,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sıradaki: ${nextReminder.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Paper.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatReminderFriendly(nextReminder.reminderAt ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = Leaf400
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (highlight) Color(0xFFFFC9AE) else Paper,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Sage300.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(24.dp)
            .background(Olive500.copy(alpha = 0.5f))
    )
}
