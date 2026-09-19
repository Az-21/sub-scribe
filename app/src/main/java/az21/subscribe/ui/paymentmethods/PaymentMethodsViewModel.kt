package az21.subscribe.ui.paymentmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.repository.PaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives payment method management: list, create, edit and delete. */
@HiltViewModel
class PaymentMethodsViewModel
  @Inject
  constructor(
    private val paymentMethodRepository: PaymentMethodRepository,
  ) : ViewModel() {
    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<PaymentMethodsUiState> =
      combine(paymentMethodRepository.observePaymentMethods(), dialogs) { methods, dialogState ->
        PaymentMethodsUiState(
          isLoading = false,
          paymentMethods = methods,
          editor = dialogState.editor,
          pendingDelete = dialogState.pendingDelete,
        )
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        PaymentMethodsUiState(),
      )

    fun startCreate() {
      dialogs.value = dialogs.value.copy(editor = PaymentMethodEditorState())
    }

    fun startEdit(paymentMethod: PaymentMethod) {
      dialogs.value =
        dialogs.value.copy(
          editor =
            PaymentMethodEditorState(
              id = paymentMethod.id,
              label = paymentMethod.label,
              color = paymentMethod.color,
            ),
        )
    }

    fun dismissEditor() {
      dialogs.value = dialogs.value.copy(editor = null)
    }

    fun onLabelChange(label: String) {
      dialogs.value = dialogs.value.copy(editor = dialogs.value.editor?.copy(label = label))
    }

    fun onColorChange(color: Int?) {
      dialogs.value = dialogs.value.copy(editor = dialogs.value.editor?.copy(color = color))
    }

    fun save() {
      val editor = dialogs.value.editor ?: return
      val label = editor.label.trim()
      if (label.isEmpty()) return
      viewModelScope.launch {
        val id = editor.id
        if (id == null) {
          paymentMethodRepository.createPaymentMethod(label, editor.color)
        } else {
          paymentMethodRepository.updatePaymentMethod(PaymentMethod(id = id, label = label, color = editor.color))
        }
        dialogs.value = dialogs.value.copy(editor = null)
      }
    }

    fun requestDelete(paymentMethod: PaymentMethod) {
      dialogs.value = dialogs.value.copy(pendingDelete = paymentMethod)
    }

    fun cancelDelete() {
      dialogs.value = dialogs.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
      val paymentMethod = dialogs.value.pendingDelete ?: return
      dialogs.value = dialogs.value.copy(pendingDelete = null)
      viewModelScope.launch { paymentMethodRepository.deletePaymentMethod(paymentMethod.id) }
    }

    private data class Dialogs(
      val editor: PaymentMethodEditorState? = null,
      val pendingDelete: PaymentMethod? = null,
    )

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
