package org.example.project.di

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import org.example.project.data.network.SupabaseClient
import org.example.project.data.repository.AuthRepositoryImpl
import org.example.project.data.repository.ClientRepositoryImpl
import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.repository.ClientRepository
import org.koin.dsl.module

val diModule = module {
    // Supabase
    single { SupabaseClient.client }
    single { get<io.github.jan.supabase.SupabaseClient>().postgrest }
    single { get<io.github.jan.supabase.SupabaseClient>().auth }

    // Repositories
    single<ClientRepository> { ClientRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
}
