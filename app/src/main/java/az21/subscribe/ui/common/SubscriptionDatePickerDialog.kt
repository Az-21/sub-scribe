package az21.subscribe.ui.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import az21.subscribe.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material date picker returning the chosen [LocalDate]; [initialDate] defaults to today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDatePickerDialog(
  initialDate: LocalDate,
  onDateSelected: (LocalDate) -> Unit,
  onDismiss: () -> Unit,
) {
  val state =
    rememberDatePickerState(
      initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          val millis = state.selectedDateMillis
          if (millis != null) {
            onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
          } else {
            onDismiss()
          }
        },
      ) {
        Text(stringResource(R.string.action_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  ) {
    DatePicker(state = state)
  }
}
