package az21.subscribe.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R
import az21.subscribe.domain.metrics.MonthlySpend
import az21.subscribe.domain.model.Currency
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.formatMoney
import az21.subscribe.ui.navigation.SubScribeFloatingToolbar
import az21.subscribe.ui.navigation.TopLevelDestination
import az21.subscribe.ui.theme.AppTheme
import java.math.BigDecimal
import java.time.Month
import java.time.format.TextStyle

private const val MONTHS_PER_ROW = 3

@Composable
fun MetricsScreen(
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MetricsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  MetricsContent(
    uiState = uiState,
    onPreviousYear = viewModel::onPreviousYear,
    onNextYear = viewModel::onNextYear,
    onMonthSelected = viewModel::onMonthSelected,
    onNavigateTopLevel = onNavigateTopLevel,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetricsContent(
  uiState: MetricsUiState,
  onPreviousYear: () -> Unit,
  onNextYear: () -> Unit,
  onMonthSelected: (Month) -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(title = stringResource(R.string.metrics_title), scrollBehavior = scrollBehavior)
    },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      SubScribeFloatingToolbar(
        selectedDestination = TopLevelDestination.METRICS,
        onNavigateTopLevel = onNavigateTopLevel,
      )
    },
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator()
      }
    } else {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        YearSelector(year = uiState.year, onPreviousYear = onPreviousYear, onNextYear = onNextYear)
        YearlySummary(uiState = uiState)
        MonthGrid(
          months = uiState.monthly,
          selectedMonth = uiState.selectedMonth,
          currency = uiState.currency,
          onMonthSelected = onMonthSelected,
        )
        SelectedMonthDetail(uiState = uiState)
      }
    }
  }
}

@Composable
private fun YearSelector(
  year: Int,
  onPreviousYear: () -> Unit,
  onNextYear: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = onPreviousYear) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = stringResource(R.string.metrics_previous_year),
      )
    }
    Text(text = year.toString(), style = MaterialTheme.typography.headlineSmall)
    IconButton(onClick = onNextYear) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = stringResource(R.string.metrics_next_year),
      )
    }
  }
}

@Composable
private fun YearlySummary(uiState: MetricsUiState) {
  val locale = Locale.current.platformLocale
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SummaryCard(
      title = stringResource(R.string.metrics_total_spend),
      amount = formatMoney(uiState.totalSpend, uiState.currency, locale),
      modifier = Modifier.weight(1f),
    )
    SummaryCard(
      title = stringResource(R.string.metrics_divvied_annual),
      amount = formatMoney(uiState.divviedAnnualSpend, uiState.currency, locale),
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun SummaryCard(
  title: String,
  amount: String,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.labelMedium)
      Text(text = amount, style = MaterialTheme.typography.titleLarge)
    }
  }
}

@Composable
private fun MonthGrid(
  months: List<MonthlySpend>,
  selectedMonth: Month,
  currency: Currency,
  onMonthSelected: (Month) -> Unit,
) {
  val spendByMonth = months.associateBy { it.month }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Month.entries.chunked(MONTHS_PER_ROW).forEach { rowMonths ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        rowMonths.forEach { month ->
          MonthCell(
            month = month,
            spend = spendByMonth[month]?.trueMonthlySpend ?: BigDecimal.ZERO,
            selected = month == selectedMonth,
            currency = currency,
            onClick = { onMonthSelected(month) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun MonthCell(
  month: Month,
  spend: BigDecimal,
  selected: Boolean,
  currency: Currency,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val locale = Locale.current.platformLocale
  Card(
    onClick = onClick,
    modifier = modifier,
    colors =
      if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      } else {
        CardDefaults.cardColors()
      },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = month.getDisplayName(TextStyle.SHORT, locale),
        style = MaterialTheme.typography.labelLarge,
      )
      Text(
        text = formatMoney(spend, currency, locale),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun SelectedMonthDetail(uiState: MetricsUiState) {
  val monthSpend = uiState.selectedMonthSpend ?: return
  val locale = Locale.current.platformLocale
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = uiState.selectedMonth.getDisplayName(TextStyle.FULL, locale),
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text =
          stringResource(
            R.string.metrics_charges,
            formatMoney(monthSpend.trueMonthlySpend, uiState.currency, locale),
          ),
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        text =
          stringResource(
            R.string.metrics_divvied,
            formatMoney(monthSpend.divviedAnnualSpend, uiState.currency, locale),
          ),
        style = MaterialTheme.typography.bodyLarge,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun MetricsContentPreview() {
  AppTheme {
    MetricsContent(
      uiState =
        MetricsUiState(
          year = 2024,
          selectedMonth = Month.MAY,
          isLoading = false,
          monthly =
            Month.entries.map { month ->
              MonthlySpend(
                year = 2024,
                month = month,
                trueMonthlySpend = BigDecimal("10.00"),
                divviedAnnualSpend = BigDecimal("10.00"),
              )
            },
          totalSpend = BigDecimal("240.00"),
          divviedAnnualSpend = BigDecimal("120.00"),
          currency = Currency.USD,
        ),
      onPreviousYear = {},
      onNextYear = {},
      onMonthSelected = {},
      onNavigateTopLevel = {},
    )
  }
}
