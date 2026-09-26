package com.sid.todoapp

import android.content.Context

object KanboardConfig {
	data class Config(val url: String, val token: String, val project: String)

	fun load(context: Context): Config? {
		val prefs = context.getSharedPreferences("kanboard", Context.MODE_PRIVATE)

		val url = prefs.getString("url", null)
		val token = prefs.getString("token", null)
		val project = prefs.getString("project", null)
		if (url.isNullOrBlank() || token.isNullOrBlank() || project.isNullOrBlank()) return null

		return Config(url, token, project)
	}

	fun save(context: Context, url: String, token: String, project: String) {
		context.getSharedPreferences("kanboard", Context.MODE_PRIVATE).edit()
			.putString("url", url).putString("token", token).putString("project", project).apply()
	}
}
