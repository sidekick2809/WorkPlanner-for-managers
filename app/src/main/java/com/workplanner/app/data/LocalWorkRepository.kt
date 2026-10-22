package com.workplanner.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Small, dependency-free persistence layer. The whole board is a few kilobytes,
 * so a single JSON document in SharedPreferences is plenty and keeps the app
 * completely offline.
 */
class LocalWorkRepository(context: Context) : WorkRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readFromDisk())
    override val state: StateFlow<WorkSnapshot> = _state.asStateFlow()

    private val writeLock = Any()

    override fun snapshot(): WorkSnapshot = _state.value

    override fun task(taskId: String): Task? = _state.value.task(taskId)

    override fun addPerson(name: String): Person {
        val trimmed = name.trim()
        val person = Person(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            colorIndex = _state.value.people.size
        )
        mutate { current ->
            current.copy(people = current.people + person)
        }
        return person
    }

    override fun renamePerson(personId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        mutate { current ->
            current.copy(
                people = current.people.map {
                    if (it.id == personId) it.copy(name = trimmed) else it
                }
            )
        }
    }

    override fun deletePerson(personId: String) {
        mutate { current ->
            current.copy(
                people = current.people.filterNot { it.id == personId },
                tasks = current.tasks.filterNot { it.personId == personId }
            )
        }
    }

    override fun setPersonPhoto(personId: String, photoPath: String?) {
        mutate { current ->
            current.copy(
                people = current.people.map {
                    if (it.id == personId) it.copy(photoPath = photoPath) else it
                }
            )
        }
    }

    override fun addTask(
        personId: String,
        title: String,
        summary: String,
        reminderAt: Long?
    ): Task {
        val task = Task(
            id = UUID.randomUUID().toString(),
            personId = personId,
            title = title.trim(),
            summary = summary.trim(),
            reminderAt = reminderAt
        )
        mutate { current ->
            current.copy(tasks = current.tasks + task)
        }
        return task
    }

    override fun updateTask(task: Task) {
        mutate { current ->
            val previous = current.task(task.id)
            val normalized = task.copy(
                title = task.title.trim(),
                summary = task.summary.trim(),
                progress = task.progress.coerceIn(0, 100),
                completedAt = when {
                    !task.isDone -> null
                    previous?.isDone == true -> previous.completedAt ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                }
            )
            current.copy(tasks = current.tasks.map { if (it.id == task.id) normalized else it })
        }
    }

    override fun setTaskDone(taskId: String, done: Boolean) {
        mutate { current ->
            current.copy(
                tasks = current.tasks.map { task ->
                    if (task.id != taskId) {
                        task
                    } else if (done) {
                        task.copy(
                            isDone = true,
                            progress = 100,
                            completedAt = System.currentTimeMillis()
                        )
                    } else {
                        task.copy(isDone = false, progress = 0, completedAt = null)
                    }
                }
            )
        }
    }

    override fun deleteTask(taskId: String) {
        mutate { current ->
            current.copy(tasks = current.tasks.filterNot { it.id == taskId })
        }
    }

    override fun restoreTask(task: Task) {
        mutate { current ->
            if (current.task(task.id) != null || current.person(task.personId) == null) {
                current
            } else {
                current.copy(tasks = current.tasks + task)
            }
        }
    }

    override fun exportJson(): String = synchronized(writeLock) {
        encode(_state.value).toString(2)
    }

    override fun replaceAll(raw: String): Boolean {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (!root.has("people") || !root.has("tasks")) return false
        val decoded = runCatching { decode(root) }.getOrNull() ?: return false
        if (decoded.people.map { it.id }.toSet().size != decoded.people.size) return false
        if (decoded.tasks.map { it.id }.toSet().size != decoded.tasks.size) return false

        synchronized(writeLock) {
            _state.value = decoded
            prefs.edit().putString(KEY_DOCUMENT, encode(decoded).toString()).apply()
        }
        return true
    }

    private inline fun mutate(transform: (WorkSnapshot) -> WorkSnapshot) {
        synchronized(writeLock) {
            val updated = transform(_state.value)
            _state.value = updated
            prefs.edit().putString(KEY_DOCUMENT, encode(updated).toString()).apply()
        }
    }

    private fun readFromDisk(): WorkSnapshot {
        val raw = prefs.getString(KEY_DOCUMENT, null) ?: return WorkSnapshot()
        return runCatching { decode(JSONObject(raw)) }.getOrElse { WorkSnapshot() }
    }

    private fun encode(snapshot: WorkSnapshot): JSONObject {
        val people = JSONArray()
        snapshot.people.forEach { person ->
            people.put(
                JSONObject()
                    .put("id", person.id)
                    .put("name", person.name)
                    .put("colorIndex", person.colorIndex)
                    .put("createdAt", person.createdAt)
                    .put("photoPath", person.photoPath ?: JSONObject.NULL)
            )
        }

        val tasks = JSONArray()
        snapshot.tasks.forEach { task ->
            tasks.put(
                JSONObject()
                    .put("id", task.id)
                    .put("personId", task.personId)
                    .put("title", task.title)
                    .put("summary", task.summary)
                    .put("notes", task.notes)
                    .put("progress", task.progress)
                    .put("isDone", task.isDone)
                    .put("reminderAt", task.reminderAt ?: JSONObject.NULL)
                    .put("createdAt", task.createdAt)
                    .put("completedAt", task.completedAt ?: JSONObject.NULL)
            )
        }

        return JSONObject()
            .put("version", 1)
            .put("people", people)
            .put("tasks", tasks)
    }

    private fun decode(root: JSONObject): WorkSnapshot {
        val people = mutableListOf<Person>()
        root.optJSONArray("people")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotEmpty() } ?: continue
                people += Person(
                    id = id,
                    name = item.optString("name"),
                    colorIndex = item.optInt("colorIndex", index),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    photoPath = item.optString("photoPath").takeIf { it.isNotEmpty() && !item.isNull("photoPath") }
                )
            }
        }

        val tasks = mutableListOf<Task>()
        root.optJSONArray("tasks")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotEmpty() } ?: continue
                val personId = item.optString("personId")
                if (people.none { it.id == personId }) continue
                tasks += Task(
                    id = id,
                    personId = personId,
                    title = item.optString("title"),
                    summary = item.optString("summary"),
                    notes = item.optString("notes"),
                    progress = item.optInt("progress", 0).coerceIn(0, 100),
                    isDone = item.optBoolean("isDone", false),
                    reminderAt = item.nullableLong("reminderAt"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    completedAt = item.nullableLong("completedAt")
                )
            }
        }

        return WorkSnapshot(people = people, tasks = tasks)
    }

    private fun JSONObject.nullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }

    companion object {
        private const val PREFS_NAME = "work_planner_store"
        private const val KEY_DOCUMENT = "board"
    }
}

/** One repository instance shared by the UI and the alarm receivers. */
object RepositoryProvider {
    @Volatile
    private var instance: LocalWorkRepository? = null

    fun get(context: Context): LocalWorkRepository =
        instance ?: synchronized(this) {
            instance ?: LocalWorkRepository(context.applicationContext).also { instance = it }
        }
}
