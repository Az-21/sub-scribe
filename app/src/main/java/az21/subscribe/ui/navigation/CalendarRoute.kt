package az21.subscribe.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Calendar destination: a month grid of the days on which subscriptions are charged. */
@Serializable
data object CalendarRoute : NavKey
