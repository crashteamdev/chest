package dev.crashteam.chest

import com.google.protobuf.Timestamp
import dev.crashteam.chest.extensions.KafkaContainerExtension
import dev.crashteam.chest.repository.WalletHistoryRepository
import dev.crashteam.chest.repository.WalletPaymentRepository
import dev.crashteam.chest.repository.WalletRepository
import dev.crashteam.payment.*
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit

@Import(PaymentListenerTest.KafkaProducerConfig::class)
class PaymentListenerTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, ByteArray>

    @Autowired
    lateinit var walletPaymentRepository: WalletPaymentRepository

    @Autowired
    lateinit var walletHistoryRepository: WalletHistoryRepository

    @Autowired
    lateinit var walletRepository: WalletRepository

    @Value("\${chest.payment-topic-name}")
    lateinit var paymentTopicName: String

    @BeforeAll
    fun setUp() {
        Thread.sleep(1000) // TODO: fix kafka send bug
    }

    @Test
    fun `test listening payment create event`() {
        // Given
        val paymentId = "test-payment-id"
        val userId = UUID.randomUUID().toString()
        val paymentEvent = PaymentEvent.newBuilder().apply {
            createdAt = Timestamp.newBuilder().setSeconds(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)).build()
            eventSource = PaymentEvent.EventSource.newBuilder().setUserId(userId).build()
            payload = PaymentEvent.EventPayload.newBuilder().apply {
                val paymentChange = PaymentChange.newBuilder().apply {
                    this.paymentCreated = PaymentCreated.newBuilder().apply {
                        this.createdAt =
                            Timestamp.newBuilder().setSeconds(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)).build()
                        this.amount = Amount.newBuilder().apply {
                            this.currency = "RUB"
                            this.value = 50000
                        }.build()
                        this.userId = userId
                        this.paymentId = paymentId
                        this.status = PaymentStatus.PENDING
                    }.build()
                }.build()
                this.paymentChange = paymentChange
            }.build()
        }.build()

        // When
        val producerRecord = ProducerRecord(paymentTopicName, paymentId, paymentEvent.toByteArray())
        kafkaTemplate.send(producerRecord).get()
        val walletPayment = await.atMost(60, TimeUnit.SECONDS)
            .untilNotNull {
                walletPaymentRepository.findByPaymentId(paymentId)
            }
        val wallet = walletRepository.findById(walletPayment.walletId!!).orElse(null)
        val walletHistory = walletHistoryRepository.findWalletHistory(walletPayment.walletId!!)

        // Then
        assertTrue(wallet != null)
        assertEquals(0, wallet.amount)
        assertEquals(0, walletHistory.entities.size)
        assertEquals(50000, walletPayment.amount)
    }

    @TestConfiguration
    class KafkaProducerConfig {

        @Value("\${chest.payment-topic-name}")
        lateinit var paymentTopicName: String

        @Bean
        fun producerFactory(): ProducerFactory<String, ByteArray> {
            val configProps: MutableMap<String, Any> = HashMap()
            configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaContainerExtension.kafka.bootstrapServers
            configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
            return DefaultKafkaProducerFactory(configProps)
        }

        @Bean
        fun kafkaTemplate(): KafkaTemplate<String, ByteArray> {
            AdminClient.create(
                mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaContainerExtension.kafka.bootstrapServers)
            ).use {
                it.createTopics(listOf(NewTopic(paymentTopicName, 1, 1)))
            }
            return KafkaTemplate(producerFactory())
        }
    }

}
