package com.workplanner.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workplanner.app.ui.home.HomeScreen
import com.workplanner.app.ui.task.TaskDetailScreen

@Composable
fun WorkPlannerApp(
    externalTaskId: String?,
    onExternalTaskHandled: () -> Unit
) {
    val viewModel: WorkViewModel = viewModel()
    val navController = rememberNavController()

    // A tapped reminder notification carries the task id into the activity.
    LaunchedEffect(externalTaskId) {
        val id = externalTaskId ?: return@LaunchedEffect
        if (viewModel.snapshot.value.task(id) != null) {
            navController.navigate("task/$id") {
                launchSingleTop = true
                popUpTo("home")
            }
        }
        onExternalTaskHandled()
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable(route = "home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenTask = { taskId -> navController.navigate("task/$taskId") }
            )
        }
        composable(
            route = "task/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            TaskDetailScreen(
                taskId = entry.arguments?.getString("taskId").orEmpty(),
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
