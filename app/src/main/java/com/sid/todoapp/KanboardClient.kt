package com.sid.todoapp

import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object KanboardClient {
	private val executor = Executors.newSingleThreadExecutor()
	private val mainThread = Handler(Looper.getMainLooper())

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

	private fun findProjectId(config: KanboardConfig.Config, onResult: (Int?) -> Unit) {
		call(config, "getProjectByName", JSONObject().put("name", config.project)) { response ->
			val id = response?.optJSONObject("result")?.optInt("id", 0) ?: 0
			onResult(id.takeIf { it != 0 })
		}
	}

	fun createTask(config: KanboardConfig.Config, title: String, onResult: (Int?) -> Unit) {
		findProjectId(config) { projectId ->
			if (projectId == null) {
				onResult(null)
				return@findProjectId
			}

			val params = JSONObject().put("title", title).put("project_id", projectId)
			call(config, "createTask", params) { response ->
				val id = response?.optInt("result", 0) ?: 0
				onResult(id.takeIf { it != 0 })
			}
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
