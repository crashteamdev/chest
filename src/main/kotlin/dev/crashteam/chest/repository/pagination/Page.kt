package dev.crashteam.chest.repository.pagination

data class Page<out T : PageableEntity<*>>(
    val entities: List<T>,
    val token: ContinuationToken?,
    val hasNext: Boolean,
)
