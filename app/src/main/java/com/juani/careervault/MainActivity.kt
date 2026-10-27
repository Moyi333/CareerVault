package com.juani.careervault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CvTheme {
                Surface(Modifier.fillMaxSize(), color = CvBg) { AppRoot() }
            }
        }
    }
}

private fun NavBackStackEntry.id(): Long = arguments!!.getLong("id")
private val idArg = listOf(navArgument("id") { type = NavType.LongType })

@Composable
fun AppRoot(vm: AppVm = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf(
        Triple("careers", "Carreras", "🏟️"),
        Triple("squads", "Plantillas", "📋"),
        Triple("plans", "Gestión", "💼")
    )
    Scaffold(
        containerColor = CvBg,
        bottomBar = {
            if (tabs.any { it.first == route }) {
                NavigationBar(containerColor = CvSurface) {
                    tabs.forEach { (r, name, emoji) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = {
                                nav.navigate(r) {
                                    popUpTo("careers") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(emoji, fontSize = 20.sp) },
                            label = { Text(name) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CvGreen.copy(alpha = 0.2f),
                                selectedTextColor = CvGreen,
                                unselectedTextColor = CvMuted
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "careers", modifier = Modifier.padding(pad)) {
            composable("careers") { CareersTab(vm, nav) }
            composable("squads") { SquadsTab(vm, nav) }
            composable("plans") { PlansTab(vm) }
            composable("career/{id}", arguments = idArg) { CareerScreen(vm, it.id(), nav) }
            composable("awards/{id}", arguments = idArg) { AwardsScreen(vm, it.id(), nav) }
            composable("stint/{id}", arguments = idArg) { StintScreen(vm, it.id(), nav) }
            composable("transfers/{id}", arguments = idArg) { TransfersScreen(vm, it.id(), nav) }
            composable("titles/{id}", arguments = idArg) { TitlesScreen(vm, it.id(), nav) }
            composable("finals/{id}", arguments = idArg) { FinalsScreen(vm, it.id(), nav) }
            composable("derbies/{id}", arguments = idArg) { DerbiesScreen(vm, it.id(), nav) }
            composable("rival/{id}", arguments = idArg) { RivalScreen(vm, it.id(), nav) }
            composable("promoted/{id}", arguments = idArg) { PromotedScreen(vm, it.id(), nav) }
            composable("retired/{id}", arguments = idArg) { RetiredScreen(vm, it.id(), nav) }
            composable("stats/{id}", arguments = idArg) { StatsScreen(vm, it.id(), nav) }
            composable("stat/{id}", arguments = idArg) { StatPlayerScreen(vm, it.id(), nav) }
            composable("squad/{id}", arguments = idArg) { SquadEditor(vm, it.id(), nav) }
        }
    }
}
