package com.tober.glyphmatrixtools.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.tober.glyphmatrixtools.R

@Composable
fun Navigation(
    selectedDestination: NavigationItem,
    onDestinationSelected: (NavigationItem) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(top = 18.dp)
        ) {
            val itemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.025f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Glyph Matrix Tools Logo",
                    modifier = Modifier.size(30.dp)
                )

                Spacer(modifier = Modifier.width(24.dp))

                Text(
                    text = "Glyph Matrix Tools",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            HorizontalDivider()

            NavigationGroup(
                modifier = Modifier.padding(vertical = 18.dp)
            ) {
                NavigationItemButton(
                    item = NavigationItem.GlyphCanvas,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    colors = itemColors
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationGroup(
                modifier = Modifier.padding(vertical = 18.dp)
            ) {
                NavigationItemButton(
                    item = NavigationItem.ScreenWakeGlyphs,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    colors = itemColors
                )

                NavigationItemButton(
                    item = NavigationItem.NotificationGlyphs,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    colors = itemColors
                )

                NavigationItemButton(
                    item = NavigationItem.CallGlyphs,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    colors = itemColors
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            NavigationGroup(
                modifier = Modifier.padding(vertical = 18.dp)
            ) {
                NavigationItemButton(
                    item = NavigationItem.Settings,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    colors = itemColors
                )
            }
        }
    }
}

@Composable
private fun NavigationGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
private fun NavigationItemButton(
    item: NavigationItem,
    selectedDestination: NavigationItem,
    onDestinationSelected: (NavigationItem) -> Unit,
    colors: NavigationDrawerItemColors
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = null
            )
        },
        label = {
            Text(text = item.title)
        },
        selected = selectedDestination == item,
        onClick = {
            onDestinationSelected(item)
        },
        colors = colors,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
