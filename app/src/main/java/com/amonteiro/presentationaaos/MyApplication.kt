package com.amonteiro.presentationaaos

import android.app.Application
import com.amonteiro.presentationaaos.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

//Démarre Koin au lancement du processus, avant MainActivity comme avant le CarAppService
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }
}
