package dev.epool.waay.android.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.epool.waay.android.adaptive.AnswerStyle
import dev.epool.waay.game.domain.Answer

/**
 * One answer control (spec 002 FR-012, FR-020, FR-021): "Yes" in the primary (filled) style, "No" in
 * the secondary (outlined) style. [AnswerStyle.Button] sits under the card, [AnswerStyle.Panel]
 * flanks it in wide layouts. Tapping throws the card the same way a swipe does (FR-013).
 */
@Composable
fun AnswerControl(
    answer: Answer,
    label: String,
    style: AnswerStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tagged = modifier.testTag(if (answer == Answer.Yes) "card.yes" else "card.no")
    when (style) {
        AnswerStyle.Button -> {
            val buttonModifier = tagged.heightIn(min = 56.dp)
            if (answer == Answer.Yes) {
                Button(onClick = onClick, modifier = buttonModifier) { Text(label, style = MaterialTheme.typography.titleMedium) }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        AnswerStyle.Panel -> {
            val isYes = answer == Answer.Yes
            Surface(
                onClick = onClick,
                modifier = tagged,
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isYes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isYes) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (isYes) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                    Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
