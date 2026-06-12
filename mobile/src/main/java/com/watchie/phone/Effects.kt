package com.watchie.phone

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TAU = 6.2831855f
private const val PI_F = 3.1415927f

/** The animated background effects you can attach to a note. */
enum class Effect { NONE, FIREWORKS, CONFETTI, HEARTS, PETALS, SPARKLE, BUBBLES, SNOW, STARBURST }

/** The shapes a single particle can be drawn as. */
private enum class Shape { CIRCLE, RING, RECT, HEART, STAR, PETAL }

/** One moving particle. Plain mutable class (not snapshot state) for speed. */
private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val life: Float,
    val size: Float,
    val color: Color,
    val shape: Shape,
    var age: Float = 0f,
    var rot: Float = 0f,
    var vrot: Float = 0f,
    val sway: Float = 0f,
    val swayFreq: Float = 0f,
    val phase: Float = 0f,
    val twinkle: Boolean = false,
    val trail: Boolean = false,
    // Crackle: when age passes crackleAt, spawn crackleCount tiny children once.
    val crackleAt: Float = -1f,
    val crackleCount: Int = 0,
    val crackleColor: Color = Color.White,
    var crackled: Boolean = false,
)

/** Picks the right effect composable for a note. */
@Composable
fun EffectLayer(effect: Effect, accent: Color, modifier: Modifier = Modifier) {
    when (effect) {
        Effect.NONE -> Unit
        Effect.FIREWORKS -> FireworkEffect(FireworkType.RAINBOW, modifier)
        Effect.CONFETTI -> Confetti(modifier, accent)
        Effect.HEARTS -> Hearts(modifier, accent)
        Effect.PETALS -> Petals(modifier, accent)
        Effect.SPARKLE -> Sparkle(modifier, accent)
        Effect.BUBBLES -> Bubbles(modifier, accent)
        Effect.SNOW -> Snow(modifier, accent)
        Effect.STARBURST -> Starburst(modifier, accent)
    }
}

private fun brightPalette(accent: Color) = listOf(
    accent,
    Color(0xFFFFFFFF),
    Color(0xFFFFD166),
    Color(0xFF8AC6FF),
    Color(0xFFFF8FB1),
    Color(0xFF9DF7C4),
)

// ---------------------------------------------------------------------------
// The shared engine: a per-frame loop that emits, moves, and draws particles.
// ---------------------------------------------------------------------------

@Composable
private fun ParticleField(
    modifier: Modifier,
    gravity: Float,
    drag: Float = 0f,
    maxParticles: Int = 240,
    emit: (list: MutableList<Particle>, bounds: Size, dtSec: Float) -> Unit,
) {
    val particles = remember { ArrayList<Particle>() }
    var tick by remember { mutableStateOf(0L) }
    var bounds by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(Unit) {
        var prev = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (prev == 0L) 0.016f else ((now - prev) / 1_000_000_000f).coerceIn(0f, 0.05f)
                prev = now
                if (bounds.width > 0f) {
                    emit(particles, bounds, dt)
                    var i = 0
                    while (i < particles.size) {
                        val p = particles[i]
                        p.vy += gravity * dt
                        if (drag != 0f) {
                            val f = 1f - drag * dt
                            p.vx *= f
                            p.vy *= f
                        }
                        if (p.swayFreq != 0f) {
                            p.x += cos(p.age * p.swayFreq + p.phase) * p.sway * dt
                        }
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.rot += p.vrot * dt
                        p.age += dt
                        if (p.age >= p.life) particles.removeAt(i) else i++
                    }
                    while (particles.size > maxParticles) particles.removeAt(0)
                }
                tick = now
            }
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged {
            bounds = Size(it.width.toFloat(), it.height.toFloat())
        },
    ) {
        tick // subscribe so we redraw every frame
        for (p in particles) drawParticle(p)
    }
}

