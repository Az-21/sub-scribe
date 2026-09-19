package az21.subscribe.ui.subscription

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.Tag
import az21.subscribe.ui.common.IconCatalog
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.SubscriptionDatePickerDialog
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.common.SubscriptionTimePickerDialog
import az21.subscribe.ui.common.segmentedListItemColors
import az21.subscribe.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val PRICE_EFFECTIVE_FIELD_WIDTH = 184.dp
private val PICKER_TRAILING_ICON_SIZE = 48.dp

private enum class DateTarget { START, END }

@Composable
fun SubscriptionFormScreen(
  subscriptionId: String?,
  onBack: () -> Unit,
  onSaved: () -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SubscriptionFormViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(subscriptionId) { viewModel.initialize(subscriptionId) }
  LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }
  RequestNotificationPermissionWhenEnabled(enabled = uiState.reminders.isNotEmpty())

  SubscriptionFormContent(
    uiState = uiState,
    onBack = onBack,
    onNameChange = viewModel::onNameChange,
    onIconChange = viewModel::onIconChange,
    onIconColorChange = viewModel::onIconColorChange,
    onStartDateChange = viewModel::onStartDateChange,
    onEndDateChange = viewModel::onEndDateChange,
    onBillingCycleChange = viewModel::onBillingCycleChange,
    onPriceEntryChange = viewModel::onPriceEntryChange,
    onPriceEntryDateChange = viewModel::onPriceEntryDateChange,
    onAddPriceEntry = viewModel::onAddPriceEntry,
    onRemovePriceEntry = viewModel::onRemovePriceEntry,
    onAddReminder = viewModel::onAddReminder,
    onRemoveReminder = viewModel::onRemoveReminder,
    onReminderDaysChange = viewModel::onReminderDaysChange,
    onReminderTimeChange = viewModel::onReminderTimeChange,
    onToggleTag = viewModel::onToggleTag,
    onPaymentMethodChange = viewModel::onPaymentMethodChange,
    onNotesChange = viewModel::onNotesChange,
    onOpenTags = onOpenTags,
    onOpenPaymentMethods = onOpenPaymentMethods,
    onSave = viewModel::save,
    modifier = modifier,
  )
}

/**
 * Asks for the notification permission the first time the user enables a reminder on this screen.
 * Deliberately not requested on cold app launch.
 */
