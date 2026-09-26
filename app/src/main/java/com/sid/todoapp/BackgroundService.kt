package com.sid.todoapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class BackgroundService : Service() {
	private lateinit var alarmManager: AlarmManager
	private lateinit var notificationManager: NotificationManager
	private lateinit var notificationBuilder: NotificationCompat.Builder
	private lateinit var notificationIntent: PendingIntent

	override fun onCreate() {
		super.onCreate()

		alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationIntent = PendingIntent.getActivity(
			this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
		)

		notificationManager.createNotificationChannel(
			NotificationChannel("todoapp", "TodoApp", NotificationManager.IMPORTANCE_HIGH)
		)
		notificationBuilder = NotificationCompat.Builder(this, "todoapp")
	}

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == "show_notification") showNotification()
		else scheduleNextTask()

		return START_NOT_STICKY
	}

	private fun scheduleNextTask() {
		val now = Calendar.getInstance().timeInMillis
		var minDelay = Long.MAX_VALUE

		for (todo in TodoStore.load(this)) {
			if (todo.done) continue

			val delay = todo.deadline - now
			if (delay in 1 until minDelay) minDelay = delay
		}
		if (minDelay == Long.MAX_VALUE) return

		val pendingIntent = PendingIntent.getService(
			this, 0, Intent(this, BackgroundService::class.java).apply {
				action = "show_notification"
			}, PendingIntent.FLAG_IMMUTABLE
		)
		val notificationTime = now + minDelay

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms())
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
		else alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
	}

	private fun showNotification() {
		if (ContextCompat.checkSelfPermission(
				this, android.Manifest.permission.POST_NOTIFICATIONS
			) != PackageManager.PERMISSION_GRANTED
		) return

		val notification = notificationBuilder.setContentTitle("Todo Deadline Reached")
			.setContentText("One of your todos has reached its deadline")
			.setSmallIcon(R.drawable.ic_time).setPriority(NotificationCompat.PRIORITY_HIGH)
			.setContentIntent(notificationIntent).build()

		notificationManager.notify(1, notification)
	}
}
