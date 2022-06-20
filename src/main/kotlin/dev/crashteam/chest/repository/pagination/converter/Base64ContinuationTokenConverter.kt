package dev.crashteam.chest.repository.pagination.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.crashteam.chest.repository.pagination.ContinuationToken
import org.springframework.stereotype.Component
import java.util.*

@Component
class Base64ContinuationTokenConverter(
    private val objectMapper: ObjectMapper,
) : ContinuationTokenConverter {

    override fun fromString(token: String): ContinuationToken {
        val decodedString = String(Base64.getDecoder().decode(token))

        return objectMapper.readValue(decodedString)
    }

    override fun toString(token: ContinuationToken): String {
        return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(token))
    }
}
