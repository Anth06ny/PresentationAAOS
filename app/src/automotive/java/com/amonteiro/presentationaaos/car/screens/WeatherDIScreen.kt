package com.amonteiro.presentationaaos.car.screens

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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amonteiro.presentationaaos.R
import com.amonteiro.presentationaaos.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.math.roundToInt

//Version de WeatherScreen qui réutilise le MainViewModel du téléphone, fourni par Koin
//Screen n'est pas un ViewModelStoreOwner : on le devient pour que le ViewModel soit nettoyé à la fermeture
class WeatherDIScreen(carContext: CarContext, city: String) : Screen(carContext), ViewModelStoreOwner, KoinComponent {

    override val viewModelStore = ViewModelStore()

    private val viewModel = ViewModelProvider(this, viewModelFactory {
        initializer { get<MainViewModel>() }
    })[MainViewModel::class.java]

    init {
        //Fermeture de l'écran : onCleared() du ViewModel, la requête en cours est annulée
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = viewModelStore.clear()
        })

        //Chaque changement d'état du ViewModel redemande le template
        lifecycleScope.launch {
            combine(viewModel.dataList, viewModel.runInProgress, viewModel.errorMessage) { _, _, _ -> }
                .collect { invalidate() }
        }

        viewModel.updateSearchText(city)
        viewModel.loadWeathers()
    }

    override fun onGetTemplate(): Template {
        val header = Header.Builder()
            .setTitle(viewModel.searchText.value)
            .setStartHeaderAction(Action.BACK)
            .build()

        val errorMessage = viewModel.errorMessage.value

        return when {
            viewModel.runInProgress.value -> ListTemplate.Builder()
                .setHeader(header)
                .setLoading(true)
                .build()

            errorMessage.isNotBlank() -> MessageTemplate.Builder(errorMessage)
                .setHeader(header)
                .setIcon(CarIcon.ERROR)
                .addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(R.string.car_retry))
                        .setOnClickListener { viewModel.loadWeathers() }
                        .build()
                )
                .build()

            else -> ListTemplate.Builder()
                .setHeader(header)
                .setSingleList(buildWeatherList())
                .build()
        }
    }

    private fun buildWeatherList(): ItemList {
        val builder = ItemList.Builder()
            .setNoItemsMessage(carContext.getString(R.string.car_no_weather, viewModel.searchText.value))
        viewModel.dataList.value.forEach { weather ->
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
