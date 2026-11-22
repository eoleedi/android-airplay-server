package io.github.jqssun.airplay.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val tv = isTv()
    val listFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var previousLogCount by remember { mutableIntStateOf(0) }
    val logsLabel = stringResource(R.string.tab_logs)

    LaunchedEffect(logs.size) {
        val followTail = !tv || previousLogCount == 0 ||
            (!listState.isScrollInProgress &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= previousLogCount - 1)
        previousLogCount = logs.size
        if (logs.isNotEmpty() && followTail) {
            if (tv) listState.scrollToItem(logs.size - 1)
            else listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.thenIf(tv) {
            focusProperties { onEnter = { listFocus.requestFocus() } }.focusGroup()
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_logs)) },
                actions = {
                    IconButton(onClick = viewModel::exportLogs, modifier = Modifier.dpadFocus(CircleShape)) {
                        Icon(Icons.Outlined.Share, stringResource(R.string.btn_export))
                    }
                    IconButton(onClick = viewModel::clearLogs, modifier = Modifier.dpadFocus(CircleShape)) {
                        Icon(Icons.Outlined.Delete, stringResource(R.string.btn_clear))
                    }
                },
            )
        },
    ) { padding ->
        LogsSelectionContainer(tv, Modifier.padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().thenIf(tv) {
                    focusRequester(listFocus)
                        .onPreviewKeyEvent { event ->
                            val direction = when (event.key) {
                                Key.DirectionUp -> -1
                                Key.DirectionDown -> 1
                                else -> return@onPreviewKeyEvent false
                            }
                            val canScroll = if (direction < 0) listState.canScrollBackward else listState.canScrollForward
                            // Let Up reach toolbar actions at the start of the list.
                            if (!canScroll && direction < 0) return@onPreviewKeyEvent false
                            if (event.type == KeyEventType.KeyDown) {
                                scope.launch {
                                    val viewport = listState.layoutInfo.viewportSize.height
                                    listState.scrollBy(direction * viewport.coerceAtLeast(1) / 3f)
                                }
                            }
                            true
                        }
                        .semantics { contentDescription = logsLabel }
                        .dpadFocus()
                        .focusable()
                },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(logs) {
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }
    }
}

@Composable
private fun LogsSelectionContainer(tv: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    // Text selection introduces another focus target that traps remote navigation.
    if (tv) Box(modifier) { content() }
    else SelectionContainer(modifier, content = content)
}
