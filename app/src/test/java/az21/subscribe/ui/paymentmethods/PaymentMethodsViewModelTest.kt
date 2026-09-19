package az21.subscribe.ui.paymentmethods

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.repository.PaymentMethodRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
  fun startCreate_exposesEmptyEditorAndSaveAddsPaymentMethod() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.startCreate()
        assertEquals("", awaitEditor().editor?.label)

        viewModel.onLabelChange("Visa ...1234")
        viewModel.save()

        val state =
          await { current ->
            current.editor == null && current.paymentMethods.singleOrNull()?.label == "Visa ...1234"
          }
        assertEquals("Visa ...1234", state.paymentMethods.single().label)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun startCreate_savesSelectedColor() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.startCreate()
        awaitEditor()

        viewModel.onLabelChange("Visa ...1234")
        viewModel.onColorChange(0xFF4FC3F7.toInt())
        viewModel.save()

        val state =
          await { current ->
            current.editor == null && current.paymentMethods.singleOrNull()?.label == "Visa ...1234"
          }
        assertEquals(0xFF4FC3F7.toInt(), state.paymentMethods.single().color)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun startEdit_prefillsColorAndSaveUpdates() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val created = repository.createPaymentMethod("PayPal", color = 0xFF81C784.toInt())

      viewModel.uiState.test {
        await { it.paymentMethods.any { method -> method.id == created.id } }

        viewModel.startEdit(created)
        assertEquals(0xFF81C784.toInt(), awaitEditor().editor?.color)

        viewModel.onColorChange(null)
        viewModel.save()

        val state = await { current -> current.editor == null }
        assertNull(state.paymentMethods.single().color)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_ignoresBlankLabel() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.startCreate()
        awaitEditor()
        viewModel.onLabelChange("   ")

        val editing = await { it.editor != null }
        assertEquals("   ", editing.editor?.label)

        viewModel.save()

        assertTrue(
          viewModel.uiState.value.paymentMethods
            .isEmpty(),
        )
        assertEquals(
          "   ",
          viewModel.uiState.value.editor
            ?.label,
        )
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun confirmDelete_removesPaymentMethodAfterPendingState() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val created = repository.createPaymentMethod("PayPal")

      viewModel.uiState.test {
        await { it.paymentMethods.any { method -> method.id == created.id } }

        viewModel.requestDelete(created)
        assertEquals(created, await { it.pendingDelete != null }.pendingDelete)

        viewModel.confirmDelete()

        await { it.pendingDelete == null && it.paymentMethods.isEmpty() }
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<PaymentMethodsUiState>.awaitLoaded(): PaymentMethodsUiState =
    await { !it.isLoading }

  private suspend fun ReceiveTurbine<PaymentMethodsUiState>.awaitEditor(): PaymentMethodsUiState =
    await { it.editor != null }

  private suspend fun ReceiveTurbine<PaymentMethodsUiState>.await(
    predicate: (PaymentMethodsUiState) -> Boolean,
  ): PaymentMethodsUiState {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }
}
