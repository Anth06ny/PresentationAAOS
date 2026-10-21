package com.amonteiro.presentationaaos.car.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

//Démo de carContext.getSystemService : services Android de l'appareil qui exécute l'application
//Sur AAOS, l'appareil est la voiture. Sur Android Auto, ce serait le téléphone
class SystemServiceScreen(carContext: CarContext) : Screen(carContext) {

    private val sensorManager = carContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = carContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val connectivityManager = carContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val audioManager = carContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val windowManager = carContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    //Dernières valeurs reçues
    private var acceleration: FloatArray? = null
    private var temperature: Float? = null
    private var location: Location? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> acceleration = event.values.clone()
                Sensor.TYPE_AMBIENT_TEMPERATURE -> temperature = event.values[0]
            }
            refresh()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val locationListener = LocationListener {
        location = it
        refresh()
    }

    init {
        //On n'écoute les capteurs que lorsque l'écran est visible
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = startListening()
            override fun onStop(owner: LifecycleOwner) = stopListening()
        })
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") //Permission vérifiée par hasLocationPermission()
    private fun startListening() {
        accelerometerSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        temperatureSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (hasLocationPermission()) {
            //Premier fournisseur actif (gps, fused, network…), il dépend du véhicule
            locationManager.getProviders(true).firstOrNull()?.let { provider ->
                location = locationManager.getLastKnownLocation(provider)
                locationManager.requestLocationUpdates(provider, 1000L, 0f, locationListener, Looper.getMainLooper())
            }
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
        locationManager.removeUpdates(locationListener)
    }

    //Les capteurs envoient beaucoup de valeurs : on regroupe les rafraîchissements (2 par seconde max)
    private val handler = Handler(Looper.getMainLooper())
    private var refreshPending = false

    private fun refresh() {
        if (!refreshPending) {
            refreshPending = true
            handler.postDelayed({
                refreshPending = false
                invalidate()
            }, 500)
        }
    }

    override fun onGetTemplate(): Template {
        val builder = ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle("getSystemService")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )

        if (!hasLocationPermission()) {
            builder.addSectionedList(
                section(
                    "Permissions",
                    Row.Builder()
                        .setTitle("Autoriser la localisation")
                        .addText("ACCESS_FINE_LOCATION")
                        .setOnClickListener {
                            carContext.requestPermissions(listOf(Manifest.permission.ACCESS_FINE_LOCATION)) { _, _ ->
                                stopListening()
                                startListening()
                                invalidate()
                            }
                        }
                        .build()
                )
            )
        }

        builder.addSectionedList(
            section(
                "SENSOR_SERVICE → SensorManager",
                row(
                    "Accéléromètre (m/s²)",
                    when {
                        accelerometerSensor == null -> "Absent de ce véhicule"
                        acceleration == null -> "En attente…"
                        else -> acceleration!!.joinToString(" / ") { "%.1f".format(it) }
                    }
                ),
                row(
                    "Température ambiante",
                    when {
                        temperatureSensor == null -> "Absent : la température extérieure vient du VHAL"
                        temperature == null -> "En attente…"
                        //L'émulateur envoie -1e30 tant qu'aucune valeur n'est réglée
                        temperature!! !in -100f..100f -> "Aucune valeur (${temperature})"
                        else -> "%.1f °C".format(temperature)
                    }
                )
            )
        )

        builder.addSectionedList(
            section(
                "LOCATION_SERVICE → LocationManager",
                row(
                    "Localisation activée : ${locationManager.isLocationEnabled}",
                    "Fournisseurs : ${locationManager.allProviders.joinToString()}",
                    "Position : " + (location?.let { "%.5f, %.5f".format(it.latitude, it.longitude) } ?: "inconnue")
                )
            )
        )

        builder.addSectionedList(section("CONNECTIVITY_SERVICE → ConnectivityManager", networkRow()))

        builder.addSectionedList(
            section(
                "AUDIO_SERVICE → AudioManager",
                row(
                    "Volume média : ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}" +
                            "/${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}",
                    "Volume fixe : ${audioManager.isVolumeFixed} — musique en cours : ${audioManager.isMusicActive}"
                )
            )
        )

        val bounds = windowManager.currentWindowMetrics.bounds
        builder.addSectionedList(
            section(
                "WINDOW_SERVICE → WindowManager",
                row(
                    "Fenêtre : ${bounds.width()} × ${bounds.height()} px",
                    "Densité : ${carContext.resources.displayMetrics.densityDpi} dpi",
                    "Avec les templates, c'est l'hôte qui dessine"
                )
            )
        )

        return builder.build()
    }

    //Type de réseau actif (nécessite ACCESS_NETWORK_STATE)
    private fun networkRow(): Row {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return row("Aucun réseau", "Une voiture perd souvent le réseau (tunnel, zone blanche)")
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellulaire"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Autre"
        }
        return row(
            "Réseau : $transport",
            "Internet : ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}"
        )
    }

    //Une Row accepte 2 lignes de texte maximum
    private fun row(title: String, vararg texts: String) =
        Row.Builder()
            .setTitle(title)
            .apply { texts.forEach { addText(it) } }
            .build()

    private fun section(header: String, vararg rows: Row) =
        SectionedItemList.create(
            ItemList.Builder().apply { rows.forEach { addItem(it) } }.build(),
            header
        )
}
