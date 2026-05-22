package org.example.project.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import org.example.project.data.model.ClientDto
import org.example.project.data.model.toDomain
import org.example.project.domain.model.Client
import org.example.project.domain.repository.ClientRepository

class ClientRepositoryImpl(
    private val postgrest: Postgrest
) : ClientRepository {
    override suspend fun getClients(): List<Client> {
        val dtos = postgrest["Client"]
            .select()
            .decodeList<ClientDto>()
        return dtos.map { it.toDomain() }
    }
}
