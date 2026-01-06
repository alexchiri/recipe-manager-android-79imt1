package com.kroslabs.recipemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kroslabs.recipemanager.ui.navigation.MainNavigation
import com.kroslabs.recipemanager.ui.theme.RecipeManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeManagerTheme {
                MainNavigation()
            }
        }
    }
}
