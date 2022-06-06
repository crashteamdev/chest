package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.EventMessage

interface EventMessageOutRepositoryCustom {
    fun deleteAndGetFirstEventLog(): EventMessage?
}
