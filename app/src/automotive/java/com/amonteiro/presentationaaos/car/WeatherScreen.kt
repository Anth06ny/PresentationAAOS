package com.amonteiro.presentationaaos.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.amonteiro.presentationaaos.R
import com.amonteiro.presentationaaos.data.remote.WeatherApiDataSource
import com.amonteiro.presentationaaos.domain.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

//Écran de résultat : appelle l'API pour la ville choisie puis affiche la météo
class WeatherScreen(carContext: CarContext, private val city: String) : Screen(carContext) {

    var weathers = emptyList<Weather>()
    var errorMessage = ""
    var loading = true
    val weatherApiDataSource = WeatherApiDataSource()

    init {
        loadWeather()
    }

    private fun loadWeather() {
        loading = true
        errorMessage = "" //Sinon l'erreur précédente reste affichée après un "Réessayer" réussi

        //Screen est un LifecycleOwner : la requête est annulée si l'écran est fermé
        //lifecycleScope tourne sur le thread principal : seule la requête part sur Dispatchers.IO
        lifecycleScope.launch {
            try {
                weathers = withContext(Dispatchers.IO) { weatherApiDataSource.loadWeather(city) }
                if (weathers.isEmpty()) {
                    errorMessage = carContext.getString(R.string.car_no_weather, city)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "${carContext.getString(R.string.car_generic_error)} : ${e.message}"
            } finally {
                loading = false
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val header = Header.Builder()
            .setTitle(city)
            .setStartHeaderAction(Action.BACK)
            .build()

        if (loading) {
            return ListTemplate.Builder()
                .setHeader(header)
                .setLoading(true)
                .build()
        } else if (errorMessage.isNotBlank()) {
            return MessageTemplate.Builder(errorMessage)
                .setHeader(header)
                .setIcon(CarIcon.ERROR)
                .addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(R.string.car_retry))
                        .setOnClickListener {
                            loadWeather()
                            invalidate()
                        }
                        .build()
                )
                .build()
        } else {
            return ListTemplate.Builder()
                .setHeader(header)
                .setSingleList(buildWeatherList(weathers))
                .build()
        }

    }

    private fun buildWeatherList(weathers: List<Weather>): ItemList {
        val builder = ItemList.Builder()
        weathers.forEach { weather ->
            builder.addItem(
                Row.Builder()
                    .setTitle("${weather.name} : ${weather.temp.roundToInt()}°")
                    .addText(weather.description.replaceFirstChar { it.uppercase() })
                    .addText(carContext.getString(R.string.wind_speed, weather.speed))
                    .build()
            )
        }
        return builder.build()
    }
}
