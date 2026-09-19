package az21.subscribe.data.fake

import az21.subscribe.data.local.TransactionRunner

/** A [TransactionRunner] that just runs the block, since JVM fakes need no real transaction. */
class FakeTransactionRunner : TransactionRunner {
  override suspend fun <T> runTransaction(block: suspend () -> T): T = block()
}
