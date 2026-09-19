package az21.subscribe.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import az21.subscribe.MainActivity
import az21.subscribe.R
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.reminder.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Builds and posts the billing and trial reminder notifications. */
@Singleton
class ReminderNotifier
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
  ) {
    fun show(
      subscription: Subscription,
      type: ReminderType,
      targetDate: LocalDate,
    ) {
      val manager = NotificationManagerCompat.from(context)
      val permissionDenied =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
      if (permissionDenied || !manager.areNotificationsEnabled()) return

      val date =
        targetDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
      val title = context.getString(titleResource(type), subscription.name)
      val body =
        when (type) {
          ReminderType.BILLING -> context.getString(R.string.reminder_billing_body, subscription.name, date)
          ReminderType.TRIAL_ENDING -> context.getString(R.string.reminder_trial_body, subscription.name, date)
        }

      val notification =
        NotificationCompat
          .Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(title)
          .setContentText(body)
          .setStyle(NotificationCompat.BigTextStyle().bigText(body))
          .setAutoCancel(true)
          .setContentIntent(contentIntent(subscription.id))
          .build()

      manager.notify(notificationId(subscription.id, type), notification)
    }

    private fun titleResource(type: ReminderType): Int =
      when (type) {
        ReminderType.BILLING -> R.string.reminder_billing_title
        ReminderType.TRIAL_ENDING -> R.string.reminder_trial_title
      }

    private fun contentIntent(subscriptionId: UUID): PendingIntent {
      val intent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(ReminderDeepLink.EXTRA_SUBSCRIPTION_ID, subscriptionId.toString())
        }
      return PendingIntent.getActivity(
        context,
        subscriptionId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }

    private fun notificationId(
      subscriptionId: UUID,
      type: ReminderType,
    ): Int = subscriptionId.hashCode() * HASH_MULTIPLIER + type.ordinal

    companion object {
      const val CHANNEL_ID: String = "subscription_reminders"

      private const val HASH_MULTIPLIER = 31

      /** Registers the notification channel. Safe to call on every app start. */
      fun createChannel(context: Context) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
          ).apply { description = context.getString(R.string.reminder_channel_description) }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
      }
    }
  }
