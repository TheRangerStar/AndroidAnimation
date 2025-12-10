package com.therangerstar.animation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.therangerstar.animation.data.AttractorDao
import com.therangerstar.animation.data.AttractorSetting
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.GifDecoder
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import coil.decode.ImageDecoderDecoder
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.therangerstar.animation.ui.theme.RangerTestTheme
import kotlin.random.Random
import androidx.compose.runtime.remember

@Composable
fun HomePage(
    viewModel: AttractorViewModel,
    onAttractorSelected: (AttractorType) -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsMap by viewModel.settings.collectAsState()
    
    // No explicit initialization logic here; handled by ViewModel init block

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // GIF at the bottom layer
            val context = LocalContext.current
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()

            AsyncImage(
                model = R.raw.android_dancing,
                imageLoader = imageLoader,
                contentDescription = "Android Dancing",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 16.dp)
                    .align(Alignment.BottomCenter),
                alignment = Alignment.Center
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Strange Attractors",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .align(Alignment.CenterHorizontally)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(AttractorType.values()) { attractor ->
                    val hue = settingsMap[attractor.name]?.hue
                    val saturation = settingsMap[attractor.name]?.saturation
                    AttractorCard(
                        attractor = attractor, 
                        hue = hue,
                        saturation = saturation,
                        onClick = { onAttractorSelected(attractor) }
                    )
                }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttractorCardPreview() {
    RangerTestTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AttractorCard(
                attractor = AttractorType.LORENZ,
                hue = 120f,
                saturation = 0.8f,
                onClick = {}
            )
        }
    }
}
@Composable
fun AttractorCard(
    attractor: AttractorType,
    hue: Float?,
    saturation: Float?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview Animation (Non-interactive, fewer particles)
            StrangeParticleAnimation(
                attractor = attractor,
                isInteractive = false,
                particleCount = 2000, // Lightweight but visible
                modifier = Modifier.fillMaxSize(),
                overrideHue = hue,
                overrideSaturation = saturation
            )
            
            // Overlay gradient for text readability
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.Transparent)
            )

            // Label
            Text(
                text = attractor.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }
}
