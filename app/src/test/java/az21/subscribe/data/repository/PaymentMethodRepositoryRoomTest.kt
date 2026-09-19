package az21.subscribe.data.repository

import az21.subscribe.data.local.SubScribeDatabase
import az21.subscribe.data.local.inMemorySubScribeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Exercises [PaymentMethodRepositoryImpl] against a real in-memory Room database. */
@RunWith(RobolectricTestRunner::class)
class PaymentMethodRepositoryRoomTest {
  private lateinit var database: SubScribeDatabase
  private lateinit var repository: PaymentMethodRepositoryImpl

  @Before
  fun setUp() {
    database = inMemorySubScribeDatabase()
    repository = PaymentMethodRepositoryImpl(database.paymentMethodDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun createAndUpdate_persistThroughRealDatabase() =
    runTest {
      val created = repository.createPaymentMethod("Visa ...1234")

      assertEquals("Visa ...1234", repository.getPaymentMethod(created.id)?.label)

      repository.updatePaymentMethod(created.copy(label = "Mastercard ...5678"))

      assertEquals("Mastercard ...5678", repository.getPaymentMethod(created.id)?.label)
    }

  @Test
  fun delete_removesPaymentMethod() =
    runTest {
      val created = repository.createPaymentMethod("PayPal")

      repository.deletePaymentMethod(created.id)

      assertNull(repository.getPaymentMethod(created.id))
    }

  @Test
  fun observePaymentMethods_ordersByLabelCaseInsensitively() =
    runTest {
      repository.createPaymentMethod("visa")
      repository.createPaymentMethod("Amex")

      assertEquals(
        listOf("Amex", "visa"),
        repository.observePaymentMethods().first().map { it.label },
      )
    }
}
