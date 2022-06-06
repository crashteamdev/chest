package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.EventMessage
import dev.crashteam.chest.repository.entity.EventMessageOut
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Component
class EventMessageOutRepositoryCustomImpl(
    @PersistenceContext
    private val entityManager: EntityManager
) : EventMessageOutRepositoryCustom {

    override fun deleteAndGetFirstEventLog(): EventMessage? {
        val resultList = entityManager.createNativeQuery(
            """
            DELETE FROM event_message_out 
                        WHERE id = (
                            SELECT id FROM event_message_out
                            ORDER BY id   
                            FOR UPDATE SKIP LOCKED   
                            LIMIT 1 
                        )
                    RETURNING id, aggregate_type, aggregate_id, type, payload
        """.trimIndent(),
            EventMessageOut::class.java
        ).resultList
        if (resultList.size == 1) {
            val eventMessageOut = resultList[0] as EventMessageOut
            return EventMessage(eventMessageOut.id!!, eventMessageOut.type!!, eventMessageOut.payload!!)
        }

        if (resultList.isEmpty()) {
            return null;
        }

        throw IllegalStateException("Something goes wrong. Too match events returning on delete query")
    }
}