@Composable
private fun RequestNotificationPermissionWhenEnabled(enabled: Boolean) {
  val context = LocalContext.current
  var requested by rememberSaveable { mutableStateOf(false) }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  LaunchedEffect(enabled) {
    if (!enabled || requested) return@LaunchedEffect
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
    val granted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) {
      requested = true
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionFormContent(
  uiState: SubscriptionFormUiState,
  onBack: () -> Unit,
  onNameChange: (String) -> Unit,
  onIconChange: (String) -> Unit,
  onIconColorChange: (Int?) -> Unit,
  onStartDateChange: (LocalDate?) -> Unit,
  onEndDateChange: (LocalDate?) -> Unit,
  onBillingCycleChange: (BillingCycle) -> Unit,
  onPriceEntryChange: (Int, String) -> Unit,
  onPriceEntryDateChange: (Int, LocalDate) -> Unit,
  onAddPriceEntry: () -> Unit,
  onRemovePriceEntry: (Int) -> Unit,
  onAddReminder: () -> Unit,
  onRemoveReminder: (Int) -> Unit,
  onReminderDaysChange: (Int, String) -> Unit,
  onReminderTimeChange: (Int, LocalTime) -> Unit,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onNotesChange: (String) -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showIconPicker by remember { mutableStateOf(false) }
  var datePickerTarget by remember { mutableStateOf<DateTarget?>(null) }
  var timePickerIndex by remember { mutableStateOf<Int?>(null) }
  var priceDatePickerIndex by remember { mutableStateOf<Int?>(null) }

  FormScaffold(
    uiState = uiState,
    onBack = onBack,
    onNameChange = onNameChange,
    onBillingCycleChange = onBillingCycleChange,
    onPriceEntryChange = onPriceEntryChange,
    onAddPriceEntry = onAddPriceEntry,
    onRemovePriceEntry = onRemovePriceEntry,
    onAddReminder = onAddReminder,
    onRemoveReminder = onRemoveReminder,
    onReminderDaysChange = onReminderDaysChange,
    onToggleTag = onToggleTag,
    onPaymentMethodChange = onPaymentMethodChange,
    onNotesChange = onNotesChange,
    onOpenTags = onOpenTags,
    onOpenPaymentMethods = onOpenPaymentMethods,
    onOpenIconPicker = { showIconPicker = true },
    onOpenStartDatePicker = { datePickerTarget = DateTarget.START },
    onOpenEndDatePicker = { datePickerTarget = DateTarget.END },
    onClearEndDate = { onEndDateChange(null) },
    onOpenPriceDatePicker = { index -> priceDatePickerIndex = index },
    onOpenTimePicker = { index -> timePickerIndex = index },
    onSave = onSave,
    modifier = modifier,
  )

  FormDialogs(
    showIconPicker = showIconPicker,
    datePickerTarget = datePickerTarget,
    timePickerIndex = timePickerIndex,
    priceDatePickerIndex = priceDatePickerIndex,
    uiState = uiState,
    onIconChange = onIconChange,
    onIconColorChange = onIconColorChange,
    onStartDateChange = onStartDateChange,
    onEndDateChange = onEndDateChange,
    onPriceEntryDateChange = { index, date ->
      onPriceEntryDateChange(index, date)
      priceDatePickerIndex = null
    },
    onReminderTimeChange = { index, time ->
      onReminderTimeChange(index, time)
      timePickerIndex = null
    },
    onCloseIconPicker = { showIconPicker = false },
    onCloseDatePicker = { datePickerTarget = null },
    onClosePriceDatePicker = { priceDatePickerIndex = null },
    onCloseTimePicker = { timePickerIndex = null },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FormScaffold(
  uiState: SubscriptionFormUiState,
  onBack: () -> Unit,
  onNameChange: (String) -> Unit,
  onBillingCycleChange: (BillingCycle) -> Unit,
  onPriceEntryChange: (Int, String) -> Unit,
  onAddPriceEntry: () -> Unit,
  onRemovePriceEntry: (Int) -> Unit,
  onAddReminder: () -> Unit,
  onRemoveReminder: (Int) -> Unit,
  onReminderDaysChange: (Int, String) -> Unit,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onNotesChange: (String) -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onOpenIconPicker: () -> Unit,
  onOpenStartDatePicker: () -> Unit,
  onOpenEndDatePicker: () -> Unit,
  onClearEndDate: () -> Unit,
  onOpenPriceDatePicker: (Int) -> Unit,
  onOpenTimePicker: (Int) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().imePadding().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title =
          stringResource(if (uiState.isEditing) R.string.form_title_edit else R.string.form_title_add),
        onNavigateUp = onBack,
        scrollBehavior = scrollBehavior,
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.action_save)) },
        icon = { Icon(imageVector = Icons.Default.Check, contentDescription = null) },
        onClick = onSave,
      )
    },
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        LoadingIndicator()
      }
    } else {
      FormFields(
        uiState = uiState,
        onNameChange = onNameChange,
        onOpenIconPicker = onOpenIconPicker,
        onOpenStartDatePicker = onOpenStartDatePicker,
        onOpenEndDatePicker = onOpenEndDatePicker,
        onClearEndDate = onClearEndDate,
        onBillingCycleChange = onBillingCycleChange,
        onPriceEntryChange = onPriceEntryChange,
        onAddPriceEntry = onAddPriceEntry,
        onRemovePriceEntry = onRemovePriceEntry,
        onAddReminder = onAddReminder,
        onRemoveReminder = onRemoveReminder,
        onReminderDaysChange = onReminderDaysChange,
        onOpenPriceDatePicker = onOpenPriceDatePicker,
        onOpenTimePicker = onOpenTimePicker,
        onToggleTag = onToggleTag,
        onPaymentMethodChange = onPaymentMethodChange,
        onNotesChange = onNotesChange,
        onOpenTags = onOpenTags,
        onOpenPaymentMethods = onOpenPaymentMethods,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

@Composable
private fun FormDialogs(
  showIconPicker: Boolean,
  datePickerTarget: DateTarget?,
  timePickerIndex: Int?,
  priceDatePickerIndex: Int?,
  uiState: SubscriptionFormUiState,
  onIconChange: (String) -> Unit,
  onIconColorChange: (Int?) -> Unit,
  onStartDateChange: (LocalDate?) -> Unit,
  onEndDateChange: (LocalDate?) -> Unit,
  onPriceEntryDateChange: (Int, LocalDate) -> Unit,
  onReminderTimeChange: (Int, LocalTime) -> Unit,
  onCloseIconPicker: () -> Unit,
  onCloseDatePicker: () -> Unit,
  onClosePriceDatePicker: () -> Unit,
  onCloseTimePicker: () -> Unit,
) {
  if (showIconPicker) {
    IconPickerDialog(
      currentIconId = uiState.iconId,
      currentColor = uiState.iconColor,
      onSelect = { iconId, defaultColor ->
        val iconChanged = IconCatalog.normalize(iconId) != IconCatalog.normalize(uiState.iconId)
        onIconChange(iconId)
        if (iconChanged) onIconColorChange(defaultColor)
        onCloseIconPicker()
      },
      onColorChange = onIconColorChange,
      onDismiss = onCloseIconPicker,
    )
  }

  if (datePickerTarget != null) {
    SubscriptionDatePickerDialog(
      initialDate =
        when (datePickerTarget) {
          DateTarget.START -> uiState.startDate ?: LocalDate.now()
          DateTarget.END -> uiState.endDate ?: LocalDate.now()
        },
      onDateSelected = { date ->
        when (datePickerTarget) {
          DateTarget.START -> onStartDateChange(date)
          DateTarget.END -> onEndDateChange(date)
        }
        onCloseDatePicker()
      },
      onDismiss = onCloseDatePicker,
    )
  }

  val priceIndex = priceDatePickerIndex
  if (priceIndex != null) {
    val entry = uiState.priceEntries.getOrNull(priceIndex)
    SubscriptionDatePickerDialog(
      initialDate = entry?.effectiveFromDate ?: LocalDate.now(),
      onDateSelected = { date -> onPriceEntryDateChange(priceIndex, date) },
      onDismiss = onClosePriceDatePicker,
    )
  }

  val index = timePickerIndex
  if (index != null) {
    SubscriptionTimePickerDialog(
      initialTime = uiState.reminders.getOrNull(index)?.time ?: ReminderEntry.DEFAULT_TIME,
      onTimeSelected = { time -> onReminderTimeChange(index, time) },
      onDismiss = onCloseTimePicker,
    )
  }
}

@Composable
private fun FormFields(
  uiState: SubscriptionFormUiState,
  onNameChange: (String) -> Unit,
  onOpenIconPicker: () -> Unit,
  onOpenStartDatePicker: () -> Unit,
  onOpenEndDatePicker: () -> Unit,
  onClearEndDate: () -> Unit,
  onBillingCycleChange: (BillingCycle) -> Unit,
  onPriceEntryChange: (Int, String) -> Unit,
  onAddPriceEntry: () -> Unit,
  onRemovePriceEntry: (Int) -> Unit,
  onAddReminder: () -> Unit,
  onRemoveReminder: (Int) -> Unit,
  onReminderDaysChange: (Int, String) -> Unit,
  onOpenPriceDatePicker: (Int) -> Unit,
  onOpenTimePicker: (Int) -> Unit,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onNotesChange: (String) -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    DetailsSection(
      uiState = uiState,
      onNameChange = onNameChange,
      onOpenIconPicker = onOpenIconPicker,
    )
    ScheduleSection(
      uiState = uiState,
      onOpenStartDatePicker = onOpenStartDatePicker,
      onOpenEndDatePicker = onOpenEndDatePicker,
      onClearEndDate = onClearEndDate,
      onBillingCycleChange = onBillingCycleChange,
    )
    PriceSection(
      uiState = uiState,
      onPriceEntryChange = onPriceEntryChange,
      onOpenPriceDatePicker = onOpenPriceDatePicker,
      onAddPriceEntry = onAddPriceEntry,
      onRemovePriceEntry = onRemovePriceEntry,
    )
    OrganizationSection(
      uiState = uiState,
      onToggleTag = onToggleTag,
      onPaymentMethodChange = onPaymentMethodChange,
      onOpenTags = onOpenTags,
      onOpenPaymentMethods = onOpenPaymentMethods,
      onNotesChange = onNotesChange,
    )
    RemindersSection(
      uiState = uiState,
      onAddReminder = onAddReminder,
      onRemoveReminder = onRemoveReminder,
      onReminderDaysChange = onReminderDaysChange,
      onOpenTimePicker = onOpenTimePicker,
    )
    Spacer(modifier = Modifier.height(72.dp))
  }
}

/** A titled group of related form fields, separated from the next group by the parent's spacing. */
@Composable
private fun FormSection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    content()
  }
}

