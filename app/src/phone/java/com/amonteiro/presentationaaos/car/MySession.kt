package com.amonteiro.presentationaaos.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.amonteiro.presentationaaos.car.screens.WeatherPrettyScreen

//Session Android Auto : uniquement la météo jolie (la version Automotive est dans src/automotive)
class MySession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = WeatherPrettyScreen(carContext, "Toulouse")
}
