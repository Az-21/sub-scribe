package az21.subscribe.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Archive destination: cancelled and archived subscriptions, with the only delete entry point. */
@Serializable
data object ArchiveRoute : NavKey
