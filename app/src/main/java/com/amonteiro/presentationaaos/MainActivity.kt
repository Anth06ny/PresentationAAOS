package com.amonteiro.presentationaaos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.amonteiro.presentationaaos.presentation.ui.screens.SearchScreen
import com.amonteiro.presentationaaos.presentation.ui.theme.PresentationAAOSTheme

class MainActivity : ComponentActivity() {

    companion object {
        //Extra de l'intent : true pour afficher la flèche retour (ouverture depuis HomeScreen de la voiture)
        const val EXTRA_SHOW_BACK = "EXTRA_SHOW_BACK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showBack = intent.getBooleanExtra(EXTRA_SHOW_BACK, false)

        enableEdgeToEdge()
        setContent {
            PresentationAAOSTheme {
                //L'écran gère lui-même les insets pour que l'en-tête passe sous la barre de statut
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    SearchScreen(
                        modifier = Modifier.padding(innerPadding),
                        showBack = showBack
                    )


                }
            }
        }
    }
}