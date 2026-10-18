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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

//Écran de résultat : appelle l'API pour la ville choisie puis affiche la météo
class WeatherScreen(carContext: CarContext, private val city: String) : Screen(carContext) {

    private sealed interface UiState {
        data object Loading : UiState
        data class Success(val weathers: List<Weather>) : UiState
        data class Error(val message: String) : UiState
    }

    private var uiState: UiState = UiState.Loading

    init {
        loadWeather()
    }

    private fun loadWeather() {
        uiState = UiState.Loading

        //Screen est un LifecycleOwner : la requête est annulée si l'écran est fermé
        lifecycleScope.launch {
            uiState = try {
                val weathers = withContext(Dispatchers.IO) { WeatherApiDataSource.loadWeather(city) }
                if (weathers.isEmpty()) {
                    UiState.Error(carContext.getString(R.string.car_no_weather, city))
                } else {
                    UiState.Success(weathers)
                }
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                e.printStackTrace()
                UiState.Error(e.message ?: carContext.getString(R.string.car_generic_error))
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val header = Header.Builder()
            .setTitle(city)
            .setStartHeaderAction(Action.BACK)
            .build()

        return when (val state = uiState) {
            UiState.Loading -> ListTemplate.Builder()
                .setHeader(header)
                .setLoading(true)
                .build()

            is UiState.Success -> ListTemplate.Builder()
                .setHeader(header)
                .setSingleList(buildWeatherList(state.weathers))
                .build()

            is UiState.Error -> MessageTemplate.Builder(state.message)
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
