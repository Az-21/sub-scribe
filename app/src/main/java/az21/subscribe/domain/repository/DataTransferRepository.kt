package az21.subscribe.domain.repository

import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ImportSummary

/**
 * Reads the whole database as one versioned snapshot and merges a snapshot back in.
 *
 * Import is an upsert keyed by id: records from the file overwrite records with the same id, and
 * records that exist only locally are left untouched. Tag assignments for imported subscriptions are
 * replaced with the file's assignments so a subscription's tags round-trip exactly.
 */
interface DataTransferRepository {
  suspend fun exportSnapshot(): ExportDocument

  suspend fun merge(document: ExportDocument): ImportSummary
}
