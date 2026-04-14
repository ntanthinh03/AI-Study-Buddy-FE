package com.thinh.aistudybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.thinh.aistudybuddy.data.local.LocalHistoryStore
import com.thinh.aistudybuddy.data.local.NetworkConfigStore
import com.thinh.aistudybuddy.data.local.SessionStore
import com.thinh.aistudybuddy.data.network.RetrofitClient
import com.thinh.aistudybuddy.ui.theme.AppNavigation
import com.thinh.aistudybuddy.ui.theme.FEBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalHistoryStore.initialize(applicationContext)
        RetrofitClient.setBaseUrlOverride(NetworkConfigStore.readBaseUrl(applicationContext))
        val session = SessionStore.readSession(applicationContext)
        RetrofitClient.authToken = if (session.rememberLogin) session.token else null
        if (!session.rememberLogin) {
            SessionStore.clearSession(applicationContext)
        }
        setContent {
            val navController = rememberNavController()
            AppNavigation(
                navController = navController,
                initialDisplayName = session.displayName.orEmpty()
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FEBuddyTheme {
        Greeting("Android")
    }
}