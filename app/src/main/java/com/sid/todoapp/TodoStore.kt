package com.sid.todoapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TodoStore {
	data class Todo(val text: String, val deadline: Long, val done: Boolean = false)

	fun load(context: Context): ArrayList<Todo> {
		val json = context.getSharedPreferences("todos", Context.MODE_PRIVATE)
			.getString("todos", null) ?: return ArrayList()

		val todos = ArrayList<Todo>()
		val array = JSONArray(json)
		for (i in 0 until array.length()) {
			val obj = array.getJSONObject(i)
			todos.add(Todo(obj.getString("text"), obj.getLong("deadline"), obj.optBoolean("done")))
		}

		return todos
	}

	fun save(context: Context, todos: List<Todo>) {
		val array = JSONArray()
		for (todo in todos) array.put(
			JSONObject().put("text", todo.text).put("deadline", todo.deadline).put("done", todo.done)
		)

		context.getSharedPreferences("todos", Context.MODE_PRIVATE).edit()
			.putString("todos", array.toString()).apply()
	}
}
