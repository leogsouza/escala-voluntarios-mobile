package br.com.leogsouza.escalav

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import br.com.leogsouza.escalav.ui.AppNavigation
import br.com.leogsouza.escalav.ui.theme.EscalaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EscalaTheme {
                AppNavigation()
            }
        }
    }
}
