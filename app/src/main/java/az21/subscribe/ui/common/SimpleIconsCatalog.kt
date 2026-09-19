package az21.subscribe.ui.common

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.AllIcons
import compose.icons.SimpleIcons
import java.util.Locale

/**
 * Lookup over the embedded Simple Icons set.
 *
 * The library generates one `ImageVector` per icon and exposes the whole set as [AllIcons]. Each
 * vector's [ImageVector.name] is the icon identifier with separators removed (for example
 * `Youtubemusic`), which normalises to the Simple Icons slug (`youtubemusic`) used as the stored
 * `icon_id`.
 */
object SimpleIconsCatalog {
  data class IconEntry(
    val key: String,
    val displayName: String,
    val vector: ImageVector,
  )

  /** All icons, de-duplicated by normalised key and sorted for browsing. */
  val all: List<IconEntry> by lazy {
    SimpleIcons.AllIcons
      .map { vector ->
        val key = toIconKey(vector.name)
        IconEntry(key = key, displayName = vector.name, vector = vector)
      }.filter { entry -> entry.key.isNotEmpty() }
      .distinctBy { entry -> entry.key }
      .sortedBy { entry -> entry.displayName.lowercase(Locale.ROOT) }
  }

  private val byKey: Map<String, ImageVector> by lazy { all.associate { entry -> entry.key to entry.vector } }

  /** The vector for [iconId], or null when the key is unknown. */
  fun find(iconId: String?): ImageVector? = iconId?.let { key -> byKey[toIconKey(key)] }

  /** Normalises a raw identifier to the Simple Icons slug used as the stored `icon_id`. */
  fun toIconKey(raw: String): String = raw.filter { character -> character.isLetterOrDigit() }.lowercase(Locale.ROOT)
}
