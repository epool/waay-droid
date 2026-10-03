package dev.epool.waay.android.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.epool.waay.android.ui.ObserveAsEvents
import dev.epool.waay.android.ui.theme.WaayTheme
import dev.epool.waay.settings.presentation.CardCountOptionUi
import dev.epool.waay.settings.presentation.LanguageChoiceUi
import dev.epool.waay.settings.presentation.LanguageOptionUi
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            VoiceRow(state = state, onAction = onAction)
            CardCountSection(state = state, onAction = onAction)
            LanguageSection(state = state, onAction = onAction)
        }
    }
}

/** Whole row is the toggle target (accessible as a switch, FR-025). */
@Composable
private fun VoiceRow(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(
                    value = state.voiceEnabled,
                    role = Role.Switch,
                    onValueChange = { onAction(SettingsAction.OnVoiceToggle(it)) },
                ).padding(horizontal = 16.dp)
                .testTag("settings.voice"),
    ) {
        Text(state.voiceLabel, modifier = Modifier.weight(1f))
        Switch(checked = state.voiceEnabled, onCheckedChange = null)
    }
}

/** Single-choice group of 3–7 cards (FR-017); each row is one accessible radio button. */
@Composable
private fun CardCountSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        Text(
            text = state.cardCountLabel,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        state.cardCountOptions.forEach { option ->
            val selected = option.value == state.selectedCardCount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onAction(SettingsAction.OnCardCountSelect(option.value)) },
                        ).padding(horizontal = 16.dp)
                        .testTag("settings.cardCount.${option.value}"),
            ) {
                RadioButton(selected = selected, onClick = null)
                Text(option.label, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

/** Single-choice group: device language, English, Español (FR-021). */
@Composable
private fun LanguageSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        Text(
            text = state.languageLabel,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        state.languageOptions.forEach { option ->
            val selected = option.choice == state.selectedLanguage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onAction(SettingsAction.OnLanguageSelect(option.choice)) },
                        ).padding(horizontal = 16.dp)
                        .testTag("settings.language.${option.choice.name}"),
            ) {
                RadioButton(selected = selected, onClick = null)
                Text(option.label, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    WaayTheme {
        SettingsScreen(
            state =
                SettingsState(
                    title = "Settings",
                    backLabel = "Back",
                    voiceLabel = "Magician's voice",
                    voiceEnabled = true,
                    cardCountLabel = "Number of cards",
                    cardCountOptions = (3..7).map { CardCountOptionUi(it, "$it cards (1–${(1 shl it) - 1})") },
                    selectedCardCount = 5,
                    languageLabel = "Language",
                    languageOptions =
                        listOf(
                            LanguageOptionUi(LanguageChoiceUi.Device, "Device language"),
                            LanguageOptionUi(LanguageChoiceUi.English, "English"),
                            LanguageOptionUi(LanguageChoiceUi.Spanish, "Español"),
                        ),
                    selectedLanguage = LanguageChoiceUi.Device,
                ),
            onAction = {},
        )
    }
}