private fun DrawScope.drawParticle(p: Particle) {
    val linear = (1f - p.age / p.life).coerceIn(0f, 1f)
    val alpha = if (p.twinkle) sin((p.age / p.life).coerceIn(0f, 1f) * PI_F) else linear
    val color = p.color.copy(alpha = alpha.coerceIn(0f, 1f))
    val center = Offset(p.x, p.y)

    // A short streak behind fast particles (firework tails).
    if (p.trail) {
        drawLine(
            color = color.copy(alpha = alpha.coerceIn(0f, 1f) * 0.45f),
            start = Offset(p.x - p.vx * 0.05f, p.y - p.vy * 0.05f),
            end = center,
            strokeWidth = p.size * 1.1f,
        )
    }

    when (p.shape) {
        Shape.CIRCLE -> drawCircle(color, p.size, center)
        Shape.RING -> drawCircle(color, p.size, center, style = Stroke(width = 2f))
        Shape.RECT -> rotate(p.rot, center) {
            drawRect(
                color,
                topLeft = Offset(p.x - p.size, p.y - p.size * 0.6f),
                size = Size(p.size * 2f, p.size * 1.2f),
            )
        }
        Shape.HEART -> drawHeart(center, p.size, color)
        Shape.STAR -> drawSparkle(center, p.size, color)
        Shape.PETAL -> rotate(p.rot, center) {
            drawOval(
                color,
                topLeft = Offset(p.x - p.size * 0.5f, p.y - p.size),
                size = Size(p.size, p.size * 2f),
            )
        }
    }
}

private fun DrawScope.drawHeart(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val steps = 24
    for (i in 0..steps) {
        val t = i.toFloat() / steps * TAU
        val hx = 16f * sin(t) * sin(t) * sin(t)
        val hy = 13f * cos(t) - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)
        val px = center.x + hx / 16f * radius
        val py = center.y - hy / 16f * radius
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val inner = radius * 0.34f
    for (i in 0 until 8) {
        val r = if (i % 2 == 0) radius else inner
        val a = i / 8f * TAU
        val px = center.x + cos(a) * r
        val py = center.y + sin(a) * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

// ---------------------------------------------------------------------------
// Individual effects
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Fireworks: a family of distinct shell types, each its own animation.
// ---------------------------------------------------------------------------

/** The different firework shells you can send. */
enum class FireworkType(val label: String, val tagline: String) {
    PEONY("Peony", "Classic round burst"),
    CHRYSANTHEMUM("Chrysanthemum", "Long glittering tails"),
    WILLOW("Willow", "Drooping golden rain"),
    CRACKLE("Crackle", "Bursts, then crackles"),
    FIRECRACKERS("Firecrackers", "Rapid-fire pops"),
    STROBE("Strobe", "Flickering glitter"),
    RING("Ring", "A perfect expanding ring"),
    RAINBOW("Rainbow", "Every color at once"),
}

val fireworkTypes: List<FireworkType> = FireworkType.entries

private val SHELL_COLORS = listOf(
    Color(0xFFFF5D6C), Color(0xFFFFD166), Color(0xFF7CE0FF),
    Color(0xFFB388FF), Color(0xFF8DF7B0), Color(0xFFFF9EC4), Color(0xFFFFFFFF),
)

/** Plays a single firework shell type, looping, on a transparent background. */
@Composable
fun FireworkEffect(type: FireworkType, modifier: Modifier = Modifier) {
    when (type) {
        FireworkType.PEONY -> Peony(modifier)
        FireworkType.CHRYSANTHEMUM -> Chrysanthemum(modifier)
        FireworkType.WILLOW -> Willow(modifier)
        FireworkType.CRACKLE -> Crackle(modifier)
        FireworkType.FIRECRACKERS -> Firecrackers(modifier)
        FireworkType.STROBE -> Strobe(modifier)
        FireworkType.RING -> RingShell(modifier)
        FireworkType.RAINBOW -> Rainbow(modifier)
    }
}

/** Spawns one radial burst of particles centered at (cx, cy). */
private fun radialBurst(
    list: MutableList<Particle>,
    cx: Float,
    cy: Float,
    count: Int,
    speedMin: Float,
    speedMax: Float,
    life: Float,
    size: Float,
    trail: Boolean = false,
    twinkle: Boolean = false,
    jitter: Float = 0.15f,
    fixedSpeed: Boolean = false,
    crackleAt: Float = -1f,
    crackleCount: Int = 0,
    color: () -> Color,
) {
    for (k in 0 until count) {
        val a = k.toFloat() / count * TAU + Random.nextFloat() * jitter
        val sp = if (fixedSpeed) speedMax else speedMin + Random.nextFloat() * (speedMax - speedMin)
        list.add(
            Particle(
                cx, cy, cos(a) * sp, sin(a) * sp,
                life = life + Random.nextFloat() * 0.4f, size = size, color = color(),
                shape = Shape.CIRCLE, trail = trail, twinkle = twinkle,
                crackleAt = crackleAt, crackleCount = crackleCount,
            ),
        )
    }
}

private fun randomLaunchSite(bounds: Size): Offset = Offset(
    bounds.width * (0.22f + Random.nextFloat() * 0.56f),
    bounds.height * (0.20f + Random.nextFloat() * 0.38f),
)

@Composable
private fun Peony(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 110f, drag = 0.9f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.5f + Random.nextFloat() * 0.5f
            val site = randomLaunchSite(bounds)
            val color = SHELL_COLORS[Random.nextInt(SHELL_COLORS.size)]
            radialBurst(list, site.x, site.y, count = 28, speedMin = 60f, speedMax = 150f, life = 1.1f, size = 3f) { color }
        }
    }
}

