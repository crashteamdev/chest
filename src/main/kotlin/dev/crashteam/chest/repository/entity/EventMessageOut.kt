package dev.crashteam.chest.repository.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "event_message_out")
class EventMessageOut : BaseEntity<Long>() {
    @Column(nullable = false)
    var aggregateType: String? = null

    @Column(nullable = false)
    var aggregateId: String? = null

    @Column(nullable = false)
    var type: String? = null

    @Column(nullable = false)
    var payload: ByteArray? = null
}

data class EventMessage(
    val id: Long,
    val type: String,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventMessage

        if (id != other.id) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
