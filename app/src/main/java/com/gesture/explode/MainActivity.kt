package com.gesture.explode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gesture.explode.ui.theme.GestureExplodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让应用铺满全屏，沉浸式体验
        enableEdgeToEdge()
        setContent {
            // 调用 MD3 主题
            GestureExplodeTheme {
                // Surface 就是我们的基础画布底层
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 我们稍后会在这里加入捕捉手指滑动的 Canvas

                }
            }
        }
    }
}