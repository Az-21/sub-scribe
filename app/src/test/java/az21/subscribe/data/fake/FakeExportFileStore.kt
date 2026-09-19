package az21.subscribe.data.fake

import az21.subscribe.domain.repository.ExportFileStore
import java.io.IOException

/** In-memory [ExportFileStore] for JVM-only ViewModel tests. */
class FakeExportFileStore : ExportFileStore {
  val files = mutableMapOf<String, ByteArray>()
  var failRead = false
  var failWrite = false

  override suspend fun read(uri: String): ByteArray {
    if (failRead) throw IOException("read failed")
    return files[uri] ?: throw IOException("no file at $uri")
  }

  override suspend fun write(
    uri: String,
    bytes: ByteArray,
  ) {
    if (failWrite) throw IOException("write failed")
    files[uri] = bytes
  }
}