@Composable
private fun DetailsSection(
  uiState: SubscriptionFormUiState,
  onNameChange: (String) -> Unit,
  onOpenIconPicker: () -> Unit,
) {
  FormSection(title = stringResource(R.string.form_section_details)) {
    IconAndNameField(
      iconId = uiState.iconId,
      iconColor = uiState.iconColor,
      name = uiState.name,
      isNameError = uiState.errors.name,
      onNameChange = onNameChange,
      onOpenIconPicker = onOpenIconPicker,
    )
  }
}

@Composable
private fun ScheduleSection(
  uiState: SubscriptionFormUiState,
  onOpenStartDatePicker: () -> Unit,
  onOpenEndDatePicker: () -> Unit,
  onClearEndDate: () -> Unit,
  onBillingCycleChange: (BillingCycle) -> Unit,
) {
  FormSection(title = stringResource(R.string.form_section_schedule)) {
    DateField(
      label = stringResource(R.string.form_start_date),
      date = uiState.startDate,
      placeholder = stringResource(R.string.form_start_date_hint),
      onOpenPicker = onOpenStartDatePicker,
    )
    DateField(
      label = stringResource(R.string.form_end_date),
      date = uiState.endDate,
      placeholder = stringResource(R.string.form_end_date_hint),
      onOpenPicker = onOpenEndDatePicker,
      onClear = onClearEndDate,
    )
    BillingCycleField(cycle = uiState.billingCycle, onBillingCycleChange = onBillingCycleChange)
  }
}

