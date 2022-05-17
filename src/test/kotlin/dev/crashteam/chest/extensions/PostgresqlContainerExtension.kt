package dev.crashteam.chest.extensions

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.PostgreSQLContainer

class PostgresqlContainerExtension : BeforeAllCallback, AfterAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        postgresql.start()
    }

    override fun afterAll(context: ExtensionContext) {
        postgresql.stop()
    }

    companion object {
        val postgresql: PostgreSQLContainer<Nothing> by lazy {
            PostgreSQLContainer<Nothing>("postgres:14").apply {
                withDatabaseName("postgresql")
                withUsername("user")
                withPassword("password")
            }
        }
    }

}
