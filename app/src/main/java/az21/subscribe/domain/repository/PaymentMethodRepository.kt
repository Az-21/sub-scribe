package az21.subscribe.domain.repository

import az21.subscribe.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** CRUD for payment methods. Deleting one detaches it from any subscription that used it. */
interface PaymentMethodRepository {
  fun observePaymentMethods(): Flow<List<PaymentMethod>>

  suspend fun getPaymentMethod(id: UUID): PaymentMethod?

  suspend fun createPaymentMethod(label: String): PaymentMethod

  suspend fun updatePaymentMethod(paymentMethod: PaymentMethod)

  suspend fun deletePaymentMethod(id: UUID)
}
