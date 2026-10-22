package com.workplanner.app.data

import kotlinx.coroutines.flow.StateFlow

interface WorkRepository {
    val state: StateFlow<WorkSnapshot>

    fun snapshot(): WorkSnapshot

    fun addPerson(name: String): Person

    fun renamePerson(personId: String, name: String)

    fun setPersonPhoto(personId: String, photoPath: String?)

    fun deletePerson(personId: String)

    fun addTask(
        personId: String,
        title: String,
        summary: String = "",
        reminderAt: Long? = null
    ): Task

    fun updateTask(task: Task)

    fun setTaskDone(taskId: String, done: Boolean)

    fun deleteTask(taskId: String)

    fun restoreTask(task: Task)

    fun task(taskId: String): Task?

    /** Serialised copy of the current board, ready to be written to a backup file. */
    fun exportJson(): String

    /** Replaces the whole board with the contents of [raw]. Returns false for invalid JSON. */
    fun replaceAll(raw: String): Boolean
}