@Composable
private fun Chrysanthemum(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 95f, drag = 0.55f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.6f + Random.nextFloat() * 0.5f
            val site = randomLaunchSite(bounds)
            val color = SHELL_COLORS[Random.nextInt(SHELL_COLORS.size)]
            radialBurst(list, site.x, site.y, count = 26, speedMin = 80f, speedMax = 160f, life = 1.5f, size = 3f, trail = true) { color }
        }
    }
}

@Composable
private fun Willow(modifier: Modifier) {
    val gold = Color(0xFFFFD166)
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 42f, drag = 0.22f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.9f + Random.nextFloat() * 0.5f
            val site = randomLaunchSite(bounds)
            radialBurst(list, site.x, site.y, count = 24, speedMin = 45f, speedMax = 110f, life = 2.4f, size = 2.6f, trail = true) { gold }
        }
    }
}

@Composable
private fun Crackle(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 110f, drag = 0.85f) { list, bounds, dt ->
        // Secondary "crackle": when a parent reaches crackleAt, spawn tiny sparks.
        val children = ArrayList<Particle>()
        for (p in list) {
            if (p.crackleAt >= 0f && !p.crackled && p.age >= p.crackleAt) {
                p.crackled = true
                repeat(p.crackleCount) {
                    val a = Random.nextFloat() * TAU
                    val sp = 20f + Random.nextFloat() * 50f
                    children.add(
                        Particle(
                            p.x, p.y, cos(a) * sp, sin(a) * sp,
                            life = 0.35f + Random.nextFloat() * 0.2f, size = 2f,
                            color = Color.White, shape = Shape.CIRCLE, twinkle = true,
                        ),
                    )
                }
            }
        }
        list.addAll(children)

        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.6f + Random.nextFloat() * 0.5f
            val site = randomLaunchSite(bounds)
            val color = SHELL_COLORS[Random.nextInt(SHELL_COLORS.size)]
            radialBurst(
                list, site.x, site.y, count = 20, speedMin = 60f, speedMax = 130f,
                life = 1.1f, size = 3f, crackleAt = 0.8f, crackleCount = 4,
            ) { color }
        }
    }
}

