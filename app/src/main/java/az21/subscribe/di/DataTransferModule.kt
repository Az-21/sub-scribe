package az21.subscribe.di

import az21.subscribe.data.export.CsvExportCodec
import az21.subscribe.data.export.JsonExportCodec
import az21.subscribe.data.local.RoomTransactionRunner
import az21.subscribe.data.local.TransactionRunner
import az21.subscribe.data.repository.DataTransferRepositoryImpl
import az21.subscribe.data.transfer.ContentResolverExportFileStore
import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.repository.DataTransferRepository
import az21.subscribe.domain.repository.ExportFileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataTransferModule {
  @Binds
  @Singleton
  abstract fun bindDataTransferRepository(impl: DataTransferRepositoryImpl): DataTransferRepository

  @Binds
  @Singleton
  abstract fun bindExportFileStore(impl: ContentResolverExportFileStore): ExportFileStore

  @Binds
  @Singleton
  abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner

  @Binds
  @IntoSet
  abstract fun bindJsonExportCodec(impl: JsonExportCodec): ExportCodec

  @Binds
  @IntoSet
  abstract fun bindCsvExportCodec(impl: CsvExportCodec): ExportCodec
}
