package az21.subscribe.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R

/** The destinations reachable from the bottom navigation bar. */
enum class TopLevelDestination(
  @param:StringRes val labelRes: Int,
  val icon: ImageVector,
  val route: NavKey,
) {
  HOME(R.string.nav_home, Icons.Default.Home, HomeRoute),
  CALENDAR(R.string.nav_calendar, Icons.Default.DateRange, CalendarRoute),
  METRICS(R.string.nav_metrics, Icons.Default.Info, MetricsRoute),
  SETTINGS(R.string.nav_settings, Icons.Default.Settings, SettingsRoute),
}
