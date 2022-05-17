package dev.crashteam.chest.repository.pagination

import dev.crashteam.chest.repository.pagination.converter.ContinuationTokenConverter
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ContinuationTokenService(
    private val continuationTokenConverter: ContinuationTokenConverter,
) {

    fun <T : PageableEntity<*>> createPage(
        entities: List<T>,
        previousToken: ContinuationToken?,
        keyParams: Map<String, String>?,
        pageSize: Int,
        dropLast: Boolean = true,
    ): Page<T> {
        val hasNext = entities.size == pageSize
        val entities = if (hasNext && dropLast) {
            // Drop the last element if we asked for n+1 element in the query
            entities.dropLast(1)
        } else entities
        return Page(
            entities = entities,
            token = if (entities.isEmpty()) previousToken else createToken(entities, keyParams),
            hasNext = hasNext
        )
    }

    fun <T : PageableEntity<*>> createToken(entities: List<T>, keyParams: Map<String, String>?): ContinuationToken {
        val lastEntity = entities.last()
        return ContinuationToken(keyParams, lastEntity.timestamp, lastEntity.id.toString())
    }

    fun tokenToString(continuationToken: ContinuationToken): String {
        log.debug { "Convert continuation token to string: $continuationToken" }
        val tokenString = continuationTokenConverter.toString(continuationToken)
        log.debug { "String token representation: $tokenString" }

        return tokenString
    }

    fun tokenFromString(token: String): ContinuationToken {
        log.debug { "Convert string to continuation token: $token" }
        val continuationToken = continuationTokenConverter.fromString(token)
        log.debug { "Continuation token: $continuationToken" }

        return continuationToken
    }
}
