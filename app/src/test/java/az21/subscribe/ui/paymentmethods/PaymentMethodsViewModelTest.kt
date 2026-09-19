package az21.subscribe.ui.paymentmethods

import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.repository.PaymentMethodRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val repository = PaymentMethodRepositoryImpl(FakePaymentMethodDao())

  private fun createViewModel() = PaymentMethodsViewModel(repository)

  @Test
  fun createPaymentMethod_addsItToList() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.startCreate()
      viewModel.onLabelChange("Visa ...1234")

      viewModel.save()
      advanceUntilIdle()

      assertEquals(
        "Visa ...1234",
        repository
          .observePaymentMethods()
          .first()
          .single()
          .label,
      )
    }

  @Test
  fun save_ignoresBlankLabel() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.startCreate()
      viewModel.onLabelChange("   ")

      viewModel.save()
      advanceUntilIdle()

      assertTrue(repository.observePaymentMethods().first().isEmpty())
    }

  @Test
  fun confirmDelete_removesPaymentMethod() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val created = repository.createPaymentMethod("PayPal")

      viewModel.requestDelete(created)
      viewModel.confirmDelete()
      advanceUntilIdle()

      assertTrue(repository.observePaymentMethods().first().isEmpty())
    }
}
