package az21.subscribe.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import az21.subscribe.R

/**
 * Destructive delete confirmation. Warns that deletion also removes the price history from metrics
 * and recommends archiving when historic accuracy matters.
 */
@Composable
fun DeleteSubscriptionDialog(
  subscriptionName: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.delete_dialog_title)) },
    text = {
      Text(
        text = stringResource(R.string.delete_dialog_message, subscriptionName),
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )
}
