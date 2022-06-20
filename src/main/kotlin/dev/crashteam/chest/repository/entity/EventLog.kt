package dev.crashteam.chest.repository.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "event_log")
class EventLog {
    @Id
    var eventId: String? = null

    @Column(nullable = false)
    var timeOfReceiving: LocalDateTime? = null
}
