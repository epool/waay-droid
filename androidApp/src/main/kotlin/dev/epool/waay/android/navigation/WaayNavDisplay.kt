package dev.epool.waay.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.epool.waay.android.game.GameRoot
import dev.epool.waay.android.settings.SettingsRoot

/**
 * App navigation (Navigation 3, ADR-007): a saveable back stack with ViewModels scoped per entry,
 * so the Game ViewModel survives while Settings is on top (FR-016b).
 */
@Composable
fun WaayNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(GameKey)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<GameKey> {
                    GameRoot(
                        onNavigateToSettings = { if (backStack.lastOrNull() != SettingsKey) backStack.add(SettingsKey) },
                    )
                }
                entry<SettingsKey> {
                    SettingsRoot(onBack = { backStack.removeLastOrNull() })
                }
            },
    )
}
