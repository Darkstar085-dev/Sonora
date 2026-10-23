package com.sipun.sonora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sipun.sonora.navigation.SonoraRoute
import com.sipun.sonora.ui.theme.LocalAccentBrush
import com.sipun.sonora.ui.theme.accentGradientTint

private data class BottomDestination(
    val route: SonoraRoute,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(SonoraRoute.Home, "Home", Icons.Default.Home),
    BottomDestination(SonoraRoute.Favorites, "Favorites", Icons.Default.Favorite),
    BottomDestination(SonoraRoute.Library, "Library", Icons.Default.LibraryMusic),
    BottomDestination(SonoraRoute.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun SonoraBottomBar(
    navController: NavHostController,
    destinations: List<SonoraRoute>,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { route ->
                    val destination = bottomDestinations.first { it.route.route == route.route }
                    val selected = currentRoute == route.route
                    val interactionSource = remember { MutableInteractionSource() }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (selected) Modifier.background(
                                        LocalAccentBrush.current,
                                        MaterialTheme.shapes.large,
                                    ) else Modifier
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {
                                    if (!selected) {
                                        navController.navigate(route.route) {
                                            popUpTo(SonoraRoute.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                .padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .accentGradientTint(),
                            )
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
