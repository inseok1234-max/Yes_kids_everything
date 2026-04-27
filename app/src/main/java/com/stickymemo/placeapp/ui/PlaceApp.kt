package com.stickymemo.placeapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stickymemo.placeapp.data.AppDatabase
import com.stickymemo.placeapp.data.PlaceRepository
import com.stickymemo.placeapp.ui.navigation.Routes
import com.stickymemo.placeapp.ui.screen.PlaceDetailScreen
import com.stickymemo.placeapp.ui.screen.PlaceFormScreen
import com.stickymemo.placeapp.ui.screen.PlaceListScreen
import com.stickymemo.placeapp.ui.screen.VisitFormScreen
import com.stickymemo.placeapp.ui.viewmodel.PlaceDetailViewModel
import com.stickymemo.placeapp.ui.viewmodel.PlaceDetailViewModelFactory
import com.stickymemo.placeapp.ui.viewmodel.PlaceFormViewModel
import com.stickymemo.placeapp.ui.viewmodel.PlaceFormViewModelFactory
import com.stickymemo.placeapp.ui.viewmodel.PlaceListViewModel
import com.stickymemo.placeapp.ui.viewmodel.PlaceListViewModelFactory

@Composable
fun PlaceApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember {
        PlaceRepository(AppDatabase.getInstance(context).placeDao())
    }

    NavHost(navController = navController, startDestination = Routes.PlaceList.route) {
        composable(Routes.PlaceList.route) {
            val vm: PlaceListViewModel = viewModel(factory = PlaceListViewModelFactory(repository))
            PlaceListScreen(
                viewModel = vm,
                onAdd = { navController.navigate(Routes.PlaceForm.create()) },
                onClick = { id -> navController.navigate(Routes.PlaceDetail.create(id)) },
                onEdit = { id -> navController.navigate(Routes.PlaceForm.create(id)) }
            )
        }

        composable(
            route = Routes.PlaceForm.route,
            arguments = listOf(navArgument("placeId") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            val vm: PlaceFormViewModel = viewModel(factory = PlaceFormViewModelFactory(repository))
            PlaceFormScreen(
                placeId = entry.arguments?.getLong("placeId") ?: 0L,
                repository = repository,
                viewModel = vm,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PlaceDetail.route,
            arguments = listOf(navArgument("placeId") { type = NavType.LongType })
        ) { entry ->
            val placeId = entry.arguments?.getLong("placeId") ?: 0L
            val vm: PlaceDetailViewModel = viewModel(factory = PlaceDetailViewModelFactory(placeId, repository))
            PlaceDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onAddVisit = { navController.navigate(Routes.VisitForm.create(placeId)) }
            )
        }

        composable(
            route = Routes.VisitForm.route,
            arguments = listOf(navArgument("placeId") { type = NavType.LongType })
        ) { entry ->
            VisitFormScreen(
                placeId = entry.arguments?.getLong("placeId") ?: 0L,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
