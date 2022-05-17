package dev.crashteam.chest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class OvermindApplication

fun main(args: Array<String>) {
    runApplication<OvermindApplication>(*args)
}
