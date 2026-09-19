package az21.subscribe.domain.export

/** The file formats an export can be written in. */
enum class ExportFormat(
  val mimeType: String,
  val fileExtension: String,
) {
  /** Full-fidelity, versioned document. */
  JSON("application/json", "json"),

  /** Human-readable zip of per-entity CSV tables. */
  CSV("application/zip", "csv.zip"),
}

/** An encoded export ready to be written to a user-chosen file. */
class ExportedFile(
  val fileName: String,
  val mimeType: String,
  val bytes: ByteArray,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ExportedFile) return false
    return fileName == other.fileName && mimeType == other.mimeType && bytes.contentEquals(other.bytes)
  }

  override fun hashCode(): Int {
    var result = fileName.hashCode()
    result = 31 * result + mimeType.hashCode()
    result = 31 * result + bytes.contentHashCode()
    return result
  }
}

/** Encodes and decodes one export format. Implementations are stateless and safe to share. */
interface ExportCodec {
  val format: ExportFormat

  fun encode(document: ExportDocument): ByteArray

  /** Whether [bytes] look like this codec's format, used to auto-detect an import file. */
  fun canDecode(bytes: ByteArray): Boolean

  fun decode(bytes: ByteArray): ImportParseResult
}
