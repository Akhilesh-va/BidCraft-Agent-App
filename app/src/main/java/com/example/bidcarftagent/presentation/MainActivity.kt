package com.example.bidcarftagent.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bidcarftagent.presentation.ui.screens.dashboard.DashboardScreen
import com.example.bidcarftagent.presentation.home.HomeScreen
import com.example.bidcarftagent.presentation.ui.screens.login.LoginScreen
import com.example.bidcarftagent.presentation.ui.screens.login.LoginViewModel
import com.example.bidcarftagent.presentation.ui.theme.BidCarftAgentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BidCarftAgentTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        val loginViewModel: LoginViewModel = hiltViewModel()
                        LoginScreen(
                            vm = loginViewModel,
                            onNavigateToHome = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        HomeScreen(
                            onNavigateToSummary = { fileName, srsJson ->
                                val encoded = android.net.Uri.encode(fileName)
                                val route = "srs_summary/$encoded"
                                // Navigate to the summary screen with encoded filename
                                navController.navigate(route)

                                // Add a listener that waits until the specific backStackEntry for this route exists,
                                // then set both fileName and srsJson on its SavedStateHandle.
                                val listener = object : androidx.navigation.NavController.OnDestinationChangedListener {
                                    override fun onDestinationChanged(
                                        controller: androidx.navigation.NavController,
                                        destination: androidx.navigation.NavDestination,
                                        arguments: Bundle?
                                    ) {
                                        try {
                                            val entry = controller.getBackStackEntry(route)
                                            entry.savedStateHandle.set("fileName", fileName)
                                            entry.savedStateHandle.set("srsJson", srsJson)
                                            controller.removeOnDestinationChangedListener(this)
                                        } catch (_: Exception) {
                                            // backStackEntry not ready yet; ignore and wait for next destination change
                                        }
                                    }
                                }
                                navController.addOnDestinationChangedListener(listener)
                            },
                            onNavigateToProfile = {
                                navController.navigate("profile")
                            }
                        )
                    }
                    composable("profile") {
                        com.example.bidcarftagent.presentation.profile.ProfileScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "srs_summary/{fileName}",
                        arguments = listOf(androidx.navigation.navArgument("fileName") { type = androidx.navigation.NavType.StringType })
                    ) {
                        com.example.bidcarftagent.presentation.srs_summary.SrsSummaryScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToAgentProcessing = {
                                navController.navigate("agent_processing")
                            }
                        )
                    }
                    composable("agent_processing") {
                        com.example.bidcarftagent.presentation.agent_processing.AgentProcessingScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
