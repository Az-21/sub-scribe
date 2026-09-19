package az21.subscribe.data.export

/**
 * Minimal RFC 4180 CSV reader/writer. Fields containing a comma, quote, carriage return, or newline
 * are quoted, and embedded quotes are doubled. Kept dumb and dependency-free so it is easy to test.
 */
internal object CsvTableCodec {
  private const val CARRIAGE_RETURN = '\r'
  private const val LINE_FEED = '\n'
  private const val QUOTE = '"'
  private const val SEPARATOR = ','
  private val SPECIAL_CHARACTERS = setOf(SEPARATOR, QUOTE, LINE_FEED, CARRIAGE_RETURN)

  fun encode(
    headers: List<String>,
    rows: List<List<String>>,
  ): String = (listOf(headers) + rows).joinToString(CRLF) { row -> row.joinToString(",") { escape(it) } }

  fun decode(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val currentRow = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var index = 0
    while (index < text.length) {
      val character = text[index]
      when {
        inQuotes -> {
          index = consumeQuoted(text, index, field) { inQuotes = false }
        }

        character == QUOTE -> {
          inQuotes = true
        }

        character == SEPARATOR -> {
          currentRow.add(field.toString())
          field.clear()
        }

        character == LINE_FEED || character == CARRIAGE_RETURN -> {
          currentRow.add(field.toString())
          field.clear()
          rows.add(currentRow.toList())
          currentRow.clear()
          index = skipLineFeed(text, index, character)
        }

        else -> {
          field.append(character)
        }
      }
      index++
    }
    if (field.isNotEmpty() || currentRow.isNotEmpty()) {
      currentRow.add(field.toString())
      rows.add(currentRow.toList())
    }
    return rows
  }

  private fun consumeQuoted(
    text: String,
    index: Int,
    field: StringBuilder,
    onClose: () -> Unit,
  ): Int {
    var cursor = index
    if (text[cursor] == QUOTE) {
      val escaped = cursor + 1 < text.length && text[cursor + 1] == QUOTE
      if (escaped) {
        field.append(QUOTE)
        cursor++
      } else {
        onClose()
      }
    } else {
      field.append(text[cursor])
    }
    return cursor
  }

  private fun skipLineFeed(
    text: String,
    index: Int,
    character: Char,
  ): Int {
    val next = index + 1
    return if (character == CARRIAGE_RETURN && next < text.length && text[next] == LINE_FEED) next else index
  }

  private fun escape(value: String): String =
    if (value.any { it in SPECIAL_CHARACTERS }) {
      QUOTE + value.replace(QUOTE.toString(), "$QUOTE$QUOTE") + QUOTE
    } else {
      value
    }

  private const val CRLF = "\r\n"
}
