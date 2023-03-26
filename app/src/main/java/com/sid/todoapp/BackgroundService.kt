package com.sid.todoapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Calendar

class BackgroundService : Service() {
	private val todoItems = ArrayList<String>()
	private val deadlineItems = ArrayList<Long>()

	private lateinit var alarmManager: AlarmManager
	private lateinit var notificationManager: NotificationManager
	private lateinit var notificationChannel: NotificationChannel
	private lateinit var notificationBuilder: NotificationCompat.Builder
	private lateinit var notificationIntent: PendingIntent

	override fun onCreate() {
		super.onCreate()

		alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationIntent = PendingIntent.getActivity(
			this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
		)

		notificationChannel =
			NotificationChannel("todoapp", "TodoApp", NotificationManager.IMPORTANCE_HIGH)
		notificationManager.createNotificationChannel(notificationChannel)

		todoItems.addAll(
			getSharedPreferences("todos", MODE_PRIVATE).getStringSet("todoList", setOf())
				?: emptySet()
		)
		deadlineItems.addAll(getSharedPreferences(
			"todos", MODE_PRIVATE
		).getStringSet("deadlineList", setOf())?.map { it.toLong() } ?: emptyList())

		scheduleNextTask()
	}

	override fun onBind(intent: Intent?): IBinder? = null

	private fun scheduleNextTask() {
		val now = Calendar.getInstance().timeInMillis
		var minDelay = Long.MAX_VALUE

		for (deadline in deadlineItems) {
			val delay = deadline - now
			if (delay in 1 until minDelay) minDelay = delay
		}

		if (minDelay < Long.MAX_VALUE) {
			val notificationTime = now + minDelay
			alarmManager.setExact(
				AlarmManager.RTC_WAKEUP, notificationTime, PendingIntent.getService(
					this, 0, Intent(this, BackgroundService::class.java).apply {
						action = "show_notification"
					}, PendingIntent.FLAG_UPDATE_CURRENT
				)
			)
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == "show_notification") showNotification()
		return START_NOT_STICKY
	}

	private fun showNotification() {
		val notification = notificationBuilder.setContentTitle("Todo Deadline Reached")
			.setContentText("One of your todos has reached its deadline")
			.setSmallIcon(R.drawable.ic_time).setPriority(NotificationCompat.PRIORITY_HIGH)
			.setContentIntent(notificationIntent).build()

		notificationManager.notify(1, notification)
	}
}
