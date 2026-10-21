package com.amonteiro.presentationaaos.car.screens

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Accelerometer
import androidx.car.app.hardware.info.CarHardwareLocation
import androidx.car.app.hardware.info.CarSensors
import androidx.car.app.hardware.info.Compass
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.EnergyProfile
import androidx.car.app.hardware.info.Gyroscope
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Speed
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

//Démo de carContext.getCarService : services de la Car App Library, qui passent par l'hôte
//Sur AAOS, CarInfo lit le VHAL (via CarPropertyManager), mais CarSensors n'est pas implémenté
class CarServiceScreen(carContext: CarContext) : Screen(carContext) {

    private val carHardwareManager = carContext.getCarService(CarHardwareManager::class.java)
    private val executor = ContextCompat.getMainExecutor(carContext)

    //Permissions dangereuses à demander à l'exécution (les autres sont accordées à l'installation)
    private val runtimePermissions = listOf(
        "android.car.permission.CAR_SPEED",
        "android.car.permission.CAR_ENERGY"
    )

    //Dernières valeurs reçues, null tant que l'hôte n'a rien envoyé
    private var energyProfile: EnergyProfile? = null
    private var speed: Speed? = null
    private var energyLevel: EnergyLevel? = null
    private var mileage: Mileage? = null
    private var accelerometer: Accelerometer? = null
    private var gyroscope: Gyroscope? = null
    private var compass: Compass? = null
    private var location: CarHardwareLocation? = null

    private val speedListener = OnCarDataAvailableListener<Speed> { speed = it; refresh() }
    private val energyLevelListener = OnCarDataAvailableListener<EnergyLevel> { energyLevel = it; refresh() }
    private val mileageListener = OnCarDataAvailableListener<Mileage> { mileage = it; refresh() }
    private val accelerometerListener = OnCarDataAvailableListener<Accelerometer> { accelerometer = it; refresh() }
    private val gyroscopeListener = OnCarDataAvailableListener<Gyroscope> { gyroscope = it; refresh() }
    private val compassListener = OnCarDataAvailableListener<Compass> { compass = it; refresh() }
    private val locationListener = OnCarDataAvailableListener<CarHardwareLocation> { location = it; refresh() }

    init {
        //On n'écoute la voiture que lorsque l'écran est visible
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = startListening()
            override fun onStop(owner: LifecycleOwner) = stopListening()
        })
    }

    private fun startListening() {
        val carInfo = carHardwareManager.carInfo
        val carSensors = carHardwareManager.carSensors
        val rate = CarSensors.UPDATE_RATE_NORMAL

        //Valeur fixe : lue une seule fois
        safe { carInfo.fetchEnergyProfile(executor) { energyProfile = it; refresh() } }

        //Valeurs qui évoluent : abonnement
        safe { carInfo.addSpeedListener(executor, speedListener) }
        safe { carInfo.addEnergyLevelListener(executor, energyLevelListener) }
        safe { carInfo.addMileageListener(executor, mileageListener) }
        safe { carSensors.addAccelerometerListener(rate, executor, accelerometerListener) }
        safe { carSensors.addGyroscopeListener(rate, executor, gyroscopeListener) }
        safe { carSensors.addCompassListener(rate, executor, compassListener) }
        safe { carSensors.addCarHardwareLocationListener(rate, executor, locationListener) }
    }

    private fun stopListening() {
        val carInfo = carHardwareManager.carInfo
        val carSensors = carHardwareManager.carSensors

        safe { carInfo.removeSpeedListener(speedListener) }
        safe { carInfo.removeEnergyLevelListener(energyLevelListener) }
        safe { carInfo.removeMileageListener(mileageListener) }
        safe { carSensors.removeAccelerometerListener(accelerometerListener) }
        safe { carSensors.removeGyroscopeListener(gyroscopeListener) }
        safe { carSensors.removeCompassListener(compassListener) }
        safe { carSensors.removeCarHardwareLocationListener(locationListener) }
    }

    //Sans la permission, l'abonnement lève une SecurityException
    private fun safe(block: () -> Unit) {
        try {
            block()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
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
                    .setTitle("getCarService")
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
                "CarHardwareManager • CarInfo",
                row(
                    "Vitesse",
                    speed?.displaySpeedMetersPerSecond.display { "%.0f km/h".format(it * 3.6f) }
                ),
                row(
                    "Niveau d'énergie",
                    "Batterie : ${energyLevel?.batteryPercent.display { "%.0f %%".format(it) }}" +
                            " — carburant : ${energyLevel?.fuelPercent.display { "%.0f %%".format(it) }}",
                    "Autonomie : ${energyLevel?.rangeRemainingMeters.display { "%.0f km".format(it / 1000) }}" +
                            " — réserve : ${energyLevel?.energyIsLow.display()}"
                ),
                row(
                    "Profil énergétique",
                    "Carburants : ${energyProfile?.fuelTypes.display()}",
                    "Prises : ${energyProfile?.evConnectorTypes.display()}"
                ),
                row(
                    "Kilométrage",
                    mileage?.odometerMeters.display { "%.0f km".format(it / 1000) }
                )
            )
        )

        //Pas un manager : infos sur l'hôte portées directement par le CarContext
        builder.addSectionedList(
            section(
                "CarContext",
                row(
                    "Hôte : ${carContext.hostInfo?.packageName}",
                    "Car API level ${carContext.carAppApiLevel}",
                    "Mode sombre : ${carContext.isDarkMode}"
                )
            )
        )

        builder.addSectionedList(
            section(
                "ScreenManager",
                row(
                    "Pile d'écrans : ${screenManager.stackSize}",
                    "Sommet : ${screenManager.top.javaClass.simpleName}"
                )
            )
        )

        builder.addSectionedList(section("ConstraintManager", constraintsRow()))

        builder.addSectionedList(
            section(
                "CarHardwareManager • CarSensors",
                row("Accéléromètre (m/s²)", accelerometer?.forces.display { it.format() }),
                row("Gyroscope (rad/s)", gyroscope?.rotations.display { it.format() }),
                row("Boussole (°)", compass?.orientations.display { it.format() }),
                row(
                    "Position",
                    location?.location.display { "%.5f, %.5f".format(it.latitude, it.longitude) }
                )
            )
        )

        return builder.build()
    }

    //Limites imposées par la voiture : elles changent selon l'hôte et l'état de conduite
    private fun constraintsRow(): Row {
        if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_2) {
            return row("Limites de contenu", "Car API level 2 requis")
        }
        val constraintManager = carContext.getCarService(ConstraintManager::class.java)
        val list = constraintManager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        val grid = constraintManager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID)
        val pane = constraintManager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PANE)
        val refresh = if (carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_6) {
            constraintManager.isAppDrivenRefreshEnabled.toString()
        } else {
            "Car API level 6 requis"
        }
        return row(
            "Limites de contenu",
            "Liste : $list — grille : $grid — panneau : $pane",
            "Rafraîchissement libre : $refresh"
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

//Une CarValue porte un statut : la donnée peut ne pas exister sur ce véhicule
private fun <T> CarValue<T>?.display(format: (T) -> String = { it.toString() }): String {
    val value = this?.value
    return when {
        this == null -> "En attente…"
        status == CarValue.STATUS_SUCCESS && value != null -> format(value)
        status == CarValue.STATUS_UNIMPLEMENTED -> "Non implémenté"
        status == CarValue.STATUS_UNAVAILABLE -> "Indisponible"
        else -> "Inconnu"
    }
}

private fun List<Float>.format() = joinToString(" / ") { "%.1f".format(it) }
