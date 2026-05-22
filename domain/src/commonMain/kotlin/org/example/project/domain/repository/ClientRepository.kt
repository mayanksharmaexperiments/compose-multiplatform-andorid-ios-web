package org.example.project.domain.repository

import org.example.project.domain.model.Client

interface ClientRepository {
    suspend fun getClients(): List<Client>
}
