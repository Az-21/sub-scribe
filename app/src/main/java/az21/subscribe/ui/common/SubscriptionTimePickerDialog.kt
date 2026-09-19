package az21.subscribe.ui.common

import android.text.format.DateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import az21.subscribe.R
import java.time.LocalTime

/** Material time picker returning the chosen [LocalTime]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionTimePickerDialog(
  initialTime: LocalTime,
  onTimeSelected: (LocalTime) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val state =
    rememberTimePickerState(
      initialHour = initialTime.hour,
      initialMinute = initialTime.minute,
      is24Hour = DateFormat.is24HourFormat(context),
    )

  TimePickerDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.form_reminder_time_title)) },
    confirmButton = {
      TextButton(onClick = { onTimeSelected(LocalTime.of(state.hour, state.minute)) }) {
        Text(stringResource(R.string.action_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  ) {
    TimePicker(state = state)
  }
}
