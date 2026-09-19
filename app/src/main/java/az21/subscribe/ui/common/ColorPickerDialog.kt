package az21.subscribe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import az21.subscribe.R
import java.util.Locale
import android.graphics.Color as AndroidColor

/**
 * Material 3 color picker dialog. Material 3 does not ship a color picker component, so this
 * composes sliders for hue/saturation/brightness around a live preview, plus a hex input field.
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
      ColorPickerControls(
        selectedColor = selectedColor,
        hueState = hueState,
        saturationState = saturationState,
        brightnessState = brightnessState,
      )
    },
    confirmButton = {
      FilledTonalButton(onClick = { onConfirm(selectedColor) }) {
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
private fun ColorPickerControls(
  selectedColor: Int,
  hueState: SliderState,
  saturationState: SliderState,
  brightnessState: SliderState,
) {
  Column(verticalArrangement = Arrangement.spacedBy(DIALOG_SPACING)) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(PREVIEW_HEIGHT.dp)
          .background(Color(selectedColor), MaterialTheme.shapes.medium),
    )
    HexColorField(
      color = selectedColor,
      onColorChange = { color ->
        val hsv = FloatArray(HSV_COMPONENT_COUNT).also { AndroidColor.colorToHSV(color, it) }
        hueState.value = hsv[0]
        saturationState.value = hsv[1]
        brightnessState.value = hsv[2]
      },
    )
    ColorSlider(
      label = stringResource(R.string.color_picker_hue),
      state = hueState,
      gradient = hueGradient(),
    )
    ColorSlider(
      label = stringResource(R.string.color_picker_saturation),
      state = saturationState,
      gradient = saturationGradient(hueState.value),
    )
    ColorSlider(
      label = stringResource(R.string.color_picker_brightness),
      state = brightnessState,
      gradient = brightnessGradient(hueState.value, saturationState.value),
    )
  }
}

@Composable
private fun HexColorField(
  color: Int,
  onColorChange: (Int) -> Unit,
) {
  var text by rememberSaveable { mutableStateOf(formatRgbHex(color)) }
  var isEditing by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(color, isEditing) {
    if (!isEditing) text = formatRgbHex(color)
  }

  OutlinedTextField(
    value = text,
    onValueChange = { raw ->
      val digits = raw.filter { it.isHexDigit() }.take(HEX_INPUT_LENGTH).uppercase(Locale.ROOT)
      text = digits
      parseRgbHex(digits)?.let(onColorChange)
    },
    modifier =
      Modifier
        .fillMaxWidth()
        .onFocusChanged { isEditing = it.isFocused },
    label = { Text(stringResource(R.string.color_picker_hex)) },
    prefix = { Text("#") },
    singleLine = true,
    keyboardOptions =
      KeyboardOptions(
        keyboardType = KeyboardType.Ascii,
        capitalization = KeyboardCapitalization.Characters,
      ),
  )
}

@Composable
private fun ColorSlider(
  label: String,
  state: SliderState,
  gradient: Brush,
) {
  Column(verticalArrangement = Arrangement.spacedBy(SLIDER_SPACING)) {
    Text(text = label, style = MaterialTheme.typography.labelMedium)
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(GRADIENT_BAR_HEIGHT)
          .clip(MaterialTheme.shapes.small)
          .background(gradient),
    )
    Slider(state = state)
  }
}

private fun hueGradient(): Brush =
  Brush.horizontalGradient(
    HUE_STOPS.map { Color(AndroidColor.HSVToColor(floatArrayOf(it, 1f, 1f))) },
  )

private fun saturationGradient(hue: Float): Brush =
  Brush.horizontalGradient(
    listOf(Color.White, Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))),
  )

private fun brightnessGradient(
  hue: Float,
  saturation: Float,
): Brush =
  Brush.horizontalGradient(
    listOf(Color.Black, Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f)))),
  )

private const val HSV_COMPONENT_COUNT = 3
private const val HEX_INPUT_LENGTH = 6
private const val PREVIEW_HEIGHT = 64
private val DIALOG_SPACING = 12.dp
private val SLIDER_SPACING = 4.dp
private val GRADIENT_BAR_HEIGHT = 12.dp
private val HUE_RANGE = 0f..360f
private val COMPONENT_RANGE = 0f..1f
private val HUE_STOPS = (0..360 step 60).map { it.toFloat() }
