package com.amonteiro.presentationaaos.di

import com.amonteiro.presentationaaos.data.remote.WeatherApiDataSource
import com.amonteiro.presentationaaos.presentation.viewmodel.MainViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

//Déclaration des dépendances, partagée par le téléphone et la voiture
val appModule = module {
    singleOf(::WeatherApiDataSource)
    viewModelOf(::MainViewModel)
}
