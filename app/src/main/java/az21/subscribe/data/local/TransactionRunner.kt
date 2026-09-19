package az21.subscribe.data.local

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/** Runs a block atomically. Abstracted so repositories using it can be tested with a pass-through. */
interface TransactionRunner {
  suspend fun <T> runTransaction(block: suspend () -> T): T
}

@Singleton
class RoomTransactionRunner
  @Inject
  constructor(
    private val database: SubScribeDatabase,
  ) : TransactionRunner {
    override suspend fun <T> runTransaction(block: suspend () -> T): T = database.withTransaction(block)
  }
