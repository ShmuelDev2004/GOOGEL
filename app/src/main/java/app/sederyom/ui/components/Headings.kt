package app.sederyom.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

/** Text that screen readers announce as a heading, so users can jump between sections. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleMedium) {
    Text(text = text, style = style, modifier = modifier.semantics { heading() })
}
