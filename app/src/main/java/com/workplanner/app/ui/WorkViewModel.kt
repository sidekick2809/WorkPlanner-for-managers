package com.workplanner.app.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.workplanner.app.data.Person
import com.workplanner.app.data.PhotoStorage
import com.workplanner.app.data.RepositoryProvider
import com.workplanner.app.data.Task
import com.workplanner.app.data.WorkSnapshot
import com.workplanner.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for both screens. All reminder scheduling happens here
 * so the UI never has to remember to cancel or re-arm an alarm.
 */
class WorkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RepositoryProvider.get(application)
    private val reminders = ReminderScheduler(application)

    val snapshot: StateFlow<WorkSnapshot> = repository.state

    fun addPerson(name: String): Person? {
        if (name.isBlank()) return null
        return repository.addPerson(name)
    }

    fun renamePerson(personId: String, name: String) {
        repository.renamePerson(personId, name)
    }

    fun setPersonPhoto(personId: String, bitmap: Bitmap) {
        val person = snapshot.value.person(personId) ?: return
        val path = PhotoStorage.save(getApplication(), personId, bitmap) ?: return
        if (person.photoPath != null && person.photoPath != path) {
            PhotoStorage.delete(person.photoPath)
        }
        repository.setPersonPhoto(personId, path)
    }

    fun removePersonPhoto(personId: String) {
        val person = snapshot.value.person(personId) ?: return
        PhotoStorage.delete(person.photoPath)
        repository.setPersonPhoto(personId, null)
    }

    fun deletePerson(person: Person) {
        snapshot.value.tasks.filter { it.personId == person.id }.forEach {
            reminders.cancel(it.id)
        }
        PhotoStorage.delete(person.photoPath)
        repository.deletePerson(person.id)
    }

    fun addTask(personId: String, title: String, summary: String, reminderAt: Long?): Task? {
        if (title.isBlank() || snapshot.value.person(personId) == null) return null
        val task = repository.addTask(personId, title, summary, reminderAt)
        if (reminderAt != null) reminders.schedule(task)
        return task
    }

    fun setTaskDone(taskId: String, done: Boolean) {
        val task = snapshot.value.task(taskId) ?: return
        repository.setTaskDone(taskId, done)
        if (done) {
            reminders.cancel(taskId)
        } else {
            task.reminderAt?.let { reminders.schedule(task.copy(isDone = false)) }
        }
    }

    fun updateTask(task: Task) {
        repository.updateTask(task)
        reminders.schedule(task)
    }

    fun deleteTask(taskId: String) {
        reminders.cancel(taskId)
        repository.deleteTask(taskId)
    }

    fun restoreTask(task: Task) {
        repository.restoreTask(task)
        if (!task.isDone) {
            task.reminderAt?.let { reminders.schedule(task) }
        }
    }

    fun exportJson(): String = repository.exportJson()

    fun restoreFromJson(raw: String): Boolean {
        val previous = snapshot.value
        if (!repository.replaceAll(raw)) return false

        val restored = snapshot.value
        previous.tasks.forEach { reminders.cancel(it.id) }
        restored.tasks.filter { !it.isDone }.forEach { task ->
            task.reminderAt?.let { reminders.schedule(task) }
        }

        val retainedPhotos = restored.people.mapNotNull { it.photoPath }.toSet()
        previous.people
            .mapNotNull { it.photoPath }
            .filterNot { it in retainedPhotos }
            .forEach { PhotoStorage.delete(it) }
        return true
    }
}
