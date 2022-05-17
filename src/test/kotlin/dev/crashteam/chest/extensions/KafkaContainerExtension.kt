package dev.crashteam.chest.extensions

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class KafkaContainerExtension : BeforeAllCallback, AfterAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        kafka.start()
    }

    override fun afterAll(context: ExtensionContext) {
        kafka.stop()
    }

    companion object {
        val kafka: KafkaContainer by lazy {
            val tag = DockerImageName.parse("confluentinc/cp-kafka").withTag("7.1.1")
            KafkaContainer(tag).withEmbeddedZookeeper()
        }
    }

}
