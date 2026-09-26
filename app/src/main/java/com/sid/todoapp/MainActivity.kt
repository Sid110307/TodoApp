package com.sid.todoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sid.todoapp.databinding.ActivityMainBinding
import com.sid.todoapp.databinding.EditTodoBinding
import com.sid.todoapp.databinding.ListItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
	private val todos = ArrayList<TodoStore.Todo>()
	private var deadlineValue: Long = 0

	private lateinit var binding: ActivityMainBinding
	private lateinit var adapter: TodoListAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
				.launch(android.Manifest.permission.POST_NOTIFICATIONS)

		todos.addAll(TodoStore.load(this))
		adapter = TodoListAdapter(todos, ::toggleTodo, ::deleteTodo, ::editTodo)
		binding.itemList.adapter = adapter

		startService(Intent(this, BackgroundService::class.java))

		binding.btnDeadline.setOnClickListener {
			MaterialDatePicker.Builder.datePicker().setCalendarConstraints(
				CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
			).build().apply {
				addOnPositiveButtonClickListener { deadline ->
					val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(
						Date(deadline)
					)
					Snackbar.make(
						binding.btnDeadline, "Deadline set to $date", Snackbar.LENGTH_LONG
					).setAction("Change") {
						show(supportFragmentManager, "DATE_PICKER")
					}.show()

					deadlineValue = deadline
				}
			}.show(supportFragmentManager, "DATE_PICKER")
		}

		binding.btnAdd.setOnClickListener {
			val text = binding.editTodo.text.toString().trim()
			if (text.isEmpty()) {
				binding.editTodo.error = "Please enter a todo item"
				return@setOnClickListener
			}

			if (deadlineValue == 0L) {
				Snackbar.make(binding.btnAdd, "Please set a deadline", Snackbar.LENGTH_LONG)
					.setAction("Set") { binding.btnDeadline.performClick() }.show()
				return@setOnClickListener
			}

			todos.add(TodoStore.Todo(text, deadlineValue))
			deadlineValue = 0

			TodoStore.save(this, todos)
			adapter.notifyItemInserted(todos.size - 1)
			binding.editTodo.text.clear()

			startService(Intent(this, BackgroundService::class.java))
		}
	}

	private fun toggleTodo(position: Int) {
		val todo = todos[position]
		todos[position] = todo.copy(done = !todo.done)

		TodoStore.save(this, todos)
		adapter.notifyItemChanged(position)
	}

	private fun deleteTodo(position: Int) {
		val removed = todos.removeAt(position)
		TodoStore.save(this, todos)
		adapter.notifyItemRemoved(position)

		Snackbar.make(binding.itemList, "Todo item deleted", Snackbar.LENGTH_LONG).setAction("Undo") {
			todos.add(position, removed)
			TodoStore.save(this, todos)
			adapter.notifyItemInserted(position)
		}.show()

		startService(Intent(this, BackgroundService::class.java))
	}

	private fun editTodo(position: Int) {
		val editBinding = EditTodoBinding.inflate(layoutInflater)
		editBinding.editTodo.setText(todos[position].text)

		MaterialAlertDialogBuilder(this).setTitle("Edit todo item").setView(editBinding.root)
			.setPositiveButton("OK") { _, _ ->
				val newText = editBinding.editTodo.text.toString().trim()
				if (newText.isEmpty()) return@setPositiveButton

				todos[position] = todos[position].copy(text = newText)
				TodoStore.save(this, todos)
				adapter.notifyItemChanged(position)
			}.setNegativeButton("Cancel", null).create().show()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.menu_main, menu)
		return super.onCreateOptionsMenu(menu)
	}

	@Suppress("DEPRECATION")
	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		val appVersion =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) packageManager.getPackageInfo(
				packageName, PackageManager.PackageInfoFlags.of(0)
			).versionName else packageManager.getPackageInfo(packageName, 0).versionName

		when (item.itemId) {
			R.id.infoMenu -> {
				MaterialAlertDialogBuilder(this).setTitle("App Info").setIcon(R.drawable.ic_info)
					.setMessage(getString(R.string.info, appVersion))
					.setPositiveButton("OK") { _, _ -> }.create().show()
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
		outState.putLong("deadlineValue", deadlineValue)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		deadlineValue = savedInstanceState.getLong("deadlineValue")
	}

	class DeadlineCheckReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == Intent.ACTION_BOOT_COMPLETED)
				context.startService(Intent(context, BackgroundService::class.java))
		}
	}

	class TodoListAdapter(
		private val todos: List<TodoStore.Todo>,
		private val onToggle: (Int) -> Unit,
		private val onDelete: (Int) -> Unit,
		private val onEdit: (Int) -> Unit,
	) : RecyclerView.Adapter<TodoListAdapter.TodoViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TodoViewHolder(
			LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
		)

		override fun onBindViewHolder(holder: TodoViewHolder, position: Int) =
			holder.bind(todos[position])

		override fun getItemCount(): Int = todos.size

		inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			private val binding = ListItemBinding.bind(itemView)

			fun bind(todo: TodoStore.Todo) {
				val date =
					SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(todo.deadline))

				binding.txtTodo.text = todo.text
				binding.txtDeadline.text = date
				binding.txtTodo.paintFlags = if (todo.done)
					binding.txtTodo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
				else binding.txtTodo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

				binding.checkDone.setOnCheckedChangeListener(null)
				binding.checkDone.isChecked = todo.done
				binding.checkDone.setOnCheckedChangeListener { _, _ -> onToggle(adapterPosition) }

				binding.btnDelete.setOnClickListener { onDelete(adapterPosition) }
				itemView.setOnLongClickListener { onEdit(adapterPosition); true }
			}
		}
	}
}
