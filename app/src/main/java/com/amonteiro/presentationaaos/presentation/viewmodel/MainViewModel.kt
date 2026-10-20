package com.amonteiro.presentationaaos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amonteiro.presentationaaos.data.remote.WeatherApiDataSource
import com.amonteiro.presentationaaos.domain.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch




//La source de données est injectée par Koin (voir di/AppModule.kt)
class MainViewModel(private val weatherApiDataSource: WeatherApiDataSource) : ViewModel() {
    //MutableStateFlow est une donnée observable
    val dataList = MutableStateFlow(emptyList<Weather>())
    val runInProgress = MutableStateFlow(false)
    val errorMessage = MutableStateFlow("")
    var searchText = MutableStateFlow("Nice")


    fun loadWeathers() {

        runInProgress.value = true
        dataList.value = emptyList()
        errorMessage.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataList.value = weatherApiDataSource.loadWeather(searchText.value)
            }
            catch (e: Exception) {
                e.printStackTrace()
                errorMessage.value = e.message ?: "Une erreur est survenue"
            }
            finally {
                runInProgress.value = false
            }
        }
    }

    fun updateSearchText(value:String){
        searchText.value = value
    }

    fun loadFakeData(runInProgress: Boolean = false, errorMessage: String = "") {
        this.runInProgress.value = runInProgress
        this.errorMessage.value = errorMessage
        dataList.value = listOf(
            Weather(
                id = 1,
                name = "Paris",
                temp = 18.5,
                description = "ciel dégagé",
                icon = "https://picsum.photos/200",
                speed = 5.0
            ),
            Weather(
                id = 2,
                name = "Toulouse",
                temp = 22.3,
                description = "partiellement nuageux",
                icon = "https://picsum.photos/201",
                speed = 3.2
            ),
            Weather(
                id = 3,
                name = "Toulon",
                temp = 25.1,
                description = "ensoleillé",
                icon = "https://picsum.photos/202",
                speed = 6.7
            ),
            Weather(
                id = 4,
                name = "Lyon",
                temp = 19.8,
                description = "pluie légère",
                icon = "https://picsum.photos/203",
                speed = 4.5
            )
        ).shuffled() //shuffled() pour avoir un ordre différent à chaque appel
    }
}