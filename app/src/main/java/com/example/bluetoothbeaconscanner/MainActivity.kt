@file:Suppress("DEPRECATION")

package com.example.bluetoothbeaconscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.bluetoothbeaconscanner.ui.theme.BluetoothBeaconScannerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.altbeacon.beacon.*

class MainActivity : ComponentActivity(), BeaconConsumer {

    private lateinit var beaconManager: BeaconManager

    // ViewModel to hold beacon data
    private val beaconViewModel: BeaconViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission
        requestLocationPermission()

        // Initialize BeaconManager
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )
        beaconManager.bind(this)

        setContent {
            BluetoothBeaconScannerTheme {
                val beacons by beaconViewModel.beacons.collectAsState()

                // Main content
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BeaconScannerScreen(
                        beacons = beacons,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // Request Location Permission for Bluetooth
    private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    // Callback for BeaconService connection
    override fun onBeaconServiceConnect() {
        beaconManager.addRangeNotifier { beacons: Collection<Beacon>, _: Region ->
            if (beacons.isNotEmpty()) {
                // Pass beacon data to ViewModel
                beaconViewModel.updateBeacons(beacons.toList())
            }
        }

        try {
            // Start ranging for beacons
            beaconManager.startRangingBeaconsInRegion(Region("all-beacons-region", null, null, null))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }
}

// ViewModel to manage beacons list
class BeaconViewModel : ViewModel() {
    private val _beacons = MutableStateFlow<List<Beacon>>(emptyList())
    val beacons: StateFlow<List<Beacon>> = _beacons

    fun updateBeacons(newBeacons: List<Beacon>) {
        _beacons.value = newBeacons
    }
}

@Composable
fun BeaconScannerScreen(beacons: List<Beacon>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Detected Beacons:", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(8.dp))

        for (beacon in beacons) {
            Text(text = "Beacon: ${beacon.id1}, RSSI: ${beacon.rssi}")
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (beacons.isEmpty()) {
            Text(text = "No beacons detected.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothBeaconScannerTheme {
        BeaconScannerScreen(beacons = listOf())
    }
}
