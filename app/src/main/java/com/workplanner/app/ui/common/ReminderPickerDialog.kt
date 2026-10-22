package com.workplanner.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import java.util.TimeZone

/**
 * Two-step reminder picker: pick a day, then a time. Returns epoch millis in
 * the device's local timezone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerDialog(
    initial: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val start = remember(initial) {
        Calendar.getInstance().apply {
            timeInMillis = initial ?: System.currentTimeMillis()
        }
    }
    val initialUtcDay = remember(initial) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                start.get(Calendar.YEAR),
                start.get(Calendar.MONTH),
                start.get(Calendar.DAY_OF_MONTH)
            )
        }.timeInMillis
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtcDay)
    val timeState = rememberTimePickerState(
        initialHour = start.get(Calendar.HOUR_OF_DAY),
        initialMinute = start.get(Calendar.MINUTE),
        is24Hour = true
    )
    var step by rememberSaveable { mutableIntStateOf(0) }
    val title = if (step == 0) "Hatırlatma günü" else "Hatırlatma saati"

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { step = 1 },
                    enabled = dateState.selectedDateMillis != null
                ) {
                    Text("Devam")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
            }
        ) {
            DatePicker(
                state = dateState,
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                showModeToggle = false
            )
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Vazgeç") }
                        Button(
                            onClick = {
                                val selectedUtc = dateState.selectedDateMillis
                                    ?: System.currentTimeMillis()
                                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = selectedUtc
                                }
                                val local = Calendar.getInstance().apply {
                                    clear()
                                    set(
                                        utc.get(Calendar.YEAR),
                                        utc.get(Calendar.MONTH),
                                        utc.get(Calendar.DAY_OF_MONTH),
                                        timeState.hour,
                                        timeState.minute,
                                        0
                                    )
                                }
                                onConfirm(local.timeInMillis)
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}
