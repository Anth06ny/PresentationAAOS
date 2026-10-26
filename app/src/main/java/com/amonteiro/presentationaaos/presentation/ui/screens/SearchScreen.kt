package com.amonteiro.presentationaaos.presentation.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.amonteiro.presentationaaos.R
import com.amonteiro.presentationaaos.domain.Weather
import com.amonteiro.presentationaaos.presentation.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt


//Composable représentant l'ensemble de l'écran
//showBack : affiche la flèche retour (écran ouvert depuis la version voiture)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    mainViewModel: MainViewModel = koinViewModel()
) {


    val searchText = mainViewModel.searchText.collectAsStateWithLifecycle().value
    val list = mainViewModel.dataList.collectAsStateWithLifecycle().value.filter {
        it.name.contains(searchText, true)
    }
    val errorMessage = mainViewModel.errorMessage.collectAsStateWithLifecycle().value
    val runInProgress = mainViewModel.runInProgress.collectAsStateWithLifecycle().value
    val activity = LocalActivity.current

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .imePadding() //Remonte le contenu au-dessus du clavier
    ) {

        Header(
            searchText = searchText,
            onValueChange = { mainViewModel.updateSearchText(it) },
            onSearch = { mainViewModel.loadWeathers() },
            showBack = showBack,
            //Termine l'activité qui héberge l'écran (MainActivity)
            onBack = { activity?.finish() }
        )

        AnimatedVisibility(runInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(
            visible = errorMessage.isNotBlank(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ErrorBanner(message = errorMessage)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (list.isEmpty() && !runInProgress) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) {
                    PictureRowItem(data = it, modifier = Modifier.animateItem())
                }
            }
        }

        BottomActions(
            onClear = { mainViewModel.updateSearchText("") },
            onLoad = { mainViewModel.loadWeathers() }
        )
    }
}

//En-tête en dégradé contenant le titre et la barre de recherche
@Composable
fun Header(
    searchText: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    showBack: Boolean = false,
    onBack: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.onPrimary
                    )
                }
            }
            Text(
                text = stringResource(R.string.header_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary
            )
        }
        Text(
            text = stringResource(R.string.header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimary.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(20.dp))
        SearchBar(searchText = searchText, onValueChange = onValueChange, onSearch = onSearch)
    }
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    searchText: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit = {}
) {

    TextField(
        value = searchText, //Valeur affichée
        onValueChange = onValueChange, //Nouveau texte entrée
        leadingIcon = { //Image d'icône
            Icon(
                imageVector = Icons.Rounded.Search,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_clear_search)
                    )
                }
            }
        },
        singleLine = true,
        placeholder = { //Texte d'aide qui disparait
            //Pour aller le chercher dans string.xml, R de votre package com.nom.projet
            Text(stringResource(R.string.placeholder_search))
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), // Définir le bouton "Entrée" comme action de recherche
        keyboardActions = KeyboardActions(onSearch = { onSearch() }), // Déclenche l'action définie
        //Comment le composant doit se placer
        modifier = modifier
            .fillMaxWidth() // Prend toute la largeur
            .heightIn(min = 56.dp) //Hauteur minimum
    )
}

@Composable
fun ErrorBanner(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudQueue,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomActions(onClear: () -> Unit, onLoad: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedButton(
                onClick = onClear,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(
                    Icons.Rounded.FilterAltOff,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.bt_clear_filter))
            }

            Button(
                onClick = onLoad,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.load_data))
            }
        }
    }
}

@Composable //Composable affichant 1 élément
fun PictureRowItem(modifier: Modifier = Modifier, data: Weather) {

    var expended by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expended) 180f else 0f, label = "arrow")

    ElevatedCard(
        onClick = { expended = !expended },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            //Permission Internet nécessaire
            AsyncImage(
                model = data.icon,
                contentDescription = stringResource(R.string.cd_weather_icon),
                contentScale = ContentScale.Crop,
                //Image d'échec de chargement qui sera utilisé par la preview
                error = painterResource(R.drawable.error),
                onError = { println(it) },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = data.description.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${data.temp.roundToInt()}°",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
        }

        AnimatedVisibility(expended) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(Icons.Rounded.Air, stringResource(R.string.wind_speed, data.speed))
                    InfoChip(Icons.Rounded.LocationOn, stringResource(R.string.station_id, data.id))
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
