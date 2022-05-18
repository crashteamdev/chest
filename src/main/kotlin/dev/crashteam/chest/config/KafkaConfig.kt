package dev.crashteam.chest.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryingBatchErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun consumerConfigs(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        props[ConsumerConfig.GROUP_ID_CONFIG] = "chest-group"
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10"
        return props
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ByteArray> {
        return DefaultKafkaConsumerFactory(consumerConfigs())
    }

    @Bean
    fun keListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, ByteArray> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory()
        factory.isBatchListener = true
        factory.setCommonErrorHandler(DefaultErrorHandler(FixedBackOff()).apply { isAckAfterHandle = false })
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }

}
