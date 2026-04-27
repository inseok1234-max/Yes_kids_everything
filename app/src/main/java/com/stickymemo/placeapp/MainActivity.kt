package com.stickymemo.placeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stickymemo.placeapp.ui.PlaceApp
import com.stickymemo.placeapp.ui.theme.PlaceAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaceAppTheme {
                Surface(modifier = Modifier) {
                    PlaceApp()
                }
            }
        }
    }
}
