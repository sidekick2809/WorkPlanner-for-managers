package com.workplanner.app.ui.home

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workplanner.app.R
import com.workplanner.app.data.Person
import com.workplanner.app.data.Task
import com.workplanner.app.export.WorkPdfExporter
import com.workplanner.app.ui.WorkViewModel
import com.workplanner.app.ui.common.AvatarCropDialog
import com.workplanner.app.ui.common.formatShortDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: WorkViewModel,
    onOpenTask: (String) -> Unit
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAddPerson by rememberSaveable { mutableStateOf(false) }
    var personToRename by remember { mutableStateOf<Person?>(null) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }
    var addTaskForPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var photoTarget by remember { mutableStateOf<Person?>(null) }
    var cropSource by remember { mutableStateOf<Uri?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var backupMenuOpen by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropSource = uri
        } else {
            photoTarget = null
        }
    }

    val pickPhoto: (Person) -> Unit = { person ->
        photoTarget = person
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = viewModel.exportJson()
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Yedek dosyası açılamadı")
                    }.isSuccess
                }
                showMessage(if (saved) "Yedek kaydedildi" else "Yedek kaydedilemedi")
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    fun restoreBackup(uri: Uri) {
        scope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).use { reader ->
                            reader.readText()
                        }
                    }
                }.getOrNull()
            }
            if (raw.isNullOrBlank()) {
                showMessage("Yedek dosyası okunamadı")
                return@launch
            }
            val restored = viewModel.restoreFromJson(raw)
            showMessage(if (restored) "Yedek geri yüklendi" else "Geçersiz yedek dosyası")
        }
    }

    fun exportAndSharePdf() {
        if (exporting) return
        exporting = true
        scope.launch {
            val latest = viewModel.snapshot.value
            val file = withContext(Dispatchers.IO) {
                WorkPdfExporter.export(context, latest)
            }
            exporting = false
            if (file == null) {
                showMessage("PDF oluşturulamadı")
                return@launch
            }

            val uri = runCatching {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }.getOrNull()
            if (uri == null) {
                showMessage("PDF paylaşılamadı")
                return@launch
            }

            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "WorkPlanner – Ekip Görev Raporu")
                putExtra(Intent.EXTRA_TEXT, "WorkPlanner ekip görev raporu ektedir.")
                clipData = ClipData.newRawUri("WorkPlanner PDF", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(share, "PDF'yi e-posta ile gönder").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(chooser)
            }.onFailure {
                showMessage("E-posta uygulaması bulunamadı")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BrandHeader() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.navigationBarsPadding(),
                onClick = {
                    if (snapshot.people.isEmpty()) {
                        showAddPerson = true
                        showMessage("Önce ekibine bir kişi ekle")
                    } else {
                        addTaskForPersonId = null
                        showAddTask = true
                    }
                },
                icon = { Icon(Icons.Rounded.AddTask, contentDescription = null) },
                text = { Text("İş Ekle", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { OverviewCard(snapshot = snapshot) }
                item {
                    TeamHeader(
                        memberCount = snapshot.people.size,
                        pendingCount = snapshot.pendingCount,
                        onAddPerson = { showAddPerson = true }
                    )
                }

                if (snapshot.people.isEmpty()) {
                    item {
                        EmptyTeamCard(onAddPerson = { showAddPerson = true })
                    }
                } else {
                    items(snapshot.people, key = { it.id }) { person ->
                        PersonCard(
                            person = person,
                            pending = snapshot.pendingTasks(person.id),
                            done = snapshot.doneTasks(person.id),
                            onOpenTask = { onOpenTask(it.id) },
                            onToggleTask = { task, done ->
                                viewModel.setTaskDone(task.id, done)
                                if (done) {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "\"${task.title}\" tamamlandı",
                                            actionLabel = "Geri al",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.setTaskDone(task.id, false)
                                        }
                                    }
                                }
                            },
                            onDeleteTask = { task ->
                                viewModel.deleteTask(task.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "\"${task.title}\" silindi",
                                        actionLabel = "Geri al",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTask(task)
                                    }
                                }
                            },
                            onAddTask = {
                                addTaskForPersonId = person.id
                                showAddTask = true
                            },
                            onPickPhoto = { pickPhoto(person) },
                            onRemovePhoto = {
                                viewModel.removePersonPhoto(person.id)
                                showMessage("${person.name} için fotoğraf kaldırıldı")
                            },
                            onRename = { personToRename = person },
                            onDelete = { personToDelete = person }
                        )
                    }

                    item {
                        TipCard()
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = ::exportAndSharePdf,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp),
                icon = {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    }
                },
                text = { Text("PDF", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { backupMenuOpen = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = "Yedekle veya geri yükle"
                    )
                }

                DropdownMenu(
                    expanded = backupMenuOpen,
                    onDismissRequest = { backupMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Yedekle") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Backup, contentDescription = null)
                        },
                        onClick = {
                            backupMenuOpen = false
                            val stamp = SimpleDateFormat(
                                "yyyyMMdd-HHmm",
                                Locale.getDefault()
                            ).format(Date())
                            backupLauncher.launch("WorkPlanner-yedek-$stamp.json")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Geri Yükle") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Restore, contentDescription = null)
                        },
                        onClick = {
                            backupMenuOpen = false
                            restorePicker.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream"
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddPerson) {
        AddPersonDialog(
            onDismiss = { showAddPerson = false },
            onConfirm = { name ->
                val added = viewModel.addPerson(name)
                showAddPerson = false
                showMessage(if (added != null) "${added.name} eklendi" else "İsim gerekli")
            }
        )
    }

    personToRename?.let { person ->
        RenamePersonDialog(
            person = person,
            onDismiss = { personToRename = null },
            onConfirm = { name ->
                viewModel.renamePerson(person.id, name)
                personToRename = null
                showMessage("Kişi güncellendi")
            }
        )
    }

    personToDelete?.let { person ->
        DeletePersonDialog(
            person = person,
            taskCount = snapshot.tasks.count { it.personId == person.id },
            onDismiss = { personToDelete = null },
            onConfirm = {
                viewModel.deletePerson(person)
                personToDelete = null
                showMessage("${person.name} silindi")
            }
        )
    }

    if (showAddTask && snapshot.people.isNotEmpty()) {
        AddTaskDialog(
            people = snapshot.people,
            initialPersonId = addTaskForPersonId,
            onDismiss = { showAddTask = false },
            onConfirm = { personId, title, summary, reminderAt ->
                val added = viewModel.addTask(personId, title, summary, reminderAt)
                showAddTask = false
                if (added != null) {
                    showMessage(
                        if (reminderAt != null) {
                            "İş eklendi · hatırlatıcı kuruldu"
                        } else {
                            "İş eklendi"
                        }
                    )
                }
            }
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
            title = { Text("Yedeği geri yükle") },
            text = {
                Text(
                    "Mevcut ekip ve görevlerin tamamı, seçtiğin yedek dosyasındaki " +
                        "verilerle değiştirilecek. Devam etmek istiyor musun?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri = null
                        restoreBackup(uri)
                    }
                ) { Text("Geri Yükle") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Vazgeç") }
            }
        )
    }

    val pendingCrop = cropSource
    if (pendingCrop != null) {
        AvatarCropDialog(
            source = pendingCrop,
            onDismiss = {
                cropSource = null
                photoTarget = null
            },
            onCropped = { bitmap ->
                photoTarget?.let { person ->
                    viewModel.setPersonPhoto(person.id, bitmap)
                    showMessage("${person.name} için fotoğraf güncellendi")
                }
                cropSource = null
                photoTarget = null
            }
        )
    }
}

@Composable
private fun BrandHeader() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WORK PLANNER",
                    style = MaterialTheme.typography.titleLarge,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ekip Görev Dashboard",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Text(
                    text = formatShortDate(System.currentTimeMillis()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TeamHeader(
    memberCount: Int,
    pendingCount: Int,
    onAddPerson: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ekip",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (memberCount == 0) {
                    "Henüz kimse yok"
                } else {
                    "$memberCount kişi · $pendingCount bekleyen iş"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalIconButton(
            onClick = onAddPerson,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Kişi ekle")
        }
    }
}

@Composable
private fun EmptyTeamCard(onAddPerson: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .alpha(0.9f)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Ekibini kur",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Kişi kartları oluştur, işleri tek yerden takip et. Her kartta bekleyen ve tamamlanan işler ayrı görünür.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAddPerson) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("İlk kişiyi ekle")
            }
        }
    }
}

@Composable
private fun TipCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Bir işe dokunarak özetini, ilerlemesini ve notlarını düzenleyebilir; hatırlatıcı kurabilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
