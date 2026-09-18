package dev.qmurzik.forpda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.qmurzik.forpda.navigation.ForPdaNavHost
import dev.qmurzik.forpda.ui.theme.ForPdaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForPdaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ForPdaNavHost()
                }
            }
        }
    }
}
