package com.amonteiro.presentationaaos.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class MySession : Session() {
    override fun onCreateScreen(intent: Intent): Screen  = CitySearchScreen(carContext)
}