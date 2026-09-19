package az21.subscribe.ui.common

import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/** Which icon set an [IconEntry] belongs to. */
enum class IconSource { SIMPLE, MATERIAL }

/**
 * A selectable icon. [key] is the stable, source-namespaced identifier stored as `icon_id`; the
 * namespace keeps icons from different sets that share a name from colliding. [defaultColor] is the
 * ARGB color applied when the icon is first chosen (a brand color for Simple Icons, null to inherit
 * the theme for Material Icons).
 */
data class IconEntry(
  val key: String,
  val displayName: String,
  val vector: ImageVector,
  val source: IconSource,
  val defaultColor: Int? = null,
)

/**
 * The combined icon set offered by the picker: the embedded Simple Icons catalog plus a curated
 * Material Icons catalog.
 *
 * Keys are namespaced (`simple:` or `material:`) so a glyph can never collide with a same-named
 * glyph from the other set. Ids stored before namespacing existed are bare Simple Icons slugs, and
 * [normalize] resolves them against the Simple set for backwards compatibility.
 */
object IconCatalog {
  const val SIMPLE_PREFIX: String = "simple:"
  const val MATERIAL_PREFIX: String = "material:"

  /** Every icon from every set, de-duplicated by key and sorted by display name. */
  val all: List<IconEntry> by lazy {
    (SimpleIconsCatalog.all + MaterialIconsCatalog.all).sortedBy { entry ->
      entry.displayName.lowercase(Locale.ROOT)
    }
  }

  private val byEntryKey: Map<String, IconEntry> by lazy { all.associateBy { entry -> entry.key } }

  /** The catalog entry for [iconId], or null when the id is unknown. Legacy bare ids resolve Simple. */
  fun findEntry(iconId: String?): IconEntry? = iconId?.takeIf { it.isNotBlank() }?.let { byEntryKey[normalize(it)] }

  /** The vector for [iconId], or null when the id is unknown. Legacy bare ids resolve as Simple. */
  fun find(iconId: String?): ImageVector? = findEntry(iconId)?.vector

  /** Resolves a stored id to its namespaced catalog key. */
  fun normalize(raw: String): String {
    val lower = raw.trim().lowercase(Locale.ROOT)
    return when {
      lower.startsWith(MATERIAL_PREFIX) -> MATERIAL_PREFIX + slug(lower.removePrefix(MATERIAL_PREFIX))
      lower.startsWith(SIMPLE_PREFIX) -> SIMPLE_PREFIX + slug(lower.removePrefix(SIMPLE_PREFIX))
      else -> SIMPLE_PREFIX + slug(lower)
    }
  }

  /** Icons whose name or key matches [query]; the full list when [query] is blank. */
  fun search(query: String): List<IconEntry> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return all
    val needle = trimmed.lowercase(Locale.ROOT)
    val slug = slug(needle)
    return all.filter { entry ->
      entry.displayName.contains(needle, ignoreCase = true) ||
        entry.key.contains(needle) ||
        (slug.isNotEmpty() && entry.key.contains(slug))
    }
  }

  private fun slug(raw: String): String = raw.filter { character -> character.isLetterOrDigit() }
}
