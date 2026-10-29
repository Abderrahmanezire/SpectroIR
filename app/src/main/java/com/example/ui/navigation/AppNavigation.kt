package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.analyze.AnalyzeScreen
import com.example.ui.screens.analyze.AnalyzeViewModel
import com.example.ui.screens.dictionary.DictionaryScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.reference.ReferenceScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    analyzeViewModel: AnalyzeViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var dictionarySearchQuery by rememberSaveable { mutableStateOf("") }

    // Android back button handling: navigate back to Home if on sub-screen
    if (currentScreen != Screen.HOME) {
        BackHandler {
            currentScreen = Screen.HOME
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Keep bottom navigation bar sleek and compact (5 primary destinations)
            val primaryNavScreens = listOf(
                Screen.HOME,
                Screen.ANALYZE,
                Screen.DIGITIZE,
                Screen.DICTIONARY,
                Screen.HISTORY
            )
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                primaryNavScreens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.title)
                        },
                        label = {
                            Text(screen.title, fontSize = 10.sp, maxLines = 1)
                        },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    onNavigate = { currentScreen = it },
                    onLoadSpectrum = { spectrum ->
                        analyzeViewModel.loadSpectrum(spectrum)
                    },
                    onImportStream = { stream, fileName ->
                        analyzeViewModel.importFromFileStream(stream, fileName)
                    }
                )

                Screen.ANALYZE -> AnalyzeScreen(
                    viewModel = analyzeViewModel,
                    onNavigateToDictionaryWithQuery = { query ->
                        dictionarySearchQuery = query
                        currentScreen = Screen.DICTIONARY
                    },
                    onNavigateToDigitize = {
                        currentScreen = Screen.DIGITIZE
                    }
                )

                Screen.DIGITIZE -> com.example.ui.screens.digitize.SpectrumDigitizerScreen(
                    onSpectrumDigitized = { spectrum ->
                        analyzeViewModel.loadSpectrum(spectrum)
                        currentScreen = Screen.ANALYZE
                    },
                    onNavigateBack = {
                        currentScreen = Screen.HOME
                    }
                )

                Screen.MANUAL_ANALYSIS -> com.example.ui.screens.manual.ManualAnalysisScreen(
                    currentSpectrum = analyzeViewModel.uiState.value.currentSpectrum,
                    onNavigateToDictionaryWithQuery = { query ->
                        dictionarySearchQuery = query
                        currentScreen = Screen.DICTIONARY
                    }
                )

                Screen.DICTIONARY -> DictionaryScreen(
                    initialSearchQuery = dictionarySearchQuery
                )

                Screen.REFERENCE -> ReferenceScreen(
                    onLoadSpectrum = { spectrum ->
                        analyzeViewModel.loadSpectrum(spectrum)
                    },
                    onCompareReference = { ref ->
                        analyzeViewModel.selectReferenceForComparison(ref)
                        analyzeViewModel.selectTab(com.example.ui.screens.analyze.AnalyzeTab.COMPARE)
                        currentScreen = Screen.ANALYZE
                    },
                    onNavigate = { currentScreen = it }
                )

                Screen.HISTORY -> HistoryScreen(
                    onLoadSpectrum = { spectrum ->
                        analyzeViewModel.loadSpectrum(spectrum)
                    },
                    onNavigate = { currentScreen = it }
                )

                Screen.SETTINGS -> SettingsScreen()
            }
        }
    }
}