@Composable
private fun PriceSection(
  uiState: SubscriptionFormUiState,
  onPriceEntryChange: (Int, String) -> Unit,
  onOpenPriceDatePicker: (Int) -> Unit,
  onAddPriceEntry: () -> Unit,
  onRemovePriceEntry: (Int) -> Unit,
) {
  FormSection(title = stringResource(R.string.form_price_history)) {
    PriceHistoryFields(
      entries = uiState.priceEntries,
      startDate = uiState.startDate,
      endDate = uiState.endDate,
      isError = uiState.errors.price,
      onPriceChange = onPriceEntryChange,
      onOpenDatePicker = onOpenPriceDatePicker,
      onAddEntry = onAddPriceEntry,
      onRemoveEntry = onRemovePriceEntry,
    )
  }
}

@Composable
private fun OrganizationSection(
  uiState: SubscriptionFormUiState,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onNotesChange: (String) -> Unit,
) {
  FormSection(title = stringResource(R.string.form_section_organization)) {
    AssociationFields(
      uiState = uiState,
      onToggleTag = onToggleTag,
      onPaymentMethodChange = onPaymentMethodChange,
      onOpenTags = onOpenTags,
      onOpenPaymentMethods = onOpenPaymentMethods,
    )
    FormTextField(
      value = uiState.notes,
      onValueChange = onNotesChange,
      label = stringResource(R.string.form_notes),
      minLines = 3,
    )
  }
}

@Composable
private fun RemindersSection(
  uiState: SubscriptionFormUiState,
  onAddReminder: () -> Unit,
  onRemoveReminder: (Int) -> Unit,
  onReminderDaysChange: (Int, String) -> Unit,
  onOpenTimePicker: (Int) -> Unit,
) {
  FormSection(title = stringResource(R.string.form_reminders)) {
    ReminderFields(
      reminders = uiState.reminders,
      onAddReminder = onAddReminder,
      onRemoveReminder = onRemoveReminder,
      onReminderDaysChange = onReminderDaysChange,
      onOpenTimePicker = onOpenTimePicker,
    )
  }
}

