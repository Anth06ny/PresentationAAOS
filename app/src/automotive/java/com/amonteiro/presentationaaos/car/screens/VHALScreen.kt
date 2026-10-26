package com.amonteiro.presentationaaos.car.screens

import android.car.Car
import android.car.VehicleGear
import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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

//Démo du VHAL : lecture directe des propriétés du véhicule avec l'API android.car (AAOS uniquement)
//Chemin : VHAL -> CarService -> CarPropertyManager, sans passer par l'hôte de la Car App Library
class VHALScreen(carContext: CarContext) : Screen(carContext) {

    //Une propriété VHAL à afficher, avec la mise en forme de sa valeur
    private class VhalProperty(val id: Int, val label: String, val format: (Any) -> String)

    private val properties = listOf(
        VhalProperty(VehiclePropertyIds.FUEL_LEVEL, "Carburant") { "%.1f L".format((it as Float) / 1000) }, //en mL
        VhalProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED, "Vitesse") { "%.0f km/h".format((it as Float) * 3.6f) },
        VhalProperty(VehiclePropertyIds.GEAR_SELECTION, "Rapport") { gearName(it as Int) },
        VhalProperty(VehiclePropertyIds.PARKING_BRAKE_ON, "Frein à main") { if (it as Boolean) "Serré" else "Desserré" },
        VhalProperty(VehiclePropertyIds.IGNITION_STATE, "Contact") { ignitionName(it as Int) },
        VhalProperty(VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE, "Température extérieure") { "%.1f °C".format(it as Float) },
        VhalProperty(VehiclePropertyIds.NIGHT_MODE, "Mode nuit") { if (it as Boolean) "Oui" else "Non" },
        VhalProperty(VehiclePropertyIds.EV_BATTERY_LEVEL, "Batterie") { "%.1f kWh".format((it as Float) / 1000) }, //en Wh
        VhalProperty(VehiclePropertyIds.RANGE_REMAINING, "Autonomie") { "%.0f km".format((it as Float) / 1000) } //en m
    )

    //Permissions dangereuses à demander à l'exécution (les autres sont accordées à l'installation)
    private val runtimePermissions = listOf(
        "android.car.permission.CAR_SPEED",
        "android.car.permission.CAR_ENERGY"
    )

    //Connexion directe à CarService
    private val car = Car.createCar(carContext)
    private val carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    //Propriétés exposées par la voiture et autorisées, et dernières valeurs reçues
    private val supported = mutableSetOf<Int>()
    private val values = mutableMapOf<Int, CarPropertyValue<*>>()

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            values[value.propertyId] = value
            refresh()
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {}
    }

    init {
        //On n'écoute la voiture que lorsque l'écran est visible
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = startListening()
            override fun onStop(owner: LifecycleOwner) = stopListening()
            override fun onDestroy(owner: LifecycleOwner) = car.disconnect()
        })
    }

    @Suppress("DEPRECATION") //subscribePropertyEvents n'existe qu'à partir d'Android 15
    private fun startListening() {
        supported.clear()
        properties.forEach { property ->
            //null si la voiture n'expose pas la propriété ou si la permission manque
            val config = carPropertyManager.getCarPropertyConfig(property.id) ?: return@forEach
            supported += property.id

            //Propriété continue (vitesse) : fréquence fixe. Sinon : à chaque changement
            val rate = if (config.changeMode == CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_CONTINUOUS) {
                CarPropertyManager.SENSOR_RATE_NORMAL
            } else {
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            }
            try {
                carPropertyManager.registerCallback(callback, property.id, rate)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopListening() = carPropertyManager.unregisterCallback(callback)

    //Les propriétés continues envoient beaucoup de valeurs : on regroupe les rafraîchissements (2 par seconde max)
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
                    .setTitle("VHAL")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )

        val missingPermissions = runtimePermissions.filter {
            ContextCompat.checkSelfPermission(carContext, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            builder.addSectionedList(
                section(
                    "Permissions",
                    Row.Builder()
                        .setTitle("Autoriser l'accès à la vitesse et à l'énergie")
                        .addText(missingPermissions.joinToString { it.substringAfterLast('.') })
                        .setOnClickListener {
                            carContext.requestPermissions(missingPermissions) { _, _ ->
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
                "Car.PROPERTY_SERVICE → CarPropertyManager",
                *properties.map { property ->
                    Row.Builder()
                        .setTitle(property.label)
                        .addText(display(property))
                        .addText(VehiclePropertyIds.toString(property.id))
                        .build()
                }.toTypedArray()
            )
        )

        return builder.build()
    }

    @Suppress("DEPRECATION") //getPropertyStatus n'existe qu'à partir d'Android 14
    private fun display(property: VhalProperty): String {
        if (property.id !in supported) return "Non exposée ou permission manquante"
        val value = values[property.id] ?: return "En attente…"
        val raw = value.value
        return if (value.status == CarPropertyValue.STATUS_AVAILABLE && raw != null) {
            property.format(raw)
        } else {
            "Indisponible"
        }
    }

    private fun section(header: String, vararg rows: Row) =
        SectionedItemList.create(
            ItemList.Builder().apply { rows.forEach { addItem(it) } }.build(),
            header
        )
}

private fun gearName(gear: Int) = when (gear) {
    VehicleGear.GEAR_PARK -> "P"
    VehicleGear.GEAR_REVERSE -> "R"
    VehicleGear.GEAR_NEUTRAL -> "N"
    VehicleGear.GEAR_DRIVE -> "D"
    VehicleGear.GEAR_UNKNOWN -> "Inconnu"
    else -> "Rapport $gear" //Boîte manuelle : GEAR_FIRST, GEAR_SECOND…
}

private fun ignitionName(state: Int) = when (state) {
    VehicleIgnitionState.LOCK -> "Verrouillé"
    VehicleIgnitionState.OFF -> "Coupé"
    VehicleIgnitionState.ACC -> "Accessoires"
    VehicleIgnitionState.ON -> "Mis"
    VehicleIgnitionState.START -> "Démarrage"
    else -> "Inconnu"
}
