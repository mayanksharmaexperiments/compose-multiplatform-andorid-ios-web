package org.example.project.data.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import org.example.project.data.config.Config

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth) {
            alwaysAutoRefresh = true
        }
    }
}
