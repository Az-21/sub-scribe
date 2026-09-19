package az21.subscribe.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.Tag
import az21.subscribe.ui.common.SubscriptionDatePickerDialog
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
fun SubscriptionFormScreen(
  subscriptionId: String?,
  onBack: () -> Unit,
  onSaved: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SubscriptionFormViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(subscriptionId) { viewModel.initialize(subscriptionId) }
  LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

  SubscriptionFormContent(
    uiState = uiState,
    onBack = onBack,
    onNameChange = viewModel::onNameChange,
    onIconChange = viewModel::onIconChange,
    onStartDateChange = viewModel::onStartDateChange,
    onBillingCycleChange = viewModel::onBillingCycleChange,
    onFreeTrialMonthsChange = viewModel::onFreeTrialMonthsChange,
    onPriceChange = viewModel::onPriceChange,
    onReminderDaysChange = viewModel::onReminderDaysChange,
    onTrialReminderChange = viewModel::onTrialReminderChange,
    onToggleTag = viewModel::onToggleTag,
    onPaymentMethodChange = viewModel::onPaymentMethodChange,
    onNotesChange = viewModel::onNotesChange,
    onSave = viewModel::save,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFormContent(
  uiState: SubscriptionFormUiState,
  onBack: () -> Unit,
  onNameChange: (String) -> Unit,
  onIconChange: (String) -> Unit,
  onStartDateChange: (LocalDate?) -> Unit,
  onBillingCycleChange: (BillingCycle) -> Unit,
  onFreeTrialMonthsChange: (String) -> Unit,
  onPriceChange: (String) -> Unit,
  onReminderDaysChange: (String) -> Unit,
  onTrialReminderChange: (Boolean) -> Unit,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onNotesChange: (String) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showIconPicker by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(if (uiState.isEditing) R.string.form_title_edit else R.string.form_title_add),
          )
        },
        navigationIcon = {
          TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        },
        actions = {
          TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) }
        },
      )
    },
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      FormFields(
        uiState = uiState,
        onNameChange = onNameChange,
        onOpenIconPicker = { showIconPicker = true },
        onOpenDatePicker = { showDatePicker = true },
        onClearStartDate = { onStartDateChange(null) },
        onBillingCycleChange = onBillingCycleChange,
        onFreeTrialMonthsChange = onFreeTrialMonthsChange,
        onPriceChange = onPriceChange,
        onReminderDaysChange = onReminderDaysChange,
        onTrialReminderChange = onTrialReminderChange,
        onToggleTag = onToggleTag,
        onPaymentMethodChange = onPaymentMethodChange,
        onNotesChange = onNotesChange,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }

  FormDialogs(
    showIconPicker = showIconPicker,
    showDatePicker = showDatePicker,
    uiState = uiState,
    onIconChange = onIconChange,
    onStartDateChange = onStartDateChange,
    onCloseIconPicker = { showIconPicker = false },
    onCloseDatePicker = { showDatePicker = false },
  )
}

@Composable
private fun FormDialogs(
  showIconPicker: Boolean,
  showDatePicker: Boolean,
  uiState: SubscriptionFormUiState,
  onIconChange: (String) -> Unit,
  onStartDateChange: (LocalDate?) -> Unit,
  onCloseIconPicker: () -> Unit,
  onCloseDatePicker: () -> Unit,
) {
  if (showIconPicker) {
    IconPickerDialog(
      currentIconId = uiState.iconId,
      onSelect = {
        onIconChange(it)
        onCloseIconPicker()
      },
      onDismiss = onCloseIconPicker,
    )
  }

  if (showDatePicker) {
    SubscriptionDatePickerDialog(
      initialDate = uiState.startDate ?: LocalDate.now(),
      onDateSelected = {
        onStartDateChange(it)
        onCloseDatePicker()
      },
      onDismiss = onCloseDatePicker,
    )
  }
}

@Composable
private fun FormFields(
  uiState: SubscriptionFormUiState,
  onNameChange: (String) -> Unit,
  onOpenIconPicker: () -> Unit,
  onOpenDatePicker: () -> Unit,
  onClearStartDate: () -> Unit,
  onBillingCycleChange: (BillingCycle) -> Unit,
  onFreeTrialMonthsChange: (String) -> Unit,
  onPriceChange: (String) -> Unit,
  onReminderDaysChange: (String) -> Unit,
  onTrialReminderChange: (Boolean) -> Unit,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onNotesChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    IconField(iconId = uiState.iconId, name = uiState.name, onOpenIconPicker = onOpenIconPicker)
    FormTextField(
      value = uiState.name,
      onValueChange = onNameChange,
      label = stringResource(R.string.form_name),
      isError = uiState.errors.name,
      errorText = stringResource(R.string.form_error_name),
    )
    StartDateField(
      startDate = uiState.startDate,
      onOpenDatePicker = onOpenDatePicker,
      onClearStartDate = onClearStartDate,
    )
    BillingCycleField(cycle = uiState.billingCycle, onBillingCycleChange = onBillingCycleChange)
    FormTextField(
      value = uiState.freeTrialMonths,
      onValueChange = onFreeTrialMonthsChange,
      label = stringResource(R.string.form_free_trial_months),
      keyboardType = KeyboardType.Number,
      isError = uiState.errors.freeTrialMonths,
      errorText = stringResource(R.string.form_error_number),
    )
    FormTextField(
      value = uiState.price,
      onValueChange = onPriceChange,
      label = stringResource(R.string.form_price),
      keyboardType = KeyboardType.Decimal,
      isError = uiState.errors.price,
      errorText = stringResource(R.string.form_error_price),
    )
    FormTextField(
      value = uiState.reminderDaysBefore,
      onValueChange = onReminderDaysChange,
      label = stringResource(R.string.form_reminder_days),
      keyboardType = KeyboardType.Number,
      isError = uiState.errors.reminderDays,
      errorText = stringResource(R.string.form_error_number),
    )
    AssociationFields(
      uiState = uiState,
      onTrialReminderChange = onTrialReminderChange,
      onToggleTag = onToggleTag,
      onPaymentMethodChange = onPaymentMethodChange,
      onNotesChange = onNotesChange,
    )
  }
}

