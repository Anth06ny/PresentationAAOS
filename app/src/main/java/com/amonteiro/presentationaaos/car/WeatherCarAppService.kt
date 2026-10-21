package com.amonteiro.presentationaaos.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

//Point d'entrée de la Car App Library, déclaré dans src/main/AndroidManifest.xml
//MySession est propre à chaque flavor : HomeScreen sur Automotive, WeatherPrettyScreen sur Android Auto
class WeatherCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = MySession()
}
