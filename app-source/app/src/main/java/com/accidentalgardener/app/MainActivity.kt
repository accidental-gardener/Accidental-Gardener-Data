package com.accidentalgardener.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.accidentalgardener.app.ui.AppRoot
import com.accidentalgardener.app.ui.theme.AccidentalGardenerTheme
import com.accidentalgardener.app.work.Notifications
import com.accidentalgardener.app.work.WeatherWorker

class GardenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        WeatherWorker.schedule(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccidentalGardenerTheme {
                AppRoot()
            }
        }
    }
}
