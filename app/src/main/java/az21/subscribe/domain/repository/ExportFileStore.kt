package az21.subscribe.domain.repository

/**
 * Reads and writes raw bytes at a user-chosen document location. The location is an opaque string
 * (a Storage Access Framework URI) so this contract stays free of Android types.
 */
interface ExportFileStore {
  suspend fun read(uri: String): ByteArray

  suspend fun write(
    uri: String,
    bytes: ByteArray,
  )
}
