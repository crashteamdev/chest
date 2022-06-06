package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.EventMessageOut
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventMessageOutRepository : JpaRepository<EventMessageOut, String>, EventMessageOutRepositoryCustom
