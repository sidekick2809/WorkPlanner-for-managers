package com.workplanner.app.ui.task

import android.app.AlarmManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workplanner.app.data.Task
import com.workplanner.app.ui.WorkViewModel
import com.workplanner.app.ui.common.PersonAvatar
import com.workplanner.app.ui.common.ReminderPickerDialog
import com.workplanner.app.ui.common.formatDateTime
import com.workplanner.app.ui.common.formatShortDate
import com.workplanner.app.ui.common.humanizeUntil
import com.workplanner.app.ui.common.isOverdue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: WorkViewModel,
    onBack: () -> Unit
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val task = snapshot.task(taskId)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exactAlarmsAllowed = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() != false
    }

    if (task == null) {
        LaunchedEffect(taskId) { onBack() }
        return
    }

    val person = snapshot.person(task.personId)

    var summary by rememberSaveable(task.id) { mutableStateOf(task.summary) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    var progress by rememberSaveable(task.id) { mutableFloatStateOf(task.progress.toFloat()) }
    var showReminderPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val progressValue = progress.roundToInt()
    val dirty = summary != task.summary || notes != task.notes || progressValue != task.progress

    fun saveEdits(notify: Boolean = false) {
        val latest = viewModel.snapshot.value.task(taskId) ?: return
        if (summary == latest.summary && notes == latest.notes && progressValue == latest.progress) return
        viewModel.updateTask(
            latest.copy(
                summary = summary,
                notes = notes,
                progress = progressValue
            )
        )
        if (notify) {
            scope.launch {
                snackbarHostState.showSnackbar("Değişiklikler kaydedildi", duration = SnackbarDuration.Short)
            }
        }
    }

    fun close() {
        saveEdits()
        onBack()
    }

    BackHandler { close() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = task.title,
                dirty = dirty,
                onBack = ::close,
                onSave = { saveEdits(notify = true) },
                onDelete = { showDeleteDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroCard(
                task = task,
                personName = person?.name ?: "Bilinmeyen kişi",
                personColorIndex = person?.colorIndex ?: 0,
                personPhotoPath = person?.photoPath,
                onToggleDone = { done ->
                    viewModel.setTaskDone(task.id, done)
                    progress = if (done) 100f else 0f
                }
            )

            DetailCard(
                title = "İlerleme",
                icon = Icons.Rounded.Schedule
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = progressValue.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = when {
                            task.isDone -> "Tamamlandı"
                            progressValue >= 70 -> "Son düzlükte"
                            progressValue >= 30 -> "Devam ediyor"
                            else -> "Yeni başladı"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f,
                    enabled = !task.isDone,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Text(
                    text = if (task.isDone) {
                        "İş tamamlandı olarak işaretlendi."
                    } else {
                        "İlerlemeyi sürükleyerek güncelle, sonra Kaydet'e dokun."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DetailCard(
                title = "İş özeti",
                icon = Icons.Rounded.Description
            ) {
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("İşin kısa özetini yaz…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            DetailCard(
                title = "Notlar",
                icon = Icons.Rounded.EditNote
            ) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notlar, bağlantılar, hatırlatmalar…") },
                    minLines = 5,
                    maxLines = 10,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Değişiklikler sağ üstteki Kaydet ile saklanır.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ReminderCard(
                task = task,
                exactAlarmsAllowed = exactAlarmsAllowed,
                onSet = { showReminderPicker = true },
                onClear = {
                    saveEdits()
                    viewModel.snapshot.value.task(task.id)?.let { latest ->
                        viewModel.updateTask(latest.copy(reminderAt = null))
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar("Hatırlatıcı kaldırıldı", duration = SnackbarDuration.Short)
                    }
                }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Oluşturuldu · ${formatDateTime(task.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.isDone && task.completedAt != null) {
                        Text(
                            text = "Tamamlandı · ${formatDateTime(task.completedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initial = task.reminderAt,
            onDismiss = { showReminderPicker = false },
            onConfirm = { millis ->
                saveEdits()
                viewModel.snapshot.value.task(task.id)?.let { latest ->
                    viewModel.updateTask(latest.copy(reminderAt = millis))
                }
                showReminderPicker = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Hatırlatıcı kuruldu · ${formatDateTime(millis)}",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Görev silinsin mi?") },
            text = { Text("\"${task.title}\" kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTask(task.id)
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Görev Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    dirty: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "İŞ DETAYI",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onSave, enabled = dirty) {
                Text("Kaydet")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Görev Sil",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    task: Task,
    personName: String,
    personColorIndex: Int,
    personPhotoPath: String?,
    onToggleDone: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(
                    name = personName,
                    colorIndex = personColorIndex,
                    photoPath = personPhotoPath,
                    size = 38.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = personName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Oluşturuldu ${formatShortDate(task.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(isDone = task.isDone)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = if (task.isDone) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
            )

            if (task.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = task.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleDone(!task.isDone) },
                shape = RoundedCornerShape(16.dp),
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isDone,
                        onCheckedChange = { onToggleDone(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (task.isDone) "Tamamlandı" else "Tamamlandı olarak işaretle",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (task.isDone) {
                                "Yeniden açmak için dokun"
                            } else {
                                "İş bitince bekleyenlerden tamamlananlara taşınır"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.isDone) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(isDone: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isDone) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Text(
            text = if (isDone) "Tamamlandı" else "Devam ediyor",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDone) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ReminderCard(
    task: Task,
    exactAlarmsAllowed: Boolean,
    onSet: () -> Unit,
    onClear: () -> Unit
) {
    val overdue = isOverdue(task.reminderAt, task.isDone)
    DetailCard(
        title = "Hatırlatıcı",
        icon = Icons.Rounded.Alarm
    ) {
        if (task.reminderAt == null) {
            Text(
                text = "Bu iş için bildirimle hatırlatma kur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSet, enabled = !task.isDone) {
                Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hatırlatıcı kur")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDateTime(task.reminderAt),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (overdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = when {
                            task.isDone -> "İş tamamlandı, hatırlatıcı devre dışı"
                            overdue -> "Hatırlatma zamanı geçti"
                            else -> "Yaklaşık ${humanizeUntil(task.reminderAt)} kaldı"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSet, enabled = !task.isDone) {
                    Text("Değiştir")
                }
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Kaldır")
                }
            }
        }

        if (!exactAlarmsAllowed) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Kesin alarm izni kapalı. Hatırlatıcı birkaç dakika gecikebilir.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
