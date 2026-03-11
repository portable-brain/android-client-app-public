package com.portablebrain.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.portablebrain.client.auth.AuthViewModel
import com.portablebrain.client.ui.LoginScreen
import com.portablebrain.client.ui.MainScreen
import com.portablebrain.client.ui.theme.PortablebrainandroidclientTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            PortablebrainandroidclientTheme {
                val currentUser by authViewModel.currentUser.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (currentUser == null) {
                        LoginScreen(
                            viewModel = authViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        MainScreen(
                            viewModel = mainViewModel,
                            onSignOut = { authViewModel.signOut() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check accessibility status when returning from Settings
        mainViewModel.checkAccessibilityEnabled(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
