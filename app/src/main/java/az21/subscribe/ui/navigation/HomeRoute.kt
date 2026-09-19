package az21.subscribe.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 destinations. Each destination is a serializable [NavKey] so the back stack can be
 * saved and restored across configuration changes and process death.
 */
@Serializable
data object HomeRoute : NavKey
