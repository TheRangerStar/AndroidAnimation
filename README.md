# TheRangerStar Animation

一个基于 Android Jetpack Compose 开发的高性能粒子动画展示应用，探索数学之美与混沌理论的视觉呈现。

## 🌟 项目简介 (Introduction)

本项目利用 Android Jetpack Compose 的 Canvas API 实现了高性能的 3D 粒子系统。通过数学公式模拟各种混沌吸引子（Strange Attractors）和几何形态，为用户提供流畅、可交互的视觉体验。

## ✨ 主要功能 (Features)

*   **高性能渲染**: 采用批量绘制 (Batch Rendering) 和视锥剔除 (Frustum Culling) 技术，支持数万个粒子流畅运行 (60FPS)。
*   **多样的数学模型**: 内置多种经典的混沌吸引子和几何结构。
*   **特殊渲染模式**: 支持加性混合 (Additive Blending) 实现发光效果，以及贝塞尔曲线 (Path) 实现波浪效果。
*   **完全交互式体验**: 支持单指拖动旋转 (3D Rotation) 和双指捏合缩放 (Pinch to Zoom)。
*   **高度个性化定制**: 用户可以实时调节粒子数量/水位高度、运动速度、粒子颜色 (HSV 色轮)、粒子大小等参数。
*   **数据持久化**: 自动保存每个动画的个性化设置，下次打开即恢复最佳状态。
*   **现代 UI 设计**: 沉浸式透明标题栏，简洁的设置面板。

## 🎨 包含的动画 (Animations)

目前应用包含以下数学模型和视觉效果：

1.  **Lorenz Attractor (洛伦兹吸引子)**
    *   经典的蝴蝶形状吸引子，混沌理论的代表作。
2.  **Aizawa Attractor (相泽吸引子)**
    *   具有管状结构的复杂吸引子，形态优美。
3.  **Halvorsen Attractor (哈尔沃森吸引子)**
    *   一种循环对称的混沌吸引子。
4.  **Sprott B Attractor**
    *   Sprott 收集的简单混沌系统之一。
5.  **Fibonacci Sphere (斐波那契球体)**
    *   基于斐波那契格点的均匀球面分布，展现数学的和谐之美。
6.  **Nebula Cloud (星云)**
    *   **新特性**: 模拟宇宙星系的螺旋结构。
    *   采用 **加性混合 (Additive Blending)** 渲染技术，模拟恒星核心的炽热发光效果。
    *   基于物理的引力与轨道模拟，展现动态的星系演化。
7.  **Water Ripple (水波纹)**
    *   **新特性**: 动态正弦波进度条动画。
    *   双层波浪叠加，模拟水的流动与深度感。
    *   支持通过滑块调节“水位高度”，可作为创意的进度展示组件。

## 🛠 技术栈 (Tech Stack)

*   **语言**: Kotlin
*   **UI 框架**: Jetpack Compose
*   **图形绘制**: 
    *   Android Canvas API (Native Canvas drawPoints)
    *   Path API (Cubic Bezier / Sine Waves)
    *   PorterDuff Xfermode (Blend Modes)
*   **架构**: MVVM (Model-View-ViewModel)
*   **本地存储**: Room Database
*   **异步处理**: Kotlin Coroutines

## 🚀 性能优化 (Performance)

为了解决大量粒子渲染带来的性能瓶颈，本项目实施了以下优化：
*   **Native Canvas Interop**: 使用 `canvas.nativeCanvas.drawPoints` 替代 Compose 的 `drawCircle`，大幅减少 GPU 绘制指令调用 (Draw Calls)。
*   **Memory Optimization**: 使用 `FloatArray` 替代对象数组存储粒子数据，避免大量对象创建导致的 GC 压力。
*   **Frustum Culling**: 简单的视锥剔除算法，仅绘制屏幕可见范围内的粒子。
*   **Pre-calculation**: 预先计算正弦/余弦值和部分数学变换。

---
Developed by TheRangerStar
