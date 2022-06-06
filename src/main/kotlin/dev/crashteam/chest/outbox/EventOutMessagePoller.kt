package dev.crashteam.chest.outbox

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "chest", name = ["event-poller-enabled"], havingValue = "true")
class EventOutMessagePoller(
    private val eventOutMessagePublisher: EventOutMessagePublisher,
) {

    @Scheduled(fixedDelay = 50)
    @SchedulerLock(name = "scheduledTaskName")
    fun pollEventMessage() {
        eventOutMessagePublisher.publishWalletEvent()
    }
}
