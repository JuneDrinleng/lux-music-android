package cn.lux.music.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.lux.music.feature.LuxRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {
    private var backConsumer: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backConsumer?.invoke() != true) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
        setContent {
            LuxRoot(onRegisterBackConsumer = { consumer -> backConsumer = consumer })
        }
    }
}
