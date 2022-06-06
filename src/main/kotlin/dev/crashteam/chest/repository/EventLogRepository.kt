package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.EventLog
import org.springframework.data.jpa.repository.JpaRepository

interface EventLogRepository : JpaRepository<EventLog, String>
