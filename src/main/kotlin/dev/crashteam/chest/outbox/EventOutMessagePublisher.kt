package dev.crashteam.chest.outbox

import dev.crashteam.chest.event.WalletCudEvent
import dev.crashteam.chest.repository.EventMessageOutRepository
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import javax.transaction.Transactional

private val log = KotlinLogging.logger {}

@Component
class EventOutMessagePublisher(
    private val eventMessageOutRepository: EventMessageOutRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {

    @Value("\${chest.wallet-topic-name}")
    lateinit var walletTopicName: String

    @Transactional
    fun publishWalletEvent() {
        val eventMessage = eventMessageOutRepository.deleteAndGetFirstEventLog() ?: return
        val walletEvent = WalletCudEvent.parseFrom(eventMessage.payload)
        log.info { "Publish wallet eventId=${walletEvent.eventId}; walletId=${walletEvent.eventSource.walletId}" }
        val producerRecord =
            ProducerRecord(walletTopicName, walletEvent.eventSource.walletId, walletEvent.toByteArray())
        kafkaTemplate.send(producerRecord)
    }


}
