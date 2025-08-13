package com.example.scansheet

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable

import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scansheet.upload.UploadScreen

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavigation() {
    // Cria e lembra do NavController. Ele é o cérebro da navegação.
    val navController = rememberNavController()

    // NavHost é o container que vai exibir a tela correspondente à rota atual.
    NavHost(navController = navController, startDestination = "home_screen") {

        composable(route = "home_screen") {
            HomeView(navController = navController)
        }

        composable(route = "ficha_selection_screen") {
            FichaSelectionScreen(navController = navController)
        }

        composable(route = "upload_step_screen/{selectedFicha}") { backStackEntry ->
            val selectedFicha = backStackEntry.arguments?.getString("selectedFicha") ?: "outros"
            UploadScreen(navController = navController, selectedFicha = selectedFicha)
        }

        composable(route = "export_results_screen") {
            ExportResultsScreen(navController = navController)

        }
    }
}