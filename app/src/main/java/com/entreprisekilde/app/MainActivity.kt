package com.entreprisekilde.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.entreprisekilde.app.ui.navigation.EntreprisekildeApp
import com.entreprisekilde.app.ui.theme.EntreprisekildeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EntreprisekildeTheme {
                EntreprisekildeApp()
            }
        }
    }
}