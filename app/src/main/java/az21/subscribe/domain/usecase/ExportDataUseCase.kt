package az21.subscribe.domain.usecase

import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.ExportedFile
import az21.subscribe.domain.repository.DataTransferRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Serializes the whole database into a versioned file in the requested [ExportFormat]. */
class ExportDataUseCase
  @Inject
  constructor(
    private val dataTransferRepository: DataTransferRepository,
    private val codecs: Set<@JvmSuppressWildcards ExportCodec>,
    private val clock: Clock,
  ) {
    suspend operator fun invoke(format: ExportFormat): ExportedFile {
      val document = dataTransferRepository.exportSnapshot()
      val codec = codecs.first { it.format == format }
      return ExportedFile(
        fileName = "sub-scribe-export-${LocalDate.now(clock)}.${format.fileExtension}",
        mimeType = format.mimeType,
        bytes = codec.encode(document),
      )
    }
  }