@Composable
private fun PriceHistoryFields(
  entries: List<PriceEntry>,
  startDate: LocalDate?,
  endDate: LocalDate?,
  isError: Boolean,
  onPriceChange: (Int, String) -> Unit,
  onOpenDatePicker: (Int) -> Unit,
  onAddEntry: () -> Unit,
  onRemoveEntry: (Int) -> Unit,
) {
  val today = LocalDate.now()
  val effectiveDates = entries.map { entry -> entry.effectiveFromDate }
  var warningIndex by remember { mutableStateOf<Int?>(null) }
  if (isError && entries.isEmpty()) {
    Text(
      text = stringResource(R.string.form_error_price_required),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.error,
    )
  }
  entries.forEachIndexed { index, entry ->
    PriceEntryRow(
      entry = entry,
      startDate = startDate,
      endDate = endDate,
      today = today,
      allEffectiveDates = effectiveDates,
      onPriceChange = { value -> onPriceChange(index, value) },
      onOpenDatePicker = { onOpenDatePicker(index) },
      onWarning = { warningIndex = index },
      onRemove = { onRemoveEntry(index) },
    )
  }
  OutlinedButton(onClick = onAddEntry, modifier = Modifier.fillMaxWidth()) {
    Icon(imageVector = Icons.Default.Add, contentDescription = null)
    Spacer(modifier = Modifier.size(8.dp))
    Text(stringResource(R.string.form_price_add))
  }

  warningIndex?.let { index ->
    entries.getOrNull(index)?.let { entry ->
      val warnings = priceDateWarnings(entry.effectiveFromDate, startDate, endDate, today, effectiveDates)
      if (warnings.isNotEmpty()) {
        PriceDateWarningDialog(
          warnings = warnings,
          effectiveFromDate = entry.effectiveFromDate,
          startDate = startDate,
          endDate = endDate,
          onDismiss = { warningIndex = null },
        )
      }
    }
  }
}

@Composable
private fun PriceEntryRow(
  entry: PriceEntry,
  startDate: LocalDate?,
  endDate: LocalDate?,
  today: LocalDate,
  allEffectiveDates: List<LocalDate>,
  onPriceChange: (String) -> Unit,
  onOpenDatePicker: () -> Unit,
  onWarning: () -> Unit,
  onRemove: () -> Unit,
) {
  val warnings = priceDateWarnings(entry.effectiveFromDate, startDate, endDate, today, allEffectiveDates)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (warnings.isNotEmpty()) {
      IconButton(onClick = onWarning) {
        Icon(
          imageVector = Icons.Outlined.Warning,
          contentDescription = stringResource(R.string.form_price_warning),
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
    OutlinedTextField(
      value = entry.price,
      onValueChange = onPriceChange,
      label = { Text(stringResource(R.string.form_price)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      isError = entry.isError,
      supportingText =
        if (entry.isError) {
          { Text(stringResource(R.string.form_error_price)) }
        } else {
          null
        },
      singleLine = true,
      modifier = Modifier.weight(1f),
    )
    PickerField(
      value = entry.effectiveFromDate.format(DATE_FORMATTER),
      label = stringResource(R.string.form_price_effective),
      onOpen = onOpenDatePicker,
      modifier = Modifier.width(PRICE_EFFECTIVE_FIELD_WIDTH),
      trailingIcon = { Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null) },
    )
    IconButton(onClick = onRemove) {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.form_price_remove),
      )
    }
  }
}

@Composable
private fun PriceDateWarningDialog(
  warnings: List<PriceDateWarning>,
  effectiveFromDate: LocalDate,
  startDate: LocalDate?,
  endDate: LocalDate?,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(pluralStringResource(R.plurals.form_price_warning_title, warnings.size)) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        warnings.forEachIndexed { index, warning ->
          PriceDateWarningItem(
            index = index,
            count = warnings.size,
            warning = warning,
            effectiveFromDate = effectiveFromDate,
            startDate = startDate,
            endDate = endDate,
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_price_warning_confirm)) }
    },
  )
}

