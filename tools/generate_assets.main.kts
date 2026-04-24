#!/usr/bin/env kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.GsonBuilder
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

val gson = GsonBuilder().setPrettyPrinting().create()

data class EnemyCfg(val type: String, val count: Int, val speed: Float)
data class LevelCfg(
    val id: Int,
    val background: String,
    val timeSeconds: Int,
    val targetPercent: Int,
    val enemies: List<EnemyCfg>,
    val powerups: List<String>
)

fun levelConfig(id: Int): LevelCfg {
    val bg = "backgrounds/bg_%02d.png".format(id)
    return when (id) {
        in 1..10 -> LevelCfg(id, bg, 90, 75,
            listOf(EnemyCfg("spider", 1, 70f + id * 2)),
            listOf("time", "freeze"))
        in 11..20 -> LevelCfg(id, bg, 75, 75,
            listOf(EnemyCfg("spider", 2, 80f), EnemyCfg("cockroach", 1, 90f + (id - 10) * 3)),
            listOf("time", "freeze", "shield"))
        in 21..30 -> LevelCfg(id, bg, 60, 80,
            listOf(EnemyCfg("spider", 2, 90f), EnemyCfg("cockroach", 2, 100f), EnemyCfg("wasp", 1, 110f + (id - 20) * 2)),
            listOf("freeze", "speed", "shield"))
        in 31..40 -> LevelCfg(id, bg, 50, 80,
            listOf(EnemyCfg("spider", 2, 100f), EnemyCfg("cockroach", 2, 110f), EnemyCfg("wasp", 1, 120f), EnemyCfg("snail", 1, 40f)),
            listOf("time", "freeze", "speed", "shield"))
        else -> LevelCfg(id, bg, 40, 85,
            listOf(EnemyCfg("spider", 3, 110f), EnemyCfg("cockroach", 2, 120f), EnemyCfg("wasp", 2, 130f), EnemyCfg("snail", 2, 40f)),
            listOf("time", "freeze", "speed", "shield"))
    }
}

// Generate JSON files
val levelsDir = File("android/assets/levels")
levelsDir.mkdirs()
for (i in 1..50) {
    val cfg = levelConfig(i)
    val file = File(levelsDir, "level_%02d.json".format(i))
    file.writeText(gson.toJson(cfg))
    println("Generated ${file.name}")
}

// Generate placeholder PNG backgrounds
val bgDir = File("android/assets/backgrounds")
bgDir.mkdirs()
for (i in 1..50) {
    val file = File(bgDir, "bg_%02d.png".format(i))
    if (file.exists()) { println("Skipping ${file.name} (exists)"); continue }

    val img = BufferedImage(480, 800, BufferedImage.TYPE_INT_RGB)
    val g2 = img.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.color = Color(0x55, 0x55, 0x55)
    g2.fillRect(0, 0, 480, 800)
    g2.color = Color(0xFF, 0xFF, 0xFF)
    g2.font = Font("SansSerif", Font.BOLD, 36)
    val text = "LEVEL $i"
    val fm = g2.fontMetrics
    g2.drawString(text, (480 - fm.stringWidth(text)) / 2, 380)
    g2.font = Font("SansSerif", Font.PLAIN, 20)
    val sub = "PLACEHOLDER"
    g2.drawString(sub, (480 - fm.stringWidth(sub)) / 2, 430)
    g2.dispose()
    ImageIO.write(img, "PNG", file)
    println("Generated ${file.name}")
}

println("Done. 50 levels + 50 placeholder backgrounds generated.")
