package io.github.jqssun.airplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.service.AirPlayService.ServerState
import io.github.jqssun.airplay.viewmodel.LocalNetworkAccess
import io.github.jqssun.airplay.viewmodel.MainViewModel

@Composable
internal fun TvAppLayout(
    selected: Int,
    labels: List<String>,
    icons: List<ImageVector>,
    onSelect: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val pageFocus = remember(labels.size) { List(labels.size) { FocusRequester() } }
    val homeFocus = pageFocus.first()
    var sidebarExpanded by remember { mutableStateOf(true) }
    BackHandler(!sidebarExpanded || selected != 0) {
        if (!sidebarExpanded) {
            pageFocus[selected].requestFocus()
        } else {
            // Move focus before changing pages; the focus listener selects Overview.
            homeFocus.requestFocus()
        }
    }
    LaunchedEffect(Unit) { homeFocus.requestFocusUntilLanded() }
    Surface(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().safeDrawingPadding().padding(start = 24.dp, end = 48.dp, top = 27.dp, bottom = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                Modifier.width(if (sidebarExpanded) 156.dp else 64.dp).fillMaxHeight()
                    .onFocusChanged { sidebarExpanded = it.hasFocus }
                    .focusProperties { onEnter = { pageFocus[selected].requestFocus() } }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.height(112.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Cast, null, Modifier.padding(start = 16.dp).size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    if (sidebarExpanded) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                labels.forEachIndexed { index, label ->
                    val shape = RoundedCornerShape(12.dp)
                    val focus = Modifier.focusRequester(pageFocus[index])
                    FilledTonalButton(
                        onClick = { onSelect(index) },
                        shape = shape,
                        modifier = focus.fillMaxWidth().heightIn(min = 60.dp)
                            .onFocusChanged { if (it.isFocused) onSelect(index) }
                            .dpadFocus(shape)
                            .semantics {
                                this.selected = selected == index
                                if (!sidebarExpanded) contentDescription = label
                            },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selected == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (selected == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Icon(icons[index], null, Modifier.size(24.dp))
                        if (sidebarExpanded) {
                            Spacer(Modifier.width(12.dp))
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().focusGroup()) { content() }
        }
    }
}

@Composable
internal fun TvOverviewContent(
    viewModel: MainViewModel,
    video: @Composable () -> Unit,
    onFullscreen: () -> Unit,
    onLocalNetwork: () -> Unit,
    showAudioMode: Boolean,
    nowPlaying: @Composable () -> Unit
) {
    val state by viewModel.serverState.collectAsState()
    val network by viewModel.localNetwork.collectAsState()
    val name by viewModel.pref(Prefs.SERVER_NAME).collectAsState()
    val connections by viewModel.connectionCount.collectAsState()
    val mirroring by viewModel.mirroringActive.collectAsState()
    val grant = state != ServerState.RUNNING && network != LocalNetworkAccess.GRANTED
    val running = state == ServerState.RUNNING
    val status = stringResource(when {
        grant -> R.string.local_network_label
        state == ServerState.ERROR -> R.string.error_starting_server
        running && connections > 0 -> R.string.tv_receiving
        running -> R.string.tv_ready
        else -> R.string.server_stopped
    })
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp,
                if (running && connections > 0) Alignment.Start else Alignment.CenterHorizontally)) {
            Icon(if (running) Icons.Default.CastConnected else Icons.Default.Cast, null,
                tint = if (grant || state == ServerState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Text(status, style = MaterialTheme.typography.titleMedium)
        }
        if (running && connections > 0) {
            Surface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        showAudioMode -> nowPlaying()
                        mirroring -> video()
                        else -> Text(stringResource(R.string.waiting_for_playback), Modifier.padding(24.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (mirroring) {
                    Button(onClick = onFullscreen, modifier = Modifier.heightIn(min = 56.dp).dpadFocus(ButtonDefaults.shape)) {
                        Icon(Icons.Default.Fullscreen, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.cd_fullscreen))
                    }
                }
                TvServerButton(running, grant, network, onLocalNetwork, viewModel)
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.tv_connect_to), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(name, style = MaterialTheme.typography.displaySmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                TvNetworkInfo()
                if (grant || !running) {
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(if (grant) R.string.local_network_desc else R.string.tv_start_hint),
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TvServerButton(running, grant, network, onLocalNetwork, viewModel)
                Text(stringResource(if (running) R.string.tv_background_hint else R.string.tv_start_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TvServerButton(
    running: Boolean, grant: Boolean, network: LocalNetworkAccess,
    onLocalNetwork: () -> Unit, viewModel: MainViewModel
) {
    val label = when {
        grant && network == LocalNetworkAccess.SETTINGS -> R.string.btn_open_settings
        grant -> R.string.btn_allow
        running -> R.string.btn_stop
        else -> R.string.btn_start
    }
    FilledTonalButton(
        onClick = { if (grant) onLocalNetwork() else if (running) viewModel.stopServer() else viewModel.startServer() },
        modifier = Modifier.heightIn(min = 56.dp).dpadFocus(ButtonDefaults.shape),
        colors = if (running && !grant) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(if (grant) Icons.Default.LockOpen else if (running) Icons.Default.Stop else Icons.Default.PlayArrow, null)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(label))
    }
}

internal val tvSettingSections = listOf(
    R.string.section_server, R.string.section_connection, R.string.section_display,
    R.string.section_decode, R.string.section_developer
)

@Composable
internal fun TvSettingsScreen(viewModel: MainViewModel) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    val sectionFocus = remember { List(tvSettingSections.size) { FocusRequester() } }
    Column(
        Modifier.fillMaxSize()
            .focusProperties { onEnter = { sectionFocus[section].requestFocus() } }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(
                Modifier.width(164.dp).fillMaxHeight().verticalScroll(rememberScrollState())
                    .focusProperties {
                        onEnter = {
                            // Returning from options must not select the nearest tab by height.
                            if (requestedFocusDirection == FocusDirection.Left) {
                                sectionFocus[section].requestFocus()
                            }
                        }
                    }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tvSettingSections.forEachIndexed { index, label ->
                    FilledTonalButton(
                        onClick = { section = index },
                        modifier = Modifier.focusRequester(sectionFocus[index]).fillMaxWidth().heightIn(min = 56.dp)
                            .onFocusChanged { if (it.isFocused) section = index }
                            .dpadFocus()
                            .semantics { selected = section == index },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (section == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = if (section == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(stringResource(label), Modifier.fillMaxWidth()) }
                }
            }
            Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                // Recreate scroll position for each category, without moving focus away from its tab.
                key(section) { SettingsScreen(viewModel, section) }
            }
        }
    }
}
