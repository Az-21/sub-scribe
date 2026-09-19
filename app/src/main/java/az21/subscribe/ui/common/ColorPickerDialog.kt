package az21.subscribe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import az21.subscribe.R
import android.graphics.Color as AndroidColor

/**
 * Material 3 color picker dialog. Material 3 does not ship a color picker component, so this
 * composes sliders for hue/saturation/brightness around a live preview.
 */
@Composable
fun ColorPickerDialog(
  initialColor: Int,
  onConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val initialHsv =
    remember(initialColor) {
      FloatArray(HSV_COMPONENT_COUNT).also { AndroidColor.colorToHSV(initialColor, it) }
    }
  val hueState = rememberSliderState(value = initialHsv[0], trackRange = HUE_RANGE)
  val saturationState = rememberSliderState(value = initialHsv[1], trackRange = COMPONENT_RANGE)
  val brightnessState = rememberSliderState(value = initialHsv[2], trackRange = COMPONENT_RANGE)
  val selectedColor =
    AndroidColor.HSVToColor(
      floatArrayOf(hueState.value, saturationState.value, brightnessState.value),
    )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.color_picker_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(PREVIEW_HEIGHT.dp)
              .background(Color(selectedColor), MaterialTheme.shapes.medium),
        )
        ColorSlider(
          label = stringResource(R.string.color_picker_hue),
          state = hueState,
        )
        ColorSlider(
          label = stringResource(R.string.color_picker_saturation),
          state = saturationState,
        )
        ColorSlider(
          label = stringResource(R.string.color_picker_brightness),
          state = brightnessState,
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(selectedColor) }) {
        Text(stringResource(R.string.color_picker_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.color_picker_cancel))
      }
    },
  )
}

@Composable
private fun ColorSlider(
  label: String,
  state: SliderState,
) {
  Column {
    Text(text = label, style = MaterialTheme.typography.labelMedium)
    Slider(state = state)
  }
}

private const val HSV_COMPONENT_COUNT = 3
private const val PREVIEW_HEIGHT = 64
private val HUE_RANGE = 0f..360f
private val COMPONENT_RANGE = 0f..1f
