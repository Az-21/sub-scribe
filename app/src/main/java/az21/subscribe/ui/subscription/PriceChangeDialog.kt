package az21.subscribe.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import az21.subscribe.R
import az21.subscribe.ui.common.SubscriptionDatePickerDialog
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Collects a new price and the date it takes effect, which may be backdated or future-dated. */
@Composable
fun PriceChangeDialog(
  onConfirm: (BigDecimal, LocalDate) -> Unit,
  onDismiss: () -> Unit,
) {
  var priceText by remember { mutableStateOf("") }
  var effectiveDate by remember { mutableStateOf(LocalDate.now()) }
  var showDatePicker by remember { mutableStateOf(false) }

  val price = priceText.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
  val formattedDate = effectiveDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.detail_price_change_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = priceText,
          onValueChange = { priceText = it },
          label = { Text(stringResource(R.string.form_price)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
        )
        OutlinedButton(onClick = { showDatePicker = true }) {
          Text(stringResource(R.string.detail_price_change_effective, formattedDate))
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = price != null,
        onClick = { price?.let { onConfirm(it, effectiveDate) } },
      ) {
        Text(stringResource(R.string.action_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )

  if (showDatePicker) {
    SubscriptionDatePickerDialog(
      initialDate = effectiveDate,
      onDateSelected = {
        effectiveDate = it
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
    )
  }
}
