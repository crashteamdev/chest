package dev.crashteam.chest.listener

import dev.crashteam.chest.handler.payment.PaymentEventHandler
import dev.crashteam.payment.PaymentEvent
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class PaymentEventListener(
    private val paymentEventHandler: List<PaymentEventHandler>
) {

    @KafkaListener(
        topics = ["\${chest.payment-topic-name}"],
        autoStartup = "true",
        containerFactory = "paymentListenerContainerFactory"
    )
    fun receive(
        @Payload messages: List<ByteArray>,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partitions: List<Int>,
        @Header(KafkaHeaders.OFFSET) offsets: List<Long>,
        ack: Acknowledgment
    ) {
        try {
            List(messages.size) { i ->
                log.info { "Received PaymentEvent message with partition-offset=${partitions[i].toString() + "-" + offsets[i]}" }
                PaymentEvent.parseFrom(messages[i])
            }.groupBy { entry -> paymentEventHandler.find { it.isHandle(entry) } }
                .forEach { (handler, entries) ->
                    handler?.handle(entries)
                }
            ack.acknowledge()
        } catch (e: Exception) {
            log.error(e) { "Exception during handling KE fetch events" }
            throw e
        }

    }

}
