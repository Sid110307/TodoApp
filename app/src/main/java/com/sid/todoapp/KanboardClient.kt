package com.sid.todoapp

import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object KanboardClient {
	private val executor = Executors.newSingleThreadExecutor()
	private val mainThread = Handler(Looper.getMainLooper())
	private val dueDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

	private fun call(
		config: KanboardConfig.Config,
		method: String,
		params: JSONObject,
		onResult: (JSONObject?) -> Unit,
	) {
		executor.execute {
			val result = try {
				val connection =
					(URL("${config.url.trimEnd('/')}/jsonrpc.php").openConnection() as HttpURLConnection).apply {
						requestMethod = "POST"
						doOutput = true
						setRequestProperty("Content-Type", "application/json")
						setRequestProperty(
							"Authorization", "Basic " + Base64.encodeToString(
								"jsonrpc:${config.token}".toByteArray(), Base64.NO_WRAP
							)
						)
					}

				val body = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method)
					.put("params", params)
				connection.outputStream.use { it.write(body.toString().toByteArray()) }

				JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
			} catch (e: Exception) {
				null
			}

			mainThread.post { onResult(result) }
		}
	}

	fun resolveProjectId(config: KanboardConfig.Config, onResult: (Int?) -> Unit) {
		call(config, "getProjectByName", JSONObject().put("name", config.project)) { response ->
			val id = response?.optJSONObject("result")?.optInt("id", 0) ?: 0
			onResult(id.takeIf { it != 0 })
		}
	}

	fun getAllTasks(
		config: KanboardConfig.Config,
		projectId: Int,
		onResult: (List<JSONObject>) -> Unit,
	) {
		val tasks = ArrayList<JSONObject>()
		call(
			config, "getAllTasks", JSONObject().put("project_id", projectId).put("status_id", 1)
		) { openResponse ->
			(openResponse?.opt("result") as? JSONArray)?.let {
				for (i in 0 until it.length()) tasks.add(it.getJSONObject(i))
			}

			call(
				config, "getAllTasks", JSONObject().put("project_id", projectId).put("status_id", 0)
			) { closedResponse ->
				(closedResponse?.opt("result") as? JSONArray)?.let {
					for (i in 0 until it.length()) tasks.add(it.getJSONObject(i))
				}

				onResult(tasks)
			}
		}
	}

	fun createTask(
		config: KanboardConfig.Config,
		projectId: Int,
		title: String,
		dueDate: Long,
		onResult: (Int?) -> Unit,
	) {
		val params = JSONObject().put("title", title).put("project_id", projectId)
		if (dueDate > 0) params.put("date_due", dueDateFormat.format(Date(dueDate)))

		call(config, "createTask", params) { response ->
			val id = response?.optInt("result", 0) ?: 0
			onResult(id.takeIf { it != 0 })
		}
	}

	fun closeTask(config: KanboardConfig.Config, taskId: Int, onResult: (Boolean) -> Unit) {
		call(config, "closeTask", JSONObject().put("task_id", taskId)) { response ->
			onResult(response?.optBoolean("result", false) ?: false)
		}
	}

	fun openTask(config: KanboardConfig.Config, taskId: Int, onResult: (Boolean) -> Unit) {
		call(config, "openTask", JSONObject().put("task_id", taskId)) { response ->
			onResult(response?.optBoolean("result", false) ?: false)
		}
	}

	fun updateTaskTitle(
		config: KanboardConfig.Config,
		taskId: Int,
		title: String,
		onResult: (Boolean) -> Unit,
	) {
		val params = JSONObject().put("id", taskId).put("title", title)
		call(config, "updateTask", params) { response ->
			onResult(response?.optBoolean("result", false) ?: false)
		}
	}

	fun removeTask(config: KanboardConfig.Config, taskId: Int, onResult: (Boolean) -> Unit) {
		call(config, "removeTask", JSONObject().put("task_id", taskId)) { response ->
			onResult(response?.optBoolean("result", false) ?: false)
		}
	}
}
