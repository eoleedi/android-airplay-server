package io.github.jqssun.airplay.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.jqssun.airplay.R
import kotlinx.coroutines.delay

private data class TvNetwork(val label: Int, val name: String? = null, val wifi: Boolean = false)

@Suppress("DEPRECATION")
private fun readTvNetwork(context: Context): TvNetwork = try {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
    when {
        network == null -> TvNetwork(R.string.tv_network_disconnected)
        network.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            fun WifiInfo?.name() = this?.ssid?.removeSurrounding("\"")
                ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            val name = try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) network.transportInfo as? WifiInfo else null
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                info.name() ?: wifi?.connectionInfo.name()
            } catch (_: SecurityException) {
                null // Keep the optional permission action available when Android redacts the SSID.
            }
            TvNetwork(R.string.tv_network_wifi, name, wifi = true)
        }
        network.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TvNetwork(R.string.tv_network_ethernet)
        else -> TvNetwork(R.string.tv_network_connected)
    }
} catch (_: SecurityException) {
    TvNetwork(R.string.tv_network_unavailable)
}

@Composable
internal fun TvNetworkInfo() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var network by remember(context) { mutableStateOf(readTvNetwork(context)) }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        network = readTvNetwork(context)
    }
    // Refresh on resume (including returning from permission/settings screens) and while visible.
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                network = readTvNetwork(context)
                delay(5000)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (network.wifi) Icons.Default.Wifi else Icons.Default.SettingsEthernet, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.tv_network_label, network.name ?: stringResource(network.label)),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
        if (network.wifi && network.name == null) {
            Text(stringResource(R.string.tv_network_permission_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            TextButton(onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>().firstOrNull()
                val location = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                when {
                    !granted && !(requestedPermission && activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == false) -> {
                        requestedPermission = true
                        permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                    granted && !LocationManagerCompat.isLocationEnabled(location) ->
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    else -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri()))
                }
            }, modifier = Modifier.dpadFocus(ButtonDefaults.textShape)) {
                Text(stringResource(R.string.tv_network_show_name))
            }
        }
    }
}
