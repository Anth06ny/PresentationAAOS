package com.amonteiro.presentationaaos.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.amonteiro.presentationaaos.MainActivity
import com.amonteiro.presentationaaos.R
import com.amonteiro.presentationaaos.car.screens.CarServiceScreen
import com.amonteiro.presentationaaos.car.screens.SystemServiceScreen
import com.amonteiro.presentationaaos.car.screens.VHALScreen
import com.amonteiro.presentationaaos.car.screens.WeatherPrettyScreen

//Écran d'accueil : grille des fonctionnalités de l'application
class HomeScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val itemList = ItemList.Builder()
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_compose_weather))
                    .setImage(
                        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_weather))
                            .setTint(CarColor.BLUE)
                            .build()
                    )
                    //Quitte les templates pour ouvrir l'écran Compose de MainActivity
                    //applicationContext et non carContext : carContext est lié à l'écran de l'hôte des templates,
                    //sur lequel l'application n'a pas le droit de lancer une activité (SecurityException)
                    .setOnClickListener {
                        carContext.applicationContext.startActivity(
                            Intent(carContext, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                //Demande à MainActivity d'afficher sa flèche retour
                                .putExtra(MainActivity.EXTRA_SHOW_BACK, true)
                        )
                    }
                    .build()
            )
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_weather))
                    .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_weather)).build())
                    .setOnClickListener { screenManager.push(CitySearchScreen(carContext)) }
                    .build()
            )
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_weather_pretty))
                    //Icône teintée en jaune
                    .setImage(
                        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_weather))
                            .setTint(CarColor.YELLOW)
                            .build()
                    )
                    .setOnClickListener {
                        screenManager.push(
                            WeatherPrettyScreen(
                                carContext,
                                "Toulouse"
                            )
                        )
                    }
                    .build()
            )
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_car_service))
                    .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car)).build())
                    .setOnClickListener { screenManager.push(CarServiceScreen(carContext)) }
                    .build()
            )
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_system_service))
                    .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_memory)).build())
                    .setOnClickListener { screenManager.push(SystemServiceScreen(carContext)) }
                    .build()
            )
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_vhal))
                    .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_speed)).build())
                    .setOnClickListener { screenManager.push(VHALScreen(carContext)) }
                    .build()
            )
            .addItem(
                GridItem.Builder()
                    .setTitle(carContext.getString(R.string.car_airbag))
                    .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_airbag)).build())
                    //Toast affiché par l'hôte, sans changer d'écran
                    .setOnClickListener { CarToast.makeText(carContext, "Paf", CarToast.LENGTH_SHORT).show() }
                    .build()
            )
            .build()

        return GridTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.car_home_title))
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .setSingleList(itemList)
            .build()
    }
}
