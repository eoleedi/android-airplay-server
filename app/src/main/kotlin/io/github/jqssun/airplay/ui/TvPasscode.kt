package io.github.jqssun.airplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jqssun.airplay.R

@Composable
internal fun TvPasscodeDialog(pin: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(
                Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 48.dp, vertical = 27.dp),
                contentAlignment = Alignment.Center
            ) {
                val count = pin.length.coerceAtLeast(1)
                val tileWidth = ((maxWidth - 12.dp * (count - 1)) / count).coerceIn(1.dp, 80.dp)
                val description = stringResource(R.string.tv_passcode_digits, pin.toList().joinToString(" "))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.dialog_pin_title), style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.tv_passcode_instruction), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(40.dp))
                    Row(
                        Modifier.clearAndSetSemantics { contentDescription = description },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pin.forEach { digit ->
                            Surface(Modifier.size(tileWidth, tileWidth * 1.6f),
                                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(digit.toString(), fontSize = (tileWidth.value * 0.8f).sp,
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