@Composable
private fun AssociationFields(
  uiState: SubscriptionFormUiState,
  onTrialReminderChange: (Boolean) -> Unit,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onNotesChange: (String) -> Unit,
) {
  TrialReminderRow(checked = uiState.trialReminderEnabled, onCheckedChange = onTrialReminderChange)
  TagSelector(
    tags = uiState.availableTags,
    selectedTagIds = uiState.selectedTagIds,
    onToggleTag = onToggleTag,
  )
  PaymentMethodField(
    paymentMethods = uiState.paymentMethods,
    selectedId = uiState.paymentMethodId,
    onPaymentMethodChange = onPaymentMethodChange,
  )
  FormTextField(
    value = uiState.notes,
    onValueChange = onNotesChange,
    label = stringResource(R.string.form_notes),
    minLines = 3,
  )
}

@Composable
private fun FormTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  keyboardType: KeyboardType = KeyboardType.Text,
  isError: Boolean = false,
  errorText: String? = null,
  minLines: Int = 1,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(label) },
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    isError = isError,
    supportingText =
      if (isError && errorText != null) {
        { Text(errorText) }
      } else {
        null
      },
    singleLine = minLines == 1,
    minLines = minLines,
  )
}

@Composable
private fun TrialReminderRow(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(stringResource(R.string.form_trial_reminder), style = MaterialTheme.typography.bodyLarge)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSelector(
  tags: List<Tag>,
  selectedTagIds: Set<UUID>,
  onToggleTag: (UUID) -> Unit,
) {
  if (tags.isEmpty()) return
  Text(stringResource(R.string.form_tags), style = MaterialTheme.typography.titleSmall)
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    tags.forEach { tag ->
      FilterChip(
        selected = tag.id in selectedTagIds,
        onClick = { onToggleTag(tag.id) },
        label = { Text(tag.name) },
      )
    }
  }
}

@Composable
private fun IconField(
  iconId: String,
  name: String,
  onOpenIconPicker: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SubscriptionIcon(iconId = iconId, name = name.ifBlank { "?" }, modifier = Modifier.size(48.dp))
    OutlinedButton(onClick = onOpenIconPicker) { Text(stringResource(R.string.form_choose_icon)) }
  }
}

@Composable
private fun StartDateField(
  startDate: LocalDate?,
  onOpenDatePicker: () -> Unit,
  onClearStartDate: () -> Unit,
) {
  val formatted = startDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedButton(onClick = onOpenDatePicker, modifier = Modifier.weight(1f)) {
      Text(formatted ?: stringResource(R.string.form_start_date_hint))
    }
    if (startDate != null) {
      IconButton(onClick = onClearStartDate) {
        Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.form_clear_date))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingCycleField(
  cycle: BillingCycle,
  onBillingCycleChange: (BillingCycle) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(R.string.form_billing_cycle), style = MaterialTheme.typography.titleSmall)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      BillingCycle.entries.forEachIndexed { index, entry ->
        SegmentedButton(
          selected = cycle == entry,
          onClick = { onBillingCycleChange(entry) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = BillingCycle.entries.size),
        ) {
          Text(
            stringResource(
              if (entry == BillingCycle.MONTHLY) R.string.form_cycle_monthly else R.string.form_cycle_annual,
            ),
          )
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodField(
  paymentMethods: List<PaymentMethod>,
  selectedId: UUID?,
  onPaymentMethodChange: (UUID?) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val selected = paymentMethods.firstOrNull { it.id == selectedId }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(R.string.form_payment_method), style = MaterialTheme.typography.titleSmall)
    Box {
      OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selected?.label ?: stringResource(R.string.form_payment_method_none))
        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.form_payment_method_none)) },
          onClick = {
            onPaymentMethodChange(null)
            expanded = false
          },
        )
        paymentMethods.forEach { method ->
          DropdownMenuItem(
            text = { Text(method.label) },
            onClick = {
              onPaymentMethodChange(method.id)
              expanded = false
            },
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionFormPreview() {
  AppTheme {
    SubscriptionFormContent(
      uiState = SubscriptionFormUiState(),
      onBack = {},
      onNameChange = {},
      onIconChange = {},
      onStartDateChange = {},
      onBillingCycleChange = {},
      onFreeTrialMonthsChange = {},
      onPriceChange = {},
      onReminderDaysChange = {},
      onTrialReminderChange = {},
      onToggleTag = {},
      onPaymentMethodChange = {},
      onNotesChange = {},
      onSave = {},
    )
  }
}