@Composable
private fun PriceDateWarningItem(
  index: Int,
  count: Int,
  warning: PriceDateWarning,
  effectiveFromDate: LocalDate,
  startDate: LocalDate?,
  endDate: LocalDate?,
) {
  val title =
    when (warning) {
      PriceDateWarning.BEFORE_START_DATE -> {
        stringResource(R.string.form_price_warning_before_start_title)
      }

      PriceDateWarning.AFTER_END_DATE -> {
        stringResource(R.string.form_price_warning_after_end_title)
      }

      PriceDateWarning.MULTIPLE_IN_ONE_DAY -> {
        stringResource(R.string.form_price_warning_multiple_in_one_day_title)
      }

      PriceDateWarning.IN_FUTURE -> {
        stringResource(R.string.form_price_warning_in_future_title)
      }
    }
  val subtitle =
    when (warning) {
      PriceDateWarning.BEFORE_START_DATE -> {
        stringResource(R.string.form_price_warning_before_start, startDate?.format(DATE_FORMATTER).orEmpty())
      }

      PriceDateWarning.AFTER_END_DATE -> {
        stringResource(R.string.form_price_warning_after_end, endDate?.format(DATE_FORMATTER).orEmpty())
      }

      PriceDateWarning.MULTIPLE_IN_ONE_DAY -> {
        stringResource(R.string.form_price_warning_multiple_in_one_day, effectiveFromDate.format(DATE_FORMATTER))
      }

      PriceDateWarning.IN_FUTURE -> {
        stringResource(R.string.form_price_warning_in_future, effectiveFromDate.format(DATE_FORMATTER))
      }
    }
  SegmentedListItem(
    shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
    colors = segmentedListItemColors(),
    modifier = Modifier.fillMaxWidth(),
    supportingContent = { Text(subtitle) },
    content = { Text(title) },
  )
}

@Composable
private fun FormTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text,
  isError: Boolean = false,
  errorText: String? = null,
  minLines: Int = 1,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
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

/**
 * A read-only text field that opens a picker when tapped anywhere on its surface. When
 * [interactiveTrailing] is set, the trailing icon slot is excluded from the tap-to-open area so an
 * action placed there (such as a clear button) receives its own clicks.
 */
@Composable
private fun PickerField(
  value: String,
  label: String,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
  trailingIcon: (@Composable () -> Unit)? = null,
  interactiveTrailing: Boolean = false,
) {
  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(label) },
      trailingIcon = trailingIcon,
      singleLine = true,
    )
    val trailingInset = if (interactiveTrailing) PICKER_TRAILING_ICON_SIZE else 0.dp
    Box(modifier = Modifier.matchParentSize().padding(end = trailingInset).clickable(onClick = onOpen))
  }
}

@Composable
private fun DateField(
  label: String,
  date: LocalDate?,
  placeholder: String,
  onOpenPicker: () -> Unit,
  onClear: (() -> Unit)? = null,
) {
  val clear = onClear
  val clearable = clear != null && date != null
  PickerField(
    value = date?.format(DATE_FORMATTER) ?: placeholder,
    label = label,
    onOpen = onOpenPicker,
    trailingIcon = {
      if (clear != null && date != null) {
        IconButton(onClick = clear) {
          Icon(
            imageVector = Icons.Outlined.Cancel,
            contentDescription = stringResource(R.string.form_clear_date),
          )
        }
      } else {
        Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
      }
    },
    interactiveTrailing = clearable,
  )
}

@Composable
private fun IconAndNameField(
  iconId: String,
  iconColor: Int?,
  name: String,
  isNameError: Boolean,
  onNameChange: (String) -> Unit,
  onOpenIconPicker: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(56.dp)
          .clip(CircleShape)
          .clickable(onClick = onOpenIconPicker),
      contentAlignment = Alignment.Center,
    ) {
      SubscriptionIcon(
        iconId = iconId,
        name = name.ifBlank { "?" },
        iconColor = iconColor,
        modifier = Modifier.size(56.dp),
      )
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(24.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = stringResource(R.string.form_change_icon),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(4.dp),
        )
      }
    }
    FormTextField(
      value = name,
      onValueChange = onNameChange,
      label = stringResource(R.string.form_name),
      modifier = Modifier.weight(1f),
      isError = isNameError,
      errorText = stringResource(R.string.form_error_name),
    )
  }
}

@Composable
private fun ReminderFields(
  reminders: List<ReminderEntry>,
  onAddReminder: () -> Unit,
  onRemoveReminder: (Int) -> Unit,
  onReminderDaysChange: (Int, String) -> Unit,
  onOpenTimePicker: (Int) -> Unit,
) {
  reminders.forEachIndexed { index, entry ->
    ReminderRow(
      entry = entry,
      onDaysChange = { value -> onReminderDaysChange(index, value) },
      onOpenTimePicker = { onOpenTimePicker(index) },
      onRemove = { onRemoveReminder(index) },
    )
  }
  OutlinedButton(onClick = onAddReminder, modifier = Modifier.fillMaxWidth()) {
    Icon(imageVector = Icons.Default.Add, contentDescription = null)
    Spacer(modifier = Modifier.size(8.dp))
    Text(stringResource(R.string.form_reminder_add))
  }
}

