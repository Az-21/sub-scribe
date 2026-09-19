package az21.subscribe.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.common.formatMoney
import az21.subscribe.ui.navigation.SubScribeFloatingToolbar
import az21.subscribe.ui.navigation.TopLevelDestination
import az21.subscribe.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

private const val DAYS_PER_WEEK = 7

@Composable
fun CalendarScreen(
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CalendarViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  CalendarContent(
    uiState = uiState,
    onPreviousMonth = viewModel::onPreviousMonth,
    onNextMonth = viewModel::onNextMonth,
    onDaySelected = viewModel::onDaySelected,
    onNavigateTopLevel = onNavigateTopLevel,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarContent(
  uiState: CalendarUiState,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onDaySelected: (LocalDate) -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val locale = Locale.current.platformLocale
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = { SubScribeTopAppBar(title = stringResource(R.string.calendar_title)) },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      SubScribeFloatingToolbar(
        selectedDestination = TopLevelDestination.CALENDAR,
        onNavigateTopLevel = onNavigateTopLevel,
      )
    },
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        LoadingIndicator()
      }
      return@Scaffold
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MonthHeader(
        monthLabel =
          uiState.month.getDisplayName(TextStyle.FULL, locale) + " " + uiState.year,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
      )
      Text(
        text =
          stringResource(
            R.string.calendar_month_spend,
            formatMoney(uiState.monthTotal, uiState.currency, locale),
          ),
        style = MaterialTheme.typography.titleSmall,
      )
      WeekdayHeader(locale = locale)
      CalendarGrid(
        firstDayOffset = uiState.firstDayOffset,
        days = uiState.days,
        selectedDate = uiState.selectedDate,
        onDaySelected = onDaySelected,
      )
      SelectedDayCharges(uiState = uiState)
    }
  }
}

@Composable
private fun MonthHeader(
  monthLabel: String,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = onPreviousMonth) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = stringResource(R.string.calendar_previous_month),
      )
    }
    Text(text = monthLabel, style = MaterialTheme.typography.titleLarge)
    IconButton(onClick = onNextMonth) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = stringResource(R.string.calendar_next_month),
      )
    }
  }
}

@Composable
private fun WeekdayHeader(locale: java.util.Locale) {
  val weekDays = DayOfWeek.entries
  Row(modifier = Modifier.fillMaxWidth()) {
    weekDays.forEach { day ->
      Text(
        text = day.getDisplayName(TextStyle.SHORT, locale),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun CalendarGrid(
  firstDayOffset: Int,
  days: List<CalendarDay>,
  selectedDate: LocalDate?,
  onDaySelected: (LocalDate) -> Unit,
) {
  val leading = List(firstDayOffset) { null }
  val leadingAndDays: List<CalendarDay?> = leading + days
  val trailing = List((DAYS_PER_WEEK - leadingAndDays.size % DAYS_PER_WEEK) % DAYS_PER_WEEK) { null }
  val cells: List<CalendarDay?> = leadingAndDays + trailing
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    cells.chunked(DAYS_PER_WEEK).forEach { week ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        week.forEach { day ->
          if (day == null) {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
          } else {
            DayCell(
              day = day,
              selected = day.date == selectedDate,
              onClick = { onDaySelected(day.date) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DayCell(
  day: CalendarDay,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.aspectRatio(1f),
    colors =
      if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      } else {
        CardDefaults.cardColors()
      },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(text = day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
      if (day.charges.isNotEmpty()) {
        Text(
          text = day.charges.size.toString(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun SelectedDayCharges(uiState: CalendarUiState) {
  val locale = Locale.current.platformLocale
  if (uiState.selectedCharges.isEmpty()) {
    Text(
      text = stringResource(R.string.calendar_empty_day),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items(items = uiState.selectedCharges, key = { charge -> "${charge.subscriptionId}-${charge.date}" }) { charge ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SubscriptionIcon(
          iconId = charge.iconId,
          name = charge.name,
          iconColor = charge.iconColor,
          modifier = Modifier.size(32.dp),
        )
        Text(text = charge.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
          text = charge.price?.let { formatMoney(it, uiState.currency, locale) }.orEmpty(),
          style = MaterialTheme.typography.titleSmall,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun CalendarContentPreview() {
  AppTheme {
    CalendarContent(
      uiState = CalendarUiState(year = 2024, month = java.time.Month.MAY, isLoading = false),
      onPreviousMonth = {},
      onNextMonth = {},
      onDaySelected = {},
      onNavigateTopLevel = {},
    )
  }
}
