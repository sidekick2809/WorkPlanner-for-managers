package com.workplanner.app.data

/** A member of the team. Every team member owns a stack of tasks. */
data class Person(
    val id: String,
    val name: String,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** Absolute path of the circular profile picture stored in app files, if any. */
    val photoPath: String? = null
)

/** A single piece of work, optionally with a reminder alarm attached. */
data class Task(
    val id: String,
    val personId: String,
    val title: String,
    val summary: String = "",
    val notes: String = "",
    val progress: Int = 0,
    val isDone: Boolean = false,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/** Everything the app knows, kept in one immutable snapshot. */
data class WorkSnapshot(
    val people: List<Person> = emptyList(),
    val tasks: List<Task> = emptyList()
) {
    fun person(id: String): Person? = people.firstOrNull { it.id == id }

    fun task(id: String): Task? = tasks.firstOrNull { it.id == id }

    fun pendingTasks(personId: String): List<Task> =
        tasks.filter { it.personId == personId && !it.isDone }
            .sortedWith(compareBy<Task> { it.reminderAt ?: Long.MAX_VALUE }.thenBy { it.createdAt })

    fun doneTasks(personId: String): List<Task> =
        tasks.filter { it.personId == personId && it.isDone }
            .sortedByDescending { it.completedAt ?: it.createdAt }

    val pendingCount: Int get() = tasks.count { !it.isDone }

    val doneCount: Int get() = tasks.count { it.isDone }
}
