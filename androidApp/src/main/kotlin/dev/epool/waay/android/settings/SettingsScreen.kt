package dev.epool.waay.android.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.epool.waay.android.ui.ObserveAsEvents
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.settings.presentation.SettingsAction
import dev.epool.waay.settings.presentation.SettingsEvent
import dev.epool.waay.settings.presentation.SettingsState
import dev.epool.waay.settings.presentation.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

/** Stateful entry: obtains the ViewModel, collects state and events, performs navigation. */
@Composable
fun SettingsRoot(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onBack()
        }
    }
    SettingsScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}

/** Stateless, previewable screen: renders [state] and forwards actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.backLabel)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Options are added per user story (voice: US3, card count: US4, language: US5).
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    WaayTheme {
        SettingsScreen(state = SettingsState(title = "Settings", backLabel = "Back"), onAction = {})
    }
}
