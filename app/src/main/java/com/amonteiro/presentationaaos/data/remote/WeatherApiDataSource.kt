package com.amonteiro.presentationaaos.data.remote

import com.amonteiro.presentationaaos.domain.Weather
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


class WeatherApiDataSource {

    companion object {
        private const val API_URL =
            "https://api.openweathermap.org/data/2.5/find?appid=b80967f0a6bd10d23e44848547b26550&units=metric&lang=fr&q="
    }

    //Création et réglage du client
    private val client = HttpClient {
        install(Logging) {
            //(import io.ktor.client.plugins.logging.Logger)
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.INFO  // TRACE, HEADERS, BODY, etc.
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }, contentType = ContentType.Any)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    //GET Le JSON reçu sera parser en UserEntity,
    //Crash si le JSON ne correspond pas au type indiqué
    suspend fun loadWeather(cityname: String): List<Weather> {

        //Fait la requête
        val response = client.get(API_URL + cityname)
        if (!response.status.isSuccess()) {
            throw Exception("Erreur API: ${response.status} - ${response.bodyAsText()}")
        }

        //Transforme le body en Object
        return response.body<WeatherAPIResultDTO>().list.map { weather ->

            var icon = weather.weather.getOrNull(0)?.icon?.let {
                "https://openweathermap.org/img/wn/$it@4x.png"
            } ?: "-"

            Weather(
                id = weather.id,
                name = weather.name,
                temp = weather.main.temp,
                speed = weather.wind.speed,
                description = weather.weather.getOrNull(0)?.description ?: "-",
                icon = icon
            )
        }

    }


    //Ferme le Client mais celui ci ne sera plus utilisable. Uniquement pour le main
    fun close() = client.close()
}

@Serializable
data class WeatherAPIResultDTO(val list: List<WeatherDTO>)

@Serializable
data class WeatherDTO(
    val id: Int,
    val name: String,
    val main: TempDTO,
    val wind: WindDTO,
    val weather: List<DescriptionDTO>
)

@Serializable
data class TempDTO(val temp: Double)

@Serializable
data class WindDTO(var speed: Double)

@Serializable
data class DescriptionDTO(val description: String, val icon: String)
