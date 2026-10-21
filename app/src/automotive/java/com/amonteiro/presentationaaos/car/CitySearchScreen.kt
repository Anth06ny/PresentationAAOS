package com.amonteiro.presentationaaos.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.car.app.versioning.CarAppApiLevels
import com.amonteiro.presentationaaos.R
import com.amonteiro.presentationaaos.data.memory.FrenchCities

//Écran de recherche : propose les grandes villes de France filtrées au fil de la saisie
class CitySearchScreen(carContext: CarContext) : Screen(carContext) {

    private var searchText = ""

    override fun onGetTemplate(): Template {

        val itemListBuilder = ItemList.Builder()
            .setNoItemsMessage(carContext.getString(R.string.car_no_city_match))

//        val listLimit = getListLimit()
//        println("listLimit=$listLimit")

        FrenchCities.search(searchText)
            //.take(getListLimit())
            .forEach { city ->
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(city)
                    .setOnClickListener {
                        screenManager.push(WeatherScreen(carContext, city))
                    }
                    .build()
            )
        }

        return SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            //Appelé à chaque caractère saisi : on rafraîchit les propositions
            override fun onSearchTextChanged(searchText: String) {
                this@CitySearchScreen.searchText = searchText
                invalidate()
            }

            //Validation au clavier : on cherche le texte saisi tel quel
            override fun onSearchSubmitted(searchText: String) {
                if (searchText.isNotBlank()) {
                    screenManager.push(WeatherScreen(carContext, searchText.trim()))
                }
            }
        })
            .setHeaderAction(Action.BACK)
            .setSearchHint(carContext.getString(R.string.placeholder_search))
            .setInitialSearchText(searchText)
            .setShowKeyboardByDefault(false)
            .setItemList(itemListBuilder.build())
            .build()
    }


    //Nombre maximum de lignes autorisé par le véhicule (dépend de l'hôte)
    private fun getListLimit(): Int =
        if (carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_2) {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        } else {
            6
        }
}