@Composable
private fun Firecrackers(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 70f, drag = 0.4f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.07f + Random.nextFloat() * 0.12f
            val cx = bounds.width * (0.1f + Random.nextFloat() * 0.8f)
            val cy = bounds.height * (0.25f + Random.nextFloat() * 0.55f)
            val white = if (Random.nextBoolean()) Color.White else Color(0xFFFFE39A)
            // a bright flash plus a few sparks
            list.add(Particle(cx, cy, 0f, 0f, life = 0.18f, size = 7f, color = Color.White, shape = Shape.CIRCLE))
            radialBurst(list, cx, cy, count = 7, speedMin = 30f, speedMax = 95f, life = 0.5f, size = 2.4f) { white }
        }
    }
}

@Composable
private fun Strobe(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 70f, drag = 0.6f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.55f + Random.nextFloat() * 0.4f
            val site = randomLaunchSite(bounds)
            radialBurst(list, site.x, site.y, count = 30, speedMin = 50f, speedMax = 140f, life = 1.3f, size = 3f, twinkle = true) {
                if (Random.nextBoolean()) Color.White else SHELL_COLORS[Random.nextInt(SHELL_COLORS.size)]
            }
        }
    }
}

@Composable
private fun RingShell(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 80f, drag = 0.7f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.7f + Random.nextFloat() * 0.5f
            val site = randomLaunchSite(bounds)
            val color = SHELL_COLORS[Random.nextInt(SHELL_COLORS.size)]
            // fixedSpeed + no jitter => a clean expanding ring.
            radialBurst(list, site.x, site.y, count = 42, speedMin = 120f, speedMax = 120f, life = 1.2f, size = 2.6f, jitter = 0f, fixedSpeed = true) { color }
        }
    }
}

@Composable
private fun Rainbow(modifier: Modifier) {
    val timer = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 115f, drag = 0.85f) { list, bounds, dt ->
        timer[0] -= dt
        if (timer[0] <= 0f) {
            timer[0] = 0.45f + Random.nextFloat() * 0.45f
            val site = randomLaunchSite(bounds)
            radialBurst(list, site.x, site.y, count = 32, speedMin = 55f, speedMax = 150f, life = 1.1f, size = 3f) {
                SHELL_COLORS[Random.nextInt(SHELL_COLORS.size)]
            }
        }
    }
}

@Composable
private fun Confetti(modifier: Modifier, accent: Color) {
    val palette = remember(accent) { brightPalette(accent) }
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 75f, drag = 0.04f) { list, bounds, dt ->
        acc[0] += dt * 34f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            list.add(
                Particle(
                    x = Random.nextFloat() * bounds.width, y = -8f,
                    vx = (Random.nextFloat() - 0.5f) * 70f, vy = 25f + Random.nextFloat() * 45f,
                    life = 3.6f, size = 4.5f + Random.nextFloat() * 4f,
                    color = palette[Random.nextInt(palette.size)], shape = Shape.RECT,
                    rot = Random.nextFloat() * 360f, vrot = (Random.nextFloat() - 0.5f) * 480f,
                    sway = 30f, swayFreq = 3f + Random.nextFloat() * 2f, phase = Random.nextFloat() * TAU,
                ),
            )
        }
    }
}

@Composable
private fun Hearts(modifier: Modifier, accent: Color) {
    val colors = remember(accent) { listOf(accent, Color(0xFFFF4D7E), Color(0xFFFF8FB1), Color(0xFFFFC2D6)) }
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 8f) { list, bounds, dt ->
        acc[0] += dt * 7f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            list.add(
                Particle(
                    x = bounds.width * (0.15f + Random.nextFloat() * 0.7f), y = bounds.height + 12f,
                    vx = 0f, vy = -(40f + Random.nextFloat() * 45f),
                    life = 3.2f, size = 7f + Random.nextFloat() * 7f,
                    color = colors[Random.nextInt(colors.size)], shape = Shape.HEART,
                    sway = 26f, swayFreq = 2.4f + Random.nextFloat() * 1.5f, phase = Random.nextFloat() * TAU,
                ),
            )
        }
    }
}

