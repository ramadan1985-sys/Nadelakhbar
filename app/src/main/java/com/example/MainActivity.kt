package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.AdvancedSearchScreen
import com.example.ui.screens.ArticleReaderScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CountriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WireAgenciesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NewsViewModel
import com.example.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for authentic Arabic news reader experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NewsApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NewsApp(viewModel: NewsViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    BackHandler(enabled = currentScreen != ScreenDestination.HOME) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != ScreenDestination.ARTICLE_READER) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.HOME,
                        onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Feed, contentDescription = "اليوم") },
                        label = { Text("أسلاك اليوم", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_home"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.COUNTRIES,
                        onClick = { viewModel.navigateTo(ScreenDestination.COUNTRIES) },
                        icon = { Icon(Icons.Default.Public, contentDescription = "الدول") },
                        label = { Text("الدول", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_countries"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.ADVANCED_SEARCH,
                        onClick = { viewModel.navigateTo(ScreenDestination.ADVANCED_SEARCH) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "بحث متقدم") },
                        label = { Text("بحث متقدم", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_search"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.BOOKMARKS,
                        onClick = { viewModel.navigateTo(ScreenDestination.BOOKMARKS) },
                        icon = { Icon(if (currentScreen == ScreenDestination.BOOKMARKS) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "المحفوظات") },
                        label = { Text("المحفوظات", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_bookmarks"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.AGENCIES,
                        onClick = { viewModel.navigateTo(ScreenDestination.AGENCIES) },
                        icon = { Icon(Icons.Default.Hub, contentDescription = "الوكالات") },
                        label = { Text("الوكالات", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_agencies"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
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
                ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
                ScreenDestination.COUNTRIES -> CountriesScreen(viewModel = viewModel)
                ScreenDestination.ADVANCED_SEARCH -> AdvancedSearchScreen(viewModel = viewModel)
                ScreenDestination.ARTICLE_READER -> ArticleReaderScreen(viewModel = viewModel)
                ScreenDestination.BOOKMARKS -> BookmarksScreen(viewModel = viewModel)
                ScreenDestination.AGENCIES -> WireAgenciesScreen(viewModel = viewModel)
            }
        }
    }
}
