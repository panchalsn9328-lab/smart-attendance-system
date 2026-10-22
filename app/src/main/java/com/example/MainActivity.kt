package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.navigation.SmartAttendanceNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.AttendanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.firebase.FirebaseAttendanceDataSource.logFirebaseDebugStatus("APP_STARTUP")
        viewModel.restoreUserSession(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceWhite
                ) {
                    SmartAttendanceNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Kept for baseline screenshot and unit tests compatibility
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
