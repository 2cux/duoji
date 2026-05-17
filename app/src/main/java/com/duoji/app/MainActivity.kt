package com.duoji.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.duoji.app.ui.navigation.DuoJiNavGraph
import com.duoji.app.ui.theme.DuoJiTheme
import com.duoji.app.ui.theme.WarmBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DuoJiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WarmBackground
                ) {
                    val navController = rememberNavController()
                    DuoJiNavGraph(navController = navController)
                }
            }
        }
    }
}
