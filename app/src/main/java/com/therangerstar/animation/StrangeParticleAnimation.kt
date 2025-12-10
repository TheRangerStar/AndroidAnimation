package com.therangerstar.animation

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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.rememberCoroutineScope
import com.therangerstar.animation.data.AttractorDao
import com.therangerstar.animation.data.AttractorSetting
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    SPROTT_B("Sprott B Attractor")
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    initialHue: Float = 0f,
    onHueSelected: (Float) -> Unit,
    onHueConfirmed: (Float) -> Unit = {}
) {
    var selectedHue by remember { mutableFloatStateOf(initialHue) }
    
    // Update selectedHue when initialHue changes (e.g. loaded from DB)
    LaunchedEffect(initialHue) {
        selectedHue = initialHue
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 颜色预览
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = hsvToColor(selectedHue, 1f, 1f),
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
                        onDragEnd = { onHueConfirmed(selectedHue) }
                    ) { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = change.position - center
                        
                        // 计算 Hue (角度)
                        val angle = atan2(delta.y, delta.x) * (180 / Math.PI.toFloat())
                        val hue = (angle + 360) % 360
                        
                        selectedHue = hue
                        onHueSelected(hue)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                
                // 1. 绘制色相环 (Hue)
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
                
                // 3. 绘制选择器指示器
                val angleRad = selectedHue * (Math.PI / 180)
                // 固定在边缘内侧一点
                val selectorDist = radius * 0.8f
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
                    color = hsvToColor(selectedHue, 1f, 1f),
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
    dao: AttractorDao? = null,
    onBack: () -> Unit = {},
    overrideHue: Float? = null
) {
    // 状态管理
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    
    // 旋转角度 (X轴和Y轴旋转)
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    
    // 缩放比例 (用户手势控制)
    var userScale by remember { mutableStateOf(1f) }

    // 自定义设置
    var showSettings by remember { mutableStateOf(false) }
    var particleSize by remember { mutableFloatStateOf(2f) }
    // 初始化时颜色随机，只在首次组合时生成
    var colorHueOffset by remember { mutableFloatStateOf(Random.nextFloat() * 360f) }
    
    // 如果有外部传入的 overrideHue，则使用外部传入的值
    // 使用 LaunchedEffect 确保只在 overrideHue 变化时更新
    LaunchedEffect(overrideHue) {
        if (overrideHue != null) {
            colorHueOffset = overrideHue
        }
    }
    
    // 从数据库加载颜色设置
    LaunchedEffect(attractor) {
        if (dao != null) {
            val setting = dao.getSettingSync(attractor.name)
            if (setting != null) {
                colorHueOffset = setting.hue
            }
        }
    }
    
    // 粒子列表
    // 使用 remember(attractor) 确保切换时重新初始化
    val particles = remember(attractor, particleCount) {
        Array(particleCount) {
            spawnParticle(attractor)
        }
    }
    
    // 动画帧触发器
    var trigger by remember { mutableStateOf(0L) }
    
    // 动画循环
    LaunchedEffect(particles) {
        while (isActive) {
            withFrameNanos { 
                trigger = it
            }
            
            // 更新粒子位置
            // Halvorsen 吸引子对时间步长非常敏感，需要较小的 dt
            val dt = if (attractor == AttractorType.HALVORSEN) 0.005f else 0.015f
            
            for (p in particles) {
                updateParticle(p, attractor, dt)
                
                // 边界检查：防止粒子逃逸到无穷远或产生 NaN
                if (p.x.isNaN() || p.y.isNaN() || p.z.isNaN() || 
                    kotlin.math.abs(p.x) > 1000f || 
                    kotlin.math.abs(p.y) > 1000f || 
                    kotlin.math.abs(p.z) > 1000f) {
                    
                    // 重置粒子
                    val newP = spawnParticle(attractor)
                    p.x = newP.x
                    p.y = newP.y
                    p.z = newP.z
                }
            }
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
            .background(Color.Black)
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
            }
            
            // 最终缩放
            val scale = baseScale * userScale

            val cosX = cos(rotationX)
            val sinX = sin(rotationX)
            val cosY = cos(rotationY)
            val sinY = sin(rotationY)
            
            particles.forEach { p ->
                // 1. 旋转变换
                val x = p.x
                val y = p.y
                val z = p.z
                
                val y1 = y * cosX - z * sinX
                val z1 = y * sinX + z * cosX
                
                val x2 = x * cosY + z1 * sinY
                val z2 = -x * sinY + z1 * cosY
                val y2 = y1

                // 2. 投影
                val focalLength = 1000f
                val depth = z2 + 100f 
                val perspective = if (depth > 0) focalLength / (focalLength + z2) else 1f
                
                val screenX = x2 * scale + centerX
                val screenY = y2 * scale + centerY
                
                // 3. 颜色
                // 使用统一的颜色，基于用户选择的 hue
                val color = hsvToColor(colorHueOffset, 0.8f, 1f, 0.7f)

                drawCircle(
                    color = color,
                    radius = if(attractor == AttractorType.AIZAWA) particleSize * 0.75f else particleSize,
                    center = Offset(screenX, screenY)
                )
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // 颜色偏移控制 (Color Wheel)
                        Text(text = "Particle Color", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                        
                        ColorWheel(
                            initialHue = colorHueOffset,
                            onHueSelected = { colorHueOffset = it },
                            onHueConfirmed = { hue ->
                                scope.launch {
                                    dao?.insert(AttractorSetting(attractor.name, hue))
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { showSettings = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// 辅助函数：生成初始粒子
fun spawnParticle(type: AttractorType): Particle {
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
            x = Random.nextFloat() * 10 - 5,
            y = Random.nextFloat() * 10 - 5,
            z = Random.nextFloat() * 10 - 5,
            color = Color.White
        )
        AttractorType.SPROTT_B -> Particle(
            x = Random.nextFloat() * 2 - 1,
            y = Random.nextFloat() * 2 - 1,
            z = Random.nextFloat() * 2 - 1,
            color = Color.White
        )
    }
}

// 辅助函数：更新粒子位置
fun updateParticle(p: Particle, type: AttractorType, dt: Float) {
    var dx = 0f
    var dy = 0f
    var dz = 0f
    
    when(type) {
        AttractorType.LORENZ -> {
            val sigma = 10.0f
            val rho = 28.0f
            val beta = 8.0f / 3.0f
            dx = sigma * (p.y - p.x)
            dy = p.x * (rho - p.z) - p.y
            dz = p.x * p.y - beta * p.z
        }
        AttractorType.AIZAWA -> {
            val a = 0.95f
            val b = 0.7f
            val c = 0.6f
            val d = 3.5f
            val e = 0.25f
            val f = 0.1f
            
            dx = (p.z - b) * p.x - d * p.y
            dy = d * p.x + (p.z - b) * p.y
            dz = c + a * p.z - (p.z * p.z * p.z) / 3f - (p.x * p.x + p.y * p.y) * (1f + e * p.z) + f * p.z * p.x * p.x * p.x
        }
        AttractorType.HALVORSEN -> {
            val a = 1.4f // 经典值约 1.4 - 1.89
            dx = -a * p.x - 4 * p.y - 4 * p.z - p.y * p.y
            dy = -a * p.y - 4 * p.z - 4 * p.x - p.z * p.z
            dz = -a * p.z - 4 * p.x - 4 * p.y - p.x * p.x
        }
        AttractorType.SPROTT_B -> {
            val a = 0.4f
            val b = 1.2f
            dx = a * p.y * p.z
            dy = p.x - p.y
            dz = b - p.x * p.y
        }
    }
    
    p.x += dx * dt
    p.y += dy * dt
    p.z += dz * dt
}

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