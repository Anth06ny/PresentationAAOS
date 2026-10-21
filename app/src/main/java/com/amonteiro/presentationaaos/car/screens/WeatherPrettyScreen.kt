package com.amonteiro.presentationaaos.car.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarIconSpan
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.amonteiro.presentationaaos.R
import com.amonteiro.presentationaaos.data.remote.WeatherApiDataSource
import com.amonteiro.presentationaaos.domain.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

//Copie de WeatherScreen qui montre toutes les personnalisations de couleur possibles
//NB : l'hôte reste maître de l'affichage, il peut ignorer une couleur si le contraste est insuffisant
class WeatherPrettyScreen(carContext: CarContext, private val city: String) : Screen(carContext) {

    var weathers = emptyList<Weather>()
    var icons = emptyMap<String, Bitmap>() //URL -> image téléchargée
    var errorMessage = ""
    var loading = true
    val weatherApiDataSource = WeatherApiDataSource()


    //Couleur personnalisée : une valeur pour le thème clair, une pour le thème sombre
    private val customOrange = CarColor.createCustom(Color.rgb(0xE6, 0x51, 0x00), Color.rgb(0xFF, 0xB7, 0x4D))
    private val badColor = CarColor.createCustom(Color.WHITE, Color.BLACK)

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
                //Un CarIcon n'accepte pas d'URL : on télécharge les images avant d'afficher
                //(Coil gère lui-même ses threads, execute() ne bloque pas le thread principal)
                icons = weathers.map { it.icon }.distinct()
                    .mapNotNull { url -> loadBitmap(url)?.let { url to it } }
                    .toMap()
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "${carContext.getString(R.string.car_generic_error)} : ${e.message}"
            } finally {
                loading = false
            }
            invalidate()
        }
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        val request = ImageRequest.Builder(carContext)
            .data(url)
            .allowHardware(false) //Bitmap hardware non transmissible à l'hôte
            .build()
        val result = SingletonImageLoader.get(carContext).execute(request)
        return (result as? SuccessResult)?.image?.toBitmap()
    }

    override fun onGetTemplate(): Template {
        val header = Header.Builder()
            .setTitle(city)
            .setStartHeaderAction(Action.BACK)
            //Action d'en-tête avec une icône teintée
            .addEndHeaderAction(
                Action.Builder()
                    .setIcon(tintedIcon(R.drawable.ic_refresh, CarColor.BLUE))
                    .setOnClickListener {
                        loadWeather()
                        invalidate()
                    }
                    .build()
            )
            .build()

        if (loading) {
            return ListTemplate.Builder()
                .setHeader(header)
                .setLoading(true)
                .build()
        } else if (errorMessage.isNotBlank()) {
            return MessageTemplate.Builder(errorMessage)
                .setHeader(header)
                //Icône système recolorée
                .setIcon(CarIcon.Builder(CarIcon.ERROR).setTint(CarColor.RED).build())
                .addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(R.string.car_retry))
                        //Couleur de fond du bouton + mise en avant comme action principale
                        .setBackgroundColor(customOrange)
                        .setFlags(Action.FLAG_PRIMARY)
                        .setOnClickListener {
                            loadWeather()
                            invalidate()
                        }
                        .build()
                )
                .build()
        } else {
            val builder = ListTemplate.Builder()
                .setHeader(header)
                .setSingleList(buildWeatherList(weathers))

            //Bouton flottant (FAB) : icône + couleur de fond obligatoires, Car API 6 minimum
            //Inutile si l'écran est déjà la racine (cas d'Android Auto, où il est le 1er écran)
            if (carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_6 && screenManager.stackSize > 1) {
                builder.addAction(
                    Action.Builder()
                        .setIcon(tintedIcon(R.drawable.ic_home, CarColor.createCustom(Color.WHITE, Color.BLACK)))
                        .setBackgroundColor(customOrange)
                        .setOnClickListener { screenManager.popToRoot() }
                        .build()
                )
            }
            return builder.build()
        }
    }

    private fun buildWeatherList(weathers: List<Weather>): ItemList {
        val builder = ItemList.Builder()
        weathers.forEach { weather ->
            val rowBuilder = Row.Builder()
                //Le titre n'accepte pas de couleur (seulement Distance/Duration/CarIconSpan)
                .setTitle(weather.name)
                .addText(temperatureText(weather))
                .addText(windText(weather))

            //Image de la météo, IMAGE_TYPE_LARGE : affichée en couleur, non teintée par l'hôte
            icons[weather.icon]?.let {
                rowBuilder.setImage(
                    CarIcon.Builder(IconCompat.createWithBitmap(it)).build(),
                    Row.IMAGE_TYPE_LARGE
                )
            }
            builder.addItem(rowBuilder.build())
        }
        return builder.build()
    }

    //Température colorée selon sa valeur + description en couleur secondaire (ForegroundCarColorSpan)
    private fun temperatureText(weather: Weather): CharSequence {
        val color = when {
            weather.temp < 5 -> CarColor.BLUE
            weather.temp < 15 -> CarColor.GREEN
            weather.temp < 25 -> CarColor.YELLOW
            else -> CarColor.RED
        }
        val temp = "${weather.temp.roundToInt()}°"
        val description = weather.description.replaceFirstChar { it.uppercase() }
        val text = SpannableString("$temp · $description")
        text.setSpan(ForegroundCarColorSpan.create(color), 0, temp.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        text.setSpan(
            ForegroundCarColorSpan.create(CarColor.SECONDARY),
            text.length - description.length, text.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return text
    }

    //Icône teintée insérée dans le texte avec CarIconSpan
    private fun windText(weather: Weather): CharSequence {
        //Le 1er caractère est remplacé à l'affichage par l'icône
        val text = SpannableString("  " + carContext.getString(R.string.wind_speed, weather.speed))
        text.setSpan(
            CarIconSpan.create(tintedIcon(R.drawable.ic_air, CarColor.BLUE), CarIconSpan.ALIGN_CENTER),
            0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return text
    }

    private fun tintedIcon(resId: Int, color: CarColor) =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId))
            .setTint(color)
            .build()
}
