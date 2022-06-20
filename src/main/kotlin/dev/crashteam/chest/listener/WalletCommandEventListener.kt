package dev.crashteam.chest.listener

import dev.crashteam.chest.event.WalletCommandEvent
import dev.crashteam.chest.handler.wallet.WalletCommandEventHandler
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class WalletCommandEventListener(
    private val walletCommandEventHandler: List<WalletCommandEventHandler>
) {

    @KafkaListener(
        topics = ["\${chest.wallet-command-topic-name}"],
        autoStartup = "true",
        containerFactory = "walletCommandListenerContainerFactory"
    )
    fun receive(
        @Payload messages: List<ByteArray>,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partitions: List<Int>,
        @Header(KafkaHeaders.OFFSET) offsets: List<Long>,
        ack: Acknowledgment
    ) {
        try {
            List(messages.size) { i ->
                log.info { "Received WalletCommandEvent message with partition-offset=${partitions[i].toString() + "-" + offsets[i]}" }
                WalletCommandEvent.parseFrom(messages[i])
            }.groupBy { entry -> walletCommandEventHandler.find { it.isHandle(entry) } }
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
