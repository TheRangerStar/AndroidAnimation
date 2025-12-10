package com.therangerstar.animation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 粒子数据类
class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var color: Color
)

// 吸引子类型枚举
enum class AttractorType(val title: String) {
    LORENZ("Lorenz Attractor"),
    AIZAWA("Aizawa Attractor"),
    HALVORSEN("Halvorsen Attractor"),
    SPROTT_B("Sprott B Attractor"),
    FIBONACCI_SPHERE("Fibonacci Sphere"),
    NEBULA("Nebula Cloud")
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    initialHue: Float = 0f,
    initialSaturation: Float = 1f,
    onColorSelected: (Float, Float) -> Unit,
    onColorConfirmed: (Float, Float) -> Unit = { _, _ -> }
) {
    var selectedHue by remember { mutableFloatStateOf(initialHue) }
    var selectedSaturation by remember { mutableFloatStateOf(initialSaturation) }
    
    // Update state when initials change (e.g. loaded from DB)
    LaunchedEffect(initialHue, initialSaturation) {
        selectedHue = initialHue
        selectedSaturation = initialSaturation
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 颜色预览
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = hsvToColor(selectedHue, selectedSaturation, 1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .padding(bottom = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onColorConfirmed(selectedHue, selectedSaturation) }
                    ) { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = change.position - center
                        val radius = size.width / 2f
                        
                        // 计算 Hue (角度)
                        val angle = atan2(delta.y, delta.x) * (180 / Math.PI.toFloat())
                        val hue = (angle + 360) % 360
                        
                        // Calculate Saturation (Distance from center)
                        val distance = kotlin.math.sqrt(delta.x * delta.x + delta.y * delta.y)
                        val saturation = (distance / radius).coerceIn(0f, 1f)

                        selectedHue = hue
                        selectedSaturation = saturation
                        onColorSelected(hue, saturation)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                
                // 1. 绘制色相环 (Hue) + Saturation Gradient
                // We draw a sweep gradient for Hue, and override with a radial gradient for saturation
                // But a simple ColorWheel usually just changes hue.
                // To support "inner colors", we usually mix white in the center.
                
                // Draw Hue Sweep
                val sweepGradient = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red, Color.Magenta, Color.Blue, Color.Cyan,
                        Color.Green, Color.Yellow, Color.Red
                    ),
                    center = center
                )
                
                drawCircle(
                    brush = sweepGradient,
                    radius = radius,
                    center = center
                )
                
                // Draw Saturation Radial Gradient (White at center -> Transparent at edge)
                val radialGradient = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                )
                 drawCircle(
                    brush = radialGradient,
                    radius = radius,
                    center = center
                )
                
                // 3. 绘制选择器指示器
                val angleRad = selectedHue * (Math.PI / 180)
                // Distance based on saturation
                val selectorDist = radius * selectedSaturation
                val selectorX = center.x + selectorDist * cos(angleRad).toFloat()
                val selectorY = center.y + selectorDist * sin(angleRad).toFloat()
                
                // 指示器外圈 (黑)
                drawCircle(
                    color = Color.Black,
                    radius = 10.dp.toPx(),
                    center = Offset(selectorX, selectorY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
                // 指示器内圈 (当前颜色)
                drawCircle(
                    color = hsvToColor(selectedHue, selectedSaturation, 1f),
                    radius = 8.dp.toPx(),
                    center = Offset(selectorX, selectorY)
                )
            }
        }
    }
}