@Composable
private fun Petals(modifier: Modifier, accent: Color) {
    val colors = remember(accent) { listOf(Color(0xFFFFC2D6), Color(0xFFFFB3C8), Color(0xFFFFD7E5), accent) }
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 16f, drag = 0.05f) { list, bounds, dt ->
        acc[0] += dt * 12f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            list.add(
                Particle(
                    x = Random.nextFloat() * bounds.width, y = -10f,
                    vx = (Random.nextFloat() - 0.5f) * 20f, vy = 18f + Random.nextFloat() * 22f,
                    life = 5f, size = 5f + Random.nextFloat() * 4f,
                    color = colors[Random.nextInt(colors.size)], shape = Shape.PETAL,
                    rot = Random.nextFloat() * 360f, vrot = (Random.nextFloat() - 0.5f) * 220f,
                    sway = 60f, swayFreq = 2f + Random.nextFloat() * 1.5f, phase = Random.nextFloat() * TAU,
                ),
            )
        }
    }
}

@Composable
private fun Sparkle(modifier: Modifier, accent: Color) {
    val colors = remember(accent) { listOf(Color(0xFFFFFFFF), Color(0xFFFFE39A), Color(0xFFFFD166), accent) }
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 0f) { list, bounds, dt ->
        acc[0] += dt * 26f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            list.add(
                Particle(
                    x = Random.nextFloat() * bounds.width, y = Random.nextFloat() * bounds.height,
                    vx = 0f, vy = 0f,
                    life = 0.7f + Random.nextFloat() * 0.6f, size = 4f + Random.nextFloat() * 6f,
                    color = colors[Random.nextInt(colors.size)], shape = Shape.STAR,
                    vrot = 60f, twinkle = true,
                ),
            )
        }
    }
}

@Composable
private fun Bubbles(modifier: Modifier, accent: Color) {
    val colors = remember(accent) { listOf(Color(0xFFFFFFFF), Color(0xFFBFE3FF), accent) }
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = -26f, drag = 0.1f) { list, bounds, dt ->
        acc[0] += dt * 10f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            list.add(
                Particle(
                    x = Random.nextFloat() * bounds.width, y = bounds.height + 10f,
                    vx = 0f, vy = -(20f + Random.nextFloat() * 30f),
                    life = 3.5f, size = 4f + Random.nextFloat() * 9f,
                    color = colors[Random.nextInt(colors.size)], shape = Shape.RING,
                    sway = 22f, swayFreq = 2f + Random.nextFloat() * 1.5f, phase = Random.nextFloat() * TAU,
                ),
            )
        }
    }
}

@Composable
private fun Snow(modifier: Modifier, accent: Color) {
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 14f, drag = 0.02f) { list, bounds, dt ->
        acc[0] += dt * 16f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            list.add(
                Particle(
                    x = Random.nextFloat() * bounds.width, y = -8f,
                    vx = 0f, vy = 16f + Random.nextFloat() * 18f,
                    life = 6f, size = 2.5f + Random.nextFloat() * 3.5f,
                    color = Color(0xFFFFFFFF), shape = Shape.CIRCLE,
                    sway = 34f, swayFreq = 1.6f + Random.nextFloat() * 1.4f, phase = Random.nextFloat() * TAU,
                ),
            )
        }
    }
}

@Composable
private fun Starburst(modifier: Modifier, accent: Color) {
    val colors = remember(accent) { listOf(accent, Color(0xFFFFD166), Color(0xFFFFFFFF)) }
    val acc = remember { floatArrayOf(0f) }
    ParticleField(modifier, gravity = 0f, drag = 0.5f) { list, bounds, dt ->
        acc[0] += dt * 40f
        val cx = bounds.width / 2f
        val cy = bounds.height / 2f
        while (acc[0] >= 1f) {
            acc[0] -= 1f
            val a = Random.nextFloat() * TAU
            val sp = 90f + Random.nextFloat() * 70f
            list.add(
                Particle(
                    cx, cy, cos(a) * sp, sin(a) * sp,
                    life = 1.0f + Random.nextFloat() * 0.5f, size = 2.5f + Random.nextFloat() * 2f,
                    color = colors[Random.nextInt(colors.size)], shape = Shape.STAR,
                    vrot = 120f,
                ),
            )
        }
    }
}