@Composable
private fun ReminderRow(
  entry: ReminderEntry,
  onDaysChange: (String) -> Unit,
  onOpenTimePicker: () -> Unit,
  onRemove: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedTextField(
      value = entry.daysBefore,
      onValueChange = onDaysChange,
      label = { Text(stringResource(R.string.form_reminder_days)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      isError = entry.isError,
      supportingText =
        if (entry.isError) {
          { Text(stringResource(R.string.form_error_number)) }
        } else {
          null
        },
      singleLine = true,
      modifier = Modifier.weight(1f),
    )
    PickerField(
      value = entry.time.format(TIME_FORMATTER),
      label = stringResource(R.string.form_reminder_time),
      onOpen = onOpenTimePicker,
      modifier = Modifier.weight(1f),
      trailingIcon = { Icon(imageVector = Icons.Outlined.AccessTime, contentDescription = null) },
    )
    IconButton(onClick = onRemove) {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.form_reminder_remove),
      )
    }
  }
}

@Composable
private fun AssociationFields(
  uiState: SubscriptionFormUiState,
  onToggleTag: (UUID) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
) {
  TagSelector(
    tags = uiState.availableTags,
    selectedTagIds = uiState.selectedTagIds,
    onToggleTag = onToggleTag,
    onAddTag = onOpenTags,
  )
  PaymentMethodSelector(
    paymentMethods = uiState.paymentMethods,
    selectedId = uiState.paymentMethodId,
    onPaymentMethodChange = onPaymentMethodChange,
    onAddPaymentMethod = onOpenPaymentMethods,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSelector(
  tags: List<Tag>,
  selectedTagIds: Set<UUID>,
  onToggleTag: (UUID) -> Unit,
  onAddTag: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.form_tags), style = MaterialTheme.typography.titleSmall)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AssistChip(
        onClick = onAddTag,
        label = { Text(stringResource(R.string.tags_add)) },
        leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
      )
      tags.forEach { tag ->
        FilterChip(
          selected = tag.id in selectedTagIds,
          onClick = { onToggleTag(tag.id) },
          label = { Text(tag.name) },
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodSelector(
  paymentMethods: List<PaymentMethod>,
  selectedId: UUID?,
  onPaymentMethodChange: (UUID?) -> Unit,
  onAddPaymentMethod: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.form_payment_method), style = MaterialTheme.typography.titleSmall)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AssistChip(
        onClick = onAddPaymentMethod,
        label = { Text(stringResource(R.string.payment_methods_add)) },
        leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
      )
      paymentMethods.forEach { method ->
        val selected = method.id == selectedId
        FilterChip(
          selected = selected,
          onClick = { onPaymentMethodChange(if (selected) null else method.id) },
          label = { Text(method.label) },
        )
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
  val labels =
    BillingCycle.entries.associateWith { entry ->
      stringResource(
        if (entry == BillingCycle.MONTHLY) R.string.form_cycle_monthly else R.string.form_cycle_annual,
      )
    }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(R.string.form_billing_cycle), style = MaterialTheme.typography.titleSmall)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      BillingCycle.entries.forEachIndexed { index, entry ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = BillingCycle.entries.size),
          onClick = { onBillingCycleChange(entry) },
          selected = cycle == entry,
          label = { Text(labels.getValue(entry)) },
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionFormPreview() {
  AppTheme {
    SubscriptionFormContent(
      uiState = SubscriptionFormUiState(reminders = listOf(ReminderEntry(daysBefore = "3"))),
      onBack = {},
      onNameChange = {},
      onIconChange = {},
      onIconColorChange = {},
      onStartDateChange = {},
      onEndDateChange = {},
      onBillingCycleChange = {},
      onPriceEntryChange = { _, _ -> },
      onPriceEntryDateChange = { _, _ -> },
      onAddPriceEntry = {},
      onRemovePriceEntry = {},
      onAddReminder = {},
      onRemoveReminder = {},
      onReminderDaysChange = { _, _ -> },
      onReminderTimeChange = { _, _ -> },
      onToggleTag = {},
      onPaymentMethodChange = {},
      onNotesChange = {},
      onOpenTags = {},
      onOpenPaymentMethods = {},
      onSave = {},
    )
  }
}
