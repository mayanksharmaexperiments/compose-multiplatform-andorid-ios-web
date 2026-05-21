package org.example.project.repository

import io.github.jan.supabase.postgrest.postgrest
import org.example.project.model.Client
import org.example.project.network.SupabaseClient

class ClientRepository {
    private val supabase = SupabaseClient.client

    suspend fun getClients(): List<Client> {
        return supabase.postgrest["Client"]
            .select()
            .decodeList<Client>()
    }
}
