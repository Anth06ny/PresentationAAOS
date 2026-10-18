package com.amonteiro.presentationaaos.car

import java.text.Normalizer

//Les 100 plus grandes villes de France métropolitaine (population municipale INSEE), en dur
object FrenchCities {

    val CITIES = listOf(
        "Paris", "Marseille", "Lyon", "Toulouse", "Nice",
        "Nantes", "Montpellier", "Strasbourg", "Bordeaux", "Lille",
        "Rennes", "Toulon", "Reims", "Saint-Étienne", "Le Havre",
        "Villeurbanne", "Dijon", "Grenoble", "Angers", "Saint-Denis",
        "Nîmes", "Aix-en-Provence", "Clermont-Ferrand", "Le Mans", "Brest",
        "Tours", "Amiens", "Annecy", "Limoges", "Metz",
        "Boulogne-Billancourt", "Perpignan", "Besançon", "Orléans", "Rouen",
        "Montreuil", "Caen", "Argenteuil", "Mulhouse", "Nancy",
        "Tourcoing", "Roubaix", "Nanterre", "Vitry-sur-Seine", "Créteil",
        "Avignon", "Poitiers", "Aubervilliers", "Dunkerque", "Aulnay-sous-Bois",
        "Colombes", "Asnières-sur-Seine", "Versailles", "Courbevoie", "Béziers",
        "Rueil-Malmaison", "La Rochelle", "Champigny-sur-Marne", "Pau", "Cherbourg-en-Cotentin",
        "Saint-Maur-des-Fossés", "Antibes", "Mérignac", "Ajaccio", "Cannes",
        "Saint-Nazaire", "Drancy", "Noisy-le-Grand", "Colmar", "Issy-les-Moulineaux",
        "Levallois-Perret", "Calais", "Bourges", "Quimper", "Valence",
        "Vénissieux", "Ivry-sur-Seine", "Pessac", "Cergy", "La Seyne-sur-Mer",
        "Villeneuve-d'Ascq", "Troyes", "Clichy", "Neuilly-sur-Seine", "Niort",
        "Montauban", "Chambéry", "Pantin", "Sarcelles", "Lorient",
        "Le Blanc-Mesnil", "Beauvais", "Hyères", "Maisons-Alfort", "Meaux",
        "Chelles", "Villejuif", "Narbonne", "Épinay-sur-Seine", "Fréjus"
    )

    //Filtre sans tenir compte de la casse, des accents ni des tirets ("saint etienne" -> "Saint-Étienne")
    fun search(query: String): List<String> {
        val normalizedQuery = normalize(query)
        return CITIES.filter { normalize(it).contains(normalizedQuery) }
    }

    private fun normalize(text: String) =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "") //Supprime les accents
            .replace('-', ' ')
            .lowercase()
            .trim()
}
