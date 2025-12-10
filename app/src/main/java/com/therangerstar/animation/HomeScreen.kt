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
import coil.decode.ImageDecoderDecoder
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.therangerstar.animation.ui.theme.RangerTestTheme

@Composable
fun HomeScreen(
    dao: AttractorDao,
    onAttractorSelected: (AttractorType) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by dao.getAllSettings().collectAsState(initial = emptyList())
    val settingsMap = settings.associate { it.attractorName to it.hue }

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
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(AttractorType.values()) { attractor ->
                        val hue = settingsMap[attractor.name]
                        AttractorCard(
                            attractor = attractor, 
                            hue = hue,
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
fun HomeScreenPreview() {
    RangerTestTheme {
        HomeScreen(
            dao = object : AttractorDao {
                override fun getSetting(name: String): Flow<AttractorSetting?> {
                    return flowOf(null)
                }

                override suspend fun getSettingSync(name: String): AttractorSetting? {
                    return null
                }

                override fun getAllSettings(): Flow<List<AttractorSetting>> {
                    return flowOf(emptyList())
                }

                override suspend fun insert(setting: AttractorSetting) {
                    // No-op
                }
            },
            onAttractorSelected = {}
        )
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
                onClick = {}
            )
        }
    }
}
@Composable
fun AttractorCard(
    attractor: AttractorType,
    hue: Float?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview Animation (Non-interactive, fewer particles)
            StrangeParticleAnimation(
                attractor = attractor,
                isInteractive = false,
                particleCount = 2000, // Lightweight but visible
                modifier = Modifier.fillMaxSize(),
                overrideHue = hue
            )
            
            // Overlay gradient for text readability
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
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
