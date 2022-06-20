package dev.crashteam.chest.repository.pagination

interface PageableEntity<T> {
    val id: T?
    val timestamp: Long
}
