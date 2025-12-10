package com.therangerstar.animation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimationDetailPage(
    viewModel: AttractorViewModel,
    initialAttractor: AttractorType,
    onBack: () -> Unit
) {
    val attractors = AttractorType.values()
    val initialPage = attractors.indexOf(initialAttractor).coerceAtLeast(0)
    
    // Create pager state with initial page
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { attractors.size }
    )
    
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val attractor = attractors[page]
            val setting = settings[attractor.name]
            
            StrangeParticleAnimation(
                attractor = attractor,
                isInteractive = true,
                onBack = onBack,
                overrideHue = setting?.hue,
                overrideSaturation = setting?.saturation,
                overrideSpeed = setting?.speed,
                overrideParticleCount = setting?.particleCount,
                onSaveColor = { h, s -> 
                    // Should be replaced by onSaveSettings call for full save
                    // But keeping it if color only change happens
                    val currentSpeed = setting?.speed ?: 1.0f
                    val currentCount = setting?.particleCount ?: 8000
                    viewModel.saveSettings(attractor.name, h, s, currentSpeed, currentCount)
                },
                onSaveSettings = { h, s, speed, count ->
                    viewModel.saveSettings(attractor.name, h, s, speed, count)
                },
                contentPadding = innerPadding
            )
        }
    }
}
