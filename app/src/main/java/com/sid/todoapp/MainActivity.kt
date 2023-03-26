package com.sid.todoapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sid.todoapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
	private val todoItems = ArrayList<String>()
	private val deadlineItems = ArrayList<Long>()

	private var deadlineValue: Long = 0
	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		val prefs = getSharedPreferences("todos", MODE_PRIVATE)
		startService(Intent(this, BackgroundService::class.java))

		val todoList = ArrayList<Pair<ArrayList<String>, ArrayList<Long>>>()
		val adapter = TodoListAdapter(todoList)
		binding.itemList.adapter = adapter

		prefs.getStringSet("todoList", setOf())?.toCollection(todoItems)
		prefs.getStringSet("deadlineList", setOf())?.map { it.toLong() }
			?.toCollection(deadlineItems)
		todoList.add(Pair(todoItems, deadlineItems))

		binding.btnDeadline.setOnClickListener {
			MaterialDatePicker.Builder.datePicker().build().apply {
				addOnPositiveButtonClickListener { deadline ->
					val date = SimpleDateFormat(
						"MMMM dd, yyyy", Locale.getDefault()
					).format(Date(deadline))
					Snackbar.make(
						binding.btnDeadline, "Deadline set to $date", Snackbar.LENGTH_LONG
					).setAction("Change") { show(supportFragmentManager, "DATE_PICKER") }.show()

					deadlineValue = deadline
				}
			}.show(supportFragmentManager, "DATE_PICKER")
		}

		binding.btnAdd.setOnClickListener {
			val todoItem = binding.editTodo.text.toString().trim()
			if (todoItem.isEmpty()) {
				binding.editTodo.error = "Please enter a todo item"
				return@setOnClickListener
			}

			if (deadlineValue == 0L) {
				Snackbar.make(binding.btnAdd, "Please set a deadline", Snackbar.LENGTH_LONG)
					.setAction("Set") { binding.btnDeadline.performClick() }.show()
				return@setOnClickListener
			}

			todoList.add(Pair(todoItems, deadlineItems))
			prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
			prefs.edit().putStringSet("deadlineList", deadlineItems.map { it.toString() }.toSet())
				.apply()

			adapter.notifyItemInserted(todoItems.size - 1)
			binding.editTodo.text.clear()
		}

		binding.itemList.findViewById<Button>(R.id.btnDelete).setOnClickListener { view ->
			val position = binding.itemList.getChildAdapterPosition(view.parent as View)

			todoItems.removeAt(position)
			deadlineItems.removeAt(position)

			prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
			prefs.edit().putStringSet("deadlineList", deadlineItems.map { it.toString() }.toSet())
				.apply()

			Snackbar.make(view, "Todo item deleted", Snackbar.LENGTH_LONG).setAction("Undo") {
				todoItems.add(position, todoItems[position])
				deadlineItems.add(position, deadlineItems[position])

				prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
				prefs.edit()
					.putStringSet("deadlineList", deadlineItems.map { it.toString() }.toSet())
					.apply()

				adapter.notifyItemInserted(position)
			}.show()

			adapter.notifyItemRemoved(position)
		}

		(getSystemService(ALARM_SERVICE) as AlarmManager).setRepeating(
			AlarmManager.RTC_WAKEUP,
			System.currentTimeMillis(),
			AlarmManager.INTERVAL_HALF_DAY,
			PendingIntent.getBroadcast(
				this,
				0,
				Intent(this, DeadlineCheckReceiver::class.java),
				PendingIntent.FLAG_UPDATE_CURRENT
			)
		)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.menu_main, menu)

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.infoMenu -> {
				MaterialAlertDialogBuilder(this).setTitle("App Info").setIcon(R.drawable.ic_info)
					.setMessage(
						"TodoApp\nVersion ${BuildConfig.VERSION_NAME}\n\nThis application manages notes and todos with deadlines and notifies the user when the deadline is reached."
					).setPositiveButton("OK") { _, _ -> }.create().show()
			}
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onDestroy() {
		super.onDestroy()
		stopService(Intent(this, BackgroundService::class.java))
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		outState.putStringArrayList("todoItems", todoItems)
		outState.putStringArrayList("deadlineItems",
			deadlineItems.map { it.toString() } as ArrayList<String>)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)

		todoItems.clear()
		deadlineItems.clear()

		todoItems.addAll(savedInstanceState.getStringArrayList("todoItems")!!)
		deadlineItems.addAll(savedInstanceState.getStringArrayList("deadlineItems")!!
			.map { it.toLong() })
	}

	class DeadlineCheckReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
				val prefs = context.getSharedPreferences("todos", MODE_PRIVATE)

				val todoItems = prefs.getStringSet("todoList", setOf())?.toCollection(ArrayList())
				val deadlineItems = prefs.getStringSet("deadlineList", setOf())?.map { it.toLong() }
					?.toCollection(ArrayList())

				if (todoItems == null || deadlineItems == null) return

				val overdueItems = ArrayList<String>()
				val overdueItemPositions = ArrayList<Int>()

				for (i in deadlineItems.indices) {
					if (deadlineItems[i] < System.currentTimeMillis()) {
						overdueItems.add(todoItems[i])
						overdueItemPositions.add(i)
					}
				}

				if (overdueItems.isNotEmpty()) {
					val overdueItemsString = overdueItems.joinToString("\n")

					MaterialAlertDialogBuilder(context).setTitle("Overdue Items")
						.setMessage(overdueItemsString).setPositiveButton("OK") { _, _ ->
							for (i in overdueItemPositions.indices) {
								todoItems.removeAt(overdueItemPositions[i])
								deadlineItems.removeAt(overdueItemPositions[i])
							}

							prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
							prefs.edit().putStringSet(
								"deadlineList", deadlineItems.map { it.toString() }.toSet()
							).apply()
						}.create().show()
				}
			}
		}
	}

	class TodoListAdapter(private val todoList: ArrayList<Pair<ArrayList<String>, ArrayList<Long>>>) :
		RecyclerView.Adapter<TodoListAdapter.TodoViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
			val view =
				LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
			return TodoViewHolder(view)
		}

		override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
			val todoItem = todoList[position].first[position]
			val deadlineItem = todoList[position].second[position]

			holder.todoItem.text = todoItem
			holder.deadlineItem.text = deadlineItem.toString()
		}

		override fun getItemCount(): Int = todoList.size

		inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			val todoItem: TextView = itemView.findViewById(R.id.txtTodo)
			val deadlineItem: TextView = itemView.findViewById(R.id.txtDeadline)
		}
	}
}
