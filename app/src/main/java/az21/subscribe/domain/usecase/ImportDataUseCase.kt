package az21.subscribe.domain.usecase

import az21.subscribe.domain.export.EXPORT_SCHEMA_VERSION
import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ExportValidator
import az21.subscribe.domain.export.ImportIssue
import az21.subscribe.domain.export.ImportIssueReason
import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.ImportSummary
import az21.subscribe.domain.export.toImportSummary
import az21.subscribe.domain.repository.DataTransferRepository
import javax.inject.Inject

/**
 * Reads an exported file and merges it into the database.
 *
 * [parse] does no writes: it detects the format, validates the schema version, repairs dangling
 * references, and reports exactly what a subsequent [commit] would change. [commit] performs the
 * merge by id inside a single transaction.
 */
class ImportDataUseCase
  @Inject
  constructor(
    private val dataTransferRepository: DataTransferRepository,
    private val codecs: Set<@JvmSuppressWildcards ExportCodec>,
  ) {
    fun parse(bytes: ByteArray): ImportParseResult {
      val codec = codecs.firstOrNull { it.canDecode(bytes) }
      return if (codec == null) {
        failure(ImportIssueReason.MALFORMED_FILE, "")
      } else {
        validateDecoded(codec.decode(bytes))
      }
    }

    private fun validateDecoded(decoded: ImportParseResult): ImportParseResult =
      when {
        decoded !is ImportParseResult.Success -> {
          decoded
        }

        decoded.document.schemaVersion != EXPORT_SCHEMA_VERSION -> {
          failure(ImportIssueReason.UNSUPPORTED_VERSION, decoded.document.schemaVersion.toString())
        }

        else -> {
          val validated = ExportValidator.validate(decoded.document)
          ImportParseResult.Success(
            document = validated.document,
            summary = validated.document.toImportSummary(),
            issues = decoded.issues + validated.issues,
          )
        }
      }

    suspend fun commit(document: ExportDocument): ImportSummary = dataTransferRepository.merge(document)

    private fun failure(
      reason: ImportIssueReason,
      location: String,
    ): ImportParseResult.Failure = ImportParseResult.Failure(listOf(ImportIssue(reason, location)))
  }
