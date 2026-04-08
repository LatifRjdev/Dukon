package com.dokonpro.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dokonpro.android.navigation.AppNavigation
import com.dokonpro.android.ui.theme.DokonProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DokonProTheme {
                AppNavigation()
            }
        }
    }
}
