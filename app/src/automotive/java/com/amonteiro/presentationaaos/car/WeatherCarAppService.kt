package com.amonteiro.presentationaaos.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

//Point d'entrée de la Car App Library, déclaré dans src/automotive/AndroidManifest.xml
class WeatherCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        //En debug on accepte tous les hôtes, en release uniquement ceux de la liste fournie par la librairie
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = CitySearchScreen(carContext)
    }
}