@Composable
fun StrangeParticleAnimation(
    attractor: AttractorType,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isInteractive: Boolean = true,
    particleCount: Int = 8000,
    onBack: () -> Unit = {},
    overrideHue: Float? = null,
    overrideSaturation: Float? = null,
    overrideSpeed: Float? = null,
    overrideParticleCount: Int? = null,
    onSaveColor: (Float, Float) -> Unit = { _, _ -> },
    onSaveSettings: (Float, Float, Float, Int) -> Unit = { _, _, _, _ -> }
) {
    // 状态管理
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    
    // 旋转角度 (X轴和Y轴旋转)
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    
    // 缩放比例 (用户手势控制)
    var userScale by remember { mutableFloatStateOf(1f) }

    // 自定义设置
    var showSettings by remember { mutableStateOf(false) }
    var particleSize by remember { mutableFloatStateOf(2f) }
    
    // Settings state
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var currentParticleCount by remember { mutableStateOf(particleCount) }
    
    // 初始化时颜色随机，只在首次组合时生成
    var colorHueOffset by remember { mutableFloatStateOf(Random.nextFloat() * 360f) }
    // Saturation state for particle color
    var colorSaturation by remember { mutableFloatStateOf(0.8f) }
    
    // Apply overrides if provided
    LaunchedEffect(overrideHue, overrideSaturation, overrideSpeed, overrideParticleCount) {
        if (overrideHue != null) {
            colorHueOffset = overrideHue
        }
        if (overrideSaturation != null) {
            colorSaturation = overrideSaturation
        }
        if (overrideSpeed != null) {
            speedMultiplier = overrideSpeed
        }
        if (overrideParticleCount != null) {
            currentParticleCount = overrideParticleCount
        }
    }
    
    // 粒子列表
    // 使用 remember(attractor, currentParticleCount) 确保切换时重新初始化
    // 优化：使用 FloatArray 而不是 Array<Particle> 对象，减少对象开销
    val particleData = remember(attractor, currentParticleCount) {
        val data = FloatArray(currentParticleCount * 4) // x, y, z, unused
        for (i in 0 until currentParticleCount) {
            val p = spawnParticle(attractor, i, currentParticleCount)
            data[i * 4] = p.x
            data[i * 4 + 1] = p.y
            data[i * 4 + 2] = p.z
        }
        data
    }
    
    // 屏幕坐标缓存，避免每帧分配
    val screenPoints = remember(currentParticleCount) { FloatArray(currentParticleCount * 2) }
    
    // 动画帧触发器
    var trigger by remember { mutableStateOf(0L) }
    
    // 动画循环
    LaunchedEffect(particleData, speedMultiplier) {
        while (isActive) {
            withFrameNanos { 
                trigger = it
            }
            
            // 更新粒子位置
            // Halvorsen 吸引子对时间步长非常敏感，需要较小的 dt
            // 应用速度倍率
            val baseDt = if (attractor == AttractorType.HALVORSEN) 0.005f else 0.015f
            val dt = baseDt * speedMultiplier
            
            // 批量更新，避免对象创建
            updateParticles(particleData, currentParticleCount, attractor, dt)
        }
    }

    // 手势修改器
    val gesturesModifier = if (isInteractive) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        userScale = 1f
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.filter { it.pressed }
                        
                        if (pointers.size == 1) {
                            // 单指拖动 -> 旋转
                            val change = pointers.first()
                            val dragAmount = change.position - change.previousPosition
                            
                            if (dragAmount != Offset.Zero) {
                                rotationY += dragAmount.x * 0.01f
                                rotationX -= dragAmount.y * 0.01f
                            }
                            change.consume()
                        } else if (pointers.size == 2) {
                            // 双指 -> 缩放
                            val change1 = pointers[0]
                            val change2 = pointers[1]
                            
                            val p1 = change1.position
                            val p2 = change2.position
                            val prevP1 = change1.previousPosition
                            val prevP2 = change2.previousPosition
                            
                            val currentDist = (p1 - p2).getDistance()
                            val prevDist = (prevP1 - prevP2).getDistance()
                            
                            if (prevDist > 0f) {
                                val zoom = currentDist / prevDist
                                userScale *= zoom
                                userScale = userScale.coerceIn(0.5f, 5f)
                            }
                            
                            change1.consume()
                            change2.consume()
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .then(gesturesModifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 触发重绘
            trigger

            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            // 根据不同吸引子调整缩放比例
            val baseScale = when(attractor) {
                AttractorType.LORENZ -> 12f
                AttractorType.AIZAWA -> 180f
                AttractorType.HALVORSEN -> 15f
                AttractorType.SPROTT_B -> 60f
                AttractorType.FIBONACCI_SPHERE -> 2.5f
                AttractorType.NEBULA -> 2.0f
            }
            
            // 最终缩放
            val scale = baseScale * userScale

            val cosX = cos(rotationX)
            val sinX = sin(rotationX)
            val cosY = cos(rotationY)
            val sinY = sin(rotationY)
            
            // 3. 颜色
            // 使用统一的颜色，基于用户选择的 hue 和 saturation
            val color = hsvToColor(colorHueOffset, colorSaturation, 1f, 0.7f)
            
            // 优化：直接在 Canvas 循环中处理变换，不创建中间对象
            var validPointsCount = 0
            val pointSize = if(attractor == AttractorType.AIZAWA) particleSize * 0.75f else particleSize
            
            for (i in 0 until currentParticleCount) {
                val idx = i * 4
                val x = particleData[idx]
                val y = particleData[idx + 1]
                val z = particleData[idx + 2]
                
                // 1. 旋转变换
                val y1 = y * cosX - z * sinX
                val z1 = y * sinX + z * cosX
                
                val x2 = x * cosY + z1 * sinY
                val z2 = -x * sinY + z1 * cosY
                val y2 = y1

                // 2. 投影
                val focalLength = 1000f
                val depth = z2 + 100f 
                // val perspective = if (depth > 0) focalLength / (focalLength + z2) else 1f
                
                val screenX = x2 * scale + centerX
                val screenY = y2 * scale + centerY
                
                // 简单的视锥剔除
                if (screenX >= 0 && screenX <= size.width && screenY >= 0 && screenY <= size.height) {
                    screenPoints[validPointsCount * 2] = screenX
                    screenPoints[validPointsCount * 2 + 1] = screenY
                    validPointsCount++
                }
            }
            
            // 使用 nativeCanvas 批量绘制点，大幅提升性能并减少 GPU 命令数量
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    this.color = color.toArgb()
                    this.strokeWidth = pointSize * 2 // Stroke width is diameter
                    this.strokeCap = Paint.Cap.ROUND
                    this.isAntiAlias = true
                }
                canvas.nativeCanvas.drawPoints(screenPoints, 0, validPointsCount * 2, paint)
            }
        }
        
        // UI 覆盖层：仅在交互模式下显示
        if (isInteractive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                // 顶部工具栏 (返回键、标题、设置)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    // 标题
                    Text(
                        text = attractor.title,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    // 设置按钮
                    IconButton(
                        onClick = { showSettings = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
                
                Text(
                    text = "Drag to Rotate • Pinch to Zoom",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        }

        // 设置对话框
        if (showSettings && isInteractive) {
            Dialog(onDismissRequest = { showSettings = false }) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Settings",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 粒子大小控制
                        Text(text = "Particle Size: ${String.format("%.1f", particleSize)}", color = Color.White)
                        Slider(
                            value = particleSize,
                            onValueChange = { particleSize = it },
                            valueRange = 0.5f..5f,
                            steps = 9
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 粒子速度控制
                        Text(text = "Speed: ${String.format("%.1fx", speedMultiplier)}", color = Color.White)
                        Slider(
                            value = speedMultiplier,
                            onValueChange = { speedMultiplier = it },
                            valueRange = 0.1f..3.0f,
                            steps = 29
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Particle Count Control
                        Text(text = "Count: $currentParticleCount", color = Color.White)
                        Slider(
                            value = currentParticleCount.toFloat(),
                            onValueChange = { currentParticleCount = it.toInt() },
                            valueRange = 1000f..20000f,
                            steps = 19
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 颜色偏移控制 (Color Wheel)
                        Text(text = "Particle Color", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                        
                        ColorWheel(
                            initialHue = colorHueOffset,
                            initialSaturation = colorSaturation,
                            onColorSelected = { hue, saturation -> 
                                colorHueOffset = hue
                                colorSaturation = saturation
                            },
                            onColorConfirmed = { hue, saturation ->
                                onSaveColor(hue, saturation)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { 
                            showSettings = false 
                            onSaveSettings(colorHueOffset, colorSaturation, speedMultiplier, currentParticleCount)
                        }) {
                            Text("Save & Close")
                        }
                    }
                }
            }
        }
    }
}

// 优化后的批量更新函数
fun updateParticles(data: FloatArray, count: Int, type: AttractorType, dt: Float) {
    for (i in 0 until count) {
        val idx = i * 4
        val x = data[idx]
        val y = data[idx + 1]
        val z = data[idx + 2]
        
        var dx = 0f
        var dy = 0f
        var dz = 0f
        
        when(type) {
            AttractorType.LORENZ -> {
                val sigma = 10.0f
                val rho = 28.0f
                val beta = 8.0f / 3.0f
                dx = sigma * (y - x)
                dy = x * (rho - z) - y
                dz = x * y - beta * z
            }
            AttractorType.AIZAWA -> {
                val a = 0.95f
                val b = 0.7f
                val c = 0.6f
                val d = 3.5f
                val e = 0.25f
                val f = 0.1f
                
                dx = (z - b) * x - d * y
                dy = d * x + (z - b) * y
                dz = c + a * z - (z * z * z) / 3f - (x * x + y * y) * (1f + e * z) + f * z * x * x * x
            }
            AttractorType.HALVORSEN -> {
                val a = 1.4f 
                dx = -a * x - 4 * y - 4 * z - y * y
                dy = -a * y - 4 * z - 4 * x - z * z
                dz = -a * z - 4 * x - 4 * y - x * x
            }
            AttractorType.SPROTT_B -> {
                val a = 0.4f
                val b = 1.2f
                dx = a * y * z
                dy = x - y
                dz = b - x * y
            }
            AttractorType.FIBONACCI_SPHERE -> {
                // Fibonacci Sphere is a static shape usually, but we can make it rotate
                // or have a flow on the surface.
                // For now, let's just make it rotate slowly around Y axis
                val speed = 0.5f // Intrinsic rotation speed
                dx = -z * speed
                dy = 0f
                dz = x * speed
            }
            AttractorType.NEBULA -> {
                // Nebula cloud effect: Particles rotate around the center with noise
                val dist = kotlin.math.sqrt(x*x + z*z)
                val angleSpeed = 100f / (dist + 10f) // Slower at outer edges
                
                dx = -z * angleSpeed
                dy = (kotlin.math.sin(dist * 0.1f + x * 0.05f) - y) * 0.5f // Flatten to disk with waves
                dz = x * angleSpeed
            }
        }
        
        var nx = x + dx * dt
        var ny = y + dy * dt
        var nz = z + dz * dt
        
        // 边界检查
        if (nx.isNaN() || ny.isNaN() || nz.isNaN() || 
            kotlin.math.abs(nx) > 1000f || 
            kotlin.math.abs(ny) > 1000f || 
            kotlin.math.abs(nz) > 1000f) {
            
            // Respawn logic (simplified for FloatArray)
            val p = spawnParticle(type, i, count)
            nx = p.x
            ny = p.y
            nz = p.z
        }
        
        data[idx] = nx
        data[idx + 1] = ny
        data[idx + 2] = nz
    }
}

// 辅助函数：生成初始粒子
fun spawnParticle(type: AttractorType, index: Int, total: Int): Particle {
    return when(type) {
        AttractorType.LORENZ -> Particle(
            x = Random.nextFloat() * 20 - 10,
            y = Random.nextFloat() * 20 - 10,
            z = Random.nextFloat() * 20 + 10,
            color = Color.White
        )
        AttractorType.AIZAWA -> Particle(
            x = Random.nextFloat() * 2 - 1,
            y = Random.nextFloat() * 2 - 1,
            z = Random.nextFloat() * 2 - 1,
            color = Color.White
        )
        AttractorType.HALVORSEN -> Particle(
             x = Random.nextFloat() * 4 - 2,
             y = Random.nextFloat() * 4 - 2,
             z = Random.nextFloat() * 4 - 2,
             color = Color.White
        )
        AttractorType.SPROTT_B -> Particle(
            x = Random.nextFloat() * 2 - 1,
            y = Random.nextFloat() * 2 - 1,
            z = Random.nextFloat() * 2 - 1,
            color = Color.White
        )
        AttractorType.FIBONACCI_SPHERE -> {
            val samples = total
            val phi = Math.PI * (3.0 - kotlin.math.sqrt(5.0)) // golden angle in radians

            val y = 1 - (index / (samples - 1).toFloat()) * 2  // y goes from 1 to -1
            val radius = kotlin.math.sqrt(1 - y * y)  // radius at y

            val theta = phi * index  // golden angle increment

            val x = kotlin.math.cos(theta) * radius
            val z = kotlin.math.sin(theta) * radius

            val scale = 150f // Scale up for visibility
            
            Particle(
                x = x.toFloat() * scale,
                y = y * scale,
                z = z.toFloat() * scale,
                color = Color.White
            )
        }
        AttractorType.NEBULA -> {
            // Spiral galaxy distribution
            val angle = Random.nextFloat() * 360f
            val distance = Random.nextFloat() * 100f + 10f // Avoid center
            val height = (Random.nextFloat() - 0.5f) * 20f
            
            val rad = angle * (Math.PI / 180f)
            val x = kotlin.math.cos(rad) * distance
            val z = kotlin.math.sin(rad) * distance
            
            Particle(
                x = x.toFloat(),
                y = height,
                z = z.toFloat(),
                color = Color.White
            )
        }
    }
}

// 辅助函数：更新粒子位置 (已废弃，使用批量更新函数 updateParticles)

// 简单的 HSV 转 Color
fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    val h = hue % 360
    val c = value * saturation
    val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
    val m = value - c
    
    var r = 0f
    var g = 0f
    var b = 0f
    
    when {
        h < 60 -> { r = c; g = x; b = 0f }
        h < 120 -> { r = x; g = c; b = 0f }
        h < 180 -> { r = 0f; g = c; b = x }
        h < 240 -> { r = 0f; g = x; b = c }
        h < 300 -> { r = x; g = 0f; b = c }
        else -> { r = c; g = 0f; b = x }
    }
    
    return Color(r + m, g + m, b + m, alpha)
}