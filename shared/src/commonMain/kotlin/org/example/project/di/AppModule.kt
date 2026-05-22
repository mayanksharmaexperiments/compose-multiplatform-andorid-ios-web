package org.example.project.di

import org.example.project.presentation.auth.forgotpassword.ForgotPasswordViewModel
import org.example.project.presentation.auth.login.LoginViewModel
import org.example.project.presentation.auth.signup.SignUpViewModel
import org.example.project.presentation.client.ClientListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
    viewModel { ClientListViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { SignUpViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
}
