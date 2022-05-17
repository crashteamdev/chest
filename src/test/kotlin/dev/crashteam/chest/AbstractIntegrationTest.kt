package dev.crashteam.chest

import dev.crashteam.chest.extensions.KafkaContainerExtension
import dev.crashteam.chest.extensions.PostgresqlContainerExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration

@ExtendWith(KafkaContainerExtension::class, PostgresqlContainerExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    classes = [OvermindApplication::class],
    initializers = [AbstractIntegrationTest.Initializer::class]
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractIntegrationTest {

    object Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.kafka.bootstrap-servers=${KafkaContainerExtension.kafka.bootstrapServers}",
                "spring.datasource.url=${PostgresqlContainerExtension.postgresql.jdbcUrl}",
                "spring.datasource.username=${PostgresqlContainerExtension.postgresql.username}",
                "spring.datasource.password=${PostgresqlContainerExtension.postgresql.password}"
            ).applyTo(applicationContext.environment)
        }
    }

}
