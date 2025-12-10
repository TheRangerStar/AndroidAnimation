package com.therangerstar.animation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.therangerstar.animation.data.AppDatabase
import com.therangerstar.animation.ui.theme.RangerTestTheme
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
data class Detail(val attractorName: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.attractorDao()
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        window.setBackgroundDrawableResource(android.R.color.black)
        
        setContent {
            RangerTestTheme {
                // Manual backstack management for Navigation 3
                val backStack = remember { mutableStateListOf<Any>(Home) }
                
                BackHandler(enabled = backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
                
                NavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider {
                        entry<Home> {
                            HomeScreen(
                                dao = dao,
                                onAttractorSelected = { attractor ->
                                    backStack.add(Detail(attractor.name))
                                }
                            )
                        }
                        
                        entry<Detail> { detail ->
                            val attractor = AttractorType.valueOf(detail.attractorName)
                            
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = androidx.compose.ui.graphics.Color.Black
                            ) { innerPadding ->
                                StrangeParticleAnimation(
                                    attractor = attractor,
                                    contentPadding = innerPadding,
                                    isInteractive = true,
                                    dao = dao,
                                    onBack = { 
                                        if (backStack.isNotEmpty()) {
                                            backStack.removeAt(backStack.lastIndex) 
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
