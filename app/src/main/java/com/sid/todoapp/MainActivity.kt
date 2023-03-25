package com.sid.todoapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
	val todoItems = ArrayList<String>()
	val deadlineItems = ArrayList<Long>()

	var deadlineValue: Long = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val prefs = getSharedPreferences("todos", MODE_PRIVATE)

		val editTodo = findViewById<EditText>(R.id.editTodo)
		val itemList = findViewById<ListView>(R.id.itemList)
		val btnAdd = findViewById<Button>(R.id.btnAdd)
		val btnDeadline = findViewById<Button>(R.id.btnDeadline)

		val todoList = ArrayList<Pair<ArrayList<String>, ArrayList<Long>>>()
		val adapter = ArrayAdapter(this, R.layout.list_item, todoList)
		itemList.adapter = adapter

		prefs.getStringSet("todoList", setOf())?.toCollection(todoItems)
		prefs.getStringSet("deadlineList", setOf())?.map { it.toLong() }
			?.toCollection(deadlineItems)
		todoList.add(Pair(todoItems, deadlineItems))

		btnDeadline.setOnClickListener {
			MaterialDatePicker.Builder.datePicker().build().apply {
				addOnPositiveButtonClickListener { deadline ->
					val date = SimpleDateFormat(
						"MMMM dd, yyyy",
						Locale.getDefault()
					).format(Date(deadline))
					Snackbar.make(btnDeadline, "Deadline set to $date", Snackbar.LENGTH_LONG)
						.setAction("Change") { show(supportFragmentManager, "DATE_PICKER") }.show()

					deadlineValue = deadline
				}
			}.show(supportFragmentManager, "DATE_PICKER")
		}

		btnAdd.setOnClickListener {
			val todoItem = editTodo.text.toString().trim()
			if (todoItem.isEmpty()) {
				editTodo.error = "Please enter a todo item"
				return@setOnClickListener
			}

			if (deadlineValue == 0L) {
				Snackbar.make(btnAdd, "Please set a deadline", Snackbar.LENGTH_LONG)
					.setAction("Set") { btnDeadline.performClick() }.show()
				return@setOnClickListener
			}

			todoList.add(Pair(todoItems, deadlineItems))
			prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
			prefs.edit().putStringSet("deadlineList", deadlineItems.map { it.toString() }.toSet())
				.apply()

			adapter.notifyDataSetChanged()
			editTodo.text.clear()
		}

		itemList.findViewById<Button>(R.id.btnDelete).setOnClickListener {
			val position = itemList.getPositionForView(it)
			Snackbar.make(it, "Todo item \"${todoItems[position]}\" deleted", Snackbar.LENGTH_LONG)
				.setAction("Undo") {
					todoItems.add(position, todoItems[position])
					deadlineItems.add(position, deadlineItems[position])

					prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
					prefs.edit()
						.putStringSet("deadlineList", deadlineItems.map { it.toString() }.toSet())
						.apply()

					adapter.notifyDataSetChanged()
				}.show()

			todoItems.removeAt(position)
			deadlineItems.removeAt(position)

			prefs.edit().putStringSet("todoList", todoItems.toSet()).apply()
			prefs.edit().putStringSet("deadlineList", deadlineItems.map { it.toString() }.toSet())
				.apply()
			adapter.notifyDataSetChanged()
		}

		// TODO: Notify user when deadline is reached (and before too)
	}
}
