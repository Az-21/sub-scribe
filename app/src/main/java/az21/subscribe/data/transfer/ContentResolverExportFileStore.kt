package az21.subscribe.data.transfer

import android.content.Context
import android.net.Uri
import az21.subscribe.di.IoDispatcher
import az21.subscribe.domain.repository.ExportFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** [ExportFileStore] backed by the Storage Access Framework through the content resolver. */
@Singleton
class ContentResolverExportFileStore
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  ) : ExportFileStore {
    override suspend fun read(uri: String): ByteArray =
      withContext(ioDispatcher) {
        val stream = context.contentResolver.openInputStream(Uri.parse(uri))
        stream?.use { input -> input.readBytes() } ?: throw IOException("Unable to read $uri")
      }

    override suspend fun write(
      uri: String,
      bytes: ByteArray,
    ) {
      withContext(ioDispatcher) {
        val stream = context.contentResolver.openOutputStream(Uri.parse(uri), "wt")
        stream?.use { output -> output.write(bytes) } ?: throw IOException("Unable to write $uri")
      }
    }
  }
