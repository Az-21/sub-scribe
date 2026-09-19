package az21.subscribe.ui.common

import androidx.compose.ui.graphics.vector.ImageVector

/** The primary action shown as a tonal pill on the top row of [SubScribeTopAppBar]. */
data class PrimaryAppBarAction(
  val label: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
)
